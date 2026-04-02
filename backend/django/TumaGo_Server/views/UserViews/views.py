from django.conf import settings
from django.contrib.auth import get_user_model
from rest_framework.response import Response
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes, throttle_classes
from rest_framework.throttling import AnonRateThrottle
from django.shortcuts import get_object_or_404
from rest_framework.permissions import AllowAny, IsAuthenticated, IsAdminUser
import logging
from ...models import BlacklistedToken, Delivery, TermsAndConditions, TermsContent
from ...otp import is_phone_verified, clear_verification, is_email_verified, clear_email_verification
from ..EmailViews.emailService import send_email
from ..EmailViews.templates import welcome_client_email

logger = logging.getLogger(__name__)
from ...serializers.userSerializer.authserializers import (
    SignupSerializer,
    UserInfo,
    UserSerializer,
    LoginSerializer
)
from ...serializers.driverSerializer.rideSerializers import DeliverySerializer
from ...token import JWTAuthentication
from jwt.exceptions import InvalidTokenError, ExpiredSignatureError
import jwt

# Header used by the WhatsApp service for internal calls (skips OTP check)
WHATSAPP_INTERNAL_HEADER = "HTTP_X_WHATSAPP_INTERNAL"

# Scoped throttles — rates pulled from settings.DEFAULT_THROTTLE_RATES
class LoginThrottle(AnonRateThrottle):
    scope = 'login'

class SignupThrottle(AnonRateThrottle):
    scope = 'signup'

User = None

@api_view(["POST"])
@permission_classes([AllowAny])
@throttle_classes([SignupThrottle])
def signup(request):
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

    serializer = SignupSerializer(data=request.data)
    if serializer.is_valid():
        user = serializer.save()
        payload = {"id": str(user.id)}  # UUID must be cast to str

        access_token = JWTAuthentication.generate_token(payload=payload)
        refresh_token = JWTAuthentication.generate_refresh_token(payload=payload)

        # Clear the OTP verification flag so it can't be reused
        if phone and not is_whatsapp_internal:
            clear_verification(phone)

        # Mark email as verified if email OTP was completed
        if user.email and is_email_verified(user.email):
            user.verifiedEmail = True
            user.save(update_fields=["verifiedEmail"])
            clear_email_verification(user.email)

        # Send welcome email (non-blocking — don't fail signup if email fails)
        if user.email:
            try:
                subject, text, html = welcome_client_email(user.name)
                send_email(to=user.email, subject=subject, body_text=text, body_html=html)
            except Exception as e:
                logger.warning(f"Welcome email failed for {user.email}: {e}")

        return Response({
            'accessToken': access_token,
            'refreshToken': refresh_token,
        }, status=status.HTTP_201_CREATED)

    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@api_view(["POST"])
@permission_classes([AllowAny])
@throttle_classes([LoginThrottle])
def login(request):
    serializer = LoginSerializer(data=request.data)
    if serializer.is_valid():
        user = serializer.validated_data['user']
        payload = {
            "id": user.id
        }

        access_token = JWTAuthentication.generate_token(payload=payload)
        refresh_token = JWTAuthentication.generate_refresh_token(payload=payload)

        return Response({
            'accessToken': access_token,
            'refreshToken': refresh_token
        }, status=status.HTTP_200_OK)

    logger.warning("Serializer errors: %s", serializer.errors)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def update_profile(request):
    serializer = UserInfo(instance=request.user, data=request.data, partial=True)
    if serializer.is_valid():
        serializer.save()
        return Response(serializer.data, status=status.HTTP_200_OK)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def logout(request):
    refresh_token = request.data.get("refreshToken") or request.query_params.get("refreshToken")

    if not refresh_token:
        logger.debug("Logout failed: refresh token is required")
        return Response({"detail": "Refresh token is required."}, status=status.HTTP_400_BAD_REQUEST)

    try:
        payload = jwt.decode(refresh_token, settings.SECRET_KEY, algorithms=['HS256'])
        if payload.get("type") != "refresh_token":
            logger.debug("Logout failed: invalid token type")
            return Response({"detail": "Invalid token type."}, status=status.HTTP_400_BAD_REQUEST)

        BlacklistedToken.objects.get_or_create(token=refresh_token)
        return Response({"detail": "Logged out successfully."}, status=status.HTTP_200_OK)

    except (InvalidTokenError, ExpiredSignatureError, jwt.DecodeError) as e:
        logger.debug("Logout failed: invalid token")
        return Response({"detail": f"Invalid token: {str(e)}"}, status=status.HTTP_400_BAD_REQUEST)

@api_view(["POST"])
@permission_classes([AllowAny])
def refresh_token(request):
    """Exchange a valid refresh token for a new access token."""
    refresh = request.data.get("refreshToken")
    if not refresh:
        return Response({"detail": "refreshToken is required."}, status=status.HTTP_400_BAD_REQUEST)

    try:
        payload = jwt.decode(refresh, settings.SECRET_KEY, algorithms=['HS256'])

        if payload.get("type") != "refresh_token":
            return Response({"detail": "Invalid token type."}, status=status.HTTP_400_BAD_REQUEST)

        if BlacklistedToken.objects.filter(token=refresh).exists():
            return Response({"detail": "Token has been revoked."}, status=status.HTTP_401_UNAUTHORIZED)

        user_id = payload.get("id")
        if not user_id:
            return Response({"detail": "Invalid token."}, status=status.HTTP_401_UNAUTHORIZED)

        # Verify user still exists
        from django.contrib.auth import get_user_model
        User_model = get_user_model()
        User_model.objects.get(id=user_id)

        new_access = JWTAuthentication.generate_token({"id": user_id})
        return Response({"accessToken": new_access}, status=status.HTTP_200_OK)

    except (InvalidTokenError, ExpiredSignatureError, jwt.DecodeError) as e:
        return Response({"detail": f"Invalid refresh token: {str(e)}"}, status=status.HTTP_401_UNAUTHORIZED)
    except User_model.DoesNotExist:
        return Response({"detail": "User no longer exists."}, status=status.HTTP_401_UNAUTHORIZED)


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def VerifyToken(request):
    return Response(status=status.HTTP_200_OK)

@api_view(["GET"])
@permission_classes([AllowAny])
def get_terms_content(request):
    """Return the latest T&C text for the given app_type (client or driver)."""
    app_type = request.query_params.get('app_type', 'client')
    if app_type not in ('client', 'driver'):
        return Response({'detail': 'app_type must be "client" or "driver".'}, status=status.HTTP_400_BAD_REQUEST)

    latest = TermsContent.objects.filter(app_type=app_type).first()
    if not latest:
        return Response({'detail': 'No terms and conditions available yet.'}, status=status.HTTP_404_NOT_FOUND)

    return Response({
        'content': latest.content,
        'version': latest.version,
        'updated_at': latest.updated_at.isoformat(),
    })


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def check_terms(request):
    """Check if user accepted the latest T&C version for their app type."""
    user = request.user
    app_type = request.query_params.get('app_type', 'client')
    if app_type not in ('client', 'driver'):
        app_type = 'driver' if user.role == 'driver' else 'client'

    latest = TermsContent.objects.filter(app_type=app_type).first()
    if not latest:
        # No T&C published yet — let user through
        return Response({"message": "Terms accepted.", "version": 0}, status=status.HTTP_200_OK)

    terms = TermsAndConditions.objects.filter(user=user, terms_and_conditions=True).first()
    if terms and terms.accepted_version >= latest.version:
        return Response({"message": "Terms accepted.", "version": latest.version}, status=status.HTTP_200_OK)

    return Response({
        "message": "Terms not accepted.",
        "latest_version": latest.version,
    }, status=status.HTTP_403_FORBIDDEN)


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def accept_terms(request):
    """Accept the latest T&C version for the given app type."""
    user = request.user
    app_type = request.data.get('app_type', 'client')
    if app_type not in ('client', 'driver'):
        app_type = 'driver' if user.role == 'driver' else 'client'

    latest = TermsContent.objects.filter(app_type=app_type).first()
    version = latest.version if latest else 0

    terms, created = TermsAndConditions.objects.get_or_create(user=user)
    terms.terms_and_conditions = True
    terms.accepted_version = version
    terms.save(update_fields=['terms_and_conditions', 'accepted_version'])

    return Response({"message": "Successfully agreed to the terms and conditions", "version": version}, status=status.HTTP_202_ACCEPTED)

# Admin-only endpoints — require authenticated staff user
@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def get_all_users(request):
    global User
    if User is None:
        User = get_user_model()

    try:
        page = max(int(request.query_params.get('page', 1)), 1)
    except (ValueError, TypeError):
        page = 1
    page_size = 20
    total = User.objects.count()
    start = (page - 1) * page_size

    users = User.objects.all()[start:start + page_size]
    serializer = UserSerializer(users, many=True)
    return Response({
        'count': total,
        'page': page,
        'page_size': page_size,
        'results': serializer.data,
    }, status=status.HTTP_200_OK)

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def get_all_deliveries(request):
    try:
        page = max(int(request.query_params.get('page', 1)), 1)
    except (ValueError, TypeError):
        page = 1
    page_size = 20
    total = Delivery.objects.count()
    start = (page - 1) * page_size

    deliveries = Delivery.objects.select_related('driver', 'client').all()[start:start + page_size]
    serializer = DeliverySerializer(deliveries, many=True)
    return Response({
        'count': total,
        'page': page,
        'page_size': page_size,
        'results': serializer.data,
    }, status=status.HTTP_200_OK)
