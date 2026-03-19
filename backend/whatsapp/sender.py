"""
WhatsApp Cloud API message sender.

Sends template messages and free-form text/interactive messages
via the Meta Cloud API.
"""
import logging
from typing import Optional

import httpx

from config import WHATSAPP_API_URL, WHATSAPP_ACCESS_TOKEN

logger = logging.getLogger(__name__)

_client: Optional[httpx.AsyncClient] = None


def get_client() -> httpx.AsyncClient:
    global _client
    if _client is None or _client.is_closed:
        _client = httpx.AsyncClient(
            timeout=15.0,
            headers={
                "Authorization": f"Bearer {WHATSAPP_ACCESS_TOKEN}",
                "Content-Type": "application/json",
            },
        )
    return _client


async def close_client():
    global _client
    if _client and not _client.is_closed:
        await _client.aclose()
        _client = None


async def send_template(
    to: str,
    template_name: str,
    body_params: list[str] | None = None,
    button_params: list[dict] | None = None,
    language: str = "en",
) -> dict:
    """
    Send a pre-approved WhatsApp template message.

    Args:
        to: Recipient phone number with country code (e.g., "263771234567")
        template_name: Name of the approved template
        body_params: List of string values for {{1}}, {{2}}, etc.
        button_params: List of button parameter dicts for CTA/Quick Reply buttons
        language: Template language code
    """
    components = []

    if body_params:
        components.append({
            "type": "body",
            "parameters": [{"type": "text", "text": p} for p in body_params],
        })

    if button_params:
        components.extend(button_params)

    payload = {
        "messaging_product": "whatsapp",
        "to": to,
        "type": "template",
        "template": {
            "name": template_name,
            "language": {"code": language},
        },
    }

    if components:
        payload["template"]["components"] = components

    return await _send(payload)


async def send_text(to: str, text: str) -> dict:
    """Send a free-form text message (only within 24h conversation window)."""
    payload = {
        "messaging_product": "whatsapp",
        "to": to,
        "type": "text",
        "text": {"body": text},
    }
    return await _send(payload)


async def send_interactive_buttons(
    to: str,
    body: str,
    buttons: list[dict],
    header: str | None = None,
    footer: str | None = None,
) -> dict:
    """
    Send an interactive message with up to 3 buttons.
    Used within 24h window (no template needed).

    buttons: [{"id": "accept", "title": "Accept"}, ...]
    """
    action_buttons = [
        {"type": "reply", "reply": {"id": b["id"], "title": b["title"]}}
        for b in buttons
    ]

    interactive = {
        "type": "button",
        "body": {"text": body},
        "action": {"buttons": action_buttons},
    }

    if header:
        interactive["header"] = {"type": "text", "text": header}
    if footer:
        interactive["footer"] = {"text": footer}

    payload = {
        "messaging_product": "whatsapp",
        "to": to,
        "type": "interactive",
        "interactive": interactive,
    }
    return await _send(payload)


async def send_interactive_list(
    to: str,
    body: str,
    button_text: str,
    sections: list[dict],
    header: str | None = None,
    footer: str | None = None,
) -> dict:
    """
    Send an interactive list message (up to 10 rows per section).
    Used within 24h window.

    sections: [{"title": "Vehicles", "rows": [{"id": "scooter", "title": "Scooter", "description": "$2.50"}]}]
    """
    interactive = {
        "type": "list",
        "body": {"text": body},
        "action": {"button": button_text, "sections": sections},
    }

    if header:
        interactive["header"] = {"type": "text", "text": header}
    if footer:
        interactive["footer"] = {"text": footer}

    payload = {
        "messaging_product": "whatsapp",
        "to": to,
        "type": "interactive",
        "interactive": interactive,
    }
    return await _send(payload)


async def download_media(media_id: str) -> bytes | None:
    """
    Download media from WhatsApp (for driver document uploads).
    Two-step: get media URL, then download the file.
    """
    client = get_client()
    try:
        # Step 1: Get the media URL
        url_resp = await client.get(
            f"https://graph.facebook.com/v21.0/{media_id}",
        )
        url_resp.raise_for_status()
        media_url = url_resp.json().get("url")

        if not media_url:
            return None

        # Step 2: Download the actual file
        file_resp = await client.get(media_url)
        file_resp.raise_for_status()
        return file_resp.content
    except Exception as e:
        logger.error(f"Media download error for {media_id}: {e}")
        return None


async def _send(payload: dict) -> dict:
    """Send a message payload to the WhatsApp Cloud API."""
    client = get_client()
    try:
        resp = await client.post(WHATSAPP_API_URL, json=payload)
        resp.raise_for_status()
        result = resp.json()
        logger.info(f"WhatsApp sent to {payload.get('to')}: {result}")
        return result
    except httpx.HTTPStatusError as e:
        logger.error(f"WhatsApp API error {e.response.status_code}: {e.response.text}")
        return {"error": e.response.text}
    except Exception as e:
        logger.error(f"WhatsApp send error: {e}")
        return {"error": str(e)}
