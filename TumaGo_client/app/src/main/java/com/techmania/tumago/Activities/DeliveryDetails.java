package com.techmania.tumago.Activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.Token;
import com.techmania.tumago.helper.UiHelper;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryDetails extends AppCompatActivity {

    TextView driverName, driverRating, totalDriverRatings, numberPlate, carDetails, driverInitials;
    TextView deliveryIdText, dateText, pickupText, destinationText, vehicleText, paymentText, fareText;
    String vehicle_color, vehicle, vehicle_model;
    MaterialCardView cancelDelivery, goHome;
    LinearLayout layout1, layout2;
    ProgressBar progressBar;
    String delivery_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_details);

        delivery_id = Token.getDelivery_id(this);

        layout1 = findViewById(R.id.Layout1);
        layout2 = findViewById(R.id.Layout2);
        layout2.setVisibility(View.GONE);
        layout1.setVisibility(View.VISIBLE);
        goHome = findViewById(R.id.goHome);

        driverName = findViewById(R.id.name);
        driverRating = findViewById(R.id.rating);
        totalDriverRatings = findViewById(R.id.totalRatings);
        driverInitials = findViewById(R.id.driverInitials);
        numberPlate = findViewById(R.id.numberPlate);
        carDetails = findViewById(R.id.carDetails);
        ImageView vehicleImage = findViewById(R.id.vehicleImage);
        cancelDelivery = findViewById(R.id.cancelDelivery);
        progressBar = findViewById(R.id.progressBar);
        deliveryIdText = findViewById(R.id.deliveryId);
        dateText = findViewById(R.id.date);
        pickupText = findViewById(R.id.pickup);
        destinationText = findViewById(R.id.destination);
        vehicleText = findViewById(R.id.vehicle);
        paymentText = findViewById(R.id.payment);
        fareText = findViewById(R.id.fare);

        Intent intent = getIntent();
        vehicle_color = intent.getStringExtra("color");
        vehicle = intent.getStringExtra("vehicle");
        vehicle_model = intent.getStringExtra("vehicle_model");
        if (vehicle_color == null) vehicle_color = "";
        if (vehicle == null) vehicle = "";
        if (vehicle_model == null) vehicle_model = "";

        String driverFullName = intent.getStringExtra("driver");
        if (driverFullName == null) driverFullName = "";
        driverName.setText(driverFullName);
        driverRating.setText(intent.getStringExtra("rating"));
        totalDriverRatings.setText(intent.getStringExtra("total_ratings"));

        // Set driver avatar initials
        if (!driverFullName.trim().isEmpty()) {
            String[] parts = driverFullName.trim().split("\\s+");
            if (parts[0].length() > 0) {
                String initials = String.valueOf(parts[0].charAt(0));
                if (parts.length > 1 && parts[parts.length - 1].length() > 0) {
                    initials += parts[parts.length - 1].charAt(0);
                }
                driverInitials.setText(initials.toUpperCase());
            }
        }
        numberPlate.setText(intent.getStringExtra("number_plate"));
        carDetails.setText(vehicle_color + " ~ " + vehicle + " ~ " + vehicle_model);

        deliveryIdText.setText(delivery_id);
        dateText.setText(intent.getStringExtra("date"));
        vehicleText.setText(intent.getStringExtra("vehicle_type"));
        paymentText.setText(intent.getStringExtra("payment_method"));
        String fare = intent.getStringExtra("fare");
        if (fare != null) {
            fareText.setText("$" + fare);
        }

        // Reverse geocode pickup and destination
        try {
            String originLat = intent.getStringExtra("origin_lat");
            String originLng = intent.getStringExtra("origin_lng");
            String destLat = intent.getStringExtra("destination_lat");
            String destLng = intent.getStringExtra("destination_lng");
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            if (originLat != null && originLng != null) {
                List<Address> fromAddresses = geocoder.getFromLocation(
                        Double.parseDouble(originLat), Double.parseDouble(originLng), 1);
                if (fromAddresses != null && !fromAddresses.isEmpty()) {
                    pickupText.setText(fromAddresses.get(0).getAddressLine(0));
                }
            }
            if (destLat != null && destLng != null) {
                List<Address> toAddresses = geocoder.getFromLocation(
                        Double.parseDouble(destLat), Double.parseDouble(destLng), 1);
                if (toAddresses != null && !toAddresses.isEmpty()) {
                    destinationText.setText(toAddresses.get(0).getAddressLine(0));
                }
            }
        } catch (Exception e) {
            Log.e("DeliveryDetails", "Geocoding failed", e);
        }

        System.out.println(driverName + " " + driverRating + " " + totalDriverRatings + " " + numberPlate + " " + carDetails);

        switch (vehicle.toLowerCase()) {
            case "scooter":
                vehicleImage.setImageResource(R.drawable.scooter);
                break;
            case "van":
                vehicleImage.setImageResource(R.drawable.vanm);
                break;
            case "truck":
                vehicleImage.setImageResource(R.drawable.truckm);
                break;
            default:
                vehicleImage.setImageResource(R.drawable.scooter);
                break;
        }

        cancelDelivery.setOnClickListener(v -> {
            CancelDelivery();
        });

        goHome.setOnClickListener(v -> {
            Intent i = new Intent(DeliveryDetails.this, HomeActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check once
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }

        // Start monitoring changes
        NetworkUtils.registerNetworkCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        NetworkUtils.unregisterNetworkCallback(this);
    }

    private void CancelDelivery(){
        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.cancel_delivery(authHeader, delivery_id);

        UiHelper.showLoading(progressBar, cancelDelivery);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                UiHelper.hideLoading(progressBar, cancelDelivery);
                if(response.isSuccessful()){
                    layout1.setVisibility(View.GONE);
                    layout2.setVisibility(View.VISIBLE);
                } else {
                    UiHelper.showRetry(findViewById(android.R.id.content), "Failed to cancel delivery", () -> CancelDelivery());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                UiHelper.hideLoading(progressBar, cancelDelivery);
                Log.d("FAILED TO CANCEL DELIVERY", t.getMessage());
                UiHelper.showRetry(findViewById(android.R.id.content), "Failed to cancel delivery", () -> CancelDelivery());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}



