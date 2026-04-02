from django.conf import settings
from rest_framework.decorators import api_view, permission_classes, throttle_classes
from rest_framework.response import Response
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework import status
from rest_framework.throttling import AnonRateThrottle
from ...serializers.driverSerializer.authSerializers import RegisterSerializer, VehicleSerializer, LicenseUploadSerializer, ResetPasswordSerializer
from ...token import JWTAuthentication
from ...otp import is_phone_verified, clear_verification, is_email_verified, clear_email_verification
from ...busy_drivers import mark_driver_available
from ..EmailViews.emailService import send_email, send_email_async
from ..EmailViews.templates import welcome_driver_email, password_reset_email
import logging
import secrets
from django.core.cache import cache
from ...models import CustomUser
from django.shortcuts import get_object_or_404

logger = logging.getLogger(__name__)

# Header used by the WhatsApp service for internal calls (skips OTP check)
WHATSAPP_INTERNAL_HEADER = "HTTP_X_WHATSAPP_INTERNAL"

class DriverSignupThrottle(AnonRateThrottle):
    scope = 'signup'

# Register Driver / User (Unified Registration)
@api_view(['POST'])
@permission_classes([AllowAny])
@throttle_classes([DriverSignupThrottle])
def driver_register(request):
    # WhatsApp service calls include X-WhatsApp-Internal header with the
    # shared SECRET_KEY — these skip OTP because WhatsApp already verifies
    # the phone number. Android app registrations MUST verify OTP first.
    internal_secret = request.META.get(WHATSAPP_INTERNAL_HEADER, "")
    is_whatsapp_internal = internal_secret == settings.SECRET_KEY

    phone = request.data.get("phone_number", "")

    if not is_whatsapp_internal:
        # Android app registration — require OTP verification
        if not phone:
            return Response(
                {"detail": "phone_number is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if not is_phone_verified(phone):
            return Response(
                {"detail": "Phone number not verified. Complete OTP verification first."},
                status=status.HTTP_403_FORBIDDEN,
            )

    # Initialize the serializer with the incoming data
    serializer = RegisterSerializer(data=request.data)

    if serializer.is_valid():
        user = serializer.save()  # This will save the user as either 'driver' or 'user'
        payload = {
            "id": str(user.id)  # Add role info to payload (driver or user)
        }

        token = JWTAuthentication.generate_token(payload=payload)
        refresh_token = JWTAuthentication.generate_refresh_token(payload=payload)

        # Clear the OTP verification flag so it can't be reused
        if phone and not is_whatsapp_internal:
            clear_verification(phone)

        # Mark email as verified if email OTP was completed
        if user.email and is_email_verified(user.email):
            user.verifiedEmail = True
            user.save(update_fields=["verifiedEmail"])
            clear_email_verification(user.email)

        # Send welcome email via background task (retries automatically on failure)
        if user.email:
            subject, text, html = welcome_driver_email(user.name)
            send_email_async.send(user.email, subject, text, html)

        return Response({
            'accessToken': token,
            'refreshToken': refresh_token
        }, status=status.HTTP_201_CREATED)

    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@api_view(["DELETE"])
@permission_classes([IsAuthenticated])
def delete_account(request):
    user = request.user
    user.delete()
    return Response({"message": "User deleted successfully"}, status=status.HTTP_204_NO_CONTENT)

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def save_fcm_token(request):
    token = request.data.get("fcm_token")
    driver = request.user

    if token:
        driver.fcm_token = token
        driver.driver_online = True
        driver.driver_available = True
        driver.save()

        logger.info("Driver %s online", driver.id)

        return Response({"message": "Token saved"}, status=200)
    return Response({"error": "Token missing"}, status=400)

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def driver_vehicle(request):
    serializer = VehicleSerializer(data=request.data, context={'request': request})
    if serializer.is_valid():
        serializer.save()
        return Response(status=status.HTTP_201_CREATED)
    return Response(status=status.HTTP_400_BAD_REQUEST)

@api_view(['PUT'])
@permission_classes([IsAuthenticated])
def upload_license(request):
    user = request.user
    serializer = LicenseUploadSerializer(user, data=request.data, partial=True)
    if serializer.is_valid():
        serializer.save()
        return Response({"message": "License uploaded successfully."}, status=status.HTTP_200_OK)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def driver_offline(request):
    driver = request.user
    driver.driver_available = False
    driver.driver_online = False
    driver.save()
    mark_driver_available(driver.id)  # ensure removed from busy set when going offline
    logger.info("Driver %s offline", driver.id)
    return Response({"status": "driver marked offline"}, status=status.HTTP_202_ACCEPTED)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def change_password(request):
    serializer = ResetPasswordSerializer(data=request.data)
    if serializer.is_valid():
        user = request.user
        new_password = serializer.validated_data['newPassword']
        old_password = serializer.validated_data['oldPassword']

        if not user.check_password(old_password):
            return Response({'detail': 'Old password is incorrect.'}, status=status.HTTP_400_BAD_REQUEST)

        user.set_password(new_password)
        user.save()

        return Response({'detail': 'Password has been updated successfully.'}, status=status.HTTP_200_OK)
    else:
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


# ---------------------------------------------------------------------------
# Forgot Password — email-based password reset (no auth required)
# ---------------------------------------------------------------------------

RESET_TOKEN_TTL = 1800  # 30 minutes

class ForgotPasswordThrottle(AnonRateThrottle):
    scope = 'otp_send'


@api_view(['POST'])
@permission_classes([AllowAny])
@throttle_classes([ForgotPasswordThrottle])
def forgot_password(request):
    """Send a password reset link to the user's email.

    Body: { "email": "user@example.com" }

    Always returns 200 to prevent email enumeration.
    """
    email = (request.data.get("email") or "").strip().lower()
    if not email or "@" not in email:
        return Response({"detail": "A valid email is required."}, status=status.HTTP_400_BAD_REQUEST)

    # Always return success to prevent email enumeration
    try:
        user = CustomUser.objects.get(email=email)
    except CustomUser.DoesNotExist:
        return Response({"detail": "If an account with that email exists, a reset link has been sent."})

    # Generate a secure reset token and store in Redis
    token = secrets.token_urlsafe(48)
    cache.set(f"password_reset:{token}", str(user.id), timeout=RESET_TOKEN_TTL)

    # Build reset URL pointing to the frontend
    frontend_url = settings.FRONTEND_URL.rstrip("/")
    reset_url = f"{frontend_url}/reset-password?token={token}"

    subject, text, html = password_reset_email(user.name, reset_url)
    send_email(to=email, subject=subject, body_text=text, body_html=html)

    return Response({"detail": "If an account with that email exists, a reset link has been sent."})


@api_view(['POST'])
@permission_classes([AllowAny])
def reset_password_confirm(request):
    """Reset password using the token from the email link.

    Body: { "token": "...", "password": "newpassword", "confirm_password": "newpassword" }
    """
    token = (request.data.get("token") or "").strip()
    password = request.data.get("password") or ""
    confirm = request.data.get("confirm_password") or ""

    if not token:
        return Response({"detail": "Reset token is required."}, status=status.HTTP_400_BAD_REQUEST)
    if len(password) < 6:
        return Response({"detail": "Password must be at least 6 characters."}, status=status.HTTP_400_BAD_REQUEST)
    if password != confirm:
        return Response({"detail": "Passwords do not match."}, status=status.HTTP_400_BAD_REQUEST)

    # Look up the token in Redis
    user_id = cache.get(f"password_reset:{token}")
    if not user_id:
        return Response({"detail": "Invalid or expired reset link."}, status=status.HTTP_400_BAD_REQUEST)

    try:
        user = CustomUser.objects.get(id=user_id)
    except CustomUser.DoesNotExist:
        return Response({"detail": "User not found."}, status=status.HTTP_404_NOT_FOUND)

    user.set_password(password)
    user.save()

    # Delete the token so it can't be reused
    cache.delete(f"password_reset:{token}")

    logger.info("Password reset for user %s via email link", user_id)
    return Response({"detail": "Password has been reset successfully."})