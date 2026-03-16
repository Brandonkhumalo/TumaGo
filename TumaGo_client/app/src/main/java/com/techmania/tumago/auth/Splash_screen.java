package com.techmania.tumago.auth;

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

import com.techmania.tumago.Activities.HomeActivity;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.ThemeHelper;
import com.techmania.tumago.helper.Token;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Splash_screen extends AppCompatActivity {
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
        handler.removeCallbacks(runnable);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            CheckToken();
        } else {
            if (permissionRequestCount < 3) {
                permissionRequestCount++;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Continue without location instead of force-closing
                Toast.makeText(this, "Continuing without location access. Some features may be limited.", Toast.LENGTH_LONG).show();
                CheckToken();
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
                CheckToken();
            } else {
                checkLocationPermission();
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

    private void CheckToken() {
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
                        Intent mainActivity = new Intent(Splash_screen.this, HomeActivity.class);
                        startActivity(mainActivity);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    } else {
                        Intent login = new Intent(Splash_screen.this, Login.class);
                        startActivity(login);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                        Token.clearToken(Splash_screen.this);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("Failed", t.getMessage());
                    Intent login = new Intent(Splash_screen.this, Login.class);
                    startActivity(login);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                }
            });
        } else {
            Intent i = new Intent(Splash_screen.this, Login.class);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
    }
}









