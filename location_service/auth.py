"""JWT validation for the location service."""
import os
import logging
from datetime import datetime

import jwt
from jwt.exceptions import InvalidTokenError, ExpiredSignatureError

logger = logging.getLogger(__name__)

SECRET_KEY = os.environ.get("SECRET_KEY", "")


def get_user_id_from_token(token: str | None) -> str | None:
    """
    Decode the HS256 JWT and return the user ID (UUID string).
    Returns None if the token is missing, expired, or invalid.
    """
    if not token:
        return None
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=["HS256"])

        exp = payload.get("exp")
        if exp and int(datetime.utcnow().timestamp()) > int(exp):
            raise ExpiredSignatureError("Token expired")

        if payload.get("type") != "access_token":
            raise InvalidTokenError("Wrong token type")

        user_id = payload.get("id")
        return str(user_id) if user_id else None

    except (InvalidTokenError, ExpiredSignatureError, jwt.DecodeError) as e:
        logger.warning(f"Token validation failed: {e}")
        return None
