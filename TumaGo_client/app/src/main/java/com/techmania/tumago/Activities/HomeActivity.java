package com.techmania.tumago.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Interface.UserCallback;
import com.techmania.tumago.R;
import com.techmania.tumago.auth.Login;
import com.techmania.tumago.auth.TermsAgreement;
import com.techmania.tumago.fragments.DeliveryFragment;
import com.techmania.tumago.fragments.HomeFragment;
import com.techmania.tumago.fragments.ParcelsFragment;
import com.techmania.tumago.fragments.SettingsFragment;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.GetUserData;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.SendFCMtoken;
import com.techmania.tumago.helper.Token;
import com.techmania.tumago.helper.UiHelper;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private final SendFCMtoken sendFCMtoken = new SendFCMtoken();
    private static boolean userDataFetched = false;

    // Keep fragment instances to avoid recreating them on tab switch
    private final HomeFragment homeFragment = new HomeFragment();
    private final DeliveryFragment deliveryFragment = new DeliveryFragment();
    private final ParcelsFragment parcelsFragment = new ParcelsFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_home);

        bottomNav = findViewById(R.id.bottom_nav);

        // Add all fragments but only show Home initially
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, settingsFragment, "settings").hide(settingsFragment)
                    .add(R.id.fragment_container, parcelsFragment, "parcels").hide(parcelsFragment)
                    .add(R.id.fragment_container, deliveryFragment, "delivery").hide(deliveryFragment)
                    .add(R.id.fragment_container, homeFragment, "home")
                    .commit();
            activeFragment = homeFragment;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                selected = homeFragment;
            } else if (id == R.id.nav_delivery) {
                selected = deliveryFragment;
            } else if (id == R.id.nav_parcels) {
                selected = parcelsFragment;
            } else if (id == R.id.nav_settings) {
                selected = settingsFragment;
            } else {
                return false;
            }

            if (selected != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                        .hide(activeFragment)
                        .show(selected)
                        .commit();
                activeFragment = selected;
            }
            return true;
        });

        // Default to Home tab
        bottomNav.setSelectedItemId(R.id.nav_home);

        checkUserTerms();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }
    }

    private void checkUserTerms() {
        String accessToken = Token.getAccessToken(this);

        if (accessToken == null || accessToken.isEmpty()) {
            Intent i = new Intent(HomeActivity.this, Login.class);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }

        SharedPreferences cache = getSharedPreferences("app_cache", MODE_PRIVATE);
        if (cache.getBoolean("terms_accepted", false)) {
            getUserData(accessToken);
            return;
        }

        String authHeader = "Bearer " + accessToken;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.checkTerms(authHeader);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    cache.edit().putBoolean("terms_accepted", true).apply();
                    getUserData(accessToken);
                } else {
                    Intent terms = new Intent(HomeActivity.this, TermsAgreement.class);
                    terms.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(terms);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                UiHelper.showRetry(findViewById(android.R.id.content), "Connection error", () -> checkUserTerms());
            }
        });
    }

    public void getUserData(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Please Log in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, Login.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }

        SharedPreferences cache = getSharedPreferences("app_cache", MODE_PRIVATE);

        if (userDataFetched && cache.getString("user_name", null) != null) {
            return;
        }

        userDataFetched = true;
        GetUserData.GetData(HomeActivity.this, accessToken, new UserCallback() {
            @Override
            public void onUserDataReceived(String name, String surname, String phoneNumber,
                    String email, double rating, String street, String addressLine, String province,
                    String city, String postalCode, String role) {
                cache.edit()
                        .putString("user_name", name)
                        .putString("user_surname", surname)
                        .putString("user_rating", String.valueOf(rating))
                        .apply();

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
                userDataFetched = false;
                UiHelper.showRetry(findViewById(android.R.id.content), "Failed to load data", () -> getUserData(accessToken));
            }
        });
    }
}
