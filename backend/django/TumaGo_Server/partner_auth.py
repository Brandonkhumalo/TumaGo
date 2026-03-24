"""
API key authentication for B2B partners.

Partners authenticate via the X-API-Key header instead of JWT.
The key is looked up against PartnerCompany.api_key.
On success, request.user is set to None and request.auth is the PartnerCompany instance.
"""
from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed

from .models import PartnerCompany


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
