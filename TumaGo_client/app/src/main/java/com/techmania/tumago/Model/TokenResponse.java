package com.techmania.tumago.Model;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    @SerializedName("accessToken")
    private String access;

    @SerializedName("refreshToken")
    private String refresh;

    public String getAccess() {
        return access;
    }

    public String getRefresh() {
        return refresh;
    }
}
