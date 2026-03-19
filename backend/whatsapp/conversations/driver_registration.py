"""
Driver registration flow via WhatsApp.

Steps:
1. Collect personal details (name, surname, email, phone, ID number, address)
2. Set password
3. Call Django driver signup API
4. Select vehicle type (Scooter / Van / Truck)
5. Collect vehicle details (name, plate, color, model)
6. Upload ID photo
7. Upload license photo
8. Upload profile photo
9. Confirm registration (pending approval)
"""
import logging
import re

from webhook_handler import IncomingMessage
from state import set_state, clear_state
from sender import send_text, send_interactive_buttons, download_media
from django_client import driver_signup, login, add_vehicle, upload_license

logger = logging.getLogger(__name__)

VEHICLE_TYPES = {"scooter", "van", "truck"}


async def start(msg: IncomingMessage):
    """Start driver registration flow."""
    await set_state(msg.phone, "driver_registration", "awaiting_details")
    await send_text(
        msg.phone,
        "Welcome to TumaGo Driver registration!\n\n"
        "Please send your details in this format:\n\n"
        "Name, Surname, Email, Phone, ID Number, Street Address, City, Province, Postal Code\n\n"
        "Example:\n"
        "Tatenda, Moyo, tatenda@email.com, 0771234567, 63-123456-A-42, "
        "10 Kwame Nkrumah Ave, Harare, Harare, 00263\n\n"
        "Send 'cancel' to stop.",
    )


async def handle(msg: IncomingMessage, state: dict):
    """Handle messages within the driver registration flow."""
    step = state["step"]

    if step == "awaiting_details":
        await _handle_details(msg, state)
    elif step == "awaiting_password":
        await _handle_password(msg, state)
    elif step == "awaiting_vehicle_type":
        await _handle_vehicle_type(msg, state)
    elif step == "awaiting_vehicle_info":
        await _handle_vehicle_info(msg, state)
    elif step == "awaiting_id_photo":
        await _handle_id_photo(msg, state)
    elif step == "awaiting_license_photo":
        await _handle_license_photo(msg, state)
    elif step == "awaiting_profile_photo":
        await _handle_profile_photo(msg, state)


async def _handle_details(msg: IncomingMessage, state: dict):
    """Parse driver personal details."""
    parts = [p.strip() for p in msg.text.split(",")]

    if len(parts) < 9:
        await send_text(
            msg.phone,
            "I need all 9 details separated by commas.\n\n"
            "Name, Surname, Email, Phone, ID Number, Street Address, City, Province, Postal Code\n\n"
            "Example: Tatenda, Moyo, tatenda@email.com, 0771234567, 63-123456-A-42, "
            "10 Kwame Nkrumah Ave, Harare, Harare, 00263",
        )
        return

    name, surname, email, phone, id_number, street, city, province, postal = parts[:9]

    if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
        await send_text(msg.phone, "That email doesn't look valid. Please try again with all 9 details.")
        return

    data = {
        "name": name,
        "surname": surname,
        "email": email,
        "phone_number": phone,
        "identity_number": id_number,
        "streetAdress": street,
        "city": city,
        "province": province,
        "postalCode": postal,
    }

    await set_state(msg.phone, "driver_registration", "awaiting_password", data)
    await send_text(
        msg.phone,
        f"Details received:\n"
        f"Name: {name} {surname}\n"
        f"Email: {email}\n"
        f"Phone: {phone}\n"
        f"ID: {id_number}\n"
        f"Address: {street}, {city}\n\n"
        "Now choose a password (at least 8 characters).\n"
        "Send your password:",
    )


async def _handle_password(msg: IncomingMessage, state: dict):
    """Receive password and create driver account."""
    password = msg.text.strip()

    if len(password) < 8:
        await send_text(msg.phone, "Password must be at least 8 characters. Try again:")
        return

    data = state["data"]
    data["password"] = password
    data["role"] = "driver"

    result = await driver_signup(data)

    if result["status"] in (200, 201):
        # Store JWT token
        token = result["data"].get("token", "")
        if token:
            from state import get_redis
            r = await get_redis()
            await r.set(f"whatsapp:token:{msg.phone}", token, ex=86400 * 30)

        # Keep registration data and move to vehicle type
        data["token"] = token
        await set_state(msg.phone, "driver_registration", "awaiting_vehicle_type", data)

        await send_interactive_buttons(
            to=msg.phone,
            body="Account created! Now let's add your vehicle.\n\nWhat type of delivery vehicle do you have?",
            buttons=[
                {"id": "scooter", "title": "Scooter"},
                {"id": "van", "title": "Van"},
                {"id": "truck", "title": "Truck"},
            ],
            header="Vehicle Type",
        )
    else:
        error = result["data"].get("error", result["data"])
        await send_text(
            msg.phone,
            f"Registration failed: {error}\n\n"
            "Please try again or send 'cancel' to start over.",
        )
        await set_state(msg.phone, "driver_registration", "awaiting_details")


async def _handle_vehicle_type(msg: IncomingMessage, state: dict):
    """Handle vehicle type selection."""
    # Accept button reply or text
    vehicle_type = msg.button_id or msg.text.lower().strip()

    if vehicle_type not in VEHICLE_TYPES:
        await send_interactive_buttons(
            to=msg.phone,
            body="Please select your vehicle type:",
            buttons=[
                {"id": "scooter", "title": "Scooter"},
                {"id": "van", "title": "Van"},
                {"id": "truck", "title": "Truck"},
            ],
        )
        return

    data = state["data"]
    data["delivery_vehicle"] = vehicle_type.capitalize()
    await set_state(msg.phone, "driver_registration", "awaiting_vehicle_info", data)

    await send_text(
        msg.phone,
        f"You selected: {vehicle_type.capitalize()}\n\n"
        "Now send your vehicle details:\n\n"
        "Vehicle name, Number plate, Color, Model/Year\n\n"
        "Example: Honda PCX, ABG 1234, Red, 2022",
    )


async def _handle_vehicle_info(msg: IncomingMessage, state: dict):
    """Parse vehicle details."""
    parts = [p.strip() for p in msg.text.split(",")]

    if len(parts) < 4:
        await send_text(
            msg.phone,
            "I need 4 details separated by commas:\n"
            "Vehicle name, Number plate, Color, Model/Year\n\n"
            "Example: Honda PCX, ABG 1234, Red, 2022",
        )
        return

    car_name, number_plate, color, vehicle_model = parts[:4]
    data = state["data"]

    # Add vehicle via Django API
    token = data.get("token", "")
    if token:
        vehicle_data = {
            "delivery_vehicle": data.get("delivery_vehicle", ""),
            "car_name": car_name,
            "number_plate": number_plate,
            "color": color,
            "vehicle_model": vehicle_model,
        }
        result = await add_vehicle(token, vehicle_data)
        if result["status"] not in (200, 201):
            logger.error(f"Add vehicle failed: {result}")

    data["car_name"] = car_name
    data["number_plate"] = number_plate
    data["color"] = color
    data["vehicle_model"] = vehicle_model

    await set_state(msg.phone, "driver_registration", "awaiting_id_photo", data)
    await send_text(
        msg.phone,
        "Vehicle registered!\n\n"
        "Now send a clear photo of your *national ID* (front side).\n"
        "Make sure all text is readable.",
    )


async def _handle_id_photo(msg: IncomingMessage, state: dict):
    """Receive ID photo."""
    if msg.msg_type != "image":
        await send_text(msg.phone, "Please send a *photo* of your national ID. Take a clear picture and send it.")
        return

    # Download and store the media ID for later upload
    data = state["data"]
    data["id_media_id"] = msg.media_id

    await set_state(msg.phone, "driver_registration", "awaiting_license_photo", data)
    await send_text(
        msg.phone,
        "ID photo received!\n\n"
        "Now send a clear photo of your *driver's license* (front side).",
    )


async def _handle_license_photo(msg: IncomingMessage, state: dict):
    """Receive license photo and upload to Django."""
    if msg.msg_type != "image":
        await send_text(msg.phone, "Please send a *photo* of your driver's license.")
        return

    data = state["data"]
    data["license_media_id"] = msg.media_id

    # Upload license to Django
    token = data.get("token", "")
    if token and msg.media_id:
        file_data = await download_media(msg.media_id)
        if file_data:
            await upload_license(token, file_data, "license.jpg")

    await set_state(msg.phone, "driver_registration", "awaiting_profile_photo", data)
    await send_text(
        msg.phone,
        "License photo received!\n\n"
        "Last step — send a clear *photo of yourself* for your driver profile.\n"
        "This will be shown to clients.",
    )


async def _handle_profile_photo(msg: IncomingMessage, state: dict):
    """Receive profile photo and complete registration."""
    if msg.msg_type != "image":
        await send_text(msg.phone, "Please send a *photo* of yourself for your profile picture.")
        return

    data = state["data"]
    name = data.get("name", "Driver")

    await clear_state(msg.phone)
    await send_text(
        msg.phone,
        f"Registration complete, {name}! 🎉\n\n"
        "Your application is now under review. We'll notify you once approved "
        "(usually within 24-48 hours).\n\n"
        "While you wait, download the driver app:\n"
        "https://play.google.com/store/apps/details?id=com.techmania.tumago_driver",
    )
