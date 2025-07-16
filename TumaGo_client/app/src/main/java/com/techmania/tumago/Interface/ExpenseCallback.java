package com.techmania.tumago.Interface;

public interface ExpenseCallback {
    void onFareReceived(double Expense);
    void onFailure(Throwable t);
}
