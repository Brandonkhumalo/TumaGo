package com.techmania.tumago_driver.models;

public class LoginDriver {
    private String password;
    private String email;

    public LoginDriver(String email, String password) {
        this.password = password;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
