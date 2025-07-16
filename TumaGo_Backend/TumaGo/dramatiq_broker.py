import dramatiq
from dramatiq.brokers.redis import RedisBroker

redis_broker = RedisBroker(url="redis://redis:6379") #url="redis://localhost:6379"
dramatiq.set_broker(redis_broker)