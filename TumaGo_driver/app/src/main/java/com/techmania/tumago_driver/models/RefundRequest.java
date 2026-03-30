package com.techmania.tumago_driver.models;

import com.google.gson.annotations.SerializedName;

public class RefundRequest {
    private String id;

    @SerializedName("delivery_id")
    private String deliveryId;

    private String amount;
    private String reason;
    private String status;

    @SerializedName("admin_notes")
    private String adminNotes;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("reviewed_at")
    private String reviewedAt;

    public String getId() { return id; }
    public String getDeliveryId() { return deliveryId; }
    public String getAmount() { return amount; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getAdminNotes() { return adminNotes; }
    public String getCreatedAt() { return createdAt; }
    public String getReviewedAt() { return reviewedAt; }
}
