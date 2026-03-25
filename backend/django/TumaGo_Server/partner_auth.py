"""
Partner authentication — two modes:

1. PartnerAPIKeyAuthentication — for B2B delivery API (X-API-Key header).
2. PartnerJWTAuthentication  — for partner portal (Bearer JWT with partner_id claim).
"""
import jwt as pyjwt
from datetime import datetime, timedelta
from django.conf import settings
from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed

from .models import PartnerCompany


# ---------------------------------------------------------------------------
# 1. API Key auth — used by delivery endpoints
# ---------------------------------------------------------------------------

class PartnerAPIKeyAuthentication(BaseAuthentication):

    def authenticate(self, request):
        api_key = request.headers.get("X-API-Key")
        if not api_key:
            return None  # Let other auth classes try (e.g. JWT)

        try:
            partner = PartnerCompany.objects.get(api_key=api_key)
        except PartnerCompany.DoesNotExist:
            raise AuthenticationFailed("Invalid API key.")

        if not partner.is_active:
            raise AuthenticationFailed("Partner account is deactivated.")

        # request.user = None, request.auth = partner
        return (None, partner)


# ---------------------------------------------------------------------------
# 2. JWT auth — used by partner portal (dashboard, device management)
# ---------------------------------------------------------------------------

class PartnerJWTAuthentication(BaseAuthentication):
    """Authenticates partners via Bearer JWT containing a partner_id claim.

    On success: request.user = None, request.auth = PartnerCompany instance.
    """

    @staticmethod
    def generate_token(partner: PartnerCompany) -> str:
        """Create a 30-day access token for the partner portal."""
        payload = {
            "partner_id": str(partner.id),
            "type": "partner_access",
            "exp": int((datetime.utcnow() + timedelta(days=30)).timestamp()),
        }
        return pyjwt.encode(payload, key=settings.SECRET_KEY, algorithm="HS256")

    def authenticate(self, request):
        auth_header = request.headers.get("Authorization")
        if not auth_header or not auth_header.startswith("Bearer "):
            return None

        token = auth_header.split(" ", 1)[1]

        try:
            payload = pyjwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        except pyjwt.ExpiredSignatureError:
            raise AuthenticationFailed("Token has expired.")
        except pyjwt.InvalidTokenError:
            raise AuthenticationFailed("Invalid token.")

        # Only handle partner tokens — let user JWT auth handle the rest
        if payload.get("type") != "partner_access":
            return None

        partner_id = payload.get("partner_id")
        if not partner_id:
            raise AuthenticationFailed("Token missing partner_id.")

        try:
            partner = PartnerCompany.objects.get(id=partner_id)
        except PartnerCompany.DoesNotExist:
            raise AuthenticationFailed("Partner not found.")

        if not partner.is_active:
            raise AuthenticationFailed("Partner account is deactivated.")

        return (None, partner)
