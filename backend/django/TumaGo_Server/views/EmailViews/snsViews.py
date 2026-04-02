"""
Resend Webhook Endpoint — handles email bounce and complaint notifications.

Resend uses Svix under the hood for webhook delivery. Each request includes:
- svix-id: unique message ID
- svix-timestamp: unix timestamp
- svix-signature: HMAC-SHA256 signature(s) prefixed with "v1,"

We verify the signature, then add bounced/complained addresses to the
suppression list so we never email them again.

Endpoint: POST /api/v1/email/webhooks/
"""
import base64
import hashlib
import hmac
import json
import logging
import time

from django.conf import settings
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_POST

from TumaGo_Server.models import SESSuppressionList

logger = logging.getLogger(__name__)

# Max age of a webhook before we reject it (5 minutes) — prevents replay attacks
TIMESTAMP_TOLERANCE_SECONDS = 300


def _verify_webhook(payload: bytes, headers: dict) -> bool:
    """Verify the Resend/Svix webhook signature.

    The signing secret (from Resend dashboard) starts with "whsec_".
    We strip the prefix, base64-decode it, then HMAC-SHA256 the signed content:
        "{svix-id}.{svix-timestamp}.{body}"
    and compare against the signature(s) in the svix-signature header.
    """
    secret = settings.RESEND_WEBHOOK_SECRET
    if not secret:
        logger.warning("RESEND_WEBHOOK_SECRET not set — skipping signature verification")
        return True

    svix_id = headers.get("svix-id", "")
    svix_timestamp = headers.get("svix-timestamp", "")
    svix_signature = headers.get("svix-signature", "")

    if not svix_id or not svix_timestamp or not svix_signature:
        logger.error("Missing Svix headers — rejecting webhook")
        return False

    # Replay protection — reject timestamps older than 5 minutes
    try:
        ts = int(svix_timestamp)
        if abs(time.time() - ts) > TIMESTAMP_TOLERANCE_SECONDS:
            logger.error("Webhook timestamp too old — possible replay attack")
            return False
    except ValueError:
        logger.error("Invalid svix-timestamp header")
        return False

    # Decode the signing secret (strip "whsec_" prefix, base64-decode)
    secret_bytes = base64.b64decode(secret.removeprefix("whsec_"))

    # Build the signed content: "{svix-id}.{svix-timestamp}.{raw_body}"
    to_sign = f"{svix_id}.{svix_timestamp}.{payload.decode()}".encode()
    expected = base64.b64encode(
        hmac.new(secret_bytes, to_sign, hashlib.sha256).digest()
    ).decode()

    # The header may contain multiple signatures separated by spaces, each prefixed with "v1,"
    signatures = [
        s.removeprefix("v1,")
        for s in svix_signature.split(" ")
        if s.startswith("v1,")
    ]

    if not any(hmac.compare_digest(expected, sig) for sig in signatures):
        logger.error("Invalid webhook signature — rejecting")
        return False

    return True


@csrf_exempt
@require_POST
def resend_webhooks(request):
    """Handle Resend webhook events for bounces and complaints."""
    # Verify signature before processing
    headers = {
        "svix-id": request.headers.get("svix-id", ""),
        "svix-timestamp": request.headers.get("svix-timestamp", ""),
        "svix-signature": request.headers.get("svix-signature", ""),
    }
    if not _verify_webhook(request.body, headers):
        return JsonResponse({"error": "Invalid signature"}, status=401)

    # Parse the webhook payload
    try:
        payload = json.loads(request.body)
    except (json.JSONDecodeError, ValueError):
        return JsonResponse({"error": "Invalid JSON"}, status=400)

    event_type = payload.get("type", "")

    if event_type == "email.bounced":
        _handle_bounce(payload.get("data", {}))
    elif event_type == "email.complained":
        _handle_complaint(payload.get("data", {}))
    else:
        logger.debug(f"Resend webhook event ignored: {event_type}")

    return JsonResponse({"status": "processed"})


def _handle_bounce(data: dict):
    """Process a bounce event. Add the recipient to the suppression list."""
    to_list = data.get("to", [])
    email_id = data.get("email_id", "")

    for email in to_list:
        email = email.strip().lower()
        if not email:
            continue

        SESSuppressionList.objects.update_or_create(
            email=email,
            defaults={
                "reason": SESSuppressionList.BOUNCE,
                "bounce_type": "Permanent",
                "diagnostic": f"Resend bounce — email_id: {email_id}",
            },
        )
        logger.info(f"Suppressed (bounce): {email}")


def _handle_complaint(data: dict):
    """Process a complaint event. Recipient marked the email as spam."""
    to_list = data.get("to", [])
    email_id = data.get("email_id", "")

    for email in to_list:
        email = email.strip().lower()
        if not email:
            continue

        SESSuppressionList.objects.update_or_create(
            email=email,
            defaults={
                "reason": SESSuppressionList.COMPLAINT,
                "bounce_type": "",
                "diagnostic": f"Resend complaint — email_id: {email_id}",
            },
        )
        logger.info(f"Suppressed (complaint): {email}")
