"""
Email sending via AWS SES using boto3.

Checks the SESSuppressionList before every send — skips bounced/complained addresses.
All emails are sent from the verified domain tishanyq.co.zw.

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

import boto3
from botocore.exceptions import ClientError
from django.conf import settings

from TumaGo_Server.models import SESSuppressionList

logger = logging.getLogger(__name__)

# boto3 SES client — initialized lazily on first use
_ses_client = None


def _get_ses_client():
    """Create the SES client once and reuse it. boto3 handles connection pooling internally."""
    global _ses_client
    if _ses_client is None:
        _ses_client = boto3.client(
            "ses",
            region_name=settings.AWS_SES_REGION,
            aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
        )
    return _ses_client


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
    """Send a single email via SES. Returns True on success, False on failure.

    Args:
        to: Recipient email address
        subject: Email subject line
        body_text: Plain text body (required — fallback for email clients that don't render HTML)
        body_html: HTML body (optional — rich formatted email)
        from_email: Sender address (defaults to SES_FROM_EMAIL setting)
    """
    to = to.strip().lower()

    # Check suppression list first — never send to bounced/complained addresses
    if is_suppressed(to):
        logger.warning(f"Email to {to} skipped — address is on suppression list")
        return False

    sender = from_email or settings.SES_FROM_EMAIL

    # Build the email message
    body = {"Text": {"Charset": "UTF-8", "Data": body_text}}
    if body_html:
        body["Html"] = {"Charset": "UTF-8", "Data": body_html}

    try:
        client = _get_ses_client()
        response = client.send_email(
            Source=sender,
            Destination={"ToAddresses": [to]},
            Message={
                "Subject": {"Charset": "UTF-8", "Data": subject},
                "Body": body,
            },
        )
        message_id = response.get("MessageId", "unknown")
        logger.info(f"Email sent to {to} — MessageId: {message_id}")
        return True

    except ClientError as e:
        error_code = e.response["Error"]["Code"]
        error_msg = e.response["Error"]["Message"]
        logger.error(f"SES error sending to {to}: [{error_code}] {error_msg}")
        return False
    except Exception as e:
        logger.error(f"Unexpected error sending email to {to}: {e}")
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
