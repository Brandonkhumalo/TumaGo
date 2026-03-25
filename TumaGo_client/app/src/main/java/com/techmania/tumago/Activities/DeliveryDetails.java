package com.techmania.tumago.Activities;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.PackagePickedUpHelper;
import com.techmania.tumago.helper.Token;
import com.techmania.tumago.helper.UiHelper;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryDetails extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "DeliveryDetails";
    private static final int POLL_INTERVAL_MS = 5000; // 5 seconds

    TextView driverName, driverRating, totalDriverRatings, numberPlate, carDetails, driverInitials;
    TextView deliveryIdText, dateText, pickupText, destinationText, vehicleText, paymentText, fareText;
    String vehicle_color, vehicle, vehicle_model;
    MaterialCardView cancelDelivery, goHome;
    LinearLayout layout1, layout2;
    ProgressBar progressBar;
    String delivery_id;

    // Map tracking
    private GoogleMap mMap;
    private Marker driverMarker;
    private Marker pickupMarker;
    private Marker dropoffMarker;
    private LatLng pickupLatLng;
    private LatLng dropoffLatLng;
    private Handler pollHandler;
    private Runnable pollRunnable;
    private boolean isPolling = false;
    private boolean isPickedUp = false;
    private boolean initialZoomDone = false;

    // Status banner
    private TextView statusText;
    private View statusDot;

    private BroadcastReceiver pickedUpReceiver;

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

        // Status banner on the map
        statusText = findViewById(R.id.statusText);
        statusDot = findViewById(R.id.statusDot);

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

        // Copy delivery ID to clipboard
        ImageView copyDeliveryId = findViewById(R.id.copyDeliveryId);
        copyDeliveryId.setOnClickListener(v -> {
            if (delivery_id != null && !delivery_id.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Delivery ID", delivery_id);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Delivery ID copied", Toast.LENGTH_SHORT).show();
            }
        });

        dateText.setText(intent.getStringExtra("date"));
        vehicleText.setText(intent.getStringExtra("vehicle_type"));
        paymentText.setText(intent.getStringExtra("payment_method"));
        String fare = intent.getStringExtra("fare");
        if (fare != null) {
            fareText.setText("$" + fare);
        }

        // Parse pickup and destination coordinates for the map
        String originLat = intent.getStringExtra("origin_lat");
        String originLng = intent.getStringExtra("origin_lng");
        String destLat = intent.getStringExtra("destination_lat");
        String destLng = intent.getStringExtra("destination_lng");

        try {
            if (originLat != null && originLng != null) {
                pickupLatLng = new LatLng(Double.parseDouble(originLat), Double.parseDouble(originLng));
            }
            if (destLat != null && destLng != null) {
                dropoffLatLng = new LatLng(Double.parseDouble(destLat), Double.parseDouble(destLng));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid coordinates", e);
        }

        // Reverse geocode pickup and destination
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            if (pickupLatLng != null) {
                List<Address> fromAddresses = geocoder.getFromLocation(
                        pickupLatLng.latitude, pickupLatLng.longitude, 1);
                if (fromAddresses != null && !fromAddresses.isEmpty()) {
                    pickupText.setText(fromAddresses.get(0).getAddressLine(0));
                }
            }
            if (dropoffLatLng != null) {
                List<Address> toAddresses = geocoder.getFromLocation(
                        dropoffLatLng.latitude, dropoffLatLng.longitude, 1);
                if (toAddresses != null && !toAddresses.isEmpty()) {
                    destinationText.setText(toAddresses.get(0).getAddressLine(0));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Geocoding failed", e);
        }

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

        cancelDelivery.setOnClickListener(v -> CancelDelivery());

        goHome.setOnClickListener(v -> {
            Intent i = new Intent(DeliveryDetails.this, HomeActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.trackingMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up polling handler
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling) {
                    fetchDriverLocation();
                    pollHandler.postDelayed(this, POLL_INTERVAL_MS);
                }
            }
        };
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Add pickup marker (green)
        if (pickupLatLng != null) {
            pickupMarker = mMap.addMarker(new MarkerOptions()
                    .position(pickupLatLng)
                    .title("Pickup")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        // Add dropoff marker (red)
        if (dropoffLatLng != null) {
            dropoffMarker = mMap.addMarker(new MarkerOptions()
                    .position(dropoffLatLng)
                    .title("Drop-off")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        // Initial zoom to show pickup and dropoff
        zoomToFitMarkers(null);

        // Start polling immediately
        startPolling();
    }

    private void zoomToFitMarkers(LatLng driverLatLng) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasPoints = false;

        if (pickupLatLng != null) {
            builder.include(pickupLatLng);
            hasPoints = true;
        }
        if (dropoffLatLng != null) {
            builder.include(dropoffLatLng);
            hasPoints = true;
        }
        if (driverLatLng != null) {
            builder.include(driverLatLng);
            hasPoints = true;
        }

        if (hasPoints) {
            try {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 80));
            } catch (Exception e) {
                // Map not laid out yet — use a fallback
                if (pickupLatLng != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 14));
                }
            }
        }
    }

    private void startPolling() {
        if (!isPolling) {
            isPolling = true;
            pollHandler.post(pollRunnable);
        }
    }

    private void stopPolling() {
        isPolling = false;
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void fetchDriverLocation() {
        if (delivery_id == null) return;

        String accessToken = Token.getAccessToken(this);
        if (accessToken == null || accessToken.isEmpty()) return;

        String authHeader = "Bearer " + accessToken;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.trackDelivery(delivery_id, authHeader);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject data = new JSONObject(json);
                        updateDriverOnMap(data);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse tracking data", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Tracking request failed: " + t.getMessage());
            }
        });
    }

    private void updateDriverOnMap(JSONObject data) {
        double driverLat = data.optDouble("driver_lat", 0);
        double driverLng = data.optDouble("driver_lng", 0);
        boolean pickedUp = data.optBoolean("picked_up", false);
        boolean inTransit = data.optBoolean("in_transit", true);
        boolean successful = data.optBoolean("successful", true);

        // Update picked_up status
        if (pickedUp && !isPickedUp) {
            isPickedUp = true;
            cancelDelivery.setVisibility(View.GONE);
        }

        // Update status banner
        if (!inTransit && successful) {
            // Delivery complete
            statusText.setText("Delivered");
            stopPolling();
            return;
        } else if (!successful) {
            statusText.setText("Cancelled");
            stopPolling();
            return;
        } else if (pickedUp) {
            statusText.setText("Driver heading to drop-off");
        } else {
            statusText.setText("Driver en route to pickup");
        }

        // Update driver marker position
        if (mMap != null && driverLat != 0 && driverLng != 0) {
            LatLng driverPos = new LatLng(driverLat, driverLng);

            if (driverMarker == null) {
                driverMarker = mMap.addMarker(new MarkerOptions()
                        .position(driverPos)
                        .title("Driver")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                // First time we see the driver — zoom to fit all markers
                zoomToFitMarkers(driverPos);
                initialZoomDone = true;
            } else {
                driverMarker.setPosition(driverPos);
            }

            // Smoothly follow the driver after the initial zoom
            if (initialZoomDone) {
                mMap.animateCamera(CameraUpdateFactory.newLatLng(driverPos));
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }
        NetworkUtils.registerNetworkCallback(this);

        // Listen for package picked up broadcast to hide cancel button
        pickedUpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isPickedUp = true;
                cancelDelivery.setVisibility(View.GONE);
                statusText.setText("Driver heading to drop-off");
                Toast.makeText(DeliveryDetails.this,
                        "Driver has picked up your package!", Toast.LENGTH_LONG).show();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(
                pickedUpReceiver,
                new IntentFilter(PackagePickedUpHelper.ACTION_PACKAGE_PICKED_UP));

        // Resume polling if map is ready
        if (mMap != null) {
            startPolling();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPolling();
        NetworkUtils.unregisterNetworkCallback(this);
        if (pickedUpReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(pickedUpReceiver);
        }
    }

    private void CancelDelivery() {
        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.cancel_delivery(authHeader, delivery_id);

        UiHelper.showLoading(progressBar, cancelDelivery);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                UiHelper.hideLoading(progressBar, cancelDelivery);
                if (response.isSuccessful()) {
                    stopPolling();
                    layout1.setVisibility(View.GONE);
                    layout2.setVisibility(View.VISIBLE);
                } else {
                    // Backend may reject if picked_up — show a meaningful message
                    if (response.code() == 400) {
                        Toast.makeText(DeliveryDetails.this,
                                "Cannot cancel — driver has already picked up the package.",
                                Toast.LENGTH_LONG).show();
                        cancelDelivery.setVisibility(View.GONE);
                    } else {
                        UiHelper.showRetry(findViewById(android.R.id.content),
                                "Failed to cancel delivery", () -> CancelDelivery());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                UiHelper.hideLoading(progressBar, cancelDelivery);
                Log.d(TAG, "Cancel delivery failed: " + t.getMessage());
                UiHelper.showRetry(findViewById(android.R.id.content),
                        "Failed to cancel delivery", () -> CancelDelivery());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
