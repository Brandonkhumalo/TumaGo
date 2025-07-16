package com.techmania.tumago.Model;

import com.google.android.gms.maps.model.LatLng;

public class DeliveryRequest {
    private double origin_lat;
    private double origin_lng;
    private double destination_lat;
    private double destination_lng;
    private String vehicle;
    private double fare;
    private String payment_method;

    public DeliveryRequest(LatLng origin, LatLng destination, String vehicle, double fare, String payment_method) {
        this.origin_lat = origin.latitude;
        this.origin_lng = origin.longitude;
        this.destination_lat = destination.latitude;
        this.destination_lng = destination.longitude;
        this.vehicle = vehicle;
        this.fare = fare;
        this.payment_method = payment_method;
    }
}
