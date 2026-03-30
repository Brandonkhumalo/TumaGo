package com.techmania.tumago_driver.helpers;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.models.WalletTransaction;

import java.util.List;

public class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.ViewHolder> {

    private final List<WalletTransaction> transactions;

    public WalletTransactionAdapter(List<WalletTransaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletTransaction txn = transactions.get(position);

        // Icon and label based on type
        String type = txn.getType() != null ? txn.getType() : "";
        switch (type) {
            case "topup":
                holder.txnIcon.setText("+");
                holder.txnIcon.setTextColor(Color.parseColor("#10B981"));
                holder.txnDescription.setText("Wallet Top-up");
                break;
            case "commission":
                holder.txnIcon.setText("-");
                holder.txnIcon.setTextColor(Color.parseColor("#EF4444"));
                holder.txnDescription.setText("Commission");
                break;
            case "refund":
                holder.txnIcon.setText("R");
                holder.txnIcon.setTextColor(Color.parseColor("#10B981"));
                holder.txnDescription.setText("Commission Refund");
                break;
            case "transfer_in":
                holder.txnIcon.setText("T");
                holder.txnIcon.setTextColor(Color.parseColor("#10B981"));
                holder.txnDescription.setText("Transfer In");
                break;
            case "transfer_out":
                holder.txnIcon.setText("T");
                holder.txnIcon.setTextColor(Color.parseColor("#EF4444"));
                holder.txnDescription.setText("Transfer Out");
                break;
            default:
                holder.txnIcon.setText("?");
                holder.txnDescription.setText(txn.getDescription() != null ? txn.getDescription() : "Transaction");
                break;
        }

        // Override with description if available
        if (txn.getDescription() != null && !txn.getDescription().isEmpty()) {
            holder.txnDescription.setText(txn.getDescription());
        }

        // Amount with color
        String amount = txn.getAmount() != null ? txn.getAmount() : "0.00";
        try {
            double val = Double.parseDouble(amount);
            if (val >= 0) {
                holder.txnAmount.setText("+$" + String.format("%.2f", val));
                holder.txnAmount.setTextColor(Color.parseColor("#10B981"));
            } else {
                holder.txnAmount.setText("-$" + String.format("%.2f", Math.abs(val)));
                holder.txnAmount.setTextColor(Color.parseColor("#EF4444"));
            }
        } catch (NumberFormatException e) {
            holder.txnAmount.setText("$" + amount);
            holder.txnAmount.setTextColor(Color.parseColor("#3a3a3c"));
        }

        // Date — show just date portion
        String date = txn.getCreatedAt() != null ? txn.getCreatedAt() : "";
        if (date.length() > 10) date = date.substring(0, 10);
        holder.txnDate.setText(date);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txnIcon, txnDescription, txnDate, txnAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txnIcon = itemView.findViewById(R.id.txnIcon);
            txnDescription = itemView.findViewById(R.id.txnDescription);
            txnDate = itemView.findViewById(R.id.txnDate);
            txnAmount = itemView.findViewById(R.id.txnAmount);
        }
    }
}
