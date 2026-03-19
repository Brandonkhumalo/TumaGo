"""
Client registration flow via WhatsApp.

Steps:
1. Collect personal details (name, surname, email, phone, address)
2. Set password
3. Call Django signup API
4. Confirm registration
"""
import logging
import re

from webhook_handler import IncomingMessage
from state import set_state, update_data, clear_state, get_state
from sender import send_text, send_template
from django_client import signup, login

logger = logging.getLogger(__name__)


async def start(msg: IncomingMessage):
    """Start client registration flow."""
    await set_state(msg.phone, "client_registration", "awaiting_details")
    await send_text(
        msg.phone,
        "Let's create your TumaGo account!\n\n"
        "Please send your details in this format:\n\n"
        "Name, Surname, Email, Phone, Street Address, City, Province, Postal Code\n\n"
        "Example:\n"
        "John, Moyo, john@email.com, 0771234567, 123 Main St, Harare, Harare, 00263\n\n"
        "Send 'cancel' to stop.",
    )


async def handle(msg: IncomingMessage, state: dict):
    """Handle messages within the client registration flow."""
    step = state["step"]

    if step == "awaiting_details":
        await _handle_details(msg, state)
    elif step == "awaiting_password":
        await _handle_password(msg, state)
    elif step == "confirm":
        await _handle_confirm(msg, state)


async def _handle_details(msg: IncomingMessage, state: dict):
    """Parse personal details from comma-separated message."""
    parts = [p.strip() for p in msg.text.split(",")]

    if len(parts) < 8:
        await send_text(
            msg.phone,
            "I need all 8 details separated by commas.\n\n"
            "Name, Surname, Email, Phone, Street Address, City, Province, Postal Code\n\n"
            "Example: John, Moyo, john@email.com, 0771234567, 123 Main St, Harare, Harare, 00263",
        )
        return

    name, surname, email, phone, street, city, province, postal = parts[:8]

    # Basic email validation
    if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
        await send_text(msg.phone, "That email doesn't look valid. Please try again with all 8 details.")
        return

    data = {
        "name": name,
        "surname": surname,
        "email": email,
        "phone_number": phone,
        "streetAdress": street,
        "city": city,
        "province": province,
        "postalCode": postal,
    }

    await set_state(msg.phone, "client_registration", "awaiting_password", data)
    await send_text(
        msg.phone,
        f"Details received:\n"
        f"Name: {name} {surname}\n"
        f"Email: {email}\n"
        f"Phone: {phone}\n"
        f"Address: {street}, {city}\n\n"
        "Now choose a password (at least 8 characters).\n"
        "Send your password:",
    )


async def _handle_password(msg: IncomingMessage, state: dict):
    """Receive password and create account."""
    password = msg.text.strip()

    if len(password) < 8:
        await send_text(msg.phone, "Password must be at least 8 characters. Try again:")
        return

    data = state["data"]
    data["password"] = password

    # Call Django signup API
    result = await signup(data)

    if result["status"] in (200, 201):
        await clear_state(msg.phone)

        # Store the JWT token in Redis for future API calls
        token = result["data"].get("token", "")
        if token:
            from state import get_redis
            r = await get_redis()
            await r.set(f"whatsapp:token:{msg.phone}", token, ex=86400 * 30)  # 30 days

        name = data.get("name", "")
        await send_text(
            msg.phone,
            f"Account created successfully, {name}! 🎉\n\n"
            "You can now request deliveries. Just send 'deliver' to get started.\n\n"
            "Or download our app for the full experience:\n"
            "https://play.google.com/store/apps/details?id=com.techmania.tumago",
        )
    else:
        error = result["data"].get("error", result["data"])
        await send_text(
            msg.phone,
            f"Registration failed: {error}\n\n"
            "Please try again or send 'cancel' to start over.",
        )
        # Reset to details step
        await set_state(msg.phone, "client_registration", "awaiting_details")
