package com.techmania.tumago_driver.models;

import com.google.gson.annotations.SerializedName;

public class WalletTopupStatus {
    @SerializedName("payment_id")
    private String paymentId;

    private String status;
    private boolean paid;

    @SerializedName("wallet_balance")
    private String walletBalance;

    public String getPaymentId() { return paymentId; }
    public String getStatus() { return status; }
    public boolean isPaid() { return paid; }
    public String getWalletBalance() { return walletBalance; }
}
