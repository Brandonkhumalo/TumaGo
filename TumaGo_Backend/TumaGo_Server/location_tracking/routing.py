from django.urls import re_path
from . import consumers

websocket_urlpatterns = [
re_path(r'ws/driver_location/$', consumers.LocationConsumer.as_asgi()),
]