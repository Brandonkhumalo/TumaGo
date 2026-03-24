"""
SNS Notification Endpoint — handles AWS SNS subscription confirmation
and SES bounce/complaint notifications.

AWS sends 3 types of POST requests:
1. SubscriptionConfirmation — first request, must GET the SubscribeURL to confirm
2. Notification (Bounce)    — a sent email bounced, add to suppression list
3. Notification (Complaint) — recipient marked email as spam, add to suppression list

Endpoint: POST /api/v1/ses/notifications/
"""
import json
import logging

import requests
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_POST

from TumaGo_Server.models import SESSuppressionList

logger = logging.getLogger(__name__)


@csrf_exempt  # SNS sends raw POSTs without CSRF tokens
@require_POST
def ses_notifications(request):
    """Single endpoint for both SNS subscription confirmation and SES notifications.
    SNS sends JSON with Content-Type: text/plain, so we parse the body directly."""
    try:
        payload = json.loads(request.body)
    except (json.JSONDecodeError, ValueError):
        return JsonResponse({"error": "Invalid JSON"}, status=400)

    message_type = request.headers.get("x-amz-sns-message-type", "")

    # --- Step 1: Subscription Confirmation ---
    # AWS sends this once when you create the SNS subscription.
    # We must GET the SubscribeURL to confirm. Until confirmed, no notifications arrive.
    if message_type == "SubscriptionConfirmation":
        subscribe_url = payload.get("SubscribeURL")
        if not subscribe_url:
            logger.error("SubscriptionConfirmation missing SubscribeURL")
            return JsonResponse({"error": "Missing SubscribeURL"}, status=400)

        # Confirm the subscription by visiting the URL AWS provided
        try:
            resp = requests.get(subscribe_url, timeout=10)
            resp.raise_for_status()
            logger.info(f"SNS subscription confirmed: {payload.get('TopicArn')}")
            return JsonResponse({"status": "subscription_confirmed"})
        except requests.RequestException as e:
            logger.error(f"Failed to confirm SNS subscription: {e}")
            return JsonResponse({"error": "Confirmation failed"}, status=500)

    # --- Step 2: Handle Bounce/Complaint Notifications ---
    if message_type == "Notification":
        try:
            # SNS wraps the SES notification inside a "Message" field as a JSON string
            message = json.loads(payload.get("Message", "{}"))
        except (json.JSONDecodeError, ValueError):
            logger.error("Failed to parse SNS Message field")
            return JsonResponse({"error": "Invalid Message JSON"}, status=400)

        notification_type = message.get("notificationType")

        if notification_type == "Bounce":
            _handle_bounce(message)
        elif notification_type == "Complaint":
            _handle_complaint(message)
        else:
            logger.warning(f"Unknown SES notification type: {notification_type}")

        return JsonResponse({"status": "processed"})

    logger.warning(f"Unknown SNS message type: {message_type}")
    return JsonResponse({"status": "ignored"})


def _handle_bounce(message: dict):
    """Process a bounce notification. Hard bounces = permanent, must suppress.
    Transient bounces = temporary (mailbox full), log but don't suppress immediately."""
    bounce = message.get("bounce", {})
    bounce_type = bounce.get("bounceType", "")  # "Permanent" or "Transient"

    for recipient in bounce.get("bouncedRecipients", []):
        email = recipient.get("emailAddress", "").lower()
        diagnostic = recipient.get("diagnosticCode", "")

        if not email:
            continue

        # Only suppress on permanent (hard) bounces — these will never work.
        # Transient bounces (mailbox full) can resolve themselves.
        if bounce_type == "Permanent":
            SESSuppressionList.objects.update_or_create(
                email=email,
                defaults={
                    "reason": SESSuppressionList.BOUNCE,
                    "bounce_type": bounce_type,
                    "diagnostic": diagnostic,
                },
            )
            logger.info(f"Suppressed (hard bounce): {email}")
        else:
            logger.info(f"Transient bounce (not suppressed): {email} — {diagnostic}")


def _handle_complaint(message: dict):
    """Process a complaint notification. Someone marked the email as spam.
    Always suppress — SES suspends accounts above 0.1% complaint rate."""
    complaint = message.get("complaint", {})

    for recipient in complaint.get("complainedRecipients", []):
        email = recipient.get("emailAddress", "").lower()

        if not email:
            continue

        SESSuppressionList.objects.update_or_create(
            email=email,
            defaults={
                "reason": SESSuppressionList.COMPLAINT,
                "bounce_type": "",
                "diagnostic": complaint.get("complaintFeedbackType", ""),
            },
        )
        logger.info(f"Suppressed (complaint): {email}")
