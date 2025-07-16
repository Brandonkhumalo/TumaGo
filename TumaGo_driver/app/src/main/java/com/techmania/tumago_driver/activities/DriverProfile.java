package com.techmania.tumago_driver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.Interface.DriverCallback;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.auth.EmailSender;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.GetDriverData;
import com.techmania.tumago_driver.helpers.Token;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverProfile extends AppCompatActivity {
    TextView mainUsername, mainRating, mainName, mainPhonenumber, mainEmail, mainAddress, licenseSubmitted, emailVerified;
    MaterialCardView delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        mainUsername = findViewById(R.id.mainUsername);
        mainRating = findViewById(R.id.mainRating);
        mainName = findViewById(R.id.mainName);
        mainPhonenumber = findViewById(R.id.mainPhonenumber);
        mainEmail = findViewById(R.id.mainEmail);
        mainAddress = findViewById(R.id.mainAddress);
        delete = findViewById(R.id.delete);
        licenseSubmitted = findViewById(R.id.license);
        emailVerified = findViewById(R.id.Email);

        getDriverData();

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAccount();
            }
        });

        LinearLayout resetPassword = findViewById(R.id.resetPassword);
        resetPassword.setOnClickListener(v -> {
            Intent i = new Intent(DriverProfile.this, ResetPassword.class);
            startActivity(i);
        });
    }

    public void getDriverData(){
        String accessToken = Token.getAccessToken(this);

        GetDriverData.GetData(this, accessToken, new DriverCallback() {
            @Override
            public void onDriverDataReceived(String name, String surname, String phoneNumber, String email, double rating, String street,
                                             String addressLine, String province, String city, String postalCode, String role, Boolean verified, Boolean license) {

                mainUsername.setText(name + " " + surname);
                mainRating.setText(String.valueOf(rating));
                mainName.setText(name + " " + surname);
                mainPhonenumber.setText(phoneNumber);
                mainEmail.setText(email);
                mainAddress.setText(street + ", "+ city);
                licenseSubmitted.setText(license != null && license ? "Submitted" : "Not Submitted");
                emailVerified.setText(verified ? "Verified" : "Not Verified");

                licenseSubmitted.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Only proceed if the text says "Not Submitted"
                        if ("Not Submitted".equals(licenseSubmitted.getText().toString())) {
                            Intent intent = new Intent(DriverProfile.this, UploadLicenseActivity.class);
                            startActivity(intent);
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

    public void deleteAccount(){
        String accessToken = Token.getAccessToken(this);

        String authHeader = "Bearer " + accessToken;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.deleteAccount(authHeader);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()){
                    Token.clearToken(DriverProfile.this);
                    Intent intent = new Intent(DriverProfile.this, EmailSender.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // optional: clear back stack
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }
}