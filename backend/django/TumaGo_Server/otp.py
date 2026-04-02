"""
OTP (One-Time Password) utilities for Android app registrations.

OTP is ONLY required for Android app registrations (client + driver).
WhatsApp registrations skip OTP — the phone number is already verified by WhatsApp.

Flow:
1. Android app calls POST /api/v1/otp/send/ with phone_number
2. Backend generates 6-digit OTP, stores in Redis (10min TTL), sends via WhatsApp template
3. Android app calls POST /api/v1/otp/verify/ with phone_number + otp
4. Backend verifies, stores verification flag in Redis (15min TTL)
5. Android app calls POST /api/v1/signup/ or /api/v1/driver/signup/ — backend checks verification flag
"""
import logging
import secrets
import re

import requests
from django.conf import settings
from django.core.cache import cache

logger = logging.getLogger(__name__)

# OTP valid for 10 minutes
OTP_TTL_SECONDS = 600

# After OTP is verified, the verification flag lasts 15 minutes
# (enough time for the user to complete the signup form)
VERIFICATION_TTL_SECONDS = 900

# Max OTP attempts before lockout
MAX_OTP_ATTEMPTS = 5

# Lockout duration after too many failed attempts (30 minutes)
LOCKOUT_TTL_SECONDS = 1800


def _cache_key_otp(phone: str) -> str:
    return f"otp:{phone}"


def _cache_key_attempts(phone: str) -> str:
    return f"otp_attempts:{phone}"


def _cache_key_verified(phone: str) -> str:
    return f"otp_verified:{phone}"


def normalize_phone(phone: str) -> str:
    """
    Normalize Zimbabwe phone numbers to international format (263XXXXXXXXX).
    Accepts: 0771234567, +263771234567, 263771234567
    """
    phone = re.sub(r'[^\d+]', '', phone)  # strip spaces, dashes
    if phone.startswith('+'):
        phone = phone[1:]
    if phone.startswith('0'):
        phone = '263' + phone[1:]
    return phone


def generate_otp(phone: str) -> str:
    """
    Generate a 6-digit OTP and store it in Redis with a 10-minute TTL.
    Returns the OTP string.
    """
    phone = normalize_phone(phone)

    # Generate a cryptographically secure 6-digit code
    otp = f"{secrets.randbelow(900000) + 100000}"

    # Store in Redis cache with 10-minute expiry
    cache.set(_cache_key_otp(phone), otp, timeout=OTP_TTL_SECONDS)

    # Reset attempt counter when a new OTP is generated
    cache.delete(_cache_key_attempts(phone))

    logger.info("OTP generated for %s", phone)
    return otp


def verify_otp(phone: str, otp: str) -> tuple[bool, str]:
    """
    Verify the OTP for a phone number.

    Returns:
        (success: bool, message: str)
    """
    phone = normalize_phone(phone)

    # Check lockout
    attempts = cache.get(_cache_key_attempts(phone), 0)
    if attempts >= MAX_OTP_ATTEMPTS:
        return False, "Too many failed attempts. Try again in 30 minutes."

    # Get stored OTP
    stored_otp = cache.get(_cache_key_otp(phone))
    if stored_otp is None:
        return False, "OTP expired or not found. Request a new one."

    # Compare (constant-time comparison to prevent timing attacks)
    if not secrets.compare_digest(str(stored_otp), str(otp)):
        # Increment failed attempts
        cache.set(
            _cache_key_attempts(phone),
            attempts + 1,
            timeout=LOCKOUT_TTL_SECONDS,
        )
        remaining = MAX_OTP_ATTEMPTS - (attempts + 1)
        return False, f"Invalid OTP. {remaining} attempts remaining."

    # OTP is valid — set verification flag and clean up
    cache.set(_cache_key_verified(phone), True, timeout=VERIFICATION_TTL_SECONDS)
    cache.delete(_cache_key_otp(phone))
    cache.delete(_cache_key_attempts(phone))

    logger.info("OTP verified for %s", phone)
    return True, "Phone number verified."


def is_phone_verified(phone: str) -> bool:
    """Check if a phone number has been verified via OTP."""
    phone = normalize_phone(phone)
    return cache.get(_cache_key_verified(phone)) is True


def clear_verification(phone: str):
    """Clear the verification flag after successful signup."""
    phone = normalize_phone(phone)
    cache.delete(_cache_key_verified(phone))


def send_otp_email(email: str, otp: str) -> bool:
    """
    Send the OTP to the user via email using Resend.

    Args:
        email: Email address to send the OTP to
        otp: The 6-digit OTP code
    """
    from TumaGo_Server.views.EmailViews.emailService import send_email

    subject = "TumaGo — Your Verification Code"
    body_text = f"Your TumaGo verification code is: {otp}\n\nThis code expires in 10 minutes."
    body_html = f"""
    <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px;">
        <h2 style="color: #0e74bc;">TumaGo</h2>
        <p>Your verification code is:</p>
        <div style="background: #EFF8FE; padding: 16px; border-radius: 8px; text-align: center; margin: 16px 0;">
            <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #0e74bc;">{otp}</span>
        </div>
        <p style="color: #666; font-size: 14px;">This code expires in 10 minutes. If you didn't request this, you can safely ignore this email.</p>
    </div>
    """

    try:
        return send_email(to=email, subject=subject, body_text=body_text, body_html=body_html)
    except Exception as e:
        logger.error("Failed to send OTP email to %s: %s", email, e)
        return False


def generate_email_otp(email: str) -> str:
    """Generate a 6-digit OTP for email verification and store in Redis."""
    email = email.strip().lower()
    otp = f"{secrets.randbelow(900000) + 100000}"
    cache.set(f"email_otp:{email}", otp, timeout=OTP_TTL_SECONDS)
    cache.delete(f"email_otp_attempts:{email}")
    logger.info("Email OTP generated for %s", email)
    return otp


def verify_email_otp(email: str, otp: str) -> tuple[bool, str]:
    """Verify the OTP sent to an email address."""
    email = email.strip().lower()

    attempts = cache.get(f"email_otp_attempts:{email}", 0)
    if attempts >= MAX_OTP_ATTEMPTS:
        return False, "Too many failed attempts. Try again in 30 minutes."

    stored_otp = cache.get(f"email_otp:{email}")
    if stored_otp is None:
        return False, "OTP expired or not found. Request a new one."

    if not secrets.compare_digest(str(stored_otp), str(otp)):
        cache.set(f"email_otp_attempts:{email}", attempts + 1, timeout=LOCKOUT_TTL_SECONDS)
        remaining = MAX_OTP_ATTEMPTS - (attempts + 1)
        return False, f"Invalid OTP. {remaining} attempts remaining."

    # OTP valid — set verification flag and clean up
    cache.set(f"email_verified:{email}", True, timeout=VERIFICATION_TTL_SECONDS)
    cache.delete(f"email_otp:{email}")
    cache.delete(f"email_otp_attempts:{email}")
    logger.info("Email OTP verified for %s", email)
    return True, "Email verified."


def is_email_verified(email: str) -> bool:
    """Check if an email has been verified via OTP."""
    email = email.strip().lower()
    return cache.get(f"email_verified:{email}") is True


def clear_email_verification(email: str):
    """Clear the email verification flag after successful signup."""
    email = email.strip().lower()
    cache.delete(f"email_verified:{email}")


def send_otp_whatsapp(phone: str, otp: str, template_name: str = "client_otp"):
    """
    Send the OTP to the user via WhatsApp template message.

    Uses the WhatsApp Cloud API directly (sync) since Django views are synchronous.
    The template has one variable: {{1}} = the OTP code.

    Args:
        phone: Phone number in any format (will be normalized)
        otp: The 6-digit OTP code
        template_name: "client_otp" for clients, "driver_otp" for drivers
    """
    phone = normalize_phone(phone)

    # WhatsApp API config from environment
    from decouple import config
    access_token = config("WHATSAPP_ACCESS_TOKEN", default="")
    phone_number_id = config("WHATSAPP_PHONE_NUMBER_ID", default="")
    api_version = config("WHATSAPP_API_VERSION", default="v21.0")

    if not access_token or not phone_number_id:
        logger.error("WhatsApp API credentials not configured — OTP not sent to %s", phone)
        return False

    url = f"https://graph.facebook.com/{api_version}/{phone_number_id}/messages"

    payload = {
        "messaging_product": "whatsapp",
        "to": phone,
        "type": "template",
        "template": {
            "name": template_name,
            "language": {"code": "en"},
            "components": [
                {
                    "type": "body",
                    "parameters": [
                        {"type": "text", "text": otp},
                    ],
                }
            ],
        },
    }

    try:
        resp = requests.post(
            url,
            json=payload,
            headers={
                "Authorization": f"Bearer {access_token}",
                "Content-Type": "application/json",
            },
            timeout=15,
        )
        resp.raise_for_status()
        logger.info("OTP WhatsApp sent to %s: %s", phone, resp.json())
        return True
    except requests.RequestException as e:
        logger.error("Failed to send OTP WhatsApp to %s: %s", phone, e)
        return False
