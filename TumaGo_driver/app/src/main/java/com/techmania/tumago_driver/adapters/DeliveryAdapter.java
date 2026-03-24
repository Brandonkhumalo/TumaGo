package com.techmania.tumago_driver.adapters;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.recyclerview.widget.DiffUtil;

public class DeliveryAdapter extends RecyclerView.Adapter<DeliveryAdapter.cardviewholder> {

    ArrayList<Deliveries> arrayList;
    Context context;
    private static final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        Deliveries item = arrayList.get(position);

        holder.delivery_id.setText("ID: " + item.getDelivery_id());
        holder.fare.setText("$" + String.format(Locale.US, "%.2f", item.getFare()));

        // Format date: "2026-03-13T14:04:00Z" → "Friday 13 March 2026 14:04"
        String startTime = item.getStart_time();
        if (startTime != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date parsed = inputFormat.parse(startTime);
                if (parsed != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE d MMMM yyyy HH:mm", Locale.US);
                    holder.date.setText(outputFormat.format(parsed));
                }
            } catch (ParseException e) {
                holder.date.setText(item.getDate() != null ? item.getDate() : "");
            }
        } else {
            holder.date.setText(item.getDate() != null ? item.getDate() : "");
        }

        // Reverse geocode destination coordinates on background thread
        double lat = item.getDestination_lat();
        double lng = item.getDestination_lng();
        holder.destination.setText("Loading...");

        geocodeExecutor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(holder.itemView.getContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String streetNumber = address.getSubThoroughfare();
                    String streetName = address.getThoroughfare();
                    String city = address.getLocality();
                    String streetFull = (streetNumber != null ? streetNumber + " " : "") + (streetName != null ? streetName : "");
                    String formattedAddress = streetFull + (city != null ? ", " + city : "");
                    mainHandler.post(() -> holder.destination.setText(
                            formattedAddress.isEmpty() ? "Unknown location" : formattedAddress));
                } else {
                    mainHandler.post(() -> holder.destination.setText("Unknown location"));
                }
            } catch (IOException e) {
                mainHandler.post(() -> holder.destination.setText("Error loading address"));
            }
        });

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

    public void submitList(ArrayList<Deliveries> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return arrayList.size(); }
            @Override public int getNewListSize() { return newList.size(); }
            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return Objects.equals(arrayList.get(oldPos).getDelivery_id(),
                        newList.get(newPos).getDelivery_id());
            }
            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                Deliveries o = arrayList.get(oldPos), n = newList.get(newPos);
                return Objects.equals(o.getDelivery_id(), n.getDelivery_id())
                        && o.getFare() == n.getFare()
                        && o.getDestination_lat() == n.getDestination_lat()
                        && o.getDestination_lng() == n.getDestination_lng();
            }
        });
        arrayList.clear();
        arrayList.addAll(newList);
        result.dispatchUpdatesTo(this);
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
