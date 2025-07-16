package com.techmania.tumago.Model;

public class User {
    private String name;
    private String surname;
    private String phone_number;
    private String email;
    private double rating;
    private String streetAdress;
    private String addressLine;
    private String province;
    private String city;
    private String postalCode;
    private String role;

    public User(String name, String surname, String phone_number, String email
            ,double rating,String streetAdress, String addressLine, String province, String city
            ,String postalCode, String role) {

        this.name = name;
        this.surname = surname;
        this.phone_number = phone_number;
        this.email = email;
        this.rating = rating;
        this.streetAdress = streetAdress;
        this.addressLine = addressLine;
        this.province = province;
        this.city = city;
        this.postalCode = postalCode;
        this.role = role;
    }

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
}
