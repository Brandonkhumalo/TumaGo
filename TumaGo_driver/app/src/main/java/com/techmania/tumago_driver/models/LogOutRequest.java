package com.techmania.tumago_driver.models;

import com.google.gson.annotations.SerializedName;

public class LogOutRequest {

    @SerializedName("refreshToken")
    private String refreshToken;

    public LogOutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
