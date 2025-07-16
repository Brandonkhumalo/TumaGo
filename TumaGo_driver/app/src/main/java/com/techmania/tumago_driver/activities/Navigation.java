package com.techmania.tumago_driver.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.techmania.tumago_driver.auth.Login;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.Token;
import com.techmania.tumago_driver.models.EndTrip;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.*;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Navigation extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String API_KEY = "AIzaSyAVw3B6eS91Vw5aBew8cCoTwhu2zy3atiI";

    private GoogleMap mMap;
    private LatLng userLatLng, driverLatLng;
    private LatLng destination;
    private FusedLocationProviderClient fusedLocationClient;
    private TextToSpeech tts;

    private LocationCallback locationCallback;
    private Marker driverMarker;
    MaterialCardView startTrip, endTrip;
    Button navigate, end;

    private boolean hasStartedTrip = false;

    private ImageView[] stars = new ImageView[5];
    private int selectedRating = 0;

    LinearLayout layout2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        startTrip = findViewById(R.id.startTrip);
        navigate = findViewById(R.id.Navigate);
        startTrip.setVisibility(View.GONE);

        endTrip = findViewById(R.id.endTrip);
        end = findViewById(R.id.End);
        endTrip.setVisibility(View.GONE);

        // Get user pickup location and destination
        String userLat = getIntent().getStringExtra("userLatitude");
        String userLng = getIntent().getStringExtra("userLongi");
        userLatLng = new LatLng(Double.parseDouble(userLat), Double.parseDouble(userLng));
        destination = getIntent().getParcelableExtra("destination");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Map setup
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        // Text-to-Speech
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR)
                tts.setLanguage(Locale.US);
        });

        navigate.setOnClickListener(v -> {
            hasStartedTrip = true;
            startTrip.setVisibility(View.GONE);
            drawRoute(driverLatLng, destination);
        });

        end.setOnClickListener( v -> {
            EndTrip();
        });

        layout2 = findViewById(R.id.layout2);
        layout2.setVisibility(View.GONE);

        stars[0] = findViewById(R.id.star1);
        stars[1] = findViewById(R.id.star2);
        stars[2] = findViewById(R.id.star3);
        stars[3] = findViewById(R.id.star4);
        stars[4] = findViewById(R.id.star5);

        for (int i = 0; i < stars.length; i++) {
            final int index = i;
            stars[i].setOnClickListener(v -> {
                selectedRating = index + 1;
                updateStarColors();
            });
        }

        Button rateButton = findViewById(R.id.rateButton);
        rateButton.setOnClickListener(v -> {
            RateDelivery();
        });
    }

    private void updateStarColors() {
        for (int i = 0; i < stars.length; i++) {
            if (i < selectedRating) {
                stars[i].setImageResource(R.drawable.ic_star_filled); // blue star
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline); // empty star
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                driverLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                drawRoute(driverLatLng, userLatLng); // Initial route to user
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Couldn't get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        if (origin == null || destination == null) return;

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + origin.latitude + "," + origin.longitude
                + "&destination=" + destination.latitude + "," + destination.longitude
                + "&mode=driving&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray routes = response.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                            String points = overviewPolyline.getString("points");

                            List<LatLng> path = decodePolyline(points);
                            mMap.clear();
                            mMap.addPolyline(new PolylineOptions().addAll(path)
                                    .width(10f).color(0xFF00796B));

                            mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));

                            if (driverLatLng != null) {
                                driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("You"));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLatLng, 15));
                            }

                            // Voice instructions
                            JSONArray steps = route.getJSONArray("legs")
                                    .getJSONObject(0).getJSONArray("steps");
                            for (int i = 0; i < steps.length(); i++) {
                                String instruction = steps.getJSONObject(i)
                                        .getString("html_instructions")
                                        .replaceAll("<.*?>", ""); // Remove HTML tags
                                tts.speak(instruction, TextToSpeech.QUEUE_ADD, null, null);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing route", Toast.LENGTH_SHORT).show();
                    }
                }, error -> Toast.makeText(this, "Route request failed", Toast.LENGTH_SHORT).show());

        queue.add(jsonRequest);
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
            lng += dlng;
            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return poly;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;

                Location location = result.getLastLocation();
                if (location != null) {
                    driverLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (driverMarker == null) {
                        driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("You"));
                    } else {
                        driverMarker.setPosition(driverLatLng);
                    }

                    mMap.animateCamera(CameraUpdateFactory.newLatLng(driverLatLng));

                    if (!hasStartedTrip) {
                        checkIfDriverArrived(location);
                    }
                    if (hasStartedTrip){
                        checkIfArrived(location);
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        }
    }

    private void checkIfDriverArrived(Location driverLocation) {
        if (userLatLng == null || driverLocation == null) return;

        float[] results = new float[1];
        Location.distanceBetween(
                driverLocation.getLatitude(),
                driverLocation.getLongitude(),
                userLatLng.latitude,
                userLatLng.longitude,
                results
        );

        float distanceInMeters = results[0];
        if (distanceInMeters <= 50) {
            startTrip.setVisibility(View.VISIBLE);
        }
    }

    private void checkIfArrived(Location driverLocation) {
        if (userLatLng == null || driverLocation == null) return;

        float[] results = new float[1];
        Location.distanceBetween(
                driverLocation.getLatitude(),
                driverLocation.getLongitude(),
                destination.latitude,
                destination.longitude,
                results
        );

        float distanceInMeters = results[0];
        if (distanceInMeters <= 50) {
            endTrip.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onMapReady(mMap);
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void RateDelivery(){
        String accessToken = Token.getAccessToken(this);

        if (accessToken != null && !accessToken.isEmpty()){
            String authHeader = "Bearer " + accessToken;
            BigDecimal delivery_cost = getCost(this);
            String delivery_id = getDelivery_id(this);

            EndTrip endTrip = new EndTrip(delivery_id, selectedRating, delivery_cost);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.endTrip(authHeader, endTrip);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()){
                        Intent i = new Intent(Navigation.this, MainActivity.class);
                        startActivity(i);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("FAILED", t.getMessage());
                }
            });
        } else {
            Intent i = new Intent(Navigation.this, Login.class);
            startActivity(i);
            finish();
        }
    }

    private void EndTrip(){
        layout2.setVisibility(View.VISIBLE);
    }

    public static BigDecimal getCost(Context context) {
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
            String costString = securePrefs.getString("delivery_cost", "0.00");
            BigDecimal deliveryCost = new BigDecimal(costString);
            return deliveryCost;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getDelivery_id(Context context) {
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

            return securePrefs.getString("delivery_id", null);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}











