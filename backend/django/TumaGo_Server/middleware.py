import logging
import time

logger = logging.getLogger(__name__)


class RequestLoggingMiddleware:
    """Logs method, path, status code, and response time for every request."""

    def __init__(self, get_response):
        self.get_response = get_response

    # Paths to skip logging — called frequently by Docker healthchecks.
    SILENT_PATHS = {"/health/", "/health"}

    def __call__(self, request):
        start = time.monotonic()
        response = self.get_response(request)

        if request.path not in self.SILENT_PATHS:
            duration_ms = (time.monotonic() - start) * 1000
            logger.info(
                "%s %s %s %.0fms",
                request.method,
                request.get_full_path(),
                response.status_code,
                duration_ms,
            )

        return response
