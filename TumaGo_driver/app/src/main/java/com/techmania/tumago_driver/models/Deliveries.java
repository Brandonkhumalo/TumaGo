package com.techmania.tumago_driver.models;

public class Deliveries {
    private String delivery_id;
    private double origin_lat;
    private double origin_lng;
    private double destination_lat;
    private double destination_lng;
    private String vehicle;
    private double fare;
    private String payment_method;
    private String date;
    private String start_time;

    public String getDate() {
        return date;
    }

    public String getStart_time() {
        return start_time;
    }

    public String getDelivery_id() {
        return delivery_id;
    }

    public double getOrigin_lat() {
        return origin_lat;
    }

    public double getOrigin_lng() {
        return origin_lng;
    }

    public double getDestination_lat() {
        return destination_lat;
    }

    public double getDestination_lng() {
        return destination_lng;
    }

    public String getVehicle() {
        return vehicle;
    }

    public double getFare() {
        return fare;
    }

    public String getPayment_method() {
        return payment_method;
    }
}
