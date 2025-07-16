package com.techmania.tumago_driver.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.auth.Login;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.Token;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryDetails extends AppCompatActivity implements OnMapReadyCallback {

    private static final String API_KEY = "AIzaSyAVw3B6eS91Vw5aBew8cCoTwhu2zy3atiI";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;
    private LatLng originLatLng;
    private FusedLocationProviderClient fusedLocationClient;

    TextView name, fare, dropOff, distance;
    LatLng destinationLatLng;
    MotionLayout motionLayout;
    MaterialCardView accept_Trip;

    String userLong;
    String userLati;
    String cost;
    static String deliveryCost;

    private Handler handler = new Handler();
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_details);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        name = findViewById(R.id.RequesterName);
        fare = findViewById(R.id.DeliveryFare);
        dropOff = findViewById(R.id.drop_off);
        distance = findViewById(R.id.distance);
        accept_Trip = findViewById(R.id.tripDetailsCard);
        LinearLayout Accept = findViewById(R.id.acceptTrip);

        Accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AcceptTrip();
            }
        });

        //motionLayout = findViewById(R.id.motionLayout);

        Intent intent = getIntent();
        String requester = intent.getStringExtra("requester_name");
        String lati = intent.getStringExtra("destination_lat");
        String longi = intent.getStringExtra("destination_lng");
        cost = intent.getStringExtra("cost");
        String Distance = intent.getStringExtra("distance_meters");
        userLati = intent.getStringExtra("userLatitude");
        userLong = intent.getStringExtra("userLongi");

        double lat = Double.parseDouble(lati);
        double lng = Double.parseDouble(longi);

        destinationLatLng = new LatLng(lat, lng);
        if (destinationLatLng != null) {
            decodeLatLng(destinationLatLng);
        }

        name.setText(requester);
        fare.setText("$" + deliveryCost);
        distance.setText(Distance + " km");

        /**motionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {}

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {}

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                if (currentId == R.id.end) {
                    // User swiped successfully — cancel timeout
                    handler.removeCallbacks(timeoutRunnable);
                    AcceptTrip(); // Your method to handle acceptance
                }
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {}
        });*/

        // Set the 10-second timeout
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(DeliveryDetails.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        };

        // Start the countdown
        handler.postDelayed(timeoutRunnable, 10000); // 10,000 ms = 10 seconds
    }

    private void decodeLatLng(LatLng latLng) {
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
                dropOff.setText(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocationAndShowRoute();
    }

    private void getCurrentLocationAndShowRoute() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        originLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        showMarkersAndDrawRoute();
                    } else {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showMarkersAndDrawRoute() {
        if (mMap == null || originLatLng == null) return;

        mMap.clear();

        mMap.addMarker(new MarkerOptions()
                .position(originLatLng)
                .title("Origin")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        mMap.addMarker(new MarkerOptions()
                .position(destinationLatLng)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 7f));

        drawRouteAndDistance(originLatLng, destinationLatLng);
    }

    private void drawRouteAndDistance(LatLng origin, LatLng destination) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + API_KEY;

        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }

                JSONObject response = new JSONObject(responseBuilder.toString());
                JSONArray routes = response.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPoints = overviewPolyline.getString("points");

                    runOnUiThread(() -> {
                        if (mMap == null) return;
                        List<LatLng> points = com.google.maps.android.PolyUtil.decode(encodedPoints);
                        mMap.addPolyline(new PolylineOptions()
                                .addAll(points)
                                .width(10f)
                                .color(getResources().getColor(R.color.dark_blue)));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(DeliveryDetails.this, "Error drawing route", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndShowRoute();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void AcceptTrip(){
        String accessToken = Token.getAccessToken(this);
        String trip_id = getTrip_id(this);
        Map<String, String> map = new HashMap<>();
        map.put("trip_id", trip_id);

        if (accessToken != null && !accessToken.isEmpty()){
            String authHeader = "Bearer " + accessToken;

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.acceptTrip(authHeader, map);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()){
                        accept_Trip.setVisibility(View.GONE);

                        try {
                            // Convert response body to String
                            String responseString = response.body().string();

                            // Parse JSON to extract delivery_id
                            JSONObject jsonObject = new JSONObject(responseString);
                            String deliveryId = jsonObject.getString("delivery_id");

                            BigDecimal trip_cost = new BigDecimal(cost);
                            store_deliveryDetails(DeliveryDetails.this, trip_cost, deliveryId);

                            // Continue with navigation
                            accept_Trip.setVisibility(View.GONE);
                            Intent i = new Intent(DeliveryDetails.this, Navigation.class);
                            i.putExtra("userLatitude", userLati);
                            i.putExtra("userLongi", userLong);
                            i.putExtra("destination", destinationLatLng);
                            i.putExtra("delivery_id", deliveryId); // pass it if needed
                            startActivity(i);
                            finish();

                        } catch (Exception e) {
                            Log.e("PARSE_ERROR", "Failed to parse delivery_id", e);
                        }
                    } else {
                        Intent intent = new Intent(DeliveryDetails.this, MainActivity.class);
                        startActivity(intent);
                        finish();

                        Log.d("response failed", response.message());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("FAILED", t.getMessage());
                }
            });
        } else {
            Intent i = new Intent(DeliveryDetails.this, Login.class);
            startActivity(i);
            finish();
        }
    }

    public static void store_deliveryDetails(Context context, BigDecimal cost, String delivery_id) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor editor = securePrefs.edit();
            editor.putString("delivery_cost", cost.toPlainString());
            editor.putString("delivery_id", delivery_id);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getTrip_id(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            return securePrefs.getString("trip_id", null);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}















