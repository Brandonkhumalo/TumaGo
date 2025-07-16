package com.techmania.tumago.Model;

import java.util.Date;

public class Deliveries {

    private String delivery_id;
    private double origin_lat;
    private double origin_lng;
    private double destination_lat;
    private double destination_lng;
    private String vehicle;
    private double fare;
    private String payment_method;
    private Date date;

    public Deliveries(String delivery_id, double origin_lat, double origin_lng, double destination_lat,
                      double destination_lng, String vehicle, double fare, String payment_method, Date date) {
        this.delivery_id = delivery_id;
        this.origin_lat = origin_lat;
        this.origin_lng = origin_lng;
        this.destination_lat = destination_lat;
        this.destination_lng = destination_lng;
        this.vehicle = vehicle;
        this.fare = fare;
        this.payment_method = payment_method;
        this.date = date;
    }

    public Date getDate() {
        return date;
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
