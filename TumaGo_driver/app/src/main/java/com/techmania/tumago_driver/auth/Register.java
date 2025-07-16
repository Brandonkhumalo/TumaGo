package com.techmania.tumago_driver.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.activities.MainActivity;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.Token;
import com.techmania.tumago_driver.models.CreateDriver;
import com.techmania.tumago_driver.models.TokenResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Register extends AppCompatActivity {
    LinearLayout personalInfo, pinLayout, progressBar;
    EditText name, surname, number, street, address, city, code, password1, password2;
    MaterialCardView continueBtn, regBtn;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        personalInfo = findViewById(R.id.personalInfo);
        pinLayout = findViewById(R.id.pinLayout);
        continueBtn = findViewById(R.id.continueBtn);
        name = findViewById(R.id.name);
        surname = findViewById(R.id.surname);
        number = findViewById(R.id.phoneNumber);
        street = findViewById(R.id.userStreet);
        address = findViewById(R.id.userAddress);
        city = findViewById(R.id.userCity);
        code = findViewById(R.id.userPostalCode);
        progressBar = findViewById(R.id.progressBar);
        password1 = findViewById(R.id.password1);
        password2 = findViewById(R.id.password2);
        regBtn = findViewById(R.id.regBtn);

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                personalInfo.setVisibility(View.GONE);
                pinLayout.setVisibility(View.VISIBLE);
            }
        });

        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmPass();
            }
        });
    }

    public void ConfirmPass(){
        String pass1 = password1.getText().toString();
        String pass2 = password2.getText().toString();

        if(!pass1.isEmpty() && !pass2.isEmpty()){
            if(pass1.equals(pass2)){
                password = pass1;
                RegisterDriver();
            } else {
                shakeOtpBoxes();
            }
        } else {
            Toast.makeText(this, "Please enter Password", Toast.LENGTH_SHORT).show();
            shakeOtpBoxes();
        }
    }

    private void shakeOtpBoxes() {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake_otp);
            password1.startAnimation(shake);
            password2.startAnimation(shake);
    }

    private void RegisterDriver(){
        String name = this.name.getText().toString();
        String surname = this.surname.getText().toString();
        String email = getEmail(this);
        Boolean verifiedEmail = getBoolean(this);
        String phone_number = this.number.getText().toString().trim();
        String streetAdress = this.street.getText().toString();
        String addressLine = this.address.getText().toString();
        String province = this.city.getText().toString();
        String city = this.city.getText().toString();
        String postalCode = this.code.getText().toString();

        progressBar.setVisibility(View.VISIBLE);
        pinLayout.setVisibility(View.GONE);

        if (!name.isEmpty() && !surname.isEmpty() && !email.isEmpty() && !phone_number.isEmpty() && !streetAdress.isEmpty() &&
                !province.isEmpty() && !city.isEmpty() && !postalCode.isEmpty()){

            CreateDriver createDriver = new CreateDriver(name, surname, email, password, phone_number, verifiedEmail,
                    streetAdress, addressLine, city, province, postalCode);

            try{

                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<TokenResponse> call = apiService.createDriver(createDriver);

                call.enqueue(new Callback<TokenResponse>() {
                    @Override
                    public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                        Intent intent;
                        if(response.isSuccessful()) {
                            intent = new Intent(Register.this, Transport.class);
                            startActivity(intent);
                            finish();

                            String accessToken = response.body().getAccess();
                            String refreshToken = response.body().getRefresh();
                            Token.storeToken(Register.this, accessToken, refreshToken);

                        } else {
                            intent = new Intent(Register.this, MainActivity.class);
                            startActivity(intent);
                            finish();

                        }
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(Call<TokenResponse> call, Throwable t) {
                        Log.e("failed", t.getMessage());
                        progressBar.setVisibility(View.GONE);
                        personalInfo.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {
                Toast.makeText(this, "File error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("File error", e.getMessage());
                progressBar.setVisibility(View.GONE);
                personalInfo.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            personalInfo.setVisibility(View.VISIBLE);
        }
    }

    public static String getEmail(Context context) {
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

            return securePrefs.getString("Email", null);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Boolean getBoolean(Context context) {
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

            return securePrefs.getBoolean("verifiedEmail", true);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}


















