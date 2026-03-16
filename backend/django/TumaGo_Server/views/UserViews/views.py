from django.conf import settings
from django.contrib.auth import get_user_model
from rest_framework.response import Response
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes, throttle_classes
from rest_framework.throttling import AnonRateThrottle
from django.shortcuts import get_object_or_404
from rest_framework.permissions import AllowAny, IsAuthenticated
import logging
from ...models import BlacklistedToken, Delivery, TermsAndConditions

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
from datetime import datetime
import pytz
import jwt

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
    serializer = SignupSerializer(data=request.data)
    if serializer.is_valid():
        user = serializer.save()
        payload = {"id": str(user.id)}  # UUID must be cast to str

        access_token = JWTAuthentication.generate_token(payload=payload)
        refresh_token = JWTAuthentication.generate_refresh_token(payload=payload)

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
@permission_classes([IsAuthenticated])
def VerifyToken(request):
    return Response(status=status.HTTP_200_OK)

@api_view(["GET"])
@permission_classes([AllowAny])
def sync_time(request):
    utc_now = datetime.utcnow().replace(tzinfo=pytz.UTC)
    return Response({"utc_time": utc_now.isoformat()})

@api_view(["GET"])
@permission_classes([IsAuthenticated])
def check_terms(request):
    user = request.user
    terms = TermsAndConditions.objects.filter(user=user, terms_and_conditions=True).first()

    if terms:
        return Response({"message": "Terms accepted."}, status=status.HTTP_200_OK)
    return Response({"message": "Terms not accepted."}, status=status.HTTP_403_FORBIDDEN)

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def accept_terms(request):
    user = request.user

    updated = TermsAndConditions.objects.filter(user=user).update(terms_and_conditions=True)
    if updated == 0:
        TermsAndConditions.objects.create(user=user, terms_and_conditions=True)

    return Response({"message": "Successfully agreed to the terms and conditions"}, status=status.HTTP_202_ACCEPTED)

# Admin/dev utilities
@api_view(['GET'])
@permission_classes([AllowAny])
def get_all_users(request):
    global User
    if User is None:
        User = get_user_model()
    users = User.objects.all()
    serializer = UserSerializer(users, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)

@api_view(['GET'])
@permission_classes([AllowAny])
def get_all_deliveries(request):
    deliveries = Delivery.objects.all()
    serializer = DeliverySerializer(deliveries, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)

@api_view(['DELETE'])
@permission_classes([AllowAny])
def delete_all_users(request):
    global User
    if User is None:
        User = get_user_model()
    User.objects.all().delete()
    return Response({"message": "All users deleted."}, status=status.HTTP_200_OK)
