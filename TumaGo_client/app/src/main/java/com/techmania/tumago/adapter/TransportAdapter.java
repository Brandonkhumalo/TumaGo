package com.techmania.tumago.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago.Activities.ConfirmDelivery;
import com.techmania.tumago.Model.TransportModel;
import com.techmania.tumago.R;

import java.util.ArrayList;

public class TransportAdapter extends RecyclerView.Adapter<TransportAdapter.cardViewHolder> {
    ArrayList<TransportModel> arrayList;
    Context context;
    private int selectedPosition = 0;

    public TransportAdapter(ArrayList<TransportModel> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public cardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.transport_design, parent, false);
        return new cardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull cardViewHolder holder, final int position) {
        TransportModel transportmodel = arrayList.get(position);
        holder.transportName.setText(transportmodel.getTransportName());
        holder.price.setText("$" + String.valueOf(transportmodel.getPrice()));
        holder.transportImage.setImageResource(context.getResources()
                .getIdentifier(transportmodel.getImageName(), "drawable", context.getPackageName()));

        // Selected state border
        if (position == selectedPosition) {
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.light_blue));
            holder.cardView.setStrokeWidth(4);
        } else {
            holder.cardView.setStrokeWidth(0);
        }

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int prev = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(prev);
                notifyItemChanged(selectedPosition);

                Intent intent = new Intent(context, ConfirmDelivery.class);
                intent.putExtra("transportName", transportmodel.getTransportName());
                intent.putExtra("transportImage", transportmodel.getImageName());
                intent.putExtra("price", transportmodel.getPrice());
                context.startActivity(intent);
                if (context instanceof Activity) {
                    ((Activity) context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            }
        });
    };

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class cardViewHolder extends RecyclerView.ViewHolder {
        ImageView transportImage;
        TextView transportName;
        TextView price;
        MaterialCardView cardView;

        public cardViewHolder(@NonNull View itemView) {
            super(itemView);

            transportImage = itemView.findViewById(R.id.transportImage);
            transportName = itemView.findViewById(R.id.transportName);
            price = itemView.findViewById(R.id.transportPrice);
            cardView = itemView.findViewById(R.id.cardViewDesign);
        }
    }
}
