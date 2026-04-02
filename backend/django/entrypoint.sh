#!/bin/sh
set -e

# Run migrations only if this is the first replica (use a Redis lock to prevent
# multiple containers migrating simultaneously). Falls back to running if Redis
# is unavailable — safe because Django migrations use DB-level locks internally.
if python -c "
import redis, os, sys
r = redis.from_url(os.environ.get('REDIS_URL', 'redis://redis:6379'))
# SET NX with 120s TTL — only one container wins the lock
if r.set('tumago:migrate_lock', '1', nx=True, ex=120):
    sys.exit(0)  # We got the lock — run migrations
sys.exit(1)      # Another container is migrating — skip
" 2>/dev/null; then
    echo "Running database migrations..."
    python manage.py migrate --noinput
else
    echo "Skipping migrations (another container is handling it)..."
fi

# If a command is passed (e.g. from docker-compose `command:`), run it.
# Otherwise default to Daphne.
if [ $# -gt 0 ]; then
    echo "Starting: $@"
    exec "$@"
else
    echo "Starting Daphne..."
    exec daphne -b 0.0.0.0 -p 8000 TumaGo.asgi:application
fi
