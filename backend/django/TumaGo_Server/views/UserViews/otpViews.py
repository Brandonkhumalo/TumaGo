"""
OTP endpoints for Android app registrations only.

WhatsApp registrations do NOT use OTP — the phone number is already
verified by WhatsApp itself.
"""
import logging

from rest_framework.decorators import api_view, permission_classes, throttle_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework import status
from rest_framework.throttling import AnonRateThrottle

from ...otp import generate_otp, verify_otp, send_otp_whatsapp

logger = logging.getLogger(__name__)


class OTPSendThrottle(AnonRateThrottle):
    """Limit OTP send requests to prevent abuse."""
    scope = 'otp_send'


class OTPVerifyThrottle(AnonRateThrottle):
    """Limit OTP verify requests to prevent brute-force."""
    scope = 'otp_verify'


@api_view(["POST"])
@permission_classes([AllowAny])
@throttle_classes([OTPSendThrottle])
def send_otp(request):
    """
    Send a 6-digit OTP to the given phone number via WhatsApp.

    Request body:
        phone_number (str): Phone number (e.g., "0771234567" or "+263771234567")
        role (str, optional): "user" or "driver" — determines which WhatsApp template to use.
                              Defaults to "user".

    The OTP is valid for 10 minutes.
    """
    phone = request.data.get("phone_number")
    if not phone:
        return Response(
            {"detail": "phone_number is required."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    # Determine which WhatsApp template to use
    role = request.data.get("role", "user")
    template_name = "driver_otp" if role == "driver" else "client_otp"

    # Generate OTP and store in Redis (10 min TTL)
    otp = generate_otp(phone)

    # Send via WhatsApp template message
    sent = send_otp_whatsapp(phone, otp, template_name=template_name)

    if not sent:
        return Response(
            {"detail": "Failed to send OTP. Please try again."},
            status=status.HTTP_502_BAD_GATEWAY,
        )

    return Response(
        {"detail": "OTP sent to your WhatsApp."},
        status=status.HTTP_200_OK,
    )


@api_view(["POST"])
@permission_classes([AllowAny])
@throttle_classes([OTPVerifyThrottle])
def verify_otp_view(request):
    """
    Verify the OTP for a phone number.

    Request body:
        phone_number (str): Phone number used when sending the OTP
        otp (str): The 6-digit code received via WhatsApp

    On success, the phone number is marked as verified for 15 minutes,
    allowing the user to complete signup.
    """
    phone = request.data.get("phone_number")
    otp = request.data.get("otp")

    if not phone or not otp:
        return Response(
            {"detail": "phone_number and otp are required."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    success, message = verify_otp(phone, otp)

    if not success:
        return Response(
            {"detail": message},
            status=status.HTTP_400_BAD_REQUEST,
        )

    return Response(
        {"detail": message, "verified": True},
        status=status.HTTP_200_OK,
    )
