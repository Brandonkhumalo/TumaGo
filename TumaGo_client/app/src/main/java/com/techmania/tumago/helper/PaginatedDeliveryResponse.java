package com.techmania.tumago.helper;

import com.techmania.tumago.Model.Deliveries;

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
