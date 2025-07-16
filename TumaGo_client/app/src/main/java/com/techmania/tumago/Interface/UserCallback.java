package com.techmania.tumago.Interface;

public interface UserCallback {
    void onUserDataReceived(String name, String surname, String phoneNumber
            ,String email, double rating, String street, String addressLine, String province
            ,String city, String postalCode, String role);
    void onFailure(Throwable t);
}
