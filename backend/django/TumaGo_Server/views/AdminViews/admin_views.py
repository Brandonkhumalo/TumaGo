from django.contrib.auth import get_user_model
from django.db.models import Count, Sum, Avg, Q, F
from django.db.models.functions import TruncDate, ExtractHour
from django.utils import timezone
from datetime import timedelta

from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated, IsAdminUser
from rest_framework.response import Response
from rest_framework import status

from decimal import Decimal

from ...models import (
    CustomUser,
    Delivery,
    Payment,
    DriverBalance,
    DriverFinances,
    DriverVehicle,
    TripRequest,
    PartnerCompany,
    PartnerDeliveryRequest,
    PartnerTransaction,
    DriverLocations,
)
from ...token import JWTAuthentication

import logging
import secrets

logger = logging.getLogger(__name__)

User = get_user_model()


# ---------------------------------------------------------------------------
# Pagination helper
# ---------------------------------------------------------------------------

def paginate_queryset(queryset, request, default_page_size=20):
    """Simple offset-based pagination.

    Reads `page` (1-indexed) and `page_size` from query params.
    Returns a dict with paginated results and metadata.
    """
    try:
        page = max(int(request.query_params.get('page', 1)), 1)
    except (ValueError, TypeError):
        page = 1

    try:
        page_size = min(max(int(request.query_params.get('page_size', default_page_size)), 1), 100)
    except (ValueError, TypeError):
        page_size = default_page_size

    total = queryset.count()
    start = (page - 1) * page_size
    end = start + page_size

    return {
        'page': page,
        'page_size': page_size,
        'total': total,
        'total_pages': (total + page_size - 1) // page_size,  # ceiling division
        'results': list(queryset[start:end]),
    }


# ---------------------------------------------------------------------------
# Date range helper
# ---------------------------------------------------------------------------

def _get_date_range(request):
    """Parse period / start_date / end_date query params and return
    (start_date, end_date) as date objects.
    """
    period = request.query_params.get('period', 'month')
    today = timezone.now().date()

    if period == 'today':
        return today, today
    elif period == 'week':
        # Last 7 days
        return today - timedelta(days=6), today
    elif period == 'custom':
        start_str = request.query_params.get('start_date')
        end_str = request.query_params.get('end_date')
        try:
            from datetime import date as dt_date
            start = dt_date.fromisoformat(start_str) if start_str else today - timedelta(days=29)
            end = dt_date.fromisoformat(end_str) if end_str else today
            return start, end
        except (ValueError, TypeError):
            return today - timedelta(days=29), today
    else:
        # Default: month (last 30 days)
        return today - timedelta(days=29), today


# ---------------------------------------------------------------------------
# Admin Login
# ---------------------------------------------------------------------------

@api_view(['POST'])
@permission_classes([AllowAny])
def admin_login(request):
    """Authenticate an admin (is_staff=True) user and return JWT tokens."""
    email = request.data.get('email')
    password = request.data.get('password')

    if not email or not password:
        return Response(
            {'detail': 'Email and password are required.'},
            status=status.HTTP_400_BAD_REQUEST,
        )

    try:
        user = User.objects.get(email=email)
    except User.DoesNotExist:
        return Response(
            {'detail': 'Invalid credentials.'},
            status=status.HTTP_401_UNAUTHORIZED,
        )

    if not user.check_password(password):
        return Response(
            {'detail': 'Invalid credentials.'},
            status=status.HTTP_401_UNAUTHORIZED,
        )

    if not user.is_staff:
        return Response(
            {'detail': 'Admin access only.'},
            status=status.HTTP_403_FORBIDDEN,
        )

    payload = {
        'id': str(user.id),
        'email': user.email,
        'role': 'admin',
    }

    access_token = JWTAuthentication.generate_token(payload)
    refresh_token = JWTAuthentication.generate_refresh_token(payload)

    return Response({
        'access_token': access_token,
        'refresh_token': refresh_token,
        'user': {
            'id': str(user.id),
            'email': user.email,
            'name': user.name,
            'surname': user.surname,
            'is_staff': user.is_staff,
            'is_superuser': user.is_superuser,
        },
    }, status=status.HTTP_200_OK)


# ---------------------------------------------------------------------------
# Overview
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_overview(request):
    """High-level dashboard overview stats."""
    today = timezone.now().date()

    # Single aggregated query for user/driver counts instead of 4 separate queries
    user_stats = CustomUser.objects.aggregate(
        total_users=Count('id', filter=Q(role='user')),
        total_drivers=Count('id', filter=Q(role='driver')),
        drivers_online=Count('id', filter=Q(role='driver', driver_online=True)),
        drivers_available=Count('id', filter=Q(role='driver', driver_available=True)),
    )

    # Single aggregated query for delivery counts instead of 3 separate queries
    delivery_stats = Delivery.objects.aggregate(
        total_deliveries=Count('id'),
        successful_deliveries=Count('id', filter=Q(successful=True)),
        cancelled_deliveries=Count('id', filter=Q(successful=False)),
        today_deliveries=Count('id', filter=Q(date=today)),
    )

    pending_trip_requests = TripRequest.objects.filter(accepted=False, cancelled=False).count()

    revenue_stats = Payment.objects.filter(status='paid').aggregate(
        total_revenue=Sum('amount'),
        today_revenue=Sum('amount', filter=Q(paid_at__date=today)),
    )

    return Response({
        **user_stats,
        **delivery_stats,
        'pending_trip_requests': pending_trip_requests,
        'total_revenue': float(revenue_stats['total_revenue'] or 0),
        'today_revenue': float(revenue_stats['today_revenue'] or 0),
    })


# ---------------------------------------------------------------------------
# Delivery Metrics
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_delivery_metrics(request):
    """Delivery analytics with period filtering."""
    start_date, end_date = _get_date_range(request)

    deliveries = Delivery.objects.filter(date__gte=start_date, date__lte=end_date)

    total = deliveries.count()
    successful = deliveries.filter(successful=True).count()
    cancelled = deliveries.filter(successful=False).count()
    completion_rate = round((successful / total * 100), 2) if total > 0 else 0.0

    avg_fare = deliveries.aggregate(avg=Avg('fare'))['avg']
    avg_fare = float(avg_fare) if avg_fare else 0.0

    # Group by vehicle type
    by_vehicle = list(
        deliveries.values('vehicle')
        .annotate(count=Count('delivery_id'))
        .order_by('-count')
    )

    # Group by hour of start_time (for heatmap)
    by_hour = list(
        deliveries.annotate(hour=ExtractHour('start_time'))
        .values('hour')
        .annotate(count=Count('delivery_id'))
        .order_by('hour')
    )

    # Group by payment method
    by_payment_method = list(
        deliveries.values('payment_method')
        .annotate(count=Count('delivery_id'))
        .order_by('-count')
    )

    # Daily trend
    daily_trend = list(
        deliveries.annotate(day=TruncDate('date'))
        .values('day')
        .annotate(count=Count('delivery_id'))
        .order_by('day')
    )
    # Convert date objects to strings for JSON serialization
    for entry in daily_trend:
        entry['day'] = str(entry['day'])

    return Response({
        'period': {
            'start_date': str(start_date),
            'end_date': str(end_date),
        },
        'total': total,
        'successful': successful,
        'cancelled': cancelled,
        'completion_rate': completion_rate,
        'avg_fare': round(avg_fare, 2),
        'by_vehicle': by_vehicle,
        'by_hour': by_hour,
        'by_payment_method': by_payment_method,
        'daily_trend': daily_trend,
    })


# ---------------------------------------------------------------------------
# Financial Metrics
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_financial_metrics(request):
    """Revenue and payment analytics."""
    start_date, end_date = _get_date_range(request)

    # Payment queries within the date range
    payments = Payment.objects.filter(
        created_at__date__gte=start_date,
        created_at__date__lte=end_date,
    )
    paid_payments = payments.filter(status='paid')

    total_revenue = float(paid_payments.aggregate(total=Sum('amount'))['total'] or 0)

    # Revenue breakdown by payment method
    by_payment_method = list(
        paid_payments.values('payment_method')
        .annotate(total=Sum('amount'), count=Count('id'))
        .order_by('-total')
    )
    # Convert Decimal to float for JSON
    for entry in by_payment_method:
        entry['total'] = float(entry['total'])

    # Average fare from deliveries in this period
    avg_fare_result = (
        Delivery.objects.filter(date__gte=start_date, date__lte=end_date)
        .aggregate(avg=Avg('fare'))['avg']
    )
    avg_fare = float(avg_fare_result) if avg_fare_result else 0.0

    # Payment success rate
    total_payments_count = payments.count()
    paid_count = paid_payments.count()
    payment_success_rate = round(
        (paid_count / total_payments_count * 100), 2
    ) if total_payments_count > 0 else 0.0

    failed_payments_count = payments.filter(status='failed').count()
    pending_payments_count = payments.filter(status='pending').count()

    # Driver balance summaries (global, not period-filtered)
    balance_agg = DriverBalance.objects.aggregate(
        total_owed=Sum('owed_amount'),
        total_paid=Sum('total_paid'),
    )
    total_owed_to_drivers = float(balance_agg['total_owed'] or 0)
    total_paid_to_drivers = float(balance_agg['total_paid'] or 0)

    return Response({
        'period': {
            'start_date': str(start_date),
            'end_date': str(end_date),
        },
        'total_revenue': total_revenue,
        'by_payment_method': by_payment_method,
        'avg_fare': round(avg_fare, 2),
        'payment_success_rate': payment_success_rate,
        'total_payments': total_payments_count,
        'paid_payments': paid_count,
        'failed_payments_count': failed_payments_count,
        'pending_payments_count': pending_payments_count,
        'total_owed_to_drivers': total_owed_to_drivers,
        'total_paid_to_drivers': total_paid_to_drivers,
    })


# ---------------------------------------------------------------------------
# Driver Metrics
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_driver_metrics(request):
    """Driver performance analytics."""
    drivers = CustomUser.objects.filter(role='driver')

    total_drivers = drivers.count()
    online_drivers = drivers.filter(driver_online=True).count()
    available_drivers = drivers.filter(driver_available=True).count()

    avg_rating_result = drivers.aggregate(avg=Avg('rating'))['avg']
    avg_rating = float(avg_rating_result) if avg_rating_result else 0.0

    # Leaderboard: top 10 drivers by total_trips from DriverFinances
    # A driver may have multiple DriverFinances rows; sum their total_trips.
    leaderboard = list(
        DriverFinances.objects.values(
            'driver__id', 'driver__name', 'driver__surname',
            'driver__email', 'driver__rating',
        )
        .annotate(
            total_trips=Sum('total_trips'),
            total_earnings=Sum('earnings'),
        )
        .order_by('-total_trips')[:10]
    )
    # Flatten keys for cleaner JSON
    leaderboard_clean = []
    for entry in leaderboard:
        leaderboard_clean.append({
            'id': str(entry['driver__id']),
            'name': f"{entry['driver__name']} {entry['driver__surname']}",
            'email': entry['driver__email'],
            'total_trips': entry['total_trips'],
            'earnings': float(entry['total_earnings'] or 0),
            'rating': float(entry['driver__rating'] or 0),
        })

    # Acceptance rate
    total_trip_requests = TripRequest.objects.count()
    accepted_requests = TripRequest.objects.filter(accepted=True).count()
    acceptance_rate = round(
        (accepted_requests / total_trip_requests * 100), 2
    ) if total_trip_requests > 0 else 0.0

    # Churn indicator: drivers whose most recent DriverFinances.today
    # is more than 7 days old (or who have no DriverFinances at all)
    seven_days_ago = timezone.now().date() - timedelta(days=7)
    # Get drivers who DO have recent activity
    active_driver_ids = (
        DriverFinances.objects.filter(today__gte=seven_days_ago)
        .values_list('driver_id', flat=True)
        .distinct()
    )
    inactive_drivers = drivers.exclude(id__in=active_driver_ids)
    drivers_no_recent_activity = list(
        inactive_drivers.values('id', 'name', 'surname', 'email')[:20]
    )
    for entry in drivers_no_recent_activity:
        entry['id'] = str(entry['id'])

    return Response({
        'total_drivers': total_drivers,
        'online_drivers': online_drivers,
        'available_drivers': available_drivers,
        'avg_rating': round(avg_rating, 2),
        'leaderboard': leaderboard_clean,
        'acceptance_rate': acceptance_rate,
        'total_trip_requests': total_trip_requests,
        'accepted_requests': accepted_requests,
        'drivers_with_no_recent_activity': {
            'count': inactive_drivers.count(),
            'sample': drivers_no_recent_activity,
        },
    })


# ---------------------------------------------------------------------------
# User Metrics
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_user_metrics(request):
    """User growth and activity analytics."""
    today = timezone.now().date()
    thirty_days_ago = today - timedelta(days=29)

    total_users = CustomUser.objects.filter(role='user').count()

    # Active users: users who have at least one delivery as client in the last 30 days
    active_user_ids = (
        Delivery.objects.filter(
            date__gte=thirty_days_ago,
            client__role='user',
        )
        .values_list('client_id', flat=True)
        .distinct()
    )
    active_users = active_user_ids.count()

    # Signups over time (last 30 days)
    signups_over_time = list(
        CustomUser.objects.filter(
            role='user',
            date_joined__date__gte=thirty_days_ago,
        )
        .annotate(date=TruncDate('date_joined'))
        .values('date')
        .annotate(count=Count('id'))
        .order_by('date')
    )
    for entry in signups_over_time:
        entry['date'] = str(entry['date'])

    # Repeat users: users with 2+ deliveries as client
    repeat_users = (
        Delivery.objects.filter(client__role='user')
        .values('client_id')
        .annotate(delivery_count=Count('delivery_id'))
        .filter(delivery_count__gte=2)
        .count()
    )

    # Top 10 senders by delivery count
    top_senders = list(
        Delivery.objects.filter(client__role='user')
        .values('client__id', 'client__name', 'client__surname', 'client__email')
        .annotate(delivery_count=Count('delivery_id'))
        .order_by('-delivery_count')[:10]
    )
    top_senders_clean = []
    for entry in top_senders:
        top_senders_clean.append({
            'id': str(entry['client__id']),
            'name': f"{entry['client__name']} {entry['client__surname']}",
            'email': entry['client__email'],
            'delivery_count': entry['delivery_count'],
        })

    return Response({
        'total_users': total_users,
        'active_users': active_users,
        'signups_over_time': signups_over_time,
        'repeat_users': repeat_users,
        'top_senders': top_senders_clean,
    })


# ---------------------------------------------------------------------------
# Partner Metrics
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_partner_metrics(request):
    """B2B partner analytics."""
    total_partners = PartnerCompany.objects.count()
    active_partners = PartnerCompany.objects.filter(is_active=True).count()

    # Deliveries per partner
    deliveries_per_partner = list(
        PartnerDeliveryRequest.objects.values('partner__id', 'partner__name')
        .annotate(count=Count('id'))
        .order_by('-count')
    )
    for entry in deliveries_per_partner:
        entry['partner_id'] = str(entry.pop('partner__id'))
        entry['partner_name'] = entry.pop('partner__name')

    # By status
    by_status = list(
        PartnerDeliveryRequest.objects.values('status')
        .annotate(count=Count('id'))
        .order_by('-count')
    )

    # Full partner list with delivery counts
    partner_list = list(
        PartnerCompany.objects.annotate(
            deliveries_count=Count('delivery_requests')
        ).values(
            'id', 'name', 'is_active', 'contact_email',
            'rate_limit', 'created_at', 'deliveries_count',
        ).order_by('-deliveries_count')
    )
    for entry in partner_list:
        entry['id'] = str(entry['id'])
        entry['created_at'] = str(entry['created_at'])

    return Response({
        'total_partners': total_partners,
        'active_partners': active_partners,
        'deliveries_per_partner': deliveries_per_partner,
        'by_status': by_status,
        'partner_list': partner_list,
    })


# ---------------------------------------------------------------------------
# System Health (placeholder)
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_system_health(request):
    """Placeholder system health endpoint.

    Real metrics come from Prometheus / Grafana. This endpoint provides
    basic info and pointers to where actual monitoring data lives.
    """
    return Response({
        'total_db_connections': 'check PgBouncer',
        'redis_status': 'check Redis directly',
        'websocket_connections': 'check Go location service',
        'recent_errors': 'placeholder for future Prometheus integration',
        'note': 'Real-time metrics are available via Prometheus at /metrics on each service.',
    })


# ---------------------------------------------------------------------------
# Users List (paginated + searchable)
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_users_list(request):
    """Paginated list of all users with optional search."""
    q = request.query_params.get('q', '').strip()
    role_filter = request.query_params.get('role', '').strip()

    users = CustomUser.objects.all().order_by('-date_joined')

    if role_filter in ('user', 'driver'):
        users = users.filter(role=role_filter)

    if q:
        users = users.filter(
            Q(name__icontains=q)
            | Q(surname__icontains=q)
            | Q(email__icontains=q)
            | Q(phone_number__icontains=q)
        )

    users = users.values(
        'id', 'name', 'surname', 'email', 'phone_number',
        'role', 'date_joined', 'is_active', 'driver_online', 'rating',
    )

    paginated = paginate_queryset(users, request)

    # Convert UUID and datetime for JSON
    for entry in paginated['results']:
        entry['id'] = str(entry['id'])
        entry['date_joined'] = str(entry['date_joined'])
        entry['rating'] = float(entry['rating']) if entry['rating'] else None

    return Response(paginated)


# ---------------------------------------------------------------------------
# Deliveries List (paginated + filterable)
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_deliveries_list(request):
    """Paginated list of all deliveries with optional filters."""
    status_filter = request.query_params.get('status', '').strip()
    start_date = request.query_params.get('start_date')
    end_date = request.query_params.get('end_date')

    deliveries = Delivery.objects.select_related('driver', 'client').order_by('-date', '-start_time')

    # Filter by success status
    if status_filter == 'successful':
        deliveries = deliveries.filter(successful=True)
    elif status_filter == 'cancelled':
        deliveries = deliveries.filter(successful=False)

    # Filter by date range
    if start_date:
        try:
            from datetime import date as dt_date
            deliveries = deliveries.filter(date__gte=dt_date.fromisoformat(start_date))
        except (ValueError, TypeError):
            pass
    if end_date:
        try:
            from datetime import date as dt_date
            deliveries = deliveries.filter(date__lte=dt_date.fromisoformat(end_date))
        except (ValueError, TypeError):
            pass

    deliveries = deliveries.values(
        'delivery_id',
        'driver__name', 'driver__surname',
        'client__name', 'client__surname',
        'fare', 'payment_method', 'successful', 'date',
        'start_time', 'end_time', 'vehicle',
    )

    paginated = paginate_queryset(deliveries, request)

    # Flatten and convert types for JSON
    for entry in paginated['results']:
        entry['delivery_id'] = str(entry['delivery_id'])
        entry['driver_name'] = f"{entry.pop('driver__name')} {entry.pop('driver__surname')}"
        entry['client_name'] = f"{entry.pop('client__name')} {entry.pop('client__surname')}"
        entry['fare'] = float(entry['fare']) if entry['fare'] else None
        entry['date'] = str(entry['date']) if entry['date'] else None
        entry['start_time'] = str(entry['start_time']) if entry['start_time'] else None
        entry['end_time'] = str(entry['end_time']) if entry['end_time'] else None

    return Response(paginated)


# ---------------------------------------------------------------------------
# Payments List (paginated + filterable)
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_payments_list(request):
    """Paginated list of payments with optional filters."""
    status_filter = request.query_params.get('status', '').strip()
    method_filter = request.query_params.get('payment_method', '').strip()

    payments = Payment.objects.select_related('client').order_by('-created_at')

    if status_filter in ('pending', 'paid', 'failed', 'cancelled'):
        payments = payments.filter(status=status_filter)

    if method_filter in ('card', 'ecocash', 'onemoney'):
        payments = payments.filter(payment_method=method_filter)

    payments = payments.values(
        'id', 'client__name', 'client__surname',
        'amount', 'payment_method', 'status',
        'created_at', 'paid_at',
    )

    paginated = paginate_queryset(payments, request)

    for entry in paginated['results']:
        entry['id'] = str(entry['id'])
        entry['client_name'] = f"{entry.pop('client__name')} {entry.pop('client__surname')}"
        entry['amount'] = float(entry['amount']) if entry['amount'] else None
        entry['created_at'] = str(entry['created_at']) if entry['created_at'] else None
        entry['paid_at'] = str(entry['paid_at']) if entry['paid_at'] else None

    return Response(paginated)


# ---------------------------------------------------------------------------
# Create Partner Company
# ---------------------------------------------------------------------------

@api_view(['POST'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_create_partner(request):
    """Create a new B2B partner company.

    Auto-generates api_key and api_secret.  Both are returned in the
    response — api_secret is only visible at creation time.
    """
    name = (request.data.get('name') or '').strip()
    contact_email = (request.data.get('contact_email') or '').strip()

    if not name:
        return Response(
            {'detail': 'name is required.'},
            status=status.HTTP_400_BAD_REQUEST,
        )

    if not contact_email or '@' not in contact_email:
        return Response(
            {'detail': 'A valid contact_email is required.'},
            status=status.HTTP_400_BAD_REQUEST,
        )

    webhook_url = (request.data.get('webhook_url') or '').strip()
    rate_limit = request.data.get('rate_limit', 100)
    is_active = request.data.get('is_active', True)

    # Coerce rate_limit to int safely
    try:
        rate_limit = int(rate_limit)
    except (ValueError, TypeError):
        rate_limit = 100

    # Coerce is_active to bool
    if isinstance(is_active, str):
        is_active = is_active.lower() in ('true', '1', 'yes')

    api_key = secrets.token_hex(16)    # 32 hex characters
    api_secret = secrets.token_hex(32)  # 64 hex characters

    partner = PartnerCompany.objects.create(
        name=name,
        contact_email=contact_email,
        webhook_url=webhook_url,
        rate_limit=rate_limit,
        is_active=is_active,
        api_key=api_key,
        api_secret=api_secret,
    )

    return Response({
        'id': str(partner.id),
        'name': partner.name,
        'contact_email': partner.contact_email,
        'webhook_url': partner.webhook_url,
        'rate_limit': partner.rate_limit,
        'is_active': partner.is_active,
        'api_key': partner.api_key,
        'api_secret': partner.api_secret,
        'created_at': str(partner.created_at),
    }, status=status.HTTP_201_CREATED)


# ---------------------------------------------------------------------------
# User Detail
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_user_detail(request, user_id):
    """Return full details for a single user by UUID."""
    try:
        user = CustomUser.objects.get(id=user_id)
    except CustomUser.DoesNotExist:
        return Response(
            {'detail': 'User not found.'},
            status=status.HTTP_404_NOT_FOUND,
        )

    user_data = {
        'id': str(user.id),
        'name': user.name,
        'surname': user.surname,
        'email': user.email,
        'phone_number': user.phone_number,
        'role': user.role,
        'rating': float(user.rating) if user.rating is not None else None,
        'rating_count': user.rating_count,
        'date_joined': str(user.date_joined),
        'is_active': user.is_active,
        'driver_online': user.driver_online,
        'driver_available': user.driver_available,
        'streetAdress': user.streetAdress,
        'addressLine': user.addressLine,
        'city': user.city,
        'province': user.province,
        'postalCode': user.postalCode,
        'is_banned': user.is_banned,
        'banned_until': str(user.banned_until) if user.banned_until else None,
        'ban_reason': user.ban_reason,
    }

    # Recent deliveries as client (last 10)
    recent_deliveries = list(
        Delivery.objects.filter(client=user)
        .order_by('-date', '-start_time')[:10]
        .values(
            'delivery_id', 'fare', 'payment_method', 'successful',
            'date', 'start_time', 'end_time', 'vehicle',
            'driver__name', 'driver__surname',
        )
    )
    for entry in recent_deliveries:
        entry['delivery_id'] = str(entry['delivery_id'])
        entry['fare'] = float(entry['fare']) if entry['fare'] is not None else None
        entry['date'] = str(entry['date']) if entry['date'] else None
        entry['start_time'] = str(entry['start_time']) if entry['start_time'] else None
        entry['end_time'] = str(entry['end_time']) if entry['end_time'] else None
        entry['driver_name'] = f"{entry.pop('driver__name', '') or ''} {entry.pop('driver__surname', '') or ''}".strip()

    user_data['recent_deliveries'] = recent_deliveries
    user_data['delivery_count'] = Delivery.objects.filter(client=user).count()

    # Recent payments (last 10)
    recent_payments = list(
        Payment.objects.filter(client=user)
        .order_by('-created_at')[:10]
        .values('id', 'amount', 'payment_method', 'status', 'created_at', 'paid_at')
    )
    for entry in recent_payments:
        entry['id'] = str(entry['id'])
        entry['amount'] = float(entry['amount']) if entry['amount'] is not None else None
        entry['created_at'] = str(entry['created_at']) if entry['created_at'] else None
        entry['paid_at'] = str(entry['paid_at']) if entry['paid_at'] else None

    user_data['recent_payments'] = recent_payments

    # Driver-specific data
    if user.role == 'driver':
        # Latest DriverFinances
        latest_finances = (
            DriverFinances.objects.filter(driver=user)
            .order_by('-today')
            .first()
        )
        if latest_finances:
            user_data['driver_finances'] = {
                'earnings': float(latest_finances.earnings),
                'charges': float(latest_finances.charges),
                'profit': float(latest_finances.profit),
                'total_trips': latest_finances.total_trips,
                'today': str(latest_finances.today) if latest_finances.today else None,
                'week_start': str(latest_finances.week_start) if latest_finances.week_start else None,
                'week_end': str(latest_finances.week_end) if latest_finances.week_end else None,
                'month_start': str(latest_finances.month_start) if latest_finances.month_start else None,
                'month_end': str(latest_finances.month_end) if latest_finances.month_end else None,
            }
        else:
            user_data['driver_finances'] = None

        # DriverVehicle
        vehicle = DriverVehicle.objects.filter(driver=user).first()
        if vehicle:
            user_data['driver_vehicle'] = {
                'delivery_vehicle': vehicle.delivery_vehicle,
                'car_name': vehicle.car_name,
                'number_plate': vehicle.number_plate,
                'color': vehicle.color,
                'vehicle_model': vehicle.vehicle_model,
            }
        else:
            user_data['driver_vehicle'] = None

        # DriverBalance
        try:
            balance = DriverBalance.objects.get(driver=user)
            user_data['driver_balance'] = {
                'owed_amount': float(balance.owed_amount),
                'total_paid': float(balance.total_paid),
                'last_paid_at': str(balance.last_paid_at) if balance.last_paid_at else None,
            }
        except DriverBalance.DoesNotExist:
            user_data['driver_balance'] = None

    return Response(user_data)


# ---------------------------------------------------------------------------
# Ban User
# ---------------------------------------------------------------------------

@api_view(['POST'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_ban_user(request, user_id):
    """Ban a user with a reason and optional duration."""
    try:
        user = CustomUser.objects.get(id=user_id)
    except CustomUser.DoesNotExist:
        return Response(
            {'detail': 'User not found.'},
            status=status.HTTP_404_NOT_FOUND,
        )

    reason = (request.data.get('reason') or '').strip()
    if not reason:
        return Response(
            {'detail': 'reason is required.'},
            status=status.HTTP_400_BAD_REQUEST,
        )

    duration = (request.data.get('duration') or 'permanent').strip()
    custom_until = request.data.get('custom_until')

    now = timezone.now()
    banned_until = None

    duration_map = {
        '1_day': timedelta(days=1),
        '7_days': timedelta(days=7),
        '30_days': timedelta(days=30),
        '90_days': timedelta(days=90),
    }

    if duration == 'permanent':
        banned_until = None
    elif duration in duration_map:
        banned_until = now + duration_map[duration]
    elif duration == 'custom':
        if not custom_until:
            return Response(
                {'detail': 'custom_until is required when duration is "custom".'},
                status=status.HTTP_400_BAD_REQUEST,
            )
        try:
            from datetime import datetime as dt
            # Parse ISO format datetime string
            banned_until = timezone.make_aware(
                dt.fromisoformat(custom_until)
            ) if timezone.is_naive(
                dt.fromisoformat(custom_until)
            ) else dt.fromisoformat(custom_until)
        except (ValueError, TypeError):
            return Response(
                {'detail': 'custom_until must be a valid ISO datetime string.'},
                status=status.HTTP_400_BAD_REQUEST,
            )
    else:
        return Response(
            {'detail': f'Invalid duration: {duration}. Must be one of: permanent, 1_day, 7_days, 30_days, 90_days, custom.'},
            status=status.HTTP_400_BAD_REQUEST,
        )

    user.is_banned = True
    user.ban_reason = reason
    user.banned_until = banned_until
    user.is_active = False
    user.save(update_fields=['is_banned', 'ban_reason', 'banned_until', 'is_active'])

    return Response({
        'detail': 'User has been banned.',
        'user': {
            'id': str(user.id),
            'name': user.name,
            'surname': user.surname,
            'email': user.email,
            'is_banned': user.is_banned,
            'ban_reason': user.ban_reason,
            'banned_until': str(user.banned_until) if user.banned_until else None,
            'is_active': user.is_active,
        },
    })


# ---------------------------------------------------------------------------
# Unban User
# ---------------------------------------------------------------------------

@api_view(['POST'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_unban_user(request, user_id):
    """Unban a user — restores is_active and clears ban fields."""
    try:
        user = CustomUser.objects.get(id=user_id)
    except CustomUser.DoesNotExist:
        return Response(
            {'detail': 'User not found.'},
            status=status.HTTP_404_NOT_FOUND,
        )

    user.is_banned = False
    user.ban_reason = ''
    user.banned_until = None
    user.is_active = True
    user.save(update_fields=['is_banned', 'ban_reason', 'banned_until', 'is_active'])

    return Response({
        'detail': 'User has been unbanned.',
        'user': {
            'id': str(user.id),
            'name': user.name,
            'surname': user.surname,
            'email': user.email,
            'is_banned': user.is_banned,
            'is_active': user.is_active,
        },
    })


# ---------------------------------------------------------------------------
# Toggle Partner Active Status
# ---------------------------------------------------------------------------

@api_view(['POST'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_toggle_partner(request, partner_id):
    """Toggle a partner company's is_active status."""
    try:
        partner = PartnerCompany.objects.get(id=partner_id)
    except PartnerCompany.DoesNotExist:
        return Response(
            {'detail': 'Partner not found.'},
            status=status.HTTP_404_NOT_FOUND,
        )

    partner.is_active = not partner.is_active
    partner.save(update_fields=['is_active'])

    return Response({
        'detail': f'Partner {"activated" if partner.is_active else "deactivated"}.',
        'partner': {
            'id': str(partner.id),
            'name': partner.name,
            'contact_email': partner.contact_email,
            'is_active': partner.is_active,
            'rate_limit': partner.rate_limit,
            'created_at': str(partner.created_at),
        },
    })


# ---------------------------------------------------------------------------
# Partner Deposit
# ---------------------------------------------------------------------------

@api_view(['POST'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_partner_deposit(request, partner_id):
    """Deposit funds into a partner's balance.

    If the partner has not yet paid the $15 setup fee, it is automatically
    deducted from the first deposit.
    """
    try:
        partner = PartnerCompany.objects.get(id=partner_id)
    except PartnerCompany.DoesNotExist:
        return Response(
            {'detail': 'Partner not found.'},
            status=status.HTTP_404_NOT_FOUND,
        )

    # Validate amount
    raw_amount = request.data.get('amount')
    if raw_amount is None:
        return Response(
            {'detail': 'amount is required.'},
            status=status.HTTP_400_BAD_REQUEST,
        )
    try:
        amount = Decimal(str(raw_amount))
    except Exception:
        return Response(
            {'detail': 'amount must be a valid number.'},
            status=status.HTTP_400_BAD_REQUEST,
        )
    if amount <= 0:
        return Response(
            {'detail': 'amount must be a positive number.'},
            status=status.HTTP_400_BAD_REQUEST,
        )

    description = (request.data.get('description') or '').strip()
    transactions = []
    setup_fee_charged = False

    # Auto-deduct $15 setup fee from first deposit if not yet paid
    if not partner.setup_fee_paid:
        setup_fee = Decimal('15.00')
        partner.balance -= setup_fee
        partner.save(update_fields=['balance'])

        setup_txn = PartnerTransaction.objects.create(
            partner=partner,
            transaction_type='setup_fee',
            amount=-setup_fee,
            balance_after=partner.balance,
            description='One-time setup fee',
        )
        transactions.append({
            'id': str(setup_txn.id),
            'type': 'setup_fee',
            'amount': float(setup_txn.amount),
            'balance_after': float(setup_txn.balance_after),
        })

        partner.setup_fee_paid = True
        partner.save(update_fields=['setup_fee_paid'])
        setup_fee_charged = True

    # Add the deposit
    partner.balance += amount
    partner.save(update_fields=['balance'])

    deposit_txn = PartnerTransaction.objects.create(
        partner=partner,
        transaction_type='deposit',
        amount=amount,
        balance_after=partner.balance,
        description=description or f'Deposit of ${float(amount):.2f}',
    )
    transactions.append({
        'id': str(deposit_txn.id),
        'type': 'deposit',
        'amount': float(deposit_txn.amount),
        'balance_after': float(deposit_txn.balance_after),
    })

    return Response({
        'detail': 'Deposit successful.',
        'partner_id': str(partner.id),
        'partner_name': partner.name,
        'balance': float(partner.balance),
        'setup_fee_charged': setup_fee_charged,
        'transactions': transactions,
    })


# ---------------------------------------------------------------------------
# Partner Transactions (paginated)
# ---------------------------------------------------------------------------

@api_view(['GET'])
@permission_classes([IsAuthenticated, IsAdminUser])
def admin_partner_transactions(request, partner_id):
    """Return paginated list of PartnerTransactions for a partner."""
    try:
        partner = PartnerCompany.objects.get(id=partner_id)
    except PartnerCompany.DoesNotExist:
        return Response(
            {'detail': 'Partner not found.'},
            status=status.HTTP_404_NOT_FOUND,
        )

    qs = PartnerTransaction.objects.filter(partner=partner).order_by('-created_at')

    # Use the existing paginate_queryset helper
    qs_values = qs.values(
        'id', 'transaction_type', 'amount', 'balance_after',
        'description', 'fare', 'system_commission', 'driver_share',
        'driver__name', 'driver__surname',
        'delivery_request_id', 'created_at',
    )

    paginated = paginate_queryset(qs_values, request)

    # Clean up for JSON serialisation
    for entry in paginated['results']:
        entry['id'] = str(entry['id'])
        entry['amount'] = float(entry['amount']) if entry['amount'] is not None else None
        entry['balance_after'] = float(entry['balance_after']) if entry['balance_after'] is not None else None
        entry['fare'] = float(entry['fare']) if entry['fare'] is not None else None
        entry['system_commission'] = float(entry['system_commission']) if entry['system_commission'] is not None else None
        entry['driver_share'] = float(entry['driver_share']) if entry['driver_share'] is not None else None

        # Flatten driver name
        driver_first = entry.pop('driver__name', None) or ''
        driver_last = entry.pop('driver__surname', None) or ''
        entry['driver_name'] = f'{driver_first} {driver_last}'.strip() or None

        entry['delivery_request_id'] = str(entry['delivery_request_id']) if entry['delivery_request_id'] else None
        entry['created_at'] = str(entry['created_at']) if entry['created_at'] else None

    return Response({
        'partner_id': str(partner.id),
        'partner_name': partner.name,
        **paginated,
    })
