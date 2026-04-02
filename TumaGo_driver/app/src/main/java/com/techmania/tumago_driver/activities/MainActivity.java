package com.techmania.tumago_driver.activities;

import com.techmania.tumago_driver.BuildConfig;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

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
import com.techmania.tumago_driver.helpers.AnimHelper;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.GetDriverData;
import com.techmania.tumago_driver.helpers.LogOutUser;
import com.techmania.tumago_driver.helpers.DriverHeartbeatService;
import com.techmania.tumago_driver.helpers.NetworkUtils;
import com.techmania.tumago_driver.helpers.SendFCMtoken;
import com.techmania.tumago_driver.helpers.Token;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    FrameLayout menuList;
    RelativeLayout UserProfile;
    LinearLayout upload, deliveries, finances, settings, logout, support;
    CardView menuButton;
    ImageView menu, close;
    TextView mainUsername, mainRating, avatarInitials;

    private GoogleMap mMap;
    private final String API_KEY = BuildConfig.MAPS_API_KEY;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private int permissionRequestCount = 0;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private static final int BACKGROUND_LOCATION_REQUEST_CODE = 200;

    private static final int UPDATE_REQUEST_CODE = 300;
    private AppUpdateManager appUpdateManager;
    private final InstallStateUpdatedListener installStateListener = state -> {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateSnackbar();
        }
    };

    SendFCMtoken sendFCMtoken = new SendFCMtoken();
    private static boolean userDataFetched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationRequest();
        setupLocationCallback();

        checkLocationPermission();

        // UI init
        menuButton = findViewById(R.id.menuButton);
        menu = findViewById(R.id.menuOption);
        menuList = findViewById(R.id.MenuList);
        close = findViewById(R.id.close);
        mainUsername = findViewById(R.id.mainUsername);
        mainRating = findViewById(R.id.mainRating);
        avatarInitials = findViewById(R.id.avatarInitials);
        UserProfile = findViewById(R.id.goToProfile);
        logout = findViewById(R.id.logOut);
        upload = findViewById(R.id.upload);
        deliveries = findViewById(R.id.deliveries);
        finances = findViewById(R.id.finances);
        settings = findViewById(R.id.goToSettings);
        support = findViewById(R.id.support);

        checkUserTerms();
        checkForAppUpdate();

        close.setOnClickListener(v -> {
            AnimHelper.slideDown(menuList);
            AnimHelper.fadeIn(menuButton);
        });

        menu.setOnClickListener(v -> {
            AnimHelper.slideUp(menuList);
            AnimHelper.fadeOut(menuButton);
        });

        UserProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DriverProfile.class)));
        upload.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, UploadLicenseActivity.class)));
        deliveries.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, GetDeliveries.class)));
        finances.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, Finances.class)));
        settings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AppSettings.class)));
        support.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, Support.class)));

        logout.setOnClickListener(v -> logOut());

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }
    }

    private void setupLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(60000); //60000 1 minute
        locationRequest.setFastestInterval(30000); // 30 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void setupLocationCallback() {
        // Location callback for the map UI only — actual tracking is in DriverHeartbeatService
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Map camera updates handled by enableMyLocation; no-op here
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

            // Start map UI location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            // Start the foreground location service for background tracking
            startLocationService();

            // Request background location permission (API 29+)
            requestBackgroundLocationPermission();
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, DriverHeartbeatService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void requestBackgroundLocationPermission() {
        // ACCESS_BACKGROUND_LOCATION only exists on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        BACKGROUND_LOCATION_REQUEST_CODE);
            }
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
        // Location updates continue in DriverHeartbeatService — no need to stop here
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Map location updates for UI
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
        // If the update was downloaded while app was in background, prompt restart
        if (appUpdateManager != null) {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(info -> {
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    showUpdateSnackbar();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appUpdateManager != null) {
            appUpdateManager.unregisterListener(installStateListener);
        }
        // DriverHeartbeatService continues running independently
    }

    // --- In-App Update (Flexible) ---

    private void checkForAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this);
        appUpdateManager.registerListener(installStateListener);

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.FLEXIBLE, this, UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    Log.e("AppUpdate", "Update flow failed", e);
                }
            }
        });
    }

    private void showUpdateSnackbar() {
        Snackbar.make(findViewById(android.R.id.content),
                        "Update downloaded. Restart to apply.", Snackbar.LENGTH_INDEFINITE)
                .setAction("RESTART", v -> appUpdateManager.completeUpdate())
                .show();
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
            return;
        }

        if (userDataFetched) {
            SharedPreferences cache = getSharedPreferences("app_cache", MODE_PRIVATE);
            String cachedName = cache.getString("driver_name", null);
            if (cachedName != null) {
                mainUsername.setText(cachedName);
                mainRating.setText(cache.getString("driver_rating", ""));
                setAvatarInitials(cachedName, cache.getString("driver_surname", ""));
                return;
            }
        }

        GetDriverData.GetData(MainActivity.this, accessToken, new DriverCallback() {
            @Override
            public void onDriverDataReceived(String name, String surname, String phoneNumber, String email,
                                             double rating, String street, String addressLine, String province,
                                             String city, String postalCode, String role, Boolean verified, Boolean license) {
                userDataFetched = true;
                getSharedPreferences("app_cache", MODE_PRIVATE).edit()
                        .putString("driver_name", name)
                        .putString("driver_surname", surname)
                        .putString("driver_rating", String.valueOf(rating))
                        .apply();
                mainUsername.setText(name);
                mainRating.setText(String.valueOf(rating));
                setAvatarInitials(name, surname);

                if (license == null || Boolean.FALSE.equals(license)){
                    Intent intent = new Intent(MainActivity.this, UploadLicenseActivity.class);
                    startActivity(intent);
                }

                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (!isFinishing() && !isDestroyed()) {
                                if (task.isSuccessful()) {
                                    String token = task.getResult();
                                    sendFCMtoken.sendFcmTokenToBackend(token, accessToken);
                                } else {
                                    Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                                }
                            }
                        });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("API", "Error: " + t.getMessage());
            }
        });
    }

    public void logOut() {
        String accessToken = Token.getAccessToken(this);
        if (accessToken != null && !accessToken.isEmpty()) {
            userDataFetched = false;
            LogOutUser.LogOut(this, accessToken);
        } else {
            Log.d("AccessToken", "No accessToken");
        }
    }

    private void setAvatarInitials(String name, String surname) {
        String initials = "";
        if (name != null && !name.isEmpty()) {
            initials += name.charAt(0);
        }
        if (surname != null && !surname.isEmpty()) {
            initials += surname.charAt(0);
        }
        avatarInitials.setText(initials.toUpperCase());
    }

    private void checkUserTerms(){
        String accessToken = Token.getAccessToken(this);

        if (accessToken == null || accessToken.isEmpty()) {
            Intent i = new Intent(MainActivity.this, Login.class);
            startActivity(i);
            finish();
            return;
        }

        // Always check with the server — the admin may have published new T&C
        String authHeader = "Bearer " + accessToken;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.checkTerms(authHeader, "driver");
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getSharedPreferences("app_cache", MODE_PRIVATE)
                            .edit().putBoolean("terms_accepted", true).apply();
                    getDriverData(accessToken);
                } else {
                    // New version or never accepted — clear cache and show T&C
                    getSharedPreferences("app_cache", MODE_PRIVATE)
                            .edit().putBoolean("terms_accepted", false).apply();
                    Intent terms = new Intent(MainActivity.this, TermsAgreement.class);
                    terms.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(terms);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("Failed", t.getMessage());
            }
        });
    }
}
