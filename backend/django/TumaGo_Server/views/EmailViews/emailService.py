"""
Email sending via Resend.

Checks the SESSuppressionList before every send — skips bounced/complained addresses.
(Model kept for suppression tracking, works the same way with Resend webhooks.)

Usage:
    from TumaGo_Server.views.EmailViews.emailService import send_email

    success = send_email(
        to="user@example.com",
        subject="Your delivery is confirmed",
        body_text="Your package is on the way!",
        body_html="<h1>Your package is on the way!</h1>",
    )
"""
import logging

import dramatiq
import resend
from django.conf import settings

from TumaGo_Server.models import SESSuppressionList

logger = logging.getLogger(__name__)

# Configure Resend API key on module load
resend.api_key = settings.RESEND_API_KEY


def is_suppressed(email: str) -> bool:
    """Check if an email is on the suppression list (bounced or complained)."""
    return SESSuppressionList.objects.filter(email=email.lower()).exists()


def send_email(
    to: str,
    subject: str,
    body_text: str,
    body_html: str = "",
    from_email: str = "",
) -> bool:
    """Send a single email via Resend. Returns True on success, False on failure.

    Args:
        to: Recipient email address
        subject: Email subject line
        body_text: Plain text body (required — fallback for email clients that don't render HTML)
        body_html: HTML body (optional — rich formatted email)
        from_email: Sender address (defaults to RESEND_FROM_EMAIL setting)
    """
    to = to.strip().lower()

    # Check suppression list first — never send to bounced/complained addresses
    if is_suppressed(to):
        logger.warning(f"Email to {to} skipped — address is on suppression list")
        return False

    sender = from_email or settings.RESEND_FROM_EMAIL

    # Build the email params ("from" is a reserved keyword in Python but works as a dict key)
    params = {
        "from": sender,
        "to": [to],
        "subject": subject,
        "text": body_text,
    }
    if body_html:
        params["html"] = body_html

    try:
        response = resend.Emails.send(params)
        email_id = response.get("id", "unknown") if isinstance(response, dict) else getattr(response, "id", "unknown")
        logger.info(f"Email sent to {to} — Resend ID: {email_id}")
        return True

    except Exception as e:
        logger.error(f"Resend error sending to {to}: {e}")
        return False


def send_email_bulk(
    recipients: list[str],
    subject: str,
    body_text: str,
    body_html: str = "",
    from_email: str = "",
) -> dict:
    """Send the same email to multiple recipients, skipping suppressed addresses.

    Returns:
        {"sent": ["a@b.com"], "suppressed": ["c@d.com"], "failed": ["e@f.com"]}
    """
    results = {"sent": [], "suppressed": [], "failed": []}

    for email in recipients:
        email = email.strip().lower()
        if is_suppressed(email):
            results["suppressed"].append(email)
            continue

        if send_email(to=email, subject=subject, body_text=body_text,
                      body_html=body_html, from_email=from_email):
            results["sent"].append(email)
        else:
            results["failed"].append(email)

    return results


@dramatiq.actor(max_retries=3, min_backoff=5000, max_backoff=60000)
def send_email_async(to: str, subject: str, body_text: str, body_html: str = "", from_email: str = ""):
    """Dramatiq task — sends email with automatic retries on failure.

    Use this for non-critical emails (welcome, delivery receipts) where
    a small delay is acceptable but delivery must be guaranteed.
    """
    send_email(to=to, subject=subject, body_text=body_text, body_html=body_html, from_email=from_email)
