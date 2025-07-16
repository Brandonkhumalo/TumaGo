package com.techmania.tumago.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.DeliveryRequest;
import com.techmania.tumago.R;
import com.techmania.tumago.auth.Login;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.Token;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfirmDelivery extends AppCompatActivity {
    TextView fare, vehicle, pickup, dropoff;
    MaterialCardView confirmDelivery;
    LinearLayout look_for_driver;
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
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

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
        fare.setText("$" + fareValue);
        vehicle.setText(vehicleValue);

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        double originLat = Double.longBitsToDouble(prefs.getLong("originLat", 0));
        double originLng = Double.longBitsToDouble(prefs.getLong("originLng", 0));
        double destLat = Double.longBitsToDouble(prefs.getLong("destLat", 0));
        double destLng = Double.longBitsToDouble(prefs.getLong("destLng", 0));

        originLatLng = new LatLng(originLat, originLng);
        destLatLng = new LatLng(destLat, destLng);

        getAddressFromLatLng(originLatLng, true);
        getAddressFromLatLng(destLatLng, false);

        confirmDelivery.setOnClickListener(view -> {
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

    public void LookForDriver(){
        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        if (accessToken != null && !accessToken.isEmpty()){
            String payment_method = "cash";

            DeliveryRequest request = new DeliveryRequest(originLatLng, destLatLng, vehicleValue, fareValue, payment_method);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.requestDelivery(authHeader, request);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        look_for_driver.setVisibility(View.VISIBLE);

                    } else {
                        Log.d("failed", response.message());
                        Intent i = new Intent(ConfirmDelivery.this, MainActivity.class);
                        startActivity(i);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("ConfirmDelivery", "Error: " + t.getMessage());
                }
            });
        } else {
            Intent i = new Intent(ConfirmDelivery.this, Login.class);
            startActivity(i);
            finish();
        }
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







