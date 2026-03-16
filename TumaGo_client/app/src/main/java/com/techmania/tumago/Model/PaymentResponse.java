package com.techmania.tumago.Model;

import com.google.gson.annotations.SerializedName;

public class PaymentResponse {
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
