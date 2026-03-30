package com.techmania.tumago_driver.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RefundRequestResponse {
    @SerializedName("refund_requests")
    private List<RefundRequest> refundRequests;

    public List<RefundRequest> getRefundRequests() { return refundRequests; }
}
