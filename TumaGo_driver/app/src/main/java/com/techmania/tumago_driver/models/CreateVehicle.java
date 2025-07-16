package com.techmania.tumago_driver.models;

public class CreateVehicle {
    String delivery_vehicle, car_name, number_plate, color, vehicle_model;

    public CreateVehicle(String vehicle_model, String color, String number_plate, String car_name, String delivery_vehicle) {
        this.vehicle_model = vehicle_model;
        this.color = color;
        this.number_plate = number_plate;
        this.car_name = car_name;
        this.delivery_vehicle = delivery_vehicle;
    }
}
