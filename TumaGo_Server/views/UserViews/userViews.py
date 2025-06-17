from rest_framework.response import Response
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated, AllowAny
from ...serializers.userSerializer.authserializers import UserSerializer
from decimal import Decimal

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def GetUserData(request):
    user = request.user
    serializer = UserSerializer(user)
    return Response(serializer.data, status=status.HTTP_200_OK)

@api_view(["GET"])
@permission_classes([AllowAny])
def GetTripExpenses(request):
    try:
        distance = request.query_params.get("distance", None)

        if distance is None:
            return Response({"error": "Distance parameter is required."}, status=status.HTTP_400_BAD_REQUEST)

        try:
            distance = float(distance)
        except ValueError:
            return Response({"error": "Invalid distance value."}, status=status.HTTP_400_BAD_REQUEST)

        if distance <= 0:
            return Response({"error": "Distance must be greater than 0."}, status=status.HTTP_400_BAD_REQUEST)

        scooterPrice = round(Decimal(0.50 * distance) + Decimal(0.20), 2)
        vanPrice = round(Decimal(1.10 * distance) + Decimal(0.30), 2)
        truckPrice = round(Decimal(2.30 * distance) + Decimal(0.50), 2)

        fare = {
            "scooter": scooterPrice,
            "van": vanPrice,
            "truck": truckPrice
        }

        return Response(fare, status=status.HTTP_200_OK)

    except (TypeError, ValueError):
        return Response({"error": "Invalid distance or time parameters."}, status=status.HTTP_400_BAD_REQUEST)