"""
Client delivery request flow via WhatsApp.

Steps:
1. Collect pickup and drop-off addresses
2. Get fare estimates from Django
3. Choose vehicle type (Scooter / Van / Truck)
4. Choose payment method (EcoCash / OneMoney / Cash)
5. For mobile money: initiate Paynow payment
6. Confirm and submit delivery request
"""
import logging

from webhook_handler import IncomingMessage
from state import set_state, clear_state, get_state
from sender import (
    send_text,
    send_interactive_buttons,
    send_interactive_list,
    send_template,
)
from django_client import (
    get_fare_estimate,
    request_delivery,
    initiate_payment,
    check_payment_status,
    get_deliveries,
)

logger = logging.getLogger(__name__)


async def _get_token(phone: str) -> str:
    """Get stored JWT token for this phone number."""
    from state import get_redis
    r = await get_redis()
    return await r.get(f"whatsapp:token:{phone}") or ""


async def start(msg: IncomingMessage):
    """Start delivery request flow."""
    token = await _get_token(msg.phone)
    if not token:
        await send_text(
            msg.phone,
            "You need an account to request deliveries.\n"
            "Send 'register' to create one, or log in on the app first.",
        )
        return

    await set_state(msg.phone, "delivery_request", "awaiting_addresses")
    await send_text(
        msg.phone,
        "Let's set up your delivery!\n\n"
        "Send the pickup and drop-off addresses separated by a comma:\n\n"
        "Pickup address, Drop-off address\n\n"
        "Example: 123 Samora Machel Ave Harare, 45 Robert Mugabe Rd Harare",
    )


async def handle(msg: IncomingMessage, state: dict):
    """Handle messages within the delivery request flow."""
    step = state["step"]

    if step == "awaiting_addresses":
        await _handle_addresses(msg, state)
    elif step == "awaiting_package":
        await _handle_package(msg, state)
    elif step == "awaiting_vehicle":
        await _handle_vehicle(msg, state)
    elif step == "awaiting_payment":
        await _handle_payment(msg, state)
    elif step == "awaiting_phone_number":
        await _handle_phone_number(msg, state)
    elif step == "awaiting_payment_confirmation":
        await _handle_payment_confirmation(msg, state)


async def _handle_addresses(msg: IncomingMessage, state: dict):
    """Parse pickup and drop-off addresses."""
    parts = [p.strip() for p in msg.text.split(",")]

    if len(parts) < 2:
        await send_text(
            msg.phone,
            "I need both addresses separated by a comma:\n"
            "Pickup address, Drop-off address\n\n"
            "Example: 123 Samora Machel Ave Harare, 45 Robert Mugabe Rd Harare",
        )
        return

    pickup = parts[0]
    dropoff = ", ".join(parts[1:])  # In case drop-off has commas

    data = state.get("data", {})
    data["pickup_address"] = pickup
    data["dropoff_address"] = dropoff

    await set_state(msg.phone, "delivery_request", "awaiting_package", data)
    await send_text(
        msg.phone,
        f"Pickup: {pickup}\n"
        f"Drop-off: {dropoff}\n\n"
        "What are you sending? Describe the package:\n\n"
        "Example: Small box with documents",
    )


async def _handle_package(msg: IncomingMessage, state: dict):
    """Receive package description and show vehicle options with prices."""
    data = state["data"]
    data["package"] = msg.text.strip()

    # Get fare estimates from Django
    token = await _get_token(msg.phone)

    # For now, use placeholder coordinates — in production, geocode the addresses
    # TODO: Add geocoding via Google Maps API
    data["origin_lat"] = -17.8292
    data["origin_lng"] = 31.0522
    data["destination_lat"] = -17.8310
    data["destination_lng"] = 31.0455

    fare_result = await get_fare_estimate(
        token,
        data["origin_lat"], data["origin_lng"],
        data["destination_lat"], data["destination_lng"],
    )

    if fare_result["status"] == 200:
        fares = fare_result["data"]
        scooter_price = fares.get("scooter", 0)
        van_price = fares.get("van", 0)
        truck_price = fares.get("truck", 0)
        data["scooter_price"] = scooter_price
        data["van_price"] = van_price
        data["truck_price"] = truck_price
    else:
        # Use default prices if API fails
        data["scooter_price"] = 2.50
        data["van_price"] = 5.00
        data["truck_price"] = 8.00

    await set_state(msg.phone, "delivery_request", "awaiting_vehicle", data)

    await send_interactive_buttons(
        to=msg.phone,
        body=(
            f"Package: {data['package']}\n"
            f"Pickup: {data['pickup_address']}\n"
            f"Drop-off: {data['dropoff_address']}\n\n"
            f"Choose your vehicle:\n\n"
            f"🛵 Scooter — ${data['scooter_price']:.2f}\n"
            f"🚐 Van — ${data['van_price']:.2f}\n"
            f"🚛 Truck — ${data['truck_price']:.2f}"
        ),
        buttons=[
            {"id": "scooter", "title": f"Scooter ${data['scooter_price']:.2f}"},
            {"id": "van", "title": f"Van ${data['van_price']:.2f}"},
            {"id": "truck", "title": f"Truck ${data['truck_price']:.2f}"},
        ],
        header="Choose Vehicle",
        footer="TumaGo Deliveries",
    )


async def _handle_vehicle(msg: IncomingMessage, state: dict):
    """Handle vehicle type selection."""
    vehicle = msg.button_id or msg.text.lower().strip()

    if vehicle not in ("scooter", "van", "truck"):
        await send_text(msg.phone, "Please tap one of the vehicle buttons above.")
        return

    data = state["data"]
    data["vehicle"] = vehicle.capitalize()

    price_key = f"{vehicle}_price"
    data["fare"] = data.get(price_key, 0)

    await set_state(msg.phone, "delivery_request", "awaiting_payment", data)

    await send_interactive_buttons(
        to=msg.phone,
        body=(
            f"Vehicle: {data['vehicle']}\n"
            f"Total: ${data['fare']:.2f}\n\n"
            "How would you like to pay?"
        ),
        buttons=[
            {"id": "ecocash", "title": "EcoCash"},
            {"id": "onemoney", "title": "OneMoney"},
            {"id": "cash", "title": "Cash"},
        ],
        header="Payment Method",
        footer="TumaGo Deliveries",
    )


async def _handle_payment(msg: IncomingMessage, state: dict):
    """Handle payment method selection."""
    method = msg.button_id or msg.text.lower().strip()

    if method not in ("ecocash", "onemoney", "cash"):
        await send_text(msg.phone, "Please tap one of the payment buttons above.")
        return

    data = state["data"]
    data["payment_method"] = method

    if method == "cash":
        # Cash — skip payment, go straight to delivery request
        await _submit_delivery(msg.phone, data)
        return

    # Mobile money — need phone number
    await set_state(msg.phone, "delivery_request", "awaiting_phone_number", data)
    method_name = "EcoCash" if method == "ecocash" else "OneMoney"
    await send_text(
        msg.phone,
        f"You selected {method_name}.\n\n"
        f"Send your {method_name} phone number (e.g., 0771234567):",
    )


async def _handle_phone_number(msg: IncomingMessage, state: dict):
    """Receive mobile money phone number and initiate payment."""
    phone = msg.text.strip()

    # Basic phone validation
    if len(phone) < 9 or not phone.replace("+", "").isdigit():
        await send_text(msg.phone, "Please send a valid phone number (e.g., 0771234567):")
        return

    data = state["data"]
    data["payment_phone"] = phone
    token = await _get_token(msg.phone)

    # Initiate Paynow payment
    result = await initiate_payment(
        token,
        float(data["fare"]),
        data["payment_method"],
        phone,
    )

    if result["status"] in (200, 201):
        payment_data = result["data"]
        data["payment_id"] = payment_data.get("payment_id", "")
        data["poll_url"] = payment_data.get("poll_url", "")

        await set_state(msg.phone, "delivery_request", "awaiting_payment_confirmation", data)

        method_name = "EcoCash" if data["payment_method"] == "ecocash" else "OneMoney"
        await send_text(
            msg.phone,
            f"A {method_name} payment prompt has been sent to {phone}.\n\n"
            f"Amount: ${data['fare']:.2f}\n\n"
            "Steps:\n"
            f"1. Check your phone for the {method_name} prompt\n"
            f"2. Enter your {method_name} PIN to confirm\n"
            "3. Once done, reply 'done' here\n\n"
            "Reply 'done' after you've paid, or 'cancel' to abort.",
        )
    else:
        error = result["data"].get("error", "Payment failed")
        await send_text(
            msg.phone,
            f"Payment error: {error}\n\n"
            "Please try again. Send your phone number:",
        )


async def _handle_payment_confirmation(msg: IncomingMessage, state: dict):
    """Check if payment was completed."""
    text = msg.text.lower().strip()

    if text not in ("done", "paid", "yes", "check"):
        await send_text(msg.phone, "Reply 'done' after you've completed the payment, or 'cancel' to abort.")
        return

    data = state["data"]
    token = await _get_token(msg.phone)

    # Check payment status
    result = await check_payment_status(token, data.get("payment_id", ""))

    if result["status"] == 200 and result["data"].get("paid"):
        await send_text(msg.phone, "Payment confirmed! Finding a driver for you...")
        await _submit_delivery(msg.phone, data)
    else:
        await send_text(
            msg.phone,
            "Payment not received yet. Please complete the payment on your phone.\n\n"
            "Reply 'done' when you've paid, or 'cancel' to abort.",
        )


async def _submit_delivery(phone: str, data: dict):
    """Submit the delivery request to Django."""
    token = await _get_token(phone)

    delivery_data = {
        "origin_lat": data.get("origin_lat"),
        "origin_lng": data.get("origin_lng"),
        "destination_lat": data.get("destination_lat"),
        "destination_lng": data.get("destination_lng"),
        "vehicle": data.get("vehicle", ""),
        "fare": float(data.get("fare", 0)),
        "payment_method": data.get("payment_method", "cash"),
    }

    result = await request_delivery(token, delivery_data)

    if result["status"] in (200, 201):
        await clear_state(phone)
        await send_text(
            phone,
            f"Delivery requested! 📦\n\n"
            f"Pickup: {data.get('pickup_address', '')}\n"
            f"Drop-off: {data.get('dropoff_address', '')}\n"
            f"Vehicle: {data.get('vehicle', '')}\n"
            f"Cost: ${data.get('fare', 0):.2f}\n"
            f"Payment: {data.get('payment_method', '').upper()}\n\n"
            "We're finding a driver for you. You'll be notified when one accepts!",
        )
    else:
        error = result["data"].get("error", result["data"])
        await send_text(phone, f"Delivery request failed: {error}\n\nPlease try again.")
        await clear_state(phone)


async def show_history(msg: IncomingMessage):
    """Show delivery history."""
    token = await _get_token(msg.phone)
    if not token:
        await send_text(msg.phone, "You need an account to view history. Send 'register' to create one.")
        return

    result = await get_deliveries(token)

    if result["status"] == 200:
        deliveries = result["data"]
        if isinstance(deliveries, list) and deliveries:
            lines = ["Your recent deliveries:\n"]
            for i, d in enumerate(deliveries[:5], 1):
                status = "Completed" if d.get("successful") else "Cancelled"
                fare = d.get("fare", "0")
                date = d.get("date", "")
                lines.append(f"{i}. ${fare} — {status} — {date}")
            await send_text(msg.phone, "\n".join(lines))
        else:
            await send_text(msg.phone, "No deliveries yet. Send 'deliver' to request your first one!")
    else:
        await send_text(msg.phone, "Couldn't fetch your deliveries. Please try again later.")
