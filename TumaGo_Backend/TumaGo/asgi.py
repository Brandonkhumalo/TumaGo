import os
import django
from django.core.asgi import get_asgi_application
from channels.routing import ProtocolTypeRouter, URLRouter
from channels.auth import AuthMiddlewareStack
import TumaGo_Server.location_tracking.routing

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "TumaGo.settings")
django.setup()

application = ProtocolTypeRouter({
    "http": get_asgi_application(),
    "websocket": AuthMiddlewareStack(
        URLRouter(
            TumaGo_Server.location_tracking.routing.websocket_urlpatterns
        )
    ),
})

'''
    docker-compose down -v
    docker-compose up --build
'''

'''
    docker-compose exec web python manage.py makemigrations
    docker-compose exec web python manage.py migrate
'''

'''
Swagger UI: http://localhost:8000/swagger/
ReDoc UI: http://localhost:8000/redoc/
'''