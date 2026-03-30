package com.techmania.tumago_driver.models;

import com.google.gson.annotations.SerializedName;

public class WalletBalance {
    @SerializedName("wallet_balance")
    private String walletBalance;

    @SerializedName("owed_balance")
    private String owedBalance;

    @SerializedName("total_paid")
    private String totalPaid;

    @SerializedName("last_paid_at")
    private String lastPaidAt;

    public String getWalletBalance() { return walletBalance; }
    public String getOwedBalance() { return owedBalance; }
    public String getTotalPaid() { return totalPaid; }
    public String getLastPaidAt() { return lastPaidAt; }
}
