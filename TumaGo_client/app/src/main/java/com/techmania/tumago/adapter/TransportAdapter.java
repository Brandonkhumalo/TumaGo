package com.techmania.tumago.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.techmania.tumago.Activities.ConfirmDelivery;
import com.techmania.tumago.Model.TransportModel;
import com.techmania.tumago.R;

import java.util.ArrayList;

public class TransportAdapter extends RecyclerView.Adapter<TransportAdapter.cardViewHolder> {
    ArrayList<TransportModel> arrayList;
    Context context;

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

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ConfirmDelivery.class);
                intent.putExtra("transportName", transportmodel.getTransportName());
                intent.putExtra("price", transportmodel.getPrice());
                context.startActivity(intent);
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
        CardView cardView;

        public cardViewHolder(@NonNull View itemView) {
            super(itemView);

            transportImage = itemView.findViewById(R.id.transportImage);
            transportName = itemView.findViewById(R.id.transportName);
            price = itemView.findViewById(R.id.transportPrice);
            cardView = itemView.findViewById(R.id.cardViewDesign);
        }
    }
}
