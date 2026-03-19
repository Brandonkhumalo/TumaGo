"""
Conversation router — directs incoming messages to the correct flow handler.

Logic:
1. Check if user has an active conversation state in Redis
2. If yes → route to that flow's handler
3. If no → check for keywords to start a new flow
4. If unknown → send help message
"""
import logging

from webhook_handler import IncomingMessage
from state import get_state, clear_state
from sender import send_text, send_interactive_buttons
from conversations import (
    client_registration,
    driver_registration,
    delivery_request,
    driver_delivery,
)

logger = logging.getLogger(__name__)

# Map flow names to handler modules
FLOW_HANDLERS = {
    "client_registration": client_registration,
    "driver_registration": driver_registration,
    "delivery_request": delivery_request,
    "driver_delivery": driver_delivery,
}

# Keywords that trigger new flows (case-insensitive)
KEYWORDS = {
    "deliver": "delivery_request",
    "send": "delivery_request",
    "register": "client_registration",
    "signup": "client_registration",
    "register driver": "driver_registration",
    "driver": "driver_registration",
    "join": "driver_registration",
    "online": "driver_online",
    "offline": "driver_offline",
    "history": "delivery_history",
    "earnings": "driver_earnings",
    "help": "help",
    "hi": "greeting",
    "hello": "greeting",
    "cancel": "cancel",
}


async def route_message(msg: IncomingMessage):
    """Route an incoming message to the appropriate conversation handler."""
    phone = msg.phone
    text_lower = msg.text.lower().strip()

    # Check for cancel command first
    if text_lower == "cancel":
        await clear_state(phone)
        await send_text(phone, "Cancelled. Send 'help' to see what I can do.")
        return

    # Check if there's an active conversation flow
    state = await get_state(phone)
    if state:
        flow_name = state["flow"]
        handler = FLOW_HANDLERS.get(flow_name)
        if handler:
            await handler.handle(msg, state)
            return

    # No active flow — check keywords
    if text_lower in ("online", "go online"):
        await driver_delivery.go_online(msg)
        return

    if text_lower in ("offline", "go offline"):
        await driver_delivery.go_offline(msg)
        return

    if text_lower in ("history", "my deliveries"):
        await delivery_request.show_history(msg)
        return

    if text_lower in ("earnings", "my earnings"):
        await driver_delivery.show_earnings(msg)
        return

    # Check for keyword matches
    for keyword, flow in KEYWORDS.items():
        if text_lower == keyword or text_lower.startswith(keyword):
            if flow == "delivery_request":
                await delivery_request.start(msg)
                return
            elif flow == "client_registration":
                await client_registration.start(msg)
                return
            elif flow == "driver_registration":
                await driver_registration.start(msg)
                return
            elif flow == "greeting":
                await send_greeting(msg)
                return
            elif flow == "help":
                await send_help(msg)
                return

    # Check if this is a button reply for a driver delivery notification
    if msg.msg_type == "button" and msg.button_id in ("accept", "decline"):
        await driver_delivery.handle_delivery_response(msg)
        return

    # Unknown message — send help
    await send_help(msg)


async def send_greeting(msg: IncomingMessage):
    """Send a friendly greeting with options."""
    name = msg.name or "there"
    await send_interactive_buttons(
        to=msg.phone,
        body=f"Hi {name}! Welcome to TumaGo. What would you like to do?",
        buttons=[
            {"id": "send_delivery", "title": "Send a Package"},
            {"id": "register_client", "title": "Register"},
            {"id": "register_driver", "title": "Become a Driver"},
        ],
        header="TumaGo Deliveries",
        footer="Reply 'help' anytime for options",
    )


async def send_help(msg: IncomingMessage):
    """Send a help message listing all available commands."""
    await send_text(
        msg.phone,
        "TumaGo — What I can do:\n\n"
        "📦 *deliver* — Request a delivery\n"
        "📝 *register* — Create a client account\n"
        "🚗 *register driver* — Sign up as a driver\n"
        "📋 *history* — View your deliveries\n"
        "💰 *earnings* — View driver earnings\n"
        "🟢 *online* — Go online (drivers)\n"
        "🔴 *offline* — Go offline (drivers)\n"
        "❌ *cancel* — Cancel current action\n"
        "❓ *help* — Show this message\n\n"
        "Just type a command to get started!",
    )
