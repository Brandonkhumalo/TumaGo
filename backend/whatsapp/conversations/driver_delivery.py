"""
Driver delivery flow — handle delivery notifications, accept/decline,
online/offline status, and earnings via WhatsApp.

This module is called in two ways:
1. Outbound: Other services publish events to Redis, this module sends
   WhatsApp messages to drivers/clients
2. Inbound: Driver replies (Accept/Decline, Delivered, etc.)
"""
import logging

from webhook_handler import IncomingMessage
from state import set_state, clear_state, get_state
from sender import send_text, send_interactive_buttons, send_template
from django_client import (
    accept_trip,
    end_trip,
    driver_offline,
    get_driver_finances,
)

logger = logging.getLogger(__name__)


async def _get_token(phone: str) -> str:
    """Get stored JWT token for this phone number."""
    from state import get_redis
    r = await get_redis()
    return await r.get(f"whatsapp:token:{phone}") or ""


async def handle(msg: IncomingMessage, state: dict):
    """Handle messages within an active driver delivery flow."""
    step = state["step"]

    if step == "awaiting_acceptance":
        await _handle_acceptance(msg, state)
    elif step == "awaiting_pickup_confirm":
        await _handle_pickup_confirm(msg, state)
    elif step == "awaiting_delivery_confirm":
        await _handle_delivery_confirm(msg, state)


async def handle_delivery_response(msg: IncomingMessage):
    """Handle Accept/Decline button taps from delivery notifications."""
    state = await get_state(msg.phone)
    if state and state["step"] == "awaiting_acceptance":
        await _handle_acceptance(msg, state)
    else:
        await send_text(msg.phone, "This delivery is no longer available.")


# ── Outbound: Send delivery notification to driver ───────────────────────────

async def notify_driver_new_delivery(
    driver_phone: str,
    trip_id: str,
    pickup: str,
    dropoff: str,
    distance_km: float,
    fare: float,
    package: str,
    payment_method: str,
):
    """
    Called by the event consumer when a new delivery is matched to a driver.
    Sends an interactive message asking the driver to accept or decline.
    """
    # Store trip context in state so we can process the reply
    await set_state(
        driver_phone,
        "driver_delivery",
        "awaiting_acceptance",
        {"trip_id": trip_id, "pickup": pickup, "dropoff": dropoff, "fare": fare},
    )

    await send_interactive_buttons(
        to=driver_phone,
        body=(
            f"New delivery request!\n\n"
            f"Pickup: {pickup}\n"
            f"Drop-off: {dropoff}\n"
            f"Distance: {distance_km:.1f} km\n"
            f"Your earnings: ${fare:.2f}\n"
            f"Package: {package}\n"
            f"Payment: {payment_method.upper()}\n\n"
            "Do you accept?"
        ),
        buttons=[
            {"id": "accept", "title": "Accept"},
            {"id": "decline", "title": "Decline"},
        ],
        header="New Delivery",
        footer="TumaGo Deliveries",
    )


async def _handle_acceptance(msg: IncomingMessage, state: dict):
    """Process driver's Accept/Decline response."""
    response = msg.button_id or msg.text.lower().strip()
    data = state["data"]
    trip_id = data.get("trip_id", "")
    token = await _get_token(msg.phone)

    if response in ("accept", "yes"):
        if token and trip_id:
            result = await accept_trip(token, trip_id)
            if result["status"] in (200, 201):
                await set_state(
                    msg.phone,
                    "driver_delivery",
                    "awaiting_pickup_confirm",
                    data,
                )
                pickup = data.get("pickup", "")
                await send_text(
                    msg.phone,
                    f"Delivery accepted! 🎉\n\n"
                    f"Navigate to pickup: {pickup}\n\n"
                    f"https://www.google.com/maps/dir/?api=1&destination={pickup}\n\n"
                    "Reply 'picked up' when you've collected the package.",
                )
                return
            else:
                error = result["data"].get("error", "Failed to accept")
                await send_text(msg.phone, f"Error: {error}")
        else:
            await send_text(msg.phone, "Error: missing token or trip ID. Please try from the app.")

        await clear_state(msg.phone)

    elif response in ("decline", "no"):
        await clear_state(msg.phone)
        await send_text(msg.phone, "Delivery declined. You'll receive new requests as they come in.")

    else:
        await send_text(msg.phone, "Please tap 'Accept' or 'Decline'.")


async def _handle_pickup_confirm(msg: IncomingMessage, state: dict):
    """Driver confirms they've picked up the package."""
    text = msg.text.lower().strip()

    if text in ("picked up", "pickup", "collected", "done"):
        data = state["data"]
        dropoff = data.get("dropoff", "")

        await set_state(msg.phone, "driver_delivery", "awaiting_delivery_confirm", data)
        await send_text(
            msg.phone,
            f"Package picked up! Navigate to drop-off:\n\n"
            f"{dropoff}\n\n"
            f"https://www.google.com/maps/dir/?api=1&destination={dropoff}\n\n"
            "Reply 'delivered' when you've dropped off the package.",
        )
    else:
        await send_text(msg.phone, "Reply 'picked up' when you've collected the package from the sender.")


async def _handle_delivery_confirm(msg: IncomingMessage, state: dict):
    """Driver confirms delivery completion."""
    text = msg.text.lower().strip()

    if text in ("delivered", "done", "complete", "completed"):
        data = state["data"]
        token = await _get_token(msg.phone)

        # End trip via Django
        if token and data.get("trip_id"):
            result = await end_trip(token, data["trip_id"])
            if result["status"] in (200, 201):
                fare = data.get("fare", 0)
                await send_text(
                    msg.phone,
                    f"Delivery complete! ✅\n\n"
                    f"${fare:.2f} has been added to your earnings.\n\n"
                    "You'll receive new delivery requests as they come in.",
                )
            else:
                await send_text(msg.phone, "Delivery marked. Thank you!")

        await clear_state(msg.phone)
    else:
        await send_text(msg.phone, "Reply 'delivered' when you've dropped off the package.")


# ── Outbound: Notify client of delivery events ───────────────────────────────

async def notify_client_driver_found(
    client_phone: str,
    delivery_id: str,
    driver_name: str,
    vehicle_type: str,
    vehicle_name: str,
    color: str,
    number_plate: str,
    eta_minutes: int,
    driver_phone: str,
):
    """Send driver found notification to client."""
    await send_text(
        client_phone,
        f"Driver found for your delivery! 🎉\n\n"
        f"Driver: {driver_name}\n"
        f"Vehicle: {vehicle_type} — {vehicle_name} ({color})\n"
        f"Number plate: {number_plate}\n"
        f"ETA: {eta_minutes} min\n"
        f"Driver phone: {driver_phone}\n\n"
        "You'll be notified of every status update.",
    )


async def notify_client_no_driver(client_phone: str):
    """Notify client no driver was found."""
    await send_interactive_buttons(
        to=client_phone,
        body="No drivers are available right now. We'll keep trying and notify you when one accepts.",
        buttons=[
            {"id": "keep_waiting", "title": "Keep Waiting"},
            {"id": "cancel_delivery", "title": "Cancel Delivery"},
        ],
        footer="TumaGo Deliveries",
    )


async def notify_client_driver_at_pickup(client_phone: str, driver_name: str):
    """Notify client the driver has arrived at pickup."""
    await send_text(
        client_phone,
        f"Driver {driver_name} has arrived at the pickup location. Please hand over your package.",
    )


async def notify_client_en_route(client_phone: str, driver_name: str):
    """Notify client the driver is en route to drop-off."""
    await send_text(
        client_phone,
        f"Your package has been picked up! Driver {driver_name} is on the way to the drop-off.",
    )


async def notify_client_delivery_complete(client_phone: str, driver_name: str, delivery_id: str):
    """Notify client delivery is complete."""
    await send_interactive_buttons(
        to=client_phone,
        body=f"Your delivery is complete! Package delivered by {driver_name}.\n\nHow was your experience?",
        buttons=[
            {"id": "rate_5", "title": "Excellent"},
            {"id": "rate_4", "title": "Good"},
            {"id": "rate_issue", "title": "Report Issue"},
        ],
        footer="TumaGo Deliveries",
    )


# ── Driver status ────────────────────────────────────────────────────────────

async def go_online(msg: IncomingMessage):
    """Set driver online."""
    token = await _get_token(msg.phone)
    if not token:
        await send_text(msg.phone, "You need a driver account. Send 'register driver' to sign up.")
        return

    result = await driver_offline(token, True)
    if result["status"] in (200, 201):
        await send_text(
            msg.phone,
            "You're now online! 🟢\n\n"
            "You'll receive delivery requests as they come in.\n"
            "Reply 'offline' to stop receiving requests.",
        )
    else:
        await send_text(msg.phone, "Failed to go online. Please try again.")


async def go_offline(msg: IncomingMessage):
    """Set driver offline."""
    token = await _get_token(msg.phone)
    if not token:
        await send_text(msg.phone, "You need a driver account. Send 'register driver' to sign up.")
        return

    result = await driver_offline(token, False)
    if result["status"] in (200, 201):
        await send_text(
            msg.phone,
            "You're now offline. 🔴\n\n"
            "You won't receive delivery requests.\n"
            "Reply 'online' to start again.",
        )
    else:
        await send_text(msg.phone, "Failed to go offline. Please try again.")


async def show_earnings(msg: IncomingMessage):
    """Show driver earnings summary."""
    token = await _get_token(msg.phone)
    if not token:
        await send_text(msg.phone, "You need a driver account. Send 'register driver' to sign up.")
        return

    result = await get_driver_finances(token)
    if result["status"] == 200:
        data = result["data"]
        await send_text(
            msg.phone,
            f"Your earnings summary:\n\n"
            f"Total trips: {data.get('total_trips', 0)}\n"
            f"Earnings: ${data.get('earnings', '0.00')}\n"
            f"Charges: ${data.get('charges', '0.00')}\n"
            f"Profit: ${data.get('profit', '0.00')}\n",
        )
    else:
        await send_text(msg.phone, "Couldn't fetch your earnings. Please try again later.")
