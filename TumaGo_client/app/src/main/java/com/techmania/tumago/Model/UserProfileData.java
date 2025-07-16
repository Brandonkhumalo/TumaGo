package com.techmania.tumago.Model;

public class UserProfileData {
    String name;
    String surname;
    String identity_number;
    String streetAdress;
    String addressLine;
    String city;
    String province;
    String postalCode;
    Boolean verifiedEmail;

    public UserProfileData(String name, String surname, String identity_number, String streetAdress,
                           String addressLine, String city, String province, String postalCode, Boolean verifiedEmail) {
        this.name = name;
        this.surname = surname;
        this.identity_number = identity_number;
        this.streetAdress = streetAdress;
        this.addressLine = addressLine;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.verifiedEmail = verifiedEmail;

    }
}
