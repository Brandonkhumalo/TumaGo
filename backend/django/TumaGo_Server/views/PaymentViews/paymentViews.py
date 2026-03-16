from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.response import Response
from rest_framework import status
from paynow import Paynow
from django.conf import settings
from django.utils import timezone
from decimal import Decimal
import logging

from ...models import Payment, DriverBalance, CustomUser

logger = logging.getLogger(__name__)

# --------------------------------------------------------------------------
# Paynow client — initialised once at module level
# --------------------------------------------------------------------------
paynow = Paynow(
    settings.PAYNOW_INTEGRATION_ID,
    settings.PAYNOW_INTEGRATION_KEY,
    settings.PAYNOW_RETURN_URL,
    settings.PAYNOW_RESULT_URL,
)

# Payment methods that use Paynow mobile money (USSD prompt sent to phone)
MOBILE_METHODS = {'ecocash', 'onemoney'}


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def initiate_payment(request):
    """
    Start a Paynow payment before requesting a delivery.

    Body: { "amount": 10.50, "payment_method": "ecocash"|"onemoney"|"card",
            "phone": "0771234567" (required for ecocash/onemoney) }

    Returns: { "payment_id", "poll_url", "redirect_url" (card only),
               "instructions" }
    """
    user = request.user
    amount = request.data.get("amount")
    payment_method = request.data.get("payment_method", "").lower()
    phone = request.data.get("phone", "")

    # --- validation ---
    if not amount:
        return Response({"error": "amount is required"}, status=status.HTTP_400_BAD_REQUEST)
    try:
        amount = Decimal(str(amount))
        if amount <= 0:
            raise ValueError
    except (ValueError, TypeError):
        return Response({"error": "Invalid amount"}, status=status.HTTP_400_BAD_REQUEST)

    if payment_method not in ('card', 'ecocash', 'onemoney'):
        return Response(
            {"error": "payment_method must be card, ecocash, or onemoney"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    if payment_method in MOBILE_METHODS and not phone:
        return Response(
            {"error": "phone is required for EcoCash/OneMoney payments"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    # --- create Payment record ---
    payment_record = Payment.objects.create(
        client=user,
        amount=amount,
        payment_method=payment_method,
    )

    # --- create Paynow payment ---
    reference = f"TumaGo-{payment_record.id}"
    paynow_payment = paynow.create_payment(reference, user.email)
    paynow_payment.add("Delivery fee", float(amount))

    try:
        if payment_method in MOBILE_METHODS:
            # EcoCash / OneMoney — sends USSD prompt to user's phone
            response = paynow.send_mobile(paynow_payment, phone, payment_method)
        else:
            # Card — returns a redirect URL to Paynow's checkout page
            response = paynow.send(paynow_payment)

        if response.success:
            payment_record.paynow_reference = reference
            payment_record.poll_url = response.poll_url
            # redirect_url only exists for web/card payments
            payment_record.redirect_url = getattr(response, 'redirect_url', '') or ''
            payment_record.save()

            result = {
                "payment_id": str(payment_record.id),
                "poll_url": response.poll_url,
            }

            if payment_method == 'card':
                result["redirect_url"] = response.redirect_url
                result["instructions"] = "Open the redirect URL to complete card payment."
            else:
                result["instructions"] = (
                    f"A payment prompt has been sent to {phone}. "
                    "Please confirm on your phone."
                )

            return Response(result, status=status.HTTP_201_CREATED)
        else:
            # Paynow rejected the request
            payment_record.status = Payment.FAILED
            payment_record.save()
            logger.error(f"Paynow rejected payment {payment_record.id}: {response.error}")
            return Response(
                {"error": f"Payment gateway error: {response.error}"},
                status=status.HTTP_502_BAD_GATEWAY,
            )
    except Exception as e:
        payment_record.status = Payment.FAILED
        payment_record.save()
        logger.exception(f"Paynow exception for payment {payment_record.id}")
        return Response(
            {"error": "Failed to connect to payment gateway"},
            status=status.HTTP_502_BAD_GATEWAY,
        )


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def check_payment_status(request):
    """
    Poll the status of a payment.

    Query: ?payment_id=<uuid>
    Returns: { "payment_id", "status", "paid" }
    """
    payment_id = request.query_params.get("payment_id")
    if not payment_id:
        return Response({"error": "payment_id is required"}, status=status.HTTP_400_BAD_REQUEST)

    try:
        payment_record = Payment.objects.get(id=payment_id, client=request.user)
    except Payment.DoesNotExist:
        return Response({"error": "Payment not found"}, status=status.HTTP_404_NOT_FOUND)

    # If already resolved, return immediately
    if payment_record.status in (Payment.PAID, Payment.FAILED, Payment.CANCELLED):
        return Response({
            "payment_id": str(payment_record.id),
            "status": payment_record.status,
            "paid": payment_record.status == Payment.PAID,
        })

    # Poll Paynow for the latest status
    if payment_record.poll_url:
        try:
            poll_response = paynow.check_transaction_status(payment_record.poll_url)
            if poll_response.paid:
                payment_record.status = Payment.PAID
                payment_record.paid_at = timezone.now()
                payment_record.save()
            elif poll_response.status and poll_response.status.lower() in ('failed', 'cancelled'):
                payment_record.status = Payment.FAILED
                payment_record.save()
        except Exception as e:
            logger.warning(f"Paynow poll failed for {payment_id}: {e}")

    return Response({
        "payment_id": str(payment_record.id),
        "status": payment_record.status,
        "paid": payment_record.status == Payment.PAID,
    })


@api_view(["POST"])
@permission_classes([AllowAny])
def paynow_callback(request):
    """
    Paynow POSTs payment results here (RESULT_URL).
    No auth — Paynow sends this server-to-server.

    Paynow sends form-encoded data with fields like:
    reference, paynowreference, amount, status, pollurl, hash
    """
    reference = request.data.get("reference", "") or request.POST.get("reference", "")
    paynow_status = request.data.get("status", "") or request.POST.get("status", "")

    if not reference:
        return Response({"error": "Missing reference"}, status=status.HTTP_400_BAD_REQUEST)

    # Reference format: TumaGo-<uuid>
    try:
        payment_uuid = reference.replace("TumaGo-", "")
        payment_record = Payment.objects.get(id=payment_uuid)
    except (Payment.DoesNotExist, ValueError):
        logger.warning(f"Paynow callback: unknown reference {reference}")
        return Response({"error": "Unknown reference"}, status=status.HTTP_404_NOT_FOUND)

    paynow_status_lower = paynow_status.lower()

    if paynow_status_lower in ('paid', 'delivered'):
        payment_record.status = Payment.PAID
        payment_record.paid_at = timezone.now()
        logger.info(f"Payment {payment_record.id} confirmed via callback")
    elif paynow_status_lower in ('failed', 'cancelled'):
        payment_record.status = Payment.FAILED
        logger.info(f"Payment {payment_record.id} failed/cancelled via callback")

    payment_record.save()
    return Response({"status": "ok"})


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def get_driver_balance(request):
    """
    Returns the accumulated amount the system owes this driver.
    Only drivers can access this.
    """
    user = request.user
    if user.role != CustomUser.DRIVER:
        return Response({"error": "Only drivers can view balance"}, status=status.HTTP_403_FORBIDDEN)

    balance, _ = DriverBalance.objects.get_or_create(driver=user)
    return Response({
        "owed_amount": str(balance.owed_amount),
        "total_paid": str(balance.total_paid),
        "last_paid_at": str(balance.last_paid_at) if balance.last_paid_at else None,
    })


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def pay_driver(request):
    """
    Admin endpoint: mark a driver's balance as paid.
    Resets owed_amount to 0, adds to total_paid.

    Body: { "driver_id": "<uuid>" }
    """
    # Only staff/superusers can settle driver balances
    if not request.user.is_staff:
        return Response({"error": "Admin access required"}, status=status.HTTP_403_FORBIDDEN)

    driver_id = request.data.get("driver_id")
    if not driver_id:
        return Response({"error": "driver_id is required"}, status=status.HTTP_400_BAD_REQUEST)

    try:
        driver = CustomUser.objects.get(id=driver_id, role=CustomUser.DRIVER)
    except CustomUser.DoesNotExist:
        return Response({"error": "Driver not found"}, status=status.HTTP_404_NOT_FOUND)

    balance, _ = DriverBalance.objects.get_or_create(driver=driver)

    if balance.owed_amount <= 0:
        return Response({"message": "Nothing owed to this driver"}, status=status.HTTP_200_OK)

    paid_amount = balance.owed_amount
    balance.total_paid += paid_amount
    balance.owed_amount = Decimal("0.00")
    balance.last_paid_at = timezone.now()
    balance.save()

    logger.info(f"Admin {request.user.email} paid driver {driver.email} ${paid_amount}")

    return Response({
        "message": f"Paid ${paid_amount} to {driver.name} {driver.surname}",
        "owed_amount": "0.00",
        "total_paid": str(balance.total_paid),
        "last_paid_at": str(balance.last_paid_at),
    })
