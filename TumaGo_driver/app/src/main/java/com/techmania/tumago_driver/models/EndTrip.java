package com.techmania.tumago_driver.models;

import java.math.BigDecimal;

public class EndTrip {
    String delivery_id;
    BigDecimal delivery_cost;
    int rating;

    public EndTrip(String delivery_id, int rating, BigDecimal delivery_cost) {
        this.delivery_id = delivery_id;
        this.rating = rating;
        this.delivery_cost = delivery_cost;
    }
}
