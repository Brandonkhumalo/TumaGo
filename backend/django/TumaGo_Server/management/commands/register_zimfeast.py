"""
One-time management command to register ZimFeast as a TumaGo partner.

Skips the $15 Paynow setup fee — marks the account as paid and generates
live API credentials immediately.

Usage (on the deployed TumaGo server):
    python manage.py register_zimfeast

If the partner already exists (by email), it prints the existing credentials
without creating a duplicate.
"""
import secrets
from decimal import Decimal

from django.core.management.base import BaseCommand
from django.contrib.auth.hashers import make_password

from TumaGo_Server.models import PartnerCompany, PartnerTransaction


# ── ZimFeast registration details ──────────────────────────────────────

PARTNER_NAME = "ZimFeast"
CONTACT_EMAIL = "brandon@tishanyq.co.zw"
PASSWORD = "ZimFeast@2026"  # Change after first login
PHONE = "+263 78 853 9918"
DESCRIPTION = "Fast food delivery system created for Zimbabwe"
ADDRESS = "7 Martin Drive, Msasa, Harare, Zimbabwe"
CITY = "Harare"
CONTACT_PERSON_NAME = "Brandon"
CONTACT_PERSON_ROLE = "System for Tishanyq Digital"
WEBHOOK_URL = "https://zimfeast.com/api/webhooks/tumago/"
INITIAL_BALANCE = Decimal("100.00")  # Starting balance so deliveries work immediately


class Command(BaseCommand):
    help = "Register ZimFeast as a TumaGo delivery partner (skips setup fee)"

    def handle(self, *args, **options):
        # Check if ZimFeast is already registered
        existing = PartnerCompany.objects.filter(contact_email=CONTACT_EMAIL).first()
        if existing:
            self.stdout.write(self.style.WARNING("\nZimFeast is already registered.\n"))
            self._print_credentials(existing)
            return

        # Generate live API credentials (same format as post-payment activation)
        api_key = f"tg_live_{secrets.token_hex(24)}"
        api_secret = f"sk_{secrets.token_hex(32)}"

        partner = PartnerCompany.objects.create(
            name=PARTNER_NAME,
            contact_email=CONTACT_EMAIL,
            password=make_password(PASSWORD),
            phone_number=PHONE,
            description=DESCRIPTION,
            address=ADDRESS,
            city=CITY,
            contact_person_name=CONTACT_PERSON_NAME,
            contact_person_role=CONTACT_PERSON_ROLE,
            api_key=api_key,
            api_secret=api_secret,
            webhook_url=WEBHOOK_URL,
            is_active=True,
            setup_fee_paid=True,          # ← skip the $15 Paynow payment
            balance=INITIAL_BALANCE,
            commission_rate=Decimal("20.00"),
        )

        # Record the waived setup fee so the transaction log is clean
        PartnerTransaction.objects.create(
            partner=partner,
            transaction_type="setup_fee",
            amount=Decimal("0.00"),
            balance_after=partner.balance,
            description="Setup fee waived — internal partner (ZimFeast)",
        )

        # Record the initial balance deposit
        PartnerTransaction.objects.create(
            partner=partner,
            transaction_type="deposit",
            amount=INITIAL_BALANCE,
            balance_after=partner.balance,
            description="Initial balance deposit — internal partner setup",
        )

        self.stdout.write(self.style.SUCCESS("\nZimFeast registered successfully!\n"))
        self._print_credentials(partner)

    def _print_credentials(self, partner):
        border = "=" * 60
        self.stdout.write(f"""
{border}
  ZIMFEAST — TUMAGO PARTNER CREDENTIALS
{border}

  Partner ID:       {partner.id}
  Company:          {partner.name}
  Email:            {partner.contact_email}
  Phone:            {partner.phone_number}

  Portal Login:     https://tumago.co.zw/partner/dashboard
  Email:            {partner.contact_email}
  Password:         {PASSWORD}

  API Key:          {partner.api_key}
  API Secret:       {partner.api_secret}
  Webhook URL:      {partner.webhook_url}

  Balance:          ${float(partner.balance):.2f}
  Commission Rate:  {float(partner.commission_rate)}%
  Setup Fee Paid:   {partner.setup_fee_paid}
  Active:           {partner.is_active}

{border}

  PUT THESE IN ZIMFEAST's .env FILE:

  TUMAGO_API_URL=https://tumago.co.zw/api/v1
  TUMAGO_API_KEY={partner.api_key}
  TUMAGO_API_SECRET={partner.api_secret}

{border}
""")
