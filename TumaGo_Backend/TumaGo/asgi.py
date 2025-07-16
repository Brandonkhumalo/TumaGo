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

'''$env:DJANGO_SETTINGS_MODULE="TumaGo.settings"
daphne TumaGo.asgi:application'''

'''daphne -b 0.0.0.0 -p 8000 TumaGo.asgi:application
dramatiq TumaGo_Server.views.DriverViews.deliveryMatching.tasks --processes 1 --threads 4

$env:DJANGO_SETTINGS_MODULE = "TumaGo.settings"
dramatiq TumaGo_Server.views.DriverViews.deliveryMatching.tasks --processes 1 --threads 4

docker run -d -p 6379:6379 --name redis-server redis'''

#docker stop redis-server
#docker rm redis-server

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