package com.techmania.tumago.Activities;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.Token;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackDelivery extends AppCompatActivity {

    private EditText inputDeliveryId;
    private ProgressBar progressBar;
    private ScrollView resultSection;
    private LinearLayout errorSection;

    // Result fields
    private TextView deliveryIdText, dateText, driverNameText, driverRatingText,
            driverRatingCountText, driverInitialsText;
    private TextView pickupText, destinationText, vehicleText, paymentText, fareText;
    private TextView statusText, errorText;
    private View statusDot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_delivery);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        inputDeliveryId = findViewById(R.id.inputDeliveryId);
        progressBar = findViewById(R.id.progressBar);
        resultSection = findViewById(R.id.resultSection);
        errorSection = findViewById(R.id.errorSection);

        MaterialCardView btnTrack = findViewById(R.id.btnTrack);
        deliveryIdText = findViewById(R.id.deliveryId);
        dateText = findViewById(R.id.date);
        driverNameText = findViewById(R.id.driverName);
        driverRatingText = findViewById(R.id.driverRating);
        driverRatingCountText = findViewById(R.id.driverRatingCount);
        driverInitialsText = findViewById(R.id.driverInitials);
        pickupText = findViewById(R.id.pickup);
        destinationText = findViewById(R.id.destination);
        vehicleText = findViewById(R.id.vehicle);
        paymentText = findViewById(R.id.payment);
        fareText = findViewById(R.id.fare);
        statusText = findViewById(R.id.statusText);
        statusDot = findViewById(R.id.statusDot);
        errorText = findViewById(R.id.errorText);

        btnTrack.setOnClickListener(v -> {
            String deliveryId = inputDeliveryId.getText().toString().trim();
            if (deliveryId.isEmpty()) {
                Toast.makeText(this, "Please enter a delivery ID", Toast.LENGTH_SHORT).show();
                return;
            }
            trackDelivery(deliveryId);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }
        NetworkUtils.registerNetworkCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        NetworkUtils.unregisterNetworkCallback(this);
    }

    private void trackDelivery(String deliveryId) {
        String accessToken = Token.getAccessToken(this);
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading, hide results and errors
        progressBar.setVisibility(View.VISIBLE);
        resultSection.setVisibility(View.GONE);
        errorSection.setVisibility(View.GONE);

        String authHeader = "Bearer " + accessToken;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.trackDelivery(deliveryId, authHeader);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonStr = response.body().string();
                        JSONObject data = new JSONObject(jsonStr);

                        displayResult(data);
                    } catch (Exception e) {
                        Log.e("TrackDelivery", "Parse error", e);
                        showError("Failed to parse delivery data");
                    }
                } else {
                    showError("Delivery not found. Make sure you entered the correct ID.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("Something went wrong. Please try again.");
                Log.e("TrackDelivery", "Request failed: " + t.getMessage());
            }
        });
    }

    private void displayResult(JSONObject data) {
        try {
            resultSection.setVisibility(View.VISIBLE);
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            resultSection.startAnimation(slideUp);
            errorSection.setVisibility(View.GONE);

            boolean inTransit = data.optBoolean("in_transit", false);
            boolean successful = data.optBoolean("successful", true);

            // Set status
            if (inTransit) {
                statusText.setText("In Transit");
                statusText.setTextColor(getResources().getColor(R.color.orange_accent, getTheme()));
                statusDot.setBackgroundColor(getResources().getColor(R.color.orange_accent, getTheme()));
            } else if (successful) {
                statusText.setText("Delivered");
                statusText.setTextColor(getResources().getColor(R.color.success, getTheme()));
                statusDot.setBackgroundColor(getResources().getColor(R.color.success, getTheme()));
            } else {
                statusText.setText("Cancelled");
                statusText.setTextColor(getResources().getColor(R.color.error, getTheme()));
                statusDot.setBackgroundColor(getResources().getColor(R.color.error, getTheme()));
            }

            // Delivery ID and date
            deliveryIdText.setText(data.optString("delivery_id", ""));
            dateText.setText(data.optString("date", ""));

            // Driver info
            String driverName = data.optString("driver_name", "");
            driverNameText.setText(driverName);
            double rating = data.optDouble("driver_rating", 0.0);
            driverRatingText.setText(String.format(Locale.getDefault(), "%.1f", rating));
            int ratingCount = data.optInt("driver_rating_count", 0);
            driverRatingCountText.setText(String.valueOf(ratingCount));

            // Driver initials
            if (!driverName.isEmpty()) {
                String[] parts = driverName.trim().split("\\s+");
                String initials = String.valueOf(parts[0].charAt(0));
                if (parts.length > 1) {
                    initials += parts[parts.length - 1].charAt(0);
                }
                driverInitialsText.setText(initials.toUpperCase());
            }

            // Route — reverse geocode
            double originLat = data.optDouble("origin_lat", 0);
            double originLng = data.optDouble("origin_lng", 0);
            double destLat = data.optDouble("destination_lat", 0);
            double destLng = data.optDouble("destination_lng", 0);

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                if (originLat != 0 && originLng != 0) {
                    List<Address> fromAddresses = geocoder.getFromLocation(originLat, originLng, 1);
                    if (fromAddresses != null && !fromAddresses.isEmpty()) {
                        pickupText.setText(fromAddresses.get(0).getAddressLine(0));
                    }
                }
                if (destLat != 0 && destLng != 0) {
                    List<Address> toAddresses = geocoder.getFromLocation(destLat, destLng, 1);
                    if (toAddresses != null && !toAddresses.isEmpty()) {
                        destinationText.setText(toAddresses.get(0).getAddressLine(0));
                    }
                }
            } catch (Exception e) {
                Log.e("TrackDelivery", "Geocoding failed", e);
                pickupText.setText("Location unavailable");
                destinationText.setText("Location unavailable");
            }

            // Payment details
            vehicleText.setText(data.optString("vehicle", ""));
            paymentText.setText(data.optString("payment_method", ""));
            double fare = data.optDouble("fare", 0);
            fareText.setText(String.format(Locale.getDefault(), "$%.2f", fare));

        } catch (Exception e) {
            Log.e("TrackDelivery", "Display error", e);
            showError("Failed to display delivery details");
        }
    }

    private void showError(String message) {
        resultSection.setVisibility(View.GONE);
        errorSection.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
