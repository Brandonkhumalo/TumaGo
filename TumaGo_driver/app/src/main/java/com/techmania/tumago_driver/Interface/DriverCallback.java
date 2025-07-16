package com.techmania.tumago_driver.Interface;

public interface DriverCallback {
    void onDriverDataReceived(String name, String surname, String phoneNumber
            ,String email, double rating, String street, String addressLine, String province
            ,String city, String postalCode, String role, Boolean verified, Boolean license);
    void onFailure(Throwable t);
}
