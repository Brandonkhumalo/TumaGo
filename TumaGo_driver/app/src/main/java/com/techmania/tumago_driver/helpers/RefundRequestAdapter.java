package com.techmania.tumago_driver.helpers;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.models.RefundRequest;

import java.util.List;

public class RefundRequestAdapter extends RecyclerView.Adapter<RefundRequestAdapter.ViewHolder> {

    private final List<RefundRequest> requests;

    public RefundRequestAdapter(List<RefundRequest> requests) {
        this.requests = requests;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_refund_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RefundRequest r = requests.get(position);

        holder.refundAmount.setText("$" + (r.getAmount() != null ? r.getAmount() : "0.00"));
        holder.refundReason.setText(r.getReason() != null ? r.getReason() : "");

        // Status badge
        String status = r.getStatus() != null ? r.getStatus() : "pending";
        holder.refundStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(20f);
        switch (status) {
            case "approved":
                badge.setColor(Color.parseColor("#D1FAE5"));
                holder.refundStatus.setTextColor(Color.parseColor("#065F46"));
                break;
            case "denied":
                badge.setColor(Color.parseColor("#FEE2E2"));
                holder.refundStatus.setTextColor(Color.parseColor("#991B1B"));
                break;
            default:
                badge.setColor(Color.parseColor("#FEF3C7"));
                holder.refundStatus.setTextColor(Color.parseColor("#92400E"));
                break;
        }
        holder.refundStatus.setBackground(badge);

        // Admin notes
        if (r.getAdminNotes() != null && !r.getAdminNotes().isEmpty()) {
            holder.refundAdminNotes.setText("Admin: " + r.getAdminNotes());
            holder.refundAdminNotes.setVisibility(View.VISIBLE);
        } else {
            holder.refundAdminNotes.setVisibility(View.GONE);
        }

        // Date
        String date = r.getCreatedAt() != null ? r.getCreatedAt() : "";
        if (date.length() > 10) date = date.substring(0, 10);
        holder.refundDate.setText(date);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView refundAmount, refundStatus, refundReason, refundAdminNotes, refundDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            refundAmount = itemView.findViewById(R.id.refundAmount);
            refundStatus = itemView.findViewById(R.id.refundStatus);
            refundReason = itemView.findViewById(R.id.refundReason);
            refundAdminNotes = itemView.findViewById(R.id.refundAdminNotes);
            refundDate = itemView.findViewById(R.id.refundDate);
        }
    }
}
