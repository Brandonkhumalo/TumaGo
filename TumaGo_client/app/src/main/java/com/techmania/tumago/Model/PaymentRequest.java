package com.techmania.tumago.Model;

public class PaymentRequest {
    private double amount;
    private String payment_method;
    private String phone;

    public PaymentRequest(double amount, String paymentMethod, String phone) {
        this.amount = amount;
        this.payment_method = paymentMethod;
        this.phone = phone;
    }
}
