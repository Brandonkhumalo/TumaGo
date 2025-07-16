package com.techmania.tumago.Model;

import com.google.android.gms.maps.model.LatLng;

public class DeliveryResponse {
    private String driverName;
    private String vehicleType;
    private String licensePlate;
    private double fare;
    private LatLng driverLocation;
    private LatLng senderLocation;

    public DeliveryResponse(String driverName, LatLng senderLocation, LatLng driverLocation, double fare,
                            String licensePlate, String vehicleType) {
        this.driverName = driverName;
        this.senderLocation = senderLocation;
        this.driverLocation = driverLocation;
        this.fare = fare;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
    }

    public String getDriverName() {
        return driverName;
    }

    public LatLng getSenderLocation() {
        return senderLocation;
    }

    public LatLng getDriverLocation() {
        return driverLocation;
    }

    public double getFare() {
        return fare;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getVehicleType() {
        return vehicleType;
    }
}
