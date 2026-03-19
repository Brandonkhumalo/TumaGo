"""
Parse incoming Meta WhatsApp webhook payloads.

Meta sends different message types: text, interactive (button replies),
image/document (media), and status updates.
"""
import logging
from dataclasses import dataclass, field
from typing import Optional

logger = logging.getLogger(__name__)


@dataclass
class IncomingMessage:
    """Parsed incoming WhatsApp message."""
    phone: str                          # Sender phone (e.g., "263771234567")
    name: str = ""                      # Sender's WhatsApp profile name
    message_id: str = ""                # WhatsApp message ID
    msg_type: str = ""                  # "text", "button", "list", "image", "document", "location"
    text: str = ""                      # Text content or button payload
    button_id: str = ""                 # Button reply ID (for interactive buttons)
    media_id: str = ""                  # Media ID (for images/documents)
    media_mime: str = ""                # MIME type of media
    latitude: float = 0.0              # Location latitude
    longitude: float = 0.0            # Location longitude
    context: dict = field(default_factory=dict)  # Reply context (quoted message)


def parse_webhook(payload: dict) -> list[IncomingMessage]:
    """
    Parse a Meta webhook payload and return a list of IncomingMessage objects.
    A single webhook can contain multiple messages.
    """
    messages = []

    for entry in payload.get("entry", []):
        for change in entry.get("changes", []):
            value = change.get("value", {})

            # Extract contact info
            contacts = {
                c.get("wa_id", ""): c.get("profile", {}).get("name", "")
                for c in value.get("contacts", [])
            }

            for msg in value.get("messages", []):
                phone = msg.get("from", "")
                parsed = IncomingMessage(
                    phone=phone,
                    name=contacts.get(phone, ""),
                    message_id=msg.get("id", ""),
                )

                msg_type = msg.get("type", "")

                if msg_type == "text":
                    parsed.msg_type = "text"
                    parsed.text = msg.get("text", {}).get("body", "").strip()

                elif msg_type == "interactive":
                    interactive = msg.get("interactive", {})
                    interactive_type = interactive.get("type", "")

                    if interactive_type == "button_reply":
                        parsed.msg_type = "button"
                        reply = interactive.get("button_reply", {})
                        parsed.button_id = reply.get("id", "")
                        parsed.text = reply.get("title", "")

                    elif interactive_type == "list_reply":
                        parsed.msg_type = "list"
                        reply = interactive.get("list_reply", {})
                        parsed.button_id = reply.get("id", "")
                        parsed.text = reply.get("title", "")

                elif msg_type == "image":
                    parsed.msg_type = "image"
                    image = msg.get("image", {})
                    parsed.media_id = image.get("id", "")
                    parsed.media_mime = image.get("mime_type", "")
                    parsed.text = image.get("caption", "")

                elif msg_type == "document":
                    parsed.msg_type = "document"
                    doc = msg.get("document", {})
                    parsed.media_id = doc.get("id", "")
                    parsed.media_mime = doc.get("mime_type", "")

                elif msg_type == "location":
                    parsed.msg_type = "location"
                    loc = msg.get("location", {})
                    parsed.latitude = loc.get("latitude", 0.0)
                    parsed.longitude = loc.get("longitude", 0.0)

                else:
                    parsed.msg_type = msg_type
                    logger.info(f"Unhandled message type: {msg_type}")

                # Context (if user replied to a specific message)
                if "context" in msg:
                    parsed.context = msg["context"]

                messages.append(parsed)

    return messages


def is_status_update(payload: dict) -> bool:
    """Check if the webhook payload is a status update (not a message)."""
    for entry in payload.get("entry", []):
        for change in entry.get("changes", []):
            value = change.get("value", {})
            if value.get("statuses"):
                return True
    return False
