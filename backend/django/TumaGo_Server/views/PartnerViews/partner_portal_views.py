"""
Partner self-service portal endpoints.

Handles: registration, setup-fee payment, login, account dashboard,
device credential management, and purchasing additional device slots.
"""
import secrets
from decimal import Decimal

from django.conf import settings
from django.contrib.auth.hashers import make_password, check_password
from paynow import Paynow
from rest_framework.decorators import api_view, authentication_classes, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework import status

from ...models import (
    PartnerCompany,
    PartnerDevice,
    PartnerTransaction,
    CustomUser,
)
from ...partner_auth import PartnerJWTAuthentication
from ..EmailViews.emailService import send_email
from ..EmailViews.templates import welcome_partner_email

import logging

logger = logging.getLogger(__name__)

# Paynow client (same config as paymentViews)
paynow = Paynow(
    settings.PAYNOW_INTEGRATION_ID,
    settings.PAYNOW_INTEGRATION_KEY,
    settings.PAYNOW_RETURN_URL,
    settings.PAYNOW_RESULT_URL,
)

MOBILE_METHODS = {"ecocash", "onemoney"}

SETUP_FEE = Decimal("15.00")
DEVICE_SLOT_PRICE = Decimal("5.00")
DEVICE_SLOT_QUANTITY = 7

# Auth decorators for portal endpoints
PORTAL_AUTH = [PartnerJWTAuthentication]
PORTAL_PERM = [AllowAny]  # Auth is handled by the JWT class itself


def _get_partner(request):
    partner = request.auth
    if not isinstance(partner, PartnerCompany):
        return None
    return partner


# ---------------------------------------------------------------------------
# 1. POST /partner/portal/register/ — Self-service registration
# ---------------------------------------------------------------------------

@api_view(["POST"])
@authentication_classes([])
@permission_classes([AllowAny])
def partner_register(request):
    """Register a new partner company.

    Body: { "name", "email", "password", "phone" }
    Returns: { "partner_id" } — account is inactive until setup fee is paid.
    """
    data = request.data
    name = (data.get("name") or "").strip()
    email = (data.get("email") or "").strip().lower()
    password = data.get("password") or ""
    phone = (data.get("phone") or "").strip()

    if not name:
        return Response({"detail": "Company name is required."}, status=status.HTTP_400_BAD_REQUEST)
    if not email or "@" not in email:
        return Response({"detail": "A valid email is required."}, status=status.HTTP_400_BAD_REQUEST)
    if len(password) < 8:
        return Response({"detail": "Password must be at least 8 characters."}, status=status.HTTP_400_BAD_REQUEST)
    if not phone:
        return Response({"detail": "Phone number is required."}, status=status.HTTP_400_BAD_REQUEST)

    # Check for duplicate email
    if PartnerCompany.objects.filter(contact_email=email).exists():
        return Response({"detail": "A partner with this email already exists."}, status=status.HTTP_409_CONFLICT)

    # Business info fields
    description = (data.get("description") or "").strip()
    address = (data.get("address") or "").strip()
    city = (data.get("city") or "").strip()
    contact_person_name = (data.get("contact_person_name") or "").strip()
    contact_person_role = (data.get("contact_person_role") or "").strip()

    # Placeholder credentials — real ones are generated after payment
    placeholder_key = f"tg_pending_{secrets.token_hex(24)}"
    placeholder_secret = ""

    partner = PartnerCompany.objects.create(
        name=name,
        contact_email=email,
        password=make_password(password),
        phone_number=phone,
        description=description,
        address=address,
        city=city,
        contact_person_name=contact_person_name,
        contact_person_role=contact_person_role,
        api_key=placeholder_key,
        api_secret=placeholder_secret,
        is_active=False,
        setup_fee_paid=False,
    )

    # Send welcome email
    try:
        subject, text, html = welcome_partner_email(name)
        send_email(to=email, subject=subject, body_text=text, body_html=html)
    except Exception as e:
        logger.warning(f"Partner welcome email failed for {email}: {e}")

    return Response(
        {
            "partner_id": str(partner.id),
            "message": "Registration successful. Please pay the $15 setup fee to activate your account.",
        },
        status=status.HTTP_201_CREATED,
    )


# ---------------------------------------------------------------------------
# 2. POST /partner/portal/pay-setup/ — Initiate $15 setup fee payment
# ---------------------------------------------------------------------------

@api_view(["POST"])
@authentication_classes([])
@permission_classes([AllowAny])
def partner_pay_setup(request):
    """Initiate Paynow payment for the $15 setup fee.

    Body: { "partner_id", "email", "payment_method": "ecocash"|"onemoney"|"card",
            "phone": "07..." (required for mobile) }
    """
    data = request.data
    partner_id = data.get("partner_id")
    email = (data.get("email") or "").strip().lower()
    payment_method = (data.get("payment_method") or "").lower()
    phone = (data.get("phone") or "").strip()

    if not partner_id or not email:
        return Response({"detail": "partner_id and email are required."}, status=status.HTTP_400_BAD_REQUEST)

    try:
        partner = PartnerCompany.objects.get(id=partner_id, contact_email=email)
    except PartnerCompany.DoesNotExist:
        return Response({"detail": "Partner not found."}, status=status.HTTP_404_NOT_FOUND)

    if partner.setup_fee_paid:
        return Response({"detail": "Setup fee already paid."}, status=status.HTTP_400_BAD_REQUEST)

    if payment_method not in ("card", "ecocash", "onemoney"):
        return Response(
            {"detail": "payment_method must be card, ecocash, or onemoney."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    if payment_method in MOBILE_METHODS and not phone:
        return Response(
            {"detail": "Phone number is required for EcoCash/OneMoney."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    # Create Paynow payment
    reference = f"TumaGo-Partner-{partner.id}"
    paynow_payment = paynow.create_payment(reference, partner.contact_email)
    paynow_payment.add("Partner Setup Fee", float(SETUP_FEE))

    try:
        if payment_method in MOBILE_METHODS:
            response = paynow.send_mobile(paynow_payment, phone, payment_method)
        else:
            response = paynow.send(paynow_payment)

        if response.success:
            partner.setup_poll_url = response.poll_url
            partner.save(update_fields=["setup_poll_url"])

            result = {"partner_id": str(partner.id), "poll_url": response.poll_url}
            if payment_method == "card":
                result["redirect_url"] = response.redirect_url
                result["instructions"] = "Open the redirect URL to complete card payment."
            else:
                result["instructions"] = (
                    f"A payment prompt has been sent to {phone}. "
                    "Please confirm on your phone."
                )
            return Response(result, status=status.HTTP_201_CREATED)
        else:
            logger.error("Paynow rejected partner setup payment %s: %s", partner.id, response.error)
            return Response(
                {"detail": f"Payment gateway error: {response.error}"},
                status=status.HTTP_502_BAD_GATEWAY,
            )
    except Exception as e:
        logger.exception("Paynow exception for partner setup %s", partner.id)
        return Response(
            {"detail": "Failed to connect to payment gateway."},
            status=status.HTTP_502_BAD_GATEWAY,
        )


# ---------------------------------------------------------------------------
# 3. GET /partner/portal/pay-setup/status/ — Poll setup fee payment
# ---------------------------------------------------------------------------

@api_view(["GET"])
@authentication_classes([])
@permission_classes([AllowAny])
def partner_pay_setup_status(request):
    """Poll Paynow for the setup fee payment status.

    Query: ?partner_id=<uuid>&email=<email>
    On success: activates partner account.
    """
    partner_id = request.query_params.get("partner_id")
    email = (request.query_params.get("email") or "").strip().lower()

    if not partner_id or not email:
        return Response({"detail": "partner_id and email are required."}, status=status.HTTP_400_BAD_REQUEST)

    try:
        partner = PartnerCompany.objects.get(id=partner_id, contact_email=email)
    except PartnerCompany.DoesNotExist:
        return Response({"detail": "Partner not found."}, status=status.HTTP_404_NOT_FOUND)

    if partner.setup_fee_paid:
        return Response({
            "paid": True,
            "message": "Setup fee already paid. You can log in.",
        })

    if not partner.setup_poll_url:
        return Response({
            "paid": False,
            "message": "No payment initiated. Please initiate payment first.",
        })

    # Poll Paynow
    try:
        poll_response = paynow.check_transaction_status(partner.setup_poll_url)
        if poll_response.paid:
            # Generate real API credentials now that payment is confirmed
            api_key = f"tg_live_{secrets.token_hex(24)}"
            api_secret = f"sk_{secrets.token_hex(32)}"

            # Activate the partner account
            partner.api_key = api_key
            partner.api_secret = api_secret
            partner.setup_fee_paid = True
            partner.is_active = True
            partner.setup_poll_url = ""
            partner.save(update_fields=[
                "api_key", "api_secret",
                "setup_fee_paid", "is_active", "setup_poll_url",
            ])

            # Record setup fee transaction
            PartnerTransaction.objects.create(
                partner=partner,
                transaction_type="setup_fee",
                amount=-SETUP_FEE,
                balance_after=partner.balance,
                description="One-time partner setup fee",
            )

            return Response({
                "paid": True,
                "message": "Payment confirmed! Your account is now active. Log in to view your API key.",
            })
        elif poll_response.status and poll_response.status.lower() in ("failed", "cancelled"):
            partner.setup_poll_url = ""
            partner.save(update_fields=["setup_poll_url"])
            return Response({
                "paid": False,
                "message": "Payment failed or was cancelled. Please try again.",
            })
    except Exception as e:
        logger.warning("Paynow poll failed for partner %s: %s", partner.id, e)

    return Response({
        "paid": False,
        "message": "Payment is still pending. Please check again shortly.",
    })


# ---------------------------------------------------------------------------
# 4. POST /partner/portal/login/ — Partner login
# ---------------------------------------------------------------------------

@api_view(["POST"])
@authentication_classes([])
@permission_classes([AllowAny])
def partner_login(request):
    """Authenticate a partner and return a JWT.

    Body: { "email", "password" }
    Returns: { "token", "partner": { id, name, ... } }
    """
    email = (request.data.get("email") or "").strip().lower()
    password = request.data.get("password") or ""

    if not email or not password:
        return Response({"detail": "Email and password are required."}, status=status.HTTP_400_BAD_REQUEST)

    try:
        partner = PartnerCompany.objects.get(contact_email=email)
    except PartnerCompany.DoesNotExist:
        return Response({"detail": "Invalid email or password."}, status=status.HTTP_401_UNAUTHORIZED)

    if not check_password(password, partner.password):
        return Response({"detail": "Invalid email or password."}, status=status.HTTP_401_UNAUTHORIZED)

    if not partner.is_active:
        return Response(
            {"detail": "Account is not active. Please complete the setup fee payment."},
            status=status.HTTP_403_FORBIDDEN,
        )

    token = PartnerJWTAuthentication.generate_token(partner)

    return Response({
        "token": token,
        "partner": {
            "id": str(partner.id),
            "name": partner.name,
            "email": partner.contact_email,
        },
    })


# ---------------------------------------------------------------------------
# 5. GET /partner/portal/account/ — Partner dashboard data
# ---------------------------------------------------------------------------

@api_view(["GET"])
@authentication_classes(PORTAL_AUTH)
@permission_classes(PORTAL_PERM)
def partner_account(request):
    """Return partner account info for the dashboard."""
    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    device_count = PartnerDevice.objects.filter(partner=partner).count()

    return Response({
        "id": str(partner.id),
        "name": partner.name,
        "email": partner.contact_email,
        "phone": partner.phone_number,
        "api_key": partner.api_key,
        "api_secret": partner.api_secret,
        "webhook_url": partner.webhook_url,
        "balance": float(partner.balance),
        "commission_rate": float(partner.commission_rate),
        "max_device_slots": partner.max_device_slots,
        "device_count": device_count,
        "created_at": partner.created_at.isoformat(),
        "setup_fee_paid": partner.setup_fee_paid,
        "is_suspended": getattr(partner, 'is_suspended', False),
        "is_permanently_banned": getattr(partner, 'is_permanently_banned', False),
    })


# ---------------------------------------------------------------------------
# 6. POST /partner/portal/devices/ — Create device login credential
# ---------------------------------------------------------------------------

@api_view(["POST"])
@authentication_classes(PORTAL_AUTH)
@permission_classes(PORTAL_PERM)
def partner_create_device(request):
    """Create a new device login credential.

    Body: { "label", "email", "password" }
    Creates a CustomUser (role=user) linked to the partner via PartnerDevice.
    """
    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    # Check device slot limit
    current_count = PartnerDevice.objects.filter(partner=partner).count()
    if current_count >= partner.max_device_slots:
        return Response(
            {
                "detail": f"Device limit reached ({partner.max_device_slots}). Purchase more slots to add devices.",
                "max_slots": partner.max_device_slots,
                "current_count": current_count,
            },
            status=status.HTTP_403_FORBIDDEN,
        )

    data = request.data
    label = (data.get("label") or "").strip()
    email = (data.get("email") or "").strip().lower()
    password = data.get("password") or ""

    if not label:
        return Response({"detail": "Device label is required."}, status=status.HTTP_400_BAD_REQUEST)
    if not email or "@" not in email:
        return Response({"detail": "A valid email is required."}, status=status.HTTP_400_BAD_REQUEST)
    if len(password) < 6:
        return Response({"detail": "Password must be at least 6 characters."}, status=status.HTTP_400_BAD_REQUEST)

    # Check email uniqueness across all users
    if CustomUser.objects.filter(email=email).exists():
        return Response({"detail": "This email is already in use."}, status=status.HTTP_409_CONFLICT)

    # Create the CustomUser account for this device
    user = CustomUser.objects.create_user(
        email=email,
        password=password,
        name=partner.name,
        surname=f"Device",
        phone_number=partner.phone_number or "0000000000",
        streetAdress="N/A",
        city="N/A",
        province="N/A",
        postalCode="0000",
        role=CustomUser.USER,
    )

    # Link to partner
    device = PartnerDevice.objects.create(
        partner=partner,
        user=user,
        label=label,
    )

    return Response(
        {
            "id": str(device.id),
            "label": device.label,
            "email": user.email,
            "is_active": device.is_active,
            "created_at": device.created_at.isoformat(),
        },
        status=status.HTTP_201_CREATED,
    )


# ---------------------------------------------------------------------------
# 7. GET /partner/portal/devices/ — List device credentials
# ---------------------------------------------------------------------------

@api_view(["GET"])
@authentication_classes(PORTAL_AUTH)
@permission_classes(PORTAL_PERM)
def partner_list_devices(request):
    """List all device credentials for this partner."""
    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    devices = PartnerDevice.objects.filter(partner=partner).select_related("user").order_by("-created_at")

    results = []
    for d in devices:
        results.append({
            "id": str(d.id),
            "label": d.label,
            "email": d.user.email,
            "is_active": d.is_active,
            "created_at": d.created_at.isoformat(),
        })

    return Response({
        "devices": results,
        "count": len(results),
        "max_slots": partner.max_device_slots,
    })


# ---------------------------------------------------------------------------
# 8. POST /partner/portal/devices/<id>/toggle/ — Activate/deactivate device
# ---------------------------------------------------------------------------

@api_view(["POST"])
@authentication_classes(PORTAL_AUTH)
@permission_classes(PORTAL_PERM)
def partner_toggle_device(request, device_id):
    """Toggle a device credential's active status."""
    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    try:
        device = PartnerDevice.objects.select_related("user").get(id=device_id, partner=partner)
    except PartnerDevice.DoesNotExist:
        return Response({"detail": "Device not found."}, status=status.HTTP_404_NOT_FOUND)

    device.is_active = not device.is_active
    device.save(update_fields=["is_active"])

    # Also toggle the underlying user account
    device.user.is_active = device.is_active
    device.user.save(update_fields=["is_active"])

    return Response({
        "id": str(device.id),
        "label": device.label,
        "is_active": device.is_active,
        "message": f"Device {'activated' if device.is_active else 'deactivated'}.",
    })


# ---------------------------------------------------------------------------
# 9. POST /partner/portal/devices/purchase/ — Buy more device slots
# ---------------------------------------------------------------------------

@api_view(["POST"])
@authentication_classes(PORTAL_AUTH)
@permission_classes(PORTAL_PERM)
def partner_purchase_device_slots(request):
    """Purchase 7 additional device slots for $5.

    Body: { "payment_method": "ecocash"|"onemoney"|"card",
            "phone": "07..." (for mobile methods) }
    Deducts $5 from partner balance.
    """
    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    # Check partner has sufficient balance
    if partner.balance < DEVICE_SLOT_PRICE:
        return Response(
            {
                "detail": f"Insufficient balance. Current: ${float(partner.balance):.2f}, required: ${float(DEVICE_SLOT_PRICE):.2f}. Please deposit funds first.",
            },
            status=status.HTTP_402_PAYMENT_REQUIRED,
        )

    # Deduct from balance
    partner.balance -= DEVICE_SLOT_PRICE
    partner.max_device_slots += DEVICE_SLOT_QUANTITY
    partner.save(update_fields=["balance", "max_device_slots"])

    # Record transaction
    PartnerTransaction.objects.create(
        partner=partner,
        transaction_type="device_purchase",
        amount=-DEVICE_SLOT_PRICE,
        balance_after=partner.balance,
        description=f"Purchased {DEVICE_SLOT_QUANTITY} additional device slots",
    )

    return Response({
        "message": f"Successfully purchased {DEVICE_SLOT_QUANTITY} device slots.",
        "max_device_slots": partner.max_device_slots,
        "balance": float(partner.balance),
    })
