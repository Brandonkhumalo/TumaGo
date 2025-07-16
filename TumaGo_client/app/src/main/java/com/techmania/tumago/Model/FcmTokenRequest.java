package com.techmania.tumago.Model;

public class FcmTokenRequest {
    private String fcm_token;

    public FcmTokenRequest(String fcm_token) {
        this.fcm_token = fcm_token;
    }

    public String getFcm_token() {
        return fcm_token;
    }

    public void setFcm_token(String fcm_token) {
        this.fcm_token = fcm_token;
    }
}