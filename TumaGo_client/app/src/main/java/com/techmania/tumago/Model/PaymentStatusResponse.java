package com.techmania.tumago.Model;

import com.google.gson.annotations.SerializedName;

public class PaymentStatusResponse {
    @SerializedName("payment_id")
    private String paymentId;

    private String status;
    private boolean paid;

    public String getPaymentId() { return paymentId; }
    public String getStatus() { return status; }
    public boolean isPaid() { return paid; }
}
