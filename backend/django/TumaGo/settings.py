from pathlib import Path
import os
from decouple import config
import dj_database_url

# Build paths inside the project like this: BASE_DIR / 'subdir'.
BASE_DIR = Path(__file__).resolve().parent.parent

# ---------------------------------------------------------------------------
# Core security — controlled entirely via environment variables
# ---------------------------------------------------------------------------
SECRET_KEY = config("SECRET_KEY")
DEBUG = config('DEBUG', default=False, cast=bool)

GOOGLE_MAPS_API_KEY = config("GOOGLE_MAPS_API_KEY", default="")

# ---------------------------------------------------------------------------
# Paynow Zimbabwe — payment gateway
# ---------------------------------------------------------------------------
PAYNOW_INTEGRATION_ID = config("PAYNOW_INTEGRATION_ID", default="")
PAYNOW_INTEGRATION_KEY = config("PAYNOW_INTEGRATION_KEY", default="")
PAYNOW_RETURN_URL = config("PAYNOW_RETURN_URL", default="http://localhost/payment-return")
PAYNOW_RESULT_URL = config("PAYNOW_RESULT_URL", default="http://localhost/api/v1/payment/callback/")

# ---------------------------------------------------------------------------
# AWS SES — email sending via tishanyq.co.zw
# ---------------------------------------------------------------------------
AWS_ACCESS_KEY_ID = config("AWS_ACCESS_KEY_ID", default="")
AWS_SECRET_ACCESS_KEY = config("AWS_SECRET_ACCESS_KEY", default="")
AWS_SES_REGION = config("AWS_SES_REGION", default="af-south-1")
SES_FROM_EMAIL = config("SES_FROM_EMAIL", default="noreply@tishanyq.co.zw")
ALLOWED_HOSTS = config('ALLOWED_HOSTS', default='*').split(',')

# Application definition
INSTALLED_APPS = [
    'django_prometheus',
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'rest_framework',
    'TumaGo_Server',
    'corsheaders',
    'drf_yasg',
]

MIDDLEWARE = [
    'django_prometheus.middleware.PrometheusBeforeMiddleware',
    'django.middleware.security.SecurityMiddleware',
    'corsheaders.middleware.CorsMiddleware',
    'whitenoise.middleware.WhiteNoiseMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
    'TumaGo_Server.middleware.RequestLoggingMiddleware',
    'django_prometheus.middleware.PrometheusAfterMiddleware',
]

ROOT_URLCONF = 'TumaGo.urls'

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [],
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.request',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
            ],
        },
    },
]

# ---------------------------------------------------------------------------
# REST Framework — throttling, auth, pagination
# ---------------------------------------------------------------------------
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': (
        'TumaGo_Server.token.JWTAuthentication',
    ),
    'DEFAULT_PAGINATION_CLASS': 'rest_framework.pagination.CursorPagination',
    'PAGE_SIZE': 5,
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticated',
    ],
    'DEFAULT_THROTTLE_CLASSES': [
        'rest_framework.throttling.AnonRateThrottle',
        'rest_framework.throttling.UserRateThrottle',
    ],
    'DEFAULT_THROTTLE_RATES': {
        'anon': '30/minute',
        'user': '300/minute',
        'login': '10/minute',
        'signup': '5/minute',
        'otp_send': '3/minute',
        'otp_verify': '5/minute',
        'delivery_request': '10/minute',
    },
}

WSGI_APPLICATION = 'TumaGo.wsgi.application'

# ---------------------------------------------------------------------------
# Database — persistent connections + health checks (PgBouncer-compatible)
# ---------------------------------------------------------------------------
DATABASES = {
    'default': dj_database_url.config(
        default=config('DATABASE_URL'),
        conn_max_age=600,
        conn_health_checks=True,
    )
}

# ---------------------------------------------------------------------------
# Redis URL — single source of truth for Channel Layer, Caches, and broker
# ---------------------------------------------------------------------------
REDIS_URL = config("REDIS_URL", default="redis://localhost:6379")

# ---------------------------------------------------------------------------
# Cache — Redis backend for query/response/session caching
# ---------------------------------------------------------------------------
CACHES = {
    "default": {
        "BACKEND": "django.core.cache.backends.redis.RedisCache",
        "LOCATION": REDIS_URL,
        "TIMEOUT": 300,
        "KEY_PREFIX": "tumago",
    }
}

# ---------------------------------------------------------------------------
# Password validation
# ---------------------------------------------------------------------------
AUTH_PASSWORD_VALIDATORS = [
    {
        'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator',
    },
]

AUTH_USER_MODEL = 'TumaGo_Server.CustomUser'

# ---------------------------------------------------------------------------
# Internationalisation
# ---------------------------------------------------------------------------
LANGUAGE_CODE = 'en-us'
TIME_ZONE = 'Africa/Harare'
USE_I18N = True
USE_TZ = True

# ---------------------------------------------------------------------------
# Static & media files
# ---------------------------------------------------------------------------
STATIC_URL = 'static/'
STATIC_ROOT = os.path.join(BASE_DIR, 'staticfiles')
MEDIA_URL = '/media/'
MEDIA_ROOT = BASE_DIR / 'media'

DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'

# ---------------------------------------------------------------------------
# CORS
# ---------------------------------------------------------------------------
_cors_origins = config('CORS_ALLOWED_ORIGINS', default='')
if DEBUG or _cors_origins == '*':
    CORS_ALLOW_ALL_ORIGINS = True
else:
    CORS_ALLOWED_ORIGINS = [
        o.strip() for o in (_cors_origins or 'https://api.tumago.com').split(',') if o.strip()
    ]

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'verbose': {
            'format': '{levelname} {asctime} {module} {process:d} {thread:d} {message}',
            'style': '{',
        },
    },
    'handlers': {
        'console': {
            'class': 'logging.StreamHandler',
            'formatter': 'verbose',
        },
    },
    'root': {
        'handlers': ['console'],
        'level': 'WARNING',
    },
    'loggers': {
        'django': {
            'handlers': ['console'],
            'level': config('DJANGO_LOG_LEVEL', default='WARNING'),
            'propagate': False,
        },
        'TumaGo_Server': {
            'handlers': ['console'],
            'level': 'INFO',
            'propagate': False,
        },
    },
}
