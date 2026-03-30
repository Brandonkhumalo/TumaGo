package com.techmania.tumago_driver.models;

import com.google.gson.annotations.SerializedName;

public class WalletTransaction {
    private String id;
    private String type;
    private String amount;

    @SerializedName("balance_after")
    private String balanceAfter;

    private String description;

    @SerializedName("delivery_id")
    private String deliveryId;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() { return id; }
    public String getType() { return type; }
    public String getAmount() { return amount; }
    public String getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public String getDeliveryId() { return deliveryId; }
    public String getCreatedAt() { return createdAt; }
}
