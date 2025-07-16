package com.techmania.tumago_driver.adapters;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.models.Deliveries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeliveryAdapter extends RecyclerView.Adapter<DeliveryAdapter.cardviewholder> {

    ArrayList<Deliveries> arrayList;
    Context context;

    public DeliveryAdapter(ArrayList<Deliveries> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public cardviewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.delivery_design, parent, false);
        return new cardviewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull cardviewholder holder, int position) {
        holder.delivery_id.setText(arrayList.get(position).getDelivery_id());
        holder.date.setText(arrayList.get(position).getDate().toString());
        holder.fare.setText(String.valueOf(arrayList.get(position).getFare()));
        Geocoder geocoder = new Geocoder(holder.itemView.getContext(), Locale.getDefault());
        double lat = arrayList.get(position).getDestination_lat();
        double lng = arrayList.get(position).getDestination_lng();

        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                String streetNumber = address.getSubThoroughfare(); // e.g., "123"
                String streetName = address.getThoroughfare();      // e.g., "Main St"
                String city = address.getLocality();                // e.g., "Harare"

                // Combine into one line, or display separately
                String formattedAddress = streetNumber + " " + streetName + ", " + city;
                holder.destination.setText(formattedAddress);
            } else {
                holder.destination.setText("Unknown location");
            }
        } catch (IOException e) {
            holder.destination.setText("Error loading address");
            e.printStackTrace();
        }

        // Set image based on vehicle type
        String vehicleType = arrayList.get(position).getVehicle().toLowerCase();

        switch (vehicleType) {
            case "scooter":
                holder.image.setImageResource(R.drawable.scooter);
                break;
            case "truck":
                holder.image.setImageResource(R.drawable.truck);
                break;
            case "van":
                holder.image.setImageResource(R.drawable.van);
                break;
            default:
                holder.image.setImageResource(R.drawable.scooter); // optional fallback
                break;
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class cardviewholder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView destination, date, delivery_id, fare;

        public cardviewholder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.delivery_image);
            destination = itemView.findViewById(R.id.destination);
            date = itemView.findViewById(R.id.date);
            delivery_id = itemView.findViewById(R.id.deliveryId);
            fare = itemView.findViewById(R.id.price);
        }
    }
}
