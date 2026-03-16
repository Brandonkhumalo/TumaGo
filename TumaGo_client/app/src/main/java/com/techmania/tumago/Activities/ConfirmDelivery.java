package com.techmania.tumago.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.model.LatLng;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.DeliveryRequest;
import com.techmania.tumago.R;
import com.techmania.tumago.auth.Login;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.Token;
import com.techmania.tumago.helper.UiHelper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfirmDelivery extends AppCompatActivity {
    TextView fare, vehicle, pickup, dropoff;
    LinearLayout confirmDelivery;
    ProgressBar look_for_driver;
    Spinner paymentSpinner;
    private LatLng originLatLng;
    private LatLng destLatLng;

    double fareValue;
    String vehicleValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_delivery);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        look_for_driver = findViewById(R.id.LookForDriver);
        look_for_driver.setVisibility(View.GONE);

        fare = findViewById(R.id.fare);
        vehicle = findViewById(R.id.vehicle);
        pickup = findViewById(R.id.pickup);
        dropoff = findViewById(R.id.dropoff);
        confirmDelivery = findViewById(R.id.confirmDelivery);

        Intent intent = getIntent();
        fareValue = intent.getDoubleExtra("price", 0);
        vehicleValue = intent.getStringExtra("transportName");
        String vehicleImageName = intent.getStringExtra("transportImage");
        fare.setText("$" + fareValue);
        vehicle.setText(vehicleValue);

        // Set vehicle image
        if (vehicleImageName != null) {
            ImageView vehicleImage = findViewById(R.id.vehicleImage);
            int resId = getResources().getIdentifier(vehicleImageName, "drawable", getPackageName());
            if (resId != 0) {
                vehicleImage.setImageResource(resId);
            }
        }

        // Set up payment spinner
        paymentSpinner = findViewById(R.id.payment);
        String[] paymentOptions = {"Cash", "Card"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, paymentOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentSpinner.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        long originLatBits = prefs.getLong("originLat", 0);
        long originLngBits = prefs.getLong("originLng", 0);
        long destLatBits = prefs.getLong("destLat", 0);
        long destLngBits = prefs.getLong("destLng", 0);

        // Only use coords if they were actually saved (avoid 0,0 default)
        if (originLatBits != 0 && originLngBits != 0) {
            double originLat = Double.longBitsToDouble(originLatBits);
            double originLng = Double.longBitsToDouble(originLngBits);
            originLatLng = new LatLng(originLat, originLng);
            getAddressFromLatLng(originLatLng, true);
        }
        if (destLatBits != 0 && destLngBits != 0) {
            double destLat = Double.longBitsToDouble(destLatBits);
            double destLng = Double.longBitsToDouble(destLngBits);
            destLatLng = new LatLng(destLat, destLng);
            getAddressFromLatLng(destLatLng, false);
        }

        findViewById(R.id.lookForTrip).setOnClickListener(view -> {
            LookForDriver();
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

    private void getAddressFromLatLng(LatLng latLng, boolean isOrigin) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addressList != null && !addressList.isEmpty()) {
                //String address = addressList.get(0).getAddressLine(0); //to display address in 1 line

                Address address = addressList.get(0);
                String street = address.getThoroughfare(); // e.g., "King Street"
                String number = address.getSubThoroughfare(); // e.g., "3"
                String city = address.getLocality(); // e.g., "germiston"

                String streetFull = (number != null ? number + " " : "") + (street != null ? street : "");
                String result = streetFull + ", " + (city != null ? city : "Unknown City");

                if (isOrigin) {
                    pickup.setText(result);
                } else {
                    dropoff.setText(result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isRequesting = false;

    public void LookForDriver(){
        if (isRequesting) return;

        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        if (accessToken != null && !accessToken.isEmpty()){
            isRequesting = true;
            look_for_driver.setVisibility(View.VISIBLE);
            findViewById(R.id.lookForTrip).setEnabled(false);

            String payment_method = paymentSpinner.getSelectedItem().toString().toLowerCase();

            DeliveryRequest request = new DeliveryRequest(originLatLng, destLatLng, vehicleValue, fareValue, payment_method);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.requestDelivery(authHeader, request);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    isRequesting = false;
                    look_for_driver.setVisibility(View.GONE);
                    findViewById(R.id.lookForTrip).setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        Intent searchIntent = new Intent(ConfirmDelivery.this, SearchingForDriver.class);
                        searchIntent.putExtra("transportImage", getIntent().getStringExtra("transportImage"));
                        startActivity(searchIntent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    } else {
                        Log.d("failed", response.message());
                        UiHelper.showRetry(findViewById(android.R.id.content), "Failed to request delivery", () -> LookForDriver());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    isRequesting = false;
                    look_for_driver.setVisibility(View.GONE);
                    findViewById(R.id.lookForTrip).setEnabled(true);
                    Log.e("ConfirmDelivery", "Error: " + t.getMessage());
                    UiHelper.showRetry(findViewById(android.R.id.content), "Connection error. Please try again.", () -> LookForDriver());
                }
            });
        } else {
            Intent i = new Intent(ConfirmDelivery.this, Login.class);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (isRequesting) {
            Toast.makeText(this, "Please wait while we process your request.", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}

/**{
 "client_info": {
 "mobilesdk_app_id": "1:1073164123872:android:84d525d6f4546af0325a85",
 "android_client_info": {
 "package_name": "com.techmania.roadlinksolutions"
 }
 },
 "oauth_client": [],
 "api_key": [
 {
 "current_key": "AIzaSyCYIoAsLwpMC_K9fdZ-jz8dyfV74Tdef8c"
 }
 ],
 "services": {
 "appinvite_service": {
 "other_platform_oauth_client": []
 }
 }
 },*/







