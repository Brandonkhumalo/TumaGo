package com.techmania.tumago.Model;

public class CreateUser {
    private String email;
    private String password;
    private String phone_number;
    private String name;
    private String surname;

    public CreateUser(String email, String password, String phone_number, String name, String surname) {
        this.email = email;
        this.password = password;
        this.phone_number = phone_number;
        this.name = name;
        this.surname = surname;
    }
}
