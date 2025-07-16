package com.techmania.tumago_driver.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.Interface.DriverCallback;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.auth.Login;
import com.techmania.tumago_driver.auth.TermsAgreement;
import com.techmania.tumago_driver.auth.splash_screen;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.GetDriverData;
import com.techmania.tumago_driver.helpers.LogOutUser;
import com.techmania.tumago_driver.helpers.NetworkUtils;
import com.techmania.tumago_driver.helpers.SendFCMtoken;
import com.techmania.tumago_driver.helpers.Token;
import com.techmania.tumago_driver.helpers.WebSocketLocationClient;

import java.net.URI;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    LinearLayout menuList, UserProfile, upload, deliveries, finances;
    ImageView menu, close;
    TextView mainUsername, mainRating;
    CardView logout;

    private GoogleMap mMap;
    private final String API_KEY = "AIzaSyAVw3B6eS91Vw5aBew8cCoTwhu2zy3atiI";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private int permissionRequestCount = 0;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private WebSocketLocationClient webSocketClient;
    SendFCMtoken sendFCMtoken = new SendFCMtoken();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationRequest();
        setupLocationCallback();

        checkLocationPermission();

        // UI init
        menu = findViewById(R.id.menuOption);
        menuList = findViewById(R.id.MenuList);
        close = findViewById(R.id.close);
        mainUsername = findViewById(R.id.mainUsername);
        mainRating = findViewById(R.id.mainRating);
        UserProfile = findViewById(R.id.goToProfile);
        logout = findViewById(R.id.logOut);
        upload = findViewById(R.id.upload);
        deliveries = findViewById(R.id.deliveries);
        finances = findViewById(R.id.finances);

        checkUserTerms();

        close.setOnClickListener(v -> {
            menu.setVisibility(View.VISIBLE);
            menuList.setVisibility(View.GONE);
        });

        menu.setOnClickListener(v -> {
            menuList.setVisibility(View.VISIBLE);
            menu.setVisibility(View.GONE);
        });

        UserProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DriverProfile.class)));
        upload.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, UploadLicenseActivity.class)));
        deliveries.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, GetDeliveries.class)));
        finances.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, Finances.class)));

        logout.setOnClickListener(v -> logOut());

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        try {
            String token = Token.getAccessToken(this);
            String wsUrl = "ws://192.168.8.147:8000/ws/driver_location/?token=" + token;
            URI uri = new URI(wsUrl);
            webSocketClient = new WebSocketLocationClient(uri);
            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void setupLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(60000); //60000 1 minute
        locationRequest.setFastestInterval(30000); // 30 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || webSocketClient == null || !webSocketClient.isOpen()) {
                    return;
                }

                double lat = locationResult.getLastLocation().getLatitude();
                double lon = locationResult.getLastLocation().getLongitude();
                webSocketClient.sendLocation(lat, lon);
                Log.d("WebSocket", "Location sent: " + lat + ", " + lon);
            }
        };
    }

    private void enableMyLocation() {
        if (mMap != null &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                }
            });

            // Start location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            if (permissionRequestCount < 3) {
                permissionRequestCount++;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Location permission denied. Closing app.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                checkLocationPermission(); // Retry
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            LatLng latLng = place.getLatLng();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Toast.makeText(this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void getDriverData(String accessToken) {

        if (accessToken == null || accessToken.isEmpty()) {
            startActivity(new Intent(MainActivity.this, Login.class));
            finish();
        } else {
            GetDriverData.GetData(MainActivity.this, accessToken, new DriverCallback() {
                @Override
                public void onDriverDataReceived(String name, String surname, String phoneNumber, String email,
                                                 double rating, String street, String addressLine, String province,
                                                 String city, String postalCode, String role, Boolean verified, Boolean license) {
                    mainUsername.setText(name);
                    mainRating.setText(String.valueOf(rating));

                    if (license == null || Boolean.FALSE.equals(license)){
                        Intent intent = new Intent(MainActivity.this, UploadLicenseActivity.class);
                        startActivity(intent);
                    }

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

    public void logOut() {
        String accessToken = Token.getAccessToken(this);
        if (accessToken != null && !accessToken.isEmpty()) {
            LogOutUser.LogOut(this, accessToken);
        } else {
            Log.d("AccessToken", "No accessToken");
        }
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
                        getDriverData(accessToken);
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
}
