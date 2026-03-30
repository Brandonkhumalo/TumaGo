package com.techmania.tumago_driver.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WalletTransactionResponse {
    private List<WalletTransaction> transactions;
    private int offset;

    @SerializedName("page_size")
    private int pageSize;

    public List<WalletTransaction> getTransactions() { return transactions; }
    public int getOffset() { return offset; }
    public int getPageSize() { return pageSize; }
}
