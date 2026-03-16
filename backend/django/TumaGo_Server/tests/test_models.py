"""Unit tests for TumaGo_Server models."""
from datetime import timedelta
from decimal import Decimal

from django.test import TestCase
from django.utils import timezone

from TumaGo_Server.models import (
    BlacklistedToken,
    CustomUser,
    Delivery,
    DriverFinances,
    DriverLocations,
    DriverVehicle,
    TermsAndConditions,
    TripRequest,
)


def make_user(**kwargs):
    """Helper to create a user with sensible defaults."""
    defaults = {
        "email": "test@example.com",
        "password": "testpass123",
        "name": "Test",
        "surname": "User",
        "phone_number": "0712345678",
        "streetAdress": "123 Main",
        "city": "Joburg",
        "province": "Gauteng",
        "postalCode": "2000",
    }
    defaults.update(kwargs)
    return CustomUser.objects.create_user(**defaults)


class CustomUserManagerTests(TestCase):
    def test_create_user_with_email(self):
        user = make_user()
        self.assertEqual(user.email, "test@example.com")
        self.assertTrue(user.check_password("testpass123"))
        self.assertEqual(user.role, CustomUser.USER)
        self.assertFalse(user.is_staff)
        self.assertFalse(user.is_superuser)

    def test_create_user_without_email_raises(self):
        with self.assertRaises(ValueError):
            CustomUser.objects.create_user(
                email="",
                password="pass",
                name="A",
                surname="B",
                phone_number="000",
                streetAdress="x",
                city="x",
                province="x",
                postalCode="x",
            )

    def test_create_superuser(self):
        su = CustomUser.objects.create_superuser(
            email="admin@example.com",
            password="admin123",
            name="Admin",
            surname="User",
            phone_number="0700000000",
            streetAdress="1 Admin",
            city="CPT",
            province="WC",
            postalCode="8000",
        )
        self.assertTrue(su.is_staff)
        self.assertTrue(su.is_superuser)

    def test_email_is_normalised(self):
        user = make_user(email="Test@EXAMPLE.COM")
        self.assertEqual(user.email, "Test@example.com")

    def test_email_is_unique(self):
        make_user()
        with self.assertRaises(Exception):
            make_user()


class CustomUserModelTests(TestCase):
    def test_str_representation(self):
        user = make_user()
        self.assertEqual(str(user), "test@example.com (user)")

    def test_driver_role(self):
        driver = make_user(email="driver@test.com", role=CustomUser.DRIVER)
        self.assertEqual(driver.role, CustomUser.DRIVER)
        self.assertEqual(str(driver), "driver@test.com (driver)")

    def test_default_rating(self):
        user = make_user()
        self.assertEqual(user.rating, Decimal("5"))
        self.assertEqual(user.rating_count, 0)

    def test_default_driver_status(self):
        user = make_user()
        self.assertFalse(user.driver_online)
        self.assertFalse(user.driver_available)

    def test_uuid_primary_key(self):
        user = make_user()
        self.assertIsNotNone(user.id)
        self.assertEqual(len(str(user.id)), 36)  # UUID format


class BlacklistedTokenTests(TestCase):
    def test_create_blacklisted_token(self):
        bt = BlacklistedToken.objects.create(token="abc.def.ghi")
        self.assertEqual(bt.token, "abc.def.ghi")
        self.assertIsNotNone(bt.blacklisted_at)

    def test_token_uniqueness(self):
        BlacklistedToken.objects.create(token="token1")
        with self.assertRaises(Exception):
            BlacklistedToken.objects.create(token="token1")


class TermsAndConditionsTests(TestCase):
    def test_create_terms(self):
        user = make_user()
        terms = TermsAndConditions.objects.create(user=user, terms_and_conditions=True)
        self.assertTrue(terms.terms_and_conditions)
        self.assertIsNotNone(terms.date)

    def test_default_false(self):
        user = make_user()
        terms = TermsAndConditions.objects.create(user=user)
        self.assertFalse(terms.terms_and_conditions)


class DriverLocationsTests(TestCase):
    def test_create_location(self):
        driver = make_user(role=CustomUser.DRIVER)
        loc = DriverLocations.objects.create(
            driver=driver, latitude="-26.2041", longitude="28.0473"
        )
        self.assertEqual(loc.latitude, "-26.2041")
        self.assertEqual(loc.longitude, "28.0473")

    def test_nullable_coordinates(self):
        driver = make_user(role=CustomUser.DRIVER)
        loc = DriverLocations.objects.create(driver=driver)
        self.assertIsNone(loc.latitude)
        self.assertIsNone(loc.longitude)


class DriverFinancesTests(TestCase):
    def test_first_save_sets_total_trips_to_1(self):
        driver = make_user(role=CustomUser.DRIVER)
        df = DriverFinances(driver=driver, earnings=Decimal("100.00"), charges=Decimal("10.00"))
        df.save()
        self.assertEqual(df.total_trips, 1)
        self.assertEqual(df.profit, Decimal("90.00"))

    def test_subsequent_save_increments_trips(self):
        driver = make_user(role=CustomUser.DRIVER)
        df = DriverFinances.objects.create(driver=driver, earnings=Decimal("50.00"), charges=Decimal("5.00"))
        self.assertEqual(df.total_trips, 1)
        df.earnings = Decimal("150.00")
        df.save()
        self.assertEqual(df.total_trips, 2)

    def test_profit_auto_calculation(self):
        driver = make_user(role=CustomUser.DRIVER)
        df = DriverFinances(driver=driver, earnings=Decimal("200.00"), charges=Decimal("30.00"))
        df.save()
        self.assertEqual(df.profit, Decimal("170.00"))

    def test_date_fields_set_on_save(self):
        driver = make_user(role=CustomUser.DRIVER)
        df = DriverFinances(driver=driver)
        df.save()
        today = timezone.now().date()
        self.assertEqual(df.today, today)
        self.assertEqual(df.month_start, today.replace(day=1))
        self.assertIsNotNone(df.month_end)
        self.assertIsNotNone(df.week_start)
        self.assertIsNotNone(df.week_end)
        # week_start should be a Sunday (weekday 6)
        self.assertEqual(df.week_start.weekday(), 6)

    def test_week_end_is_saturday(self):
        driver = make_user(role=CustomUser.DRIVER)
        df = DriverFinances(driver=driver)
        df.save()
        # week_end should be week_start + 6 days (Saturday)
        self.assertEqual(df.week_end, df.week_start + timedelta(days=6))
        self.assertEqual(df.week_end.weekday(), 5)  # Saturday


class DriverVehicleTests(TestCase):
    def test_create_vehicle(self):
        driver = make_user(role=CustomUser.DRIVER)
        v = DriverVehicle.objects.create(
            driver=driver,
            delivery_vehicle="van",
            car_name="Toyota Hilux",
            number_plate="GP 123",
            color="White",
            vehicle_model="2020",
        )
        self.assertEqual(v.delivery_vehicle, "van")
        self.assertEqual(v.driver, driver)


class DeliveryTests(TestCase):
    def test_create_delivery(self):
        client = make_user(email="client@test.com")
        driver = make_user(email="driver@test.com", role=CustomUser.DRIVER)
        delivery = Delivery.objects.create(
            driver=driver,
            client=client,
            start_time=timezone.now(),
            origin_lat=-26.2041,
            origin_lng=28.0473,
            destination_lat=-26.1076,
            destination_lng=28.0567,
            fare=Decimal("45.50"),
            payment_method="cash",
            vehicle="van",
        )
        self.assertTrue(delivery.successful)
        self.assertIsNotNone(delivery.delivery_id)
        self.assertIsNotNone(delivery.date)

    def test_cancel_delivery(self):
        client = make_user(email="client@test.com")
        driver = make_user(email="driver@test.com", role=CustomUser.DRIVER)
        delivery = Delivery.objects.create(
            driver=driver,
            client=client,
            start_time=timezone.now(),
            fare=Decimal("30.00"),
            payment_method="cash",
        )
        delivery.successful = False
        delivery.save()
        delivery.refresh_from_db()
        self.assertFalse(delivery.successful)


class TripRequestTests(TestCase):
    def test_create_trip_request(self):
        user = make_user()
        tr = TripRequest.objects.create(
            requester=user,
            delivery_details={"origin": "A", "destination": "B", "vehicle": "scooter"},
        )
        self.assertFalse(tr.accepted)
        self.assertIsNotNone(tr.created_at)
        self.assertEqual(tr.delivery_details["vehicle"], "scooter")

    def test_accept_trip(self):
        user = make_user()
        tr = TripRequest.objects.create(requester=user, delivery_details={})
        tr.accepted = True
        tr.save()
        tr.refresh_from_db()
        self.assertTrue(tr.accepted)
