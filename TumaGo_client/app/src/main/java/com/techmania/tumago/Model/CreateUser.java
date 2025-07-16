package com.techmania.tumago.Model;

public class CreateUser {
    private String email;
    private String password;
    private String phone_number;

    public CreateUser( String email, String password, String phone_number) {
        this.email = email;
        this.password = password;
        this.phone_number = phone_number;
    }
}
