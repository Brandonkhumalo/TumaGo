package com.techmania.tumago_driver.helpers;

import com.techmania.tumago_driver.models.Deliveries;

import java.util.List;

public class PaginatedDeliveryResponse {
    private String next;
    private String previous;
    private List<Deliveries> results;

    public String getNext() {
        return next;
    }
    public String getPrevious() {
        return previous;
    }
    public List<Deliveries> getResults() {
        return results;
    }
}
