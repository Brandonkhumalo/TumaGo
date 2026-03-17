#!/bin/sh
set -e

echo "Running database migrations..."
python manage.py migrate --noinput

# If a command is passed (e.g. from docker-compose `command:`), run it.
# Otherwise default to Daphne.
if [ $# -gt 0 ]; then
    echo "Starting: $@"
    exec "$@"
else
    echo "Starting Daphne..."
    exec daphne -b 0.0.0.0 -p 8000 TumaGo.asgi:application
fi
