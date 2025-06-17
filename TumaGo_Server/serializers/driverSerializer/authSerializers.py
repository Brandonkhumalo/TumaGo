from rest_framework import serializers
from django.contrib.auth import get_user_model
from ...models import DriverVehicle, CustomUser

class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = get_user_model()
        fields = ['id', 'email', 'phone_number', 'name', 'surname', 'identity_number', 'profile_picture',
                  'streetAdress', 'addressLine', 'city', 'province', 'postalCode', 'rating', 'role', 'verifiedEmail', 'license']

class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True)

    class Meta:
        model = get_user_model()
        fields = ['name', 'surname', 'email', 'phone_number', 'password',
                  'streetAdress', 'addressLine', 'city', 'province', 'postalCode', 'verifiedEmail']

    def create(self, validated_data):
        #password = validated_data.pop('password')
        role = validated_data.get('role', get_user_model().DRIVER)

        # Create the CustomUser instance (driver or user)
        user = get_user_model().objects.create(
            email=validated_data['email'],
            name=validated_data['name'],
            surname=validated_data['surname'],
            phone_number=validated_data['phone_number'],
            password=validated_data['password'],
            streetAdress=validated_data['streetAdress'],
            addressLine=validated_data['addressLine'],
            city=validated_data['city'],
            province=validated_data['province'],
            postalCode=validated_data['postalCode'],
            verifiedEmail=validated_data['verifiedEmail'],
            role=role
        )
        return user
    
class VehicleSerializer(serializers.ModelSerializer):
    class Meta:
        model = DriverVehicle
        fields = ['delivery_vehicle', 'car_name', 'number_plate', 'color', 'vehicle_model']

    def create(self, validated_data):
        user = self.context['request'].user  # Get the user from context
        return DriverVehicle.objects.create(driver=user, **validated_data)
    
class LicenseUploadSerializer(serializers.ModelSerializer):
    class Meta:
        model = CustomUser
        fields = ['license']