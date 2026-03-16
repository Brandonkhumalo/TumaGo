"""
Test settings — uses a local SQLite DB so tests run without PostgreSQL/Redis.

Usage:
    python manage.py test --settings=TumaGo.test_settings TumaGo_Server.tests -v 2
"""
import os
from pathlib import Path
from unittest.mock import MagicMock

# Stub out firebase_admin before any app code imports it, so
# initialize_firebase() (called at module level in userViews.py) doesn't crash.
import sys

_firebase_stub = MagicMock()
sys.modules.setdefault("firebase_admin", _firebase_stub)
sys.modules.setdefault("firebase_admin.credentials", _firebase_stub.credentials)
sys.modules.setdefault("firebase_admin.messaging", _firebase_stub.messaging)

# Stub googlemaps so googlemaps.Client(key="") doesn't raise at import time
# (delivery.py line 20 creates a client at module level).
_gmaps_stub = MagicMock()
sys.modules.setdefault("googlemaps", _gmaps_stub)

# Also patch the project's firebase_init so the real function is a no-op.
import TumaGo.firebase_init as _fb_init
_fb_init.initialize_firebase = lambda: None

BASE_DIR = Path(__file__).resolve().parent.parent

# Hardcoded secret for tests only — no .env required
SECRET_KEY = "test-secret-key-do-not-use-in-production"
DEBUG = True
ALLOWED_HOSTS = ["*"]

GOOGLE_MAPS_API_KEY = ""

INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "rest_framework",
    "TumaGo_Server",
    "corsheaders",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "TumaGo.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": (
        "TumaGo_Server.token.JWTAuthentication",
    ),
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.CursorPagination",
    "PAGE_SIZE": 5,
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
    ],
    # Disable throttling in tests so requests aren't blocked
    "DEFAULT_THROTTLE_CLASSES": [],
    "DEFAULT_THROTTLE_RATES": {},
}

WSGI_APPLICATION = "TumaGo.wsgi.application"

# ── SQLite for tests ────────────────────────────────────────────────────────
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.sqlite3",
        "NAME": BASE_DIR / "test_db.sqlite3",
        "TEST": {
            "NAME": BASE_DIR / "test_db.sqlite3",
        },
    }
}

# No Redis needed for tests — use in-memory cache
CACHES = {
    "default": {
        "BACKEND": "django.core.cache.backends.locmem.LocMemCache",
    }
}

AUTH_PASSWORD_VALIDATORS = []  # Skip password strength checks in tests
AUTH_USER_MODEL = "TumaGo_Server.CustomUser"

LANGUAGE_CODE = "en-us"
TIME_ZONE = "UTC"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"
STATIC_ROOT = os.path.join(BASE_DIR, "staticfiles")
MEDIA_URL = "/media/"
MEDIA_ROOT = BASE_DIR / "media"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"
CORS_ALLOW_ALL_ORIGINS = True
