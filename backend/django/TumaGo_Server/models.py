from django.db import models
from django.contrib.auth.models import AbstractUser, BaseUserManager
import uuid
from django.utils import timezone
from calendar import monthrange
from datetime import timedelta

class CustomUserManager(BaseUserManager):
    def create_user(self, email, password=None, **extra_fields):
        if not email:
            raise ValueError('The Email field must be set')
        email = self.normalize_email(email)
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)

        return self.create_user(email, password, **extra_fields)


class CustomUser(AbstractUser):
    USER = 'user'
    DRIVER = 'driver'

    ROLE_CHOICES = [
        (USER, 'User'),
        (DRIVER, 'Driver'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    role = models.CharField(max_length=10, choices=ROLE_CHOICES, default=USER, null=False, db_index=True)

    email = models.EmailField(unique=True, null=False)
    verifiedEmail = models.BooleanField(default=False)
    username = None  # Optional if using email login
    fcm_token = models.CharField(max_length=255, blank=True, null=True)

    remember = models.BooleanField(default=False)
    phone_number = models.CharField(max_length=13, null=False)
    name = models.CharField(max_length=100, null=False)
    surname = models.CharField(max_length=50, null=False)
    rating = models.DecimalField(max_digits=3, decimal_places=1, default=5, null=False, blank=False)
    rating_count = models.PositiveIntegerField(default=0)

    identity_number = models.CharField(max_length=20, null=True, blank=True)
    streetAdress = models.CharField(max_length=20, null=False)
    addressLine = models.CharField(max_length=20, null=True, blank=True)
    city = models.CharField(max_length=20, null=False)
    province = models.CharField(max_length=20, null=False)
    postalCode = models.CharField(max_length=20, null=False)

    profile_picture = models.ImageField(upload_to='profile_pictures/', null=True, blank=True)
    identity_picture = models.ImageField(upload_to='id_pictures/', null=True, blank=True)
    identity = models.BooleanField(default=False)
    license_picture = models.ImageField(upload_to='License/', null=True, blank=True) 
    license = models.BooleanField(default=False)

    driver_online = models.BooleanField(default=False, db_index=True)
    driver_available = models.BooleanField(default=False, db_index=True)

    # Admin ban fields
    is_banned = models.BooleanField(default=False, db_index=True)
    banned_until = models.DateTimeField(null=True, blank=True)
    ban_reason = models.TextField(blank=True, default='')

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['name', 'surname', 'phone_number']

    objects = CustomUserManager()

    class Meta:
        indexes = [
            models.Index(fields=['role'], name='idx_user_role'),
            # Composite index for the matching query: online + available + role
            models.Index(fields=['role', 'driver_online', 'driver_available'], name='idx_driver_matching'),
        ]

    def __str__(self):
        return f"{self.email} ({self.role})"
    
class BlacklistedToken(models.Model):
    token = models.CharField(max_length=255, unique=True)
    blacklisted_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Blacklisted at {self.blacklisted_at}"
    
class TermsAndConditions(models.Model):
    user = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='terms_and_conditions')
    terms_and_conditions = models.BooleanField(default=False)
    date = models.DateField(auto_now_add=True)

class DriverLocations(models.Model):
    driver = models.OneToOneField(CustomUser, on_delete=models.CASCADE, related_name='driver_locations')
    latitude = models.CharField(max_length=50, null=True, blank=True)
    longitude = models.CharField(max_length=50, null=True, blank=True)
    
class DriverFinances(models.Model):
    driver = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='driver_finances')
    earnings = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    charges = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    profit = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    month_start = models.DateField(null=True, blank=True)
    month_end = models.DateField(null=True, blank=True)
    week_start = models.DateField(null=True, blank=True)
    week_end = models.DateField(null=True, blank=True)
    today = models.DateField(null=True, blank=True)
    total_trips = models.IntegerField(default=0)

    def save(self, *args, **kwargs):
        today = timezone.now().date()
        self.today = today

        # Increment total_trips using the already-loaded value to avoid an
        # extra SELECT query on every save (previous version did objects.get()).
        if self.pk and self.total_trips is not None:
            self.total_trips += 1
        else:
            self.total_trips = 1

        # Calculate week_start (previous Sunday) and week_end (Saturday)
        days_since_sunday = (today.weekday() + 1) % 7
        self.week_start = today - timedelta(days=days_since_sunday)
        self.week_end = self.week_start + timedelta(days=6)

        # Calculate month_start (1st day) and month_end (last day)
        self.month_start = today.replace(day=1)
        last_day = monthrange(today.year, today.month)[1]
        self.month_end = today.replace(day=last_day)

        # Calculate profit automatically
        self.profit = self.earnings - self.charges

        super().save(*args, **kwargs)

class DriverVehicle(models.Model):
    driver = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='driver_vehicles')
    delivery_vehicle = models.CharField(max_length=50, null=False, blank=False)
    car_name = models.CharField(max_length=50, null=False, blank=False)
    number_plate = models.CharField(max_length=50, null=False, blank=False)
    color = models.CharField(max_length=50, null=False, blank=False)
    vehicle_model = models.CharField(max_length=50, null=False, blank=False)

class Delivery(models.Model):
    delivery_id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    driver = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='driver_deliveries')
    client = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='client_deliveries')
    start_time = models.DateTimeField(null=False, blank=False)
    end_time = models.DateTimeField(null=True, blank=True)
    waiting_time = models.DateTimeField(null=True, blank=True)
    origin_lat = models.FloatField(null=True, blank=True)
    origin_lng = models.FloatField(null=True, blank=True)
    destination_lat = models.FloatField(null=True, blank=True)
    destination_lng = models.FloatField(null=True, blank=True)
    date = models.DateField(auto_now_add=True)
    vehicle = models.CharField(max_length=50, null=True, blank=True)
    fare = models.DecimalField(max_digits=6, decimal_places=2, null=False, blank=False)
    payment_method = models.CharField(max_length=50, null=False, blank=False)
    successful = models.BooleanField(default=True) #True is successful, False is unsuccessful

    class Meta:
        indexes = [
            # Driver delivery history lookup (driver + date)
            models.Index(fields=['driver', 'date'], name='idx_delivery_driver_date'),
            # Client delivery history lookup (client + date)
            models.Index(fields=['client', 'date'], name='idx_delivery_client_date'),
        ]

class Payment(models.Model):
    PENDING = 'pending'
    PAID = 'paid'
    FAILED = 'failed'
    CANCELLED = 'cancelled'

    STATUS_CHOICES = [
        (PENDING, 'Pending'),
        (PAID, 'Paid'),
        (FAILED, 'Failed'),
        (CANCELLED, 'Cancelled'),
    ]

    # Payment methods that go through Paynow (not cash)
    CARD = 'card'
    ECOCASH = 'ecocash'
    ONEMONEY = 'onemoney'

    PAYMENT_METHOD_CHOICES = [
        (CARD, 'Card'),
        (ECOCASH, 'EcoCash'),
        (ONEMONEY, 'OneMoney'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    client = models.ForeignKey(CustomUser, on_delete=models.CASCADE, related_name='payments')
    delivery = models.ForeignKey(
        'Delivery', on_delete=models.SET_NULL, null=True, blank=True, related_name='payments'
    )
    amount = models.DecimalField(max_digits=10, decimal_places=2)
    payment_method = models.CharField(max_length=50, choices=PAYMENT_METHOD_CHOICES)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default=PENDING, db_index=True)
    paynow_reference = models.CharField(max_length=255, blank=True)
    poll_url = models.URLField(max_length=500, blank=True)
    redirect_url = models.URLField(max_length=500, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    paid_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        indexes = [
            models.Index(fields=['client', 'status'], name='idx_payment_client_status'),
        ]

    def __str__(self):
        return f"Payment {self.id} - {self.status} - ${self.amount}"


class DriverBalance(models.Model):
    driver = models.OneToOneField(CustomUser, on_delete=models.CASCADE, related_name='balance')
    owed_amount = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    total_paid = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    last_paid_at = models.DateTimeField(null=True, blank=True)

    def __str__(self):
        return f"{self.driver.email} — owed ${self.owed_amount}"


class TripRequest(models.Model):
    requester = models.ForeignKey(CustomUser, on_delete=models.CASCADE)
    delivery_details = models.JSONField()
    accepted = models.BooleanField(default=False)
    cancelled = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [
            models.Index(fields=['accepted', 'cancelled'], name='idx_trip_pending'),
        ]


class PartnerCompany(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=200)
    api_key = models.CharField(max_length=64, unique=True, db_index=True)
    api_secret = models.CharField(max_length=128)
    webhook_url = models.URLField(blank=True)
    is_active = models.BooleanField(default=True)
    rate_limit = models.IntegerField(default=100)
    created_at = models.DateTimeField(auto_now_add=True)
    contact_email = models.EmailField()

    # Balance system — partners pre-deposit funds and deliveries deduct from balance
    balance = models.DecimalField(max_digits=12, decimal_places=2, default=0.00)
    setup_fee_paid = models.BooleanField(default=False)

    # Commission rate — percentage the platform keeps (e.g. 20 = 20%)
    commission_rate = models.DecimalField(max_digits=5, decimal_places=2, default=20.00)

    def __str__(self):
        return self.name


class PartnerDeliveryRequest(models.Model):
    STATUSES = [
        ('pending', 'Pending'),
        ('matching', 'Matching'),
        ('driver_assigned', 'Driver Assigned'),
        ('picked_up', 'Picked Up'),
        ('delivered', 'Delivered'),
        ('cancelled', 'Cancelled'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    partner = models.ForeignKey(PartnerCompany, on_delete=models.CASCADE, related_name='delivery_requests')
    partner_reference = models.CharField(max_length=200)
    trip_request = models.ForeignKey(TripRequest, null=True, blank=True, on_delete=models.SET_NULL, related_name='partner_delivery')
    delivery = models.ForeignKey(Delivery, null=True, blank=True, on_delete=models.SET_NULL, related_name='partner_delivery')
    status = models.CharField(max_length=30, choices=STATUSES, default='pending')
    pickup_contact = models.CharField(max_length=20, blank=True)
    dropoff_contact = models.CharField(max_length=20, blank=True)
    package_description = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [
            models.Index(fields=['partner', 'status'], name='idx_partner_delivery_status'),
            models.Index(fields=['partner', 'partner_reference'], name='idx_partner_ref'),
        ]

    def __str__(self):
        return f"{self.partner.name} — {self.partner_reference} ({self.status})"


class PartnerTransaction(models.Model):
    """Audit trail for partner balance changes — deposits, delivery charges, refunds, fees."""
    DEPOSIT = 'deposit'
    DELIVERY_CHARGE = 'delivery_charge'
    REFUND = 'refund'
    SETUP_FEE = 'setup_fee'
    ADJUSTMENT = 'adjustment'

    TYPE_CHOICES = [
        (DEPOSIT, 'Deposit'),
        (DELIVERY_CHARGE, 'Delivery Charge'),
        (REFUND, 'Refund'),
        (SETUP_FEE, 'Setup Fee'),
        (ADJUSTMENT, 'Adjustment'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    partner = models.ForeignKey(PartnerCompany, on_delete=models.CASCADE, related_name='transactions')
    transaction_type = models.CharField(max_length=20, choices=TYPE_CHOICES)
    amount = models.DecimalField(max_digits=12, decimal_places=2)
    balance_after = models.DecimalField(max_digits=12, decimal_places=2)
    description = models.TextField(blank=True)
    # Link to specific delivery if this is a delivery charge or refund
    delivery_request = models.ForeignKey(
        PartnerDeliveryRequest, null=True, blank=True,
        on_delete=models.SET_NULL, related_name='transactions',
    )
    # Financial split details for delivery charges
    fare = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    system_commission = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    driver_share = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    driver = models.ForeignKey(
        CustomUser, null=True, blank=True,
        on_delete=models.SET_NULL, related_name='partner_earnings',
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['partner', 'transaction_type'], name='idx_partner_txn_type'),
        ]

    def __str__(self):
        sign = '+' if self.amount > 0 else ''
        return f"{self.partner.name} {sign}{self.amount} ({self.transaction_type})"


class SESSuppressionList(models.Model):
    """Emails that bounced or were complained about — never send to these again.
    SES suspends accounts above 5% bounce rate or 0.1% complaint rate."""

    BOUNCE = 'bounce'
    COMPLAINT = 'complaint'
    TYPE_CHOICES = [
        (BOUNCE, 'Bounce'),
        (COMPLAINT, 'Complaint'),
    ]

    email = models.EmailField(unique=True, db_index=True)
    reason = models.CharField(max_length=10, choices=TYPE_CHOICES)
    bounce_type = models.CharField(max_length=50, blank=True)  # "Permanent", "Transient", etc.
    diagnostic = models.TextField(blank=True)  # Raw diagnostic from SES
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        verbose_name = 'SES Suppression Entry'
        verbose_name_plural = 'SES Suppression List'

    def __str__(self):
        return f"{self.email} ({self.reason})"