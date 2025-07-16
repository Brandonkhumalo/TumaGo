package com.techmania.tumago_driver.models;

public class CreateDriver {
    private String name, surname, email, password, phone_number, streetAdress, addressLine, province, city, postalCode;
    Boolean verifiedEmail;

    public CreateDriver(String name, String surname, String email, String password, String phone_number, Boolean verifiedEmail,
                        String streetAdress, String addressLine, String city, String province, String postalCode) {
        this.name = name;
        this.postalCode = postalCode;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.phone_number = phone_number;
        this.verifiedEmail = verifiedEmail;
        this.streetAdress = streetAdress;
        this.addressLine = addressLine;
        this.city = city;
        this.province = province;
    }
}
