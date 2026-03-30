package com.techmania.tumago_driver.models;

import com.google.gson.annotations.SerializedName;

public class WalletTopupResponse {
    @SerializedName("payment_id")
    private String paymentId;

    @SerializedName("poll_url")
    private String pollUrl;

    @SerializedName("redirect_url")
    private String redirectUrl;

    private String instructions;

    public String getPaymentId() { return paymentId; }
    public String getPollUrl() { return pollUrl; }
    public String getRedirectUrl() { return redirectUrl; }
    public String getInstructions() { return instructions; }
}
