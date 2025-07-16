package com.techmania.tumago_driver.models;

public class Driver {
    String name;
    String surname;
    String phone_number;
    String email;
    double rating;
    String streetAdress;
    String addressLine;
    String province;
    String city;
    String postalCode;
    String role;
    Boolean verifiedEmail;
    Boolean license;

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getPhoneNumber() {
        return phone_number;
    }

    public String getEmail() {
        return email;
    }

    public double getRating() {
        return rating;
    }

    public String getStreet() {
        return streetAdress;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getRole() {
        return role;
    }

    public Boolean getVerified() {
        return verifiedEmail;
    }

    public Boolean getLicense() {
        return license;
    }
}
