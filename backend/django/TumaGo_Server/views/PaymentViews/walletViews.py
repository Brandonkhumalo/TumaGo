from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from paynow import Paynow
from django.conf import settings
from django.utils import timezone
from django.db import transaction as db_transaction
from decimal import Decimal, InvalidOperation
import logging

from ...models import (
    CustomUser, DriverWallet, WalletTransaction,
    DriverBalance, Payment, CommissionRefundRequest,
)

logger = logging.getLogger(__name__)

# Reuse the same Paynow client from paymentViews
paynow = Paynow(
    settings.PAYNOW_INTEGRATION_ID,
    settings.PAYNOW_INTEGRATION_KEY,
    settings.PAYNOW_RETURN_URL,
    settings.PAYNOW_RESULT_URL,
)

MOBILE_METHODS = {'ecocash', 'onemoney'}


def _ensure_driver(user):
    """Return error Response if user is not a driver, else None."""
    if user.role != CustomUser.DRIVER:
        return Response({"error": "Only drivers can access wallet"},
                        status=status.HTTP_403_FORBIDDEN)
    return None


# --------------------------------------------------------------------------
# Top-up: initiate Paynow payment to add funds to wallet
# --------------------------------------------------------------------------
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def wallet_topup(request):
    """
    Start a Paynow payment to top up the driver's wallet.

    Body: { "amount": 5.00, "payment_method": "ecocash"|"onemoney"|"card",
            "phone": "0771234567" (required for ecocash/onemoney) }
    """
    err = _ensure_driver(request.user)
    if err:
        return err

    amount = request.data.get("amount")
    payment_method = request.data.get("payment_method", "").lower()
    phone = request.data.get("phone", "")

    if not amount:
        return Response({"error": "amount is required"}, status=status.HTTP_400_BAD_REQUEST)

    try:
        amount = Decimal(str(amount))
        if amount < Decimal('5.00') or amount > Decimal('100.00'):
            raise ValueError
    except (ValueError, TypeError, InvalidOperation):
        return Response({"error": "Amount must be between $5.00 and $100.00"},
                        status=status.HTTP_400_BAD_REQUEST)

    if payment_method not in ('card', 'ecocash', 'onemoney'):
        return Response({"error": "payment_method must be card, ecocash, or onemoney"},
                        status=status.HTTP_400_BAD_REQUEST)

    if payment_method in MOBILE_METHODS and not phone:
        return Response({"error": "phone is required for EcoCash/OneMoney"},
                        status=status.HTTP_400_BAD_REQUEST)

    # Create Payment record (reuse existing model)
    payment_record = Payment.objects.create(
        client=request.user,
        amount=amount,
        payment_method=payment_method,
    )

    reference = f"TumaGo-Wallet-{payment_record.id}"
    paynow_payment = paynow.create_payment(reference, request.user.email)
    paynow_payment.add("Wallet top-up", float(amount))

    try:
        if payment_method in MOBILE_METHODS:
            response = paynow.send_mobile(paynow_payment, phone, payment_method)
        else:
            response = paynow.send(paynow_payment)

        if response.success:
            payment_record.paynow_reference = reference
            payment_record.poll_url = response.poll_url
            payment_record.redirect_url = getattr(response, 'redirect_url', '') or ''
            payment_record.save()

            result = {"payment_id": str(payment_record.id), "poll_url": response.poll_url}
            if payment_method == 'card':
                result["redirect_url"] = response.redirect_url
                result["instructions"] = "Open the redirect URL to complete card payment."
            else:
                result["instructions"] = f"A payment prompt has been sent to {phone}."

            return Response(result, status=status.HTTP_201_CREATED)
        else:
            payment_record.status = Payment.FAILED
            payment_record.save()
            return Response({"error": f"Payment gateway error: {response.error}"},
                            status=status.HTTP_502_BAD_GATEWAY)
    except Exception as e:
        payment_record.status = Payment.FAILED
        payment_record.save()
        logger.exception(f"Paynow wallet topup exception: {e}")
        return Response({"error": "Failed to connect to payment gateway"},
                        status=status.HTTP_502_BAD_GATEWAY)


# --------------------------------------------------------------------------
# Top-up status: poll Paynow and credit wallet on success
# --------------------------------------------------------------------------
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def wallet_topup_status(request):
    """
    Poll wallet top-up payment status. Credits wallet when paid.

    Query: ?payment_id=<uuid>
    """
    err = _ensure_driver(request.user)
    if err:
        return err

    payment_id = request.query_params.get("payment_id")
    if not payment_id:
        return Response({"error": "payment_id is required"}, status=status.HTTP_400_BAD_REQUEST)

    try:
        payment_record = Payment.objects.get(id=payment_id, client=request.user)
    except Payment.DoesNotExist:
        return Response({"error": "Payment not found"}, status=status.HTTP_404_NOT_FOUND)

    # Already resolved
    if payment_record.status in (Payment.PAID, Payment.FAILED, Payment.CANCELLED):
        wallet, _ = DriverWallet.objects.get_or_create(driver=request.user)
        return Response({
            "payment_id": str(payment_record.id),
            "status": payment_record.status,
            "paid": payment_record.status == Payment.PAID,
            "wallet_balance": str(wallet.balance),
        })

    # Poll Paynow
    if payment_record.poll_url:
        try:
            poll_response = paynow.check_transaction_status(payment_record.poll_url)
            if poll_response.paid:
                payment_record.status = Payment.PAID
                payment_record.paid_at = timezone.now()
                payment_record.save()

                # Credit wallet atomically
                with db_transaction.atomic():
                    wallet, _ = DriverWallet.objects.select_for_update().get_or_create(
                        driver=request.user
                    )
                    wallet.balance += payment_record.amount
                    wallet.save()

                    WalletTransaction.objects.create(
                        driver=request.user,
                        transaction_type=WalletTransaction.TOPUP,
                        amount=payment_record.amount,
                        balance_after=wallet.balance,
                        description=f"Top-up via {payment_record.payment_method}",
                    )

                logger.info(f"Wallet topped up: {request.user.email} +${payment_record.amount}")

            elif poll_response.status and poll_response.status.lower() in ('failed', 'cancelled'):
                payment_record.status = Payment.FAILED
                payment_record.save()
        except Exception as e:
            logger.warning(f"Paynow poll failed for wallet topup {payment_id}: {e}")

    wallet, _ = DriverWallet.objects.get_or_create(driver=request.user)
    return Response({
        "payment_id": str(payment_record.id),
        "status": payment_record.status,
        "paid": payment_record.status == Payment.PAID,
        "wallet_balance": str(wallet.balance),
    })


# --------------------------------------------------------------------------
# Balance: return both wallet and owed balances
# --------------------------------------------------------------------------
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def wallet_balance(request):
    """Return driver's wallet balance and owed balance."""
    err = _ensure_driver(request.user)
    if err:
        return err

    wallet, _ = DriverWallet.objects.get_or_create(driver=request.user)
    owed, _ = DriverBalance.objects.get_or_create(driver=request.user)

    return Response({
        "wallet_balance": str(wallet.balance),
        "owed_balance": str(owed.owed_amount),
        "total_paid": str(owed.total_paid),
        "last_paid_at": str(owed.last_paid_at) if owed.last_paid_at else None,
    })


# --------------------------------------------------------------------------
# Transfer: move money between wallet and owed balance
# --------------------------------------------------------------------------
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def wallet_transfer(request):
    """
    Transfer between wallet and owed balance.

    Body: { "direction": "wallet_to_owed" | "owed_to_wallet", "amount": 5.00 }
    """
    err = _ensure_driver(request.user)
    if err:
        return err

    direction = request.data.get("direction", "")
    amount = request.data.get("amount")

    if direction not in ('wallet_to_owed', 'owed_to_wallet'):
        return Response({"error": "direction must be wallet_to_owed or owed_to_wallet"},
                        status=status.HTTP_400_BAD_REQUEST)

    try:
        amount = Decimal(str(amount))
        if amount <= 0:
            raise ValueError
    except (ValueError, TypeError, InvalidOperation):
        return Response({"error": "amount must be a positive number"},
                        status=status.HTTP_400_BAD_REQUEST)

    with db_transaction.atomic():
        wallet, _ = DriverWallet.objects.select_for_update().get_or_create(driver=request.user)
        owed, _ = DriverBalance.objects.select_for_update().get_or_create(driver=request.user)

        if direction == 'wallet_to_owed':
            if wallet.balance < amount:
                return Response({"error": "Insufficient wallet balance"},
                                status=status.HTTP_400_BAD_REQUEST)
            wallet.balance -= amount
            owed.owed_amount += amount
            txn_type = WalletTransaction.TRANSFER_OUT
            desc = f"Transfer ${amount} from wallet to owed balance"
        else:
            if owed.owed_amount < amount:
                return Response({"error": "Insufficient owed balance"},
                                status=status.HTTP_400_BAD_REQUEST)
            owed.owed_amount -= amount
            wallet.balance += amount
            txn_type = WalletTransaction.TRANSFER_IN
            desc = f"Transfer ${amount} from owed balance to wallet"

        wallet.save()
        owed.save()

        WalletTransaction.objects.create(
            driver=request.user,
            transaction_type=txn_type,
            amount=amount if txn_type == WalletTransaction.TRANSFER_IN else -amount,
            balance_after=wallet.balance,
            description=desc,
        )

    return Response({
        "message": desc,
        "wallet_balance": str(wallet.balance),
        "owed_balance": str(owed.owed_amount),
    })


# --------------------------------------------------------------------------
# Transactions: paginated wallet transaction history
# --------------------------------------------------------------------------
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def wallet_transactions(request):
    """Return paginated wallet transaction history."""
    err = _ensure_driver(request.user)
    if err:
        return err

    page_size = 20
    offset = int(request.query_params.get("offset", 0))

    txns = WalletTransaction.objects.filter(
        driver=request.user
    ).order_by('-created_at')[offset:offset + page_size]

    data = [{
        "id": str(t.id),
        "type": t.transaction_type,
        "amount": str(t.amount),
        "balance_after": str(t.balance_after),
        "description": t.description,
        "delivery_id": str(t.delivery_id) if t.delivery_id else None,
        "created_at": t.created_at.isoformat(),
    } for t in txns]

    return Response({
        "transactions": data,
        "offset": offset,
        "page_size": page_size,
    })


# --------------------------------------------------------------------------
# Refund requests: driver views their own requests
# --------------------------------------------------------------------------
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def wallet_refund_requests(request):
    """Return driver's commission refund requests."""
    err = _ensure_driver(request.user)
    if err:
        return err

    requests_qs = CommissionRefundRequest.objects.filter(
        driver=request.user
    ).order_by('-created_at')[:50]

    data = [{
        "id": str(r.id),
        "delivery_id": str(r.delivery_id),
        "amount": str(r.amount),
        "reason": r.reason,
        "status": r.status,
        "admin_notes": r.admin_notes,
        "created_at": r.created_at.isoformat(),
        "reviewed_at": r.reviewed_at.isoformat() if r.reviewed_at else None,
    } for r in requests_qs]

    return Response({"refund_requests": data})
