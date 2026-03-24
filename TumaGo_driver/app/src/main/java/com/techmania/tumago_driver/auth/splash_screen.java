package com.techmania.tumago_driver.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.activities.MainActivity;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.BiometricHelper;
import com.techmania.tumago_driver.helpers.ThemeHelper;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class splash_screen extends androidx.fragment.app.FragmentActivity {
    ImageView imageSplash;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private int permissionRequestCount = 0;

    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        setContentView(R.layout.activity_splash_screen);

        imageSplash = findViewById(R.id.imageSplash);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.roadlink_splash);
        imageSplash.startAnimation(anim);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                checkLocationPermission();
            }
        };
        handler.postDelayed(runnable, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            CheckToken(); // ✅ Permission already granted
        } else {
            if (permissionRequestCount < 3) {
                permissionRequestCount++;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Location permission denied. Closing app.", Toast.LENGTH_LONG).show();
                finish(); // ❌ Shut down the app
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CheckToken(); // ✅ Permission granted now
            } else {
                checkLocationPermission(); // 🔁 Retry or exit
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    public void CheckToken() {
        requestNotificationPermission();
        String accessToken = Token.getAccessToken(this);

        if (accessToken != null && !accessToken.isEmpty()) {
            String authHeader = "Bearer " + accessToken;

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.verifyToken(authHeader);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        if (BiometricHelper.isEnabled(splash_screen.this)
                                && BiometricHelper.isDeviceSupported(splash_screen.this)) {
                            BiometricHelper.authenticate(splash_screen.this,
                                    () -> {
                                        startActivity(new Intent(splash_screen.this, MainActivity.class));
                                        finish();
                                    },
                                    () -> {
                                        startActivity(new Intent(splash_screen.this, Login.class));
                                        finish();
                                    });
                            return;
                        }
                        Intent mainActivity = new Intent(splash_screen.this, MainActivity.class);
                        startActivity(mainActivity);
                        finish();
                    } else {
                        Intent login = new Intent(splash_screen.this, Login.class);
                        startActivity(login);
                        finish();
                        Token.clearToken(splash_screen.this);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("Failed", t.getMessage());
                    Intent login = new Intent(splash_screen.this, Login.class);
                    startActivity(login);
                    finish();
                }
            });
        } else {
            Intent i = new Intent(splash_screen.this, Login.class);
            startActivity(i);
            finish();
            Log.d("Failed", "Token is null or empty");
        }
    }
}