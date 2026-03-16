"""Unit tests for serializers."""
from django.test import TestCase, RequestFactory

from TumaGo_Server.models import CustomUser, DriverVehicle
from TumaGo_Server.serializers.userSerializer.authserializers import (
    LoginSerializer,
    SignupSerializer,
    UserInfo,
    UserSerializer,
)
from TumaGo_Server.serializers.driverSerializer.authSerializers import (
    RegisterSerializer,
    ResetPasswordSerializer,
    VehicleSerializer,
)


def make_user(email="ser@test.com", **kwargs):
    defaults = {
        "password": "testpass123",
        "name": "Ser",
        "surname": "Test",
        "phone_number": "0712345678",
        "streetAdress": "1 St",
        "city": "JHB",
        "province": "GP",
        "postalCode": "2000",
    }
    defaults.update(kwargs)
    return CustomUser.objects.create_user(email=email, **defaults)


class SignupSerializerTests(TestCase):
    def test_valid_signup(self):
        data = {
            "email": "new@test.com",
            "password": "StrongPass1",
            "phone_number": "0700000000",
        }
        serializer = SignupSerializer(data=data)
        self.assertTrue(serializer.is_valid(), serializer.errors)
        user = serializer.save()
        self.assertTrue(user.check_password("StrongPass1"))

    def test_duplicate_email_invalid(self):
        make_user(email="dup@test.com")
        serializer = SignupSerializer(
            data={"email": "dup@test.com", "password": "pass", "phone_number": "071"}
        )
        self.assertFalse(serializer.is_valid())
        self.assertIn("email", serializer.errors)

    def test_missing_password_invalid(self):
        serializer = SignupSerializer(data={"email": "x@test.com", "phone_number": "071"})
        self.assertFalse(serializer.is_valid())


class LoginSerializerTests(TestCase):
    def test_valid_login(self):
        make_user(email="login@test.com")
        serializer = LoginSerializer(
            data={"email": "login@test.com", "password": "testpass123"}
        )
        self.assertTrue(serializer.is_valid(), serializer.errors)
        self.assertIn("user", serializer.validated_data)

    def test_wrong_password(self):
        make_user(email="login2@test.com")
        serializer = LoginSerializer(
            data={"email": "login2@test.com", "password": "wrongpass"}
        )
        self.assertFalse(serializer.is_valid())

    def test_nonexistent_user(self):
        serializer = LoginSerializer(
            data={"email": "nope@test.com", "password": "pass"}
        )
        self.assertFalse(serializer.is_valid())


class UserInfoSerializerTests(TestCase):
    def test_partial_update(self):
        user = make_user()
        serializer = UserInfo(instance=user, data={"name": "Updated"}, partial=True)
        self.assertTrue(serializer.is_valid())
        updated = serializer.save()
        self.assertEqual(updated.name, "Updated")


class UserSerializerTests(TestCase):
    def test_serializes_user_fields(self):
        user = make_user()
        data = UserSerializer(user).data
        self.assertEqual(data["email"], "ser@test.com")
        self.assertIn("id", data)
        self.assertIn("role", data)
        self.assertNotIn("password", data)


class RegisterSerializerTests(TestCase):
    def test_driver_registration(self):
        data = {
            "email": "driver@test.com",
            "password": "DriverPass1",
            "name": "Driver",
            "surname": "Test",
            "phone_number": "0700000000",
            "streetAdress": "1 St",
            "addressLine": "",
            "city": "CPT",
            "province": "WC",
            "postalCode": "8000",
            "verifiedEmail": False,
        }
        serializer = RegisterSerializer(data=data)
        self.assertTrue(serializer.is_valid(), serializer.errors)
        driver = serializer.save()
        self.assertEqual(driver.role, CustomUser.DRIVER)
        self.assertTrue(driver.check_password("DriverPass1"))


class ResetPasswordSerializerTests(TestCase):
    def test_matching_passwords(self):
        serializer = ResetPasswordSerializer(
            data={
                "oldPassword": "old",
                "newPassword": "newpass123",
                "confirmPassword": "newpass123",
            }
        )
        self.assertTrue(serializer.is_valid(), serializer.errors)

    def test_mismatched_passwords(self):
        serializer = ResetPasswordSerializer(
            data={
                "oldPassword": "old",
                "newPassword": "newpass123",
                "confirmPassword": "different",
            }
        )
        self.assertFalse(serializer.is_valid())


class VehicleSerializerTests(TestCase):
    def test_create_vehicle(self):
        driver = make_user(email="vdriver@test.com", role=CustomUser.DRIVER)
        factory = RequestFactory()
        request = factory.post("/")
        request.user = driver
        data = {
            "delivery_vehicle": "van",
            "car_name": "Toyota",
            "number_plate": "GP 123",
            "color": "White",
            "vehicle_model": "2020",
        }
        serializer = VehicleSerializer(data=data, context={"request": request})
        self.assertTrue(serializer.is_valid(), serializer.errors)
        vehicle = serializer.save()
        self.assertEqual(vehicle.driver, driver)
        self.assertEqual(vehicle.delivery_vehicle, "van")
