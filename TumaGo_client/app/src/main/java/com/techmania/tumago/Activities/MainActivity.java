package com.techmania.tumago.Activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Interface.UserCallback;
import com.techmania.tumago.Model.Expense;
import com.techmania.tumago.Model.TransportModel;
import com.techmania.tumago.R;
import com.techmania.tumago.adapter.TransportAdapter;
import com.techmania.tumago.auth.Login;
import com.techmania.tumago.auth.TermsAgreement;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.GetUserData;
import com.techmania.tumago.helper.LogOutUser;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.SendFCMtoken;
import com.techmania.tumago.helper.ServerSync;
import com.techmania.tumago.helper.Token;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int AUTOCOMPLETE_REQUEST_CODE_ORIGIN = 1;
    private static final int AUTOCOMPLETE_REQUEST_CODE_DEST = 2;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private int locationRequestAttempts = 0;
    private final int MAX_LOCATION_ATTEMPTS = 3;

    private EditText originInput;
    private EditText destInput;
    private TextView distanceText;

    private GoogleMap mMap;
    private LatLng originLatLng;
    private LatLng destLatLng;

    private final String API_KEY = "AIzaSyAVw3B6eS91Vw5aBew8cCoTwhu2zy3atiI";

    RecyclerView mainRecycler;
    CardView scooter, logout;
    TransportAdapter transportAdapter;
    ArrayList<TransportModel> arrayList;
    LinearLayout menuList, UserProfile, parcels;

    ImageView menu, close;
    TextView mainUsername, mainRating;
    double Distance;
    double ScooterPrice;
    double VanPrice;
    double TruckPrice;

    SendFCMtoken sendFCMtoken = new SendFCMtoken();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_main);

        ServerSync.syncServerTime(this);
        requestLocationPermission();

        scooter = findViewById(R.id.scooter);
        menu = findViewById(R.id.menuOption);
        menuList = findViewById(R.id.MenuList);
        close = findViewById(R.id.close);
        mainUsername = findViewById(R.id.mainUsername);
        mainRating = findViewById(R.id.mainRating);
        UserProfile = findViewById(R.id.goToProfile);
        logout = findViewById(R.id.logOut);
        parcels = findViewById(R.id.parcel);

        mainRecycler = findViewById(R.id.mainRecycler);
        mainRecycler.setLayoutManager(new LinearLayoutManager(this));

        checkUserTerms();

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.setVisibility(View.VISIBLE);
                menuList.setVisibility(View.GONE);
            }
        });

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuList.setVisibility(View.VISIBLE);
                menu.setVisibility(View.GONE);
            }
        });

        parcels.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GetParcels.class);
            startActivity(intent);
        });

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
        }

        originInput = findViewById(R.id.input_origin);
        destInput = findViewById(R.id.input_dest);
        distanceText = findViewById(R.id.distance_text);

        originInput.setFocusable(false);
        destInput.setFocusable(false);

        originInput.setOnClickListener(v -> launchAutocomplete(AUTOCOMPLETE_REQUEST_CODE_ORIGIN));
        destInput.setOnClickListener(v -> launchAutocomplete(AUTOCOMPLETE_REQUEST_CODE_DEST));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        UserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UserProfile.class);
                startActivity(intent);
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOut();
            }
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

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocationFeatures();
        } else {
            if (locationRequestAttempts < MAX_LOCATION_ATTEMPTS) {
                locationRequestAttempts++;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Continuing without location access", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableLocationFeatures() {
        if (mMap != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));
                        } else {
                            Toast.makeText(this, "Couldn't get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void launchAutocomplete(int requestCode) {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            LatLng latLng = place.getLatLng();

            if (latLng == null || mMap == null) {
                Toast.makeText(this, "Map not ready or invalid location", Toast.LENGTH_SHORT).show();
                return;
            }

            if (requestCode == AUTOCOMPLETE_REQUEST_CODE_ORIGIN) {
                originInput.setText(place.getName());
                originLatLng = latLng;
            } else if (requestCode == AUTOCOMPLETE_REQUEST_CODE_DEST) {
                destInput.setText(place.getName());
                destLatLng = latLng;
            }

            if (originLatLng != null && destLatLng != null) {
                mMap.clear(); // Clear previous markers and routes

                mMap.addMarker(new MarkerOptions().position(originLatLng).title("Origin").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                mMap.addMarker(new MarkerOptions().position(destLatLng).title("Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                drawRouteAndDistance(originLatLng, destLatLng);
                saveLatLngToSharedPreferences();

                scooter.setVisibility(View.VISIBLE);

                destInput.setVisibility(View.GONE);
                originInput.setOnClickListener(v -> {
                    destInput.setVisibility(View.VISIBLE);
                    scooter.setVisibility(View.GONE);
                });
            }

            // Move camera after small delay to avoid race
            originInput.postDelayed(() -> {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
            }, 300);

        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Toast.makeText(this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRouteAndDistance(LatLng origin, LatLng destination) {
        if (mMap == null) return;

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

                    String distance = route.getJSONArray("legs")
                            .getJSONObject(0)
                            .getJSONObject("distance")
                            .getString("text");

                    runOnUiThread(() -> {
                        if (mMap == null) return;

                        List<LatLng> points = com.google.maps.android.PolyUtil.decode(encodedPoints);
                        mMap.addPolyline(new PolylineOptions()
                                .addAll(points)
                                .width(10f)
                                .color(getResources().getColor(R.color.dark_blue)));

                        distanceText.setText("Distance: " + distance);
                        String cleanedDistance = distance.replaceAll("[^\\d.]", "");
                        double distanceKm = Double.parseDouble(cleanedDistance);
                        Distance = distanceKm;

                        GetTripExpense(distanceKm);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error drawing route", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableLocationFeatures();
    }

    public void getUserData(String accessToken){

        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Please Log in", Toast.LENGTH_SHORT).show();
            //Navigate to login screen
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
            finish();
        } else {
            GetUserData.GetData(MainActivity.this, accessToken, new UserCallback() {
                @Override
                public void onUserDataReceived(String name, String surname, String phoneNumber
                        ,String email, double rating, String street, String addressLine, String province
                        ,String city, String postalCode, String role) {
                    // user the userData here
                    mainUsername.setText(name);
                    mainRating.setText(String.valueOf(rating));

                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    String token = task.getResult();
                                    sendFCMtoken.sendFcmTokenToBackend(token, accessToken);
                                } else {
                                    Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                                }
                            });
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e("API", "Error: " + t.getMessage());
                }
            });
        }
    }

    public void GetTripExpense(double distance2) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<Expense> call = apiService.getExpense(distance2);

        call.enqueue(new Callback<Expense>() {
            @Override
            public void onResponse(Call<Expense> call, Response<Expense> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Expense fare = response.body();
                    ScooterPrice = fare.getScooter();
                    VanPrice = fare.getVan();
                    TruckPrice = fare.getTruck();

                    Log.d("Scooter price", String.valueOf(ScooterPrice));

                    arrayList = new ArrayList<>();
                    arrayList.add(new TransportModel("scooter", "Scooter", ScooterPrice));
                    arrayList.add(new TransportModel("vanm", "Van", VanPrice));
                    arrayList.add(new TransportModel("truckm", "Truck", TruckPrice));

                    transportAdapter = new TransportAdapter(arrayList, MainActivity.this);
                    mainRecycler.setAdapter(transportAdapter);
                }
            }

            @Override
            public void onFailure(Call<Expense> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserTerms(){
        String accessToken = Token.getAccessToken(this);

        if (accessToken != null && !accessToken.isEmpty()) {
            String authHeader = "Bearer " + accessToken;

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.checkTerms(authHeader);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        getUserData(accessToken);
                    } else {
                        Intent terms = new Intent(MainActivity.this, TermsAgreement.class);
                        startActivity(terms);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("Failed", t.getMessage());
                }
            });
        } else {
            Intent i = new Intent(MainActivity.this, Login.class);
            startActivity(i);
            finish();
            Log.d("Failed", "Token is null or empty");
        }
    }

    public void logOut(){
        String accessToken = Token.getAccessToken(this);
        if (accessToken != null && !accessToken.isEmpty()){
            LogOutUser.LogOut(this, accessToken);
        } else{
            Log.d("AccessToken", "No accessToken");
        }
    }

    private void saveLatLngToSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong("originLat", Double.doubleToRawLongBits(originLatLng.latitude));
        editor.putLong("originLng", Double.doubleToRawLongBits(originLatLng.longitude));
        editor.putLong("destLat", Double.doubleToRawLongBits(destLatLng.latitude));
        editor.putLong("destLng", Double.doubleToRawLongBits(destLatLng.longitude));

        editor.apply();
    }
}