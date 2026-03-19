"""
WhatsApp service configuration — all values from environment variables.
"""
import os


WHATSAPP_ACCESS_TOKEN = os.environ.get("WHATSAPP_ACCESS_TOKEN", "")
WHATSAPP_PHONE_NUMBER_ID = os.environ.get("WHATSAPP_PHONE_NUMBER_ID", "")
WHATSAPP_VERIFY_TOKEN = os.environ.get("WHATSAPP_VERIFY_TOKEN", "")
WHATSAPP_API_VERSION = os.environ.get("WHATSAPP_API_VERSION", "v21.0")
WHATSAPP_API_URL = f"https://graph.facebook.com/{WHATSAPP_API_VERSION}/{WHATSAPP_PHONE_NUMBER_ID}/messages"

REDIS_URL = os.environ.get("REDIS_URL", "redis://redis:6379")
DJANGO_API_URL = os.environ.get("DJANGO_API_URL", "http://web:8000/api/v1")
SECRET_KEY = os.environ.get("SECRET_KEY", "")

# Conversation state TTL in seconds (30 minutes)
STATE_TTL = 1800
