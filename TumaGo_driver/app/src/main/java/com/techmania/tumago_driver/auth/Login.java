package com.techmania.tumago_driver.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.messaging.FirebaseMessaging;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.activities.MainActivity;
import com.techmania.tumago_driver.helpers.AnimHelper;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.BiometricHelper;
import com.techmania.tumago_driver.helpers.SendFCMtoken;
import com.techmania.tumago_driver.helpers.Token;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.techmania.tumago_driver.models.LoginDriver;
import com.techmania.tumago_driver.models.TokenResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Login extends AppCompatActivity {
    ProgressBar progressBar;
    Button login, register;
    EditText Email, Password;

    SendFCMtoken sendFcmToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressBar = findViewById(R.id.progressBar);
        login = findViewById(R.id.login);
        register = findViewById(R.id.register);
        Email = findViewById(R.id.email);
        Password = findViewById(R.id.password);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logindriver();
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Login.this, EmailSender.class);
                startActivity(i);
                finish();
            }
        });
    }

    private void shakePinBoxes() {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake_otp);
            Email.startAnimation(shake);
            Password.startAnimation(shake);
    }

    public void Logindriver(){
        String email = Email.getText().toString();
        String password = Password.getText().toString();

        if(!email.isEmpty() && !password.isEmpty()){
            LoginDriver driver = new LoginDriver(email, password);

            AnimHelper.fadeIn(progressBar);
            login.setEnabled(false);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<TokenResponse> call = apiService.login(driver);

            call.enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    if (response.isSuccessful()) {
                        String accessToken = response.body().getAccess();
                        String refreshToken = response.body().getRefresh();
                        Token.storeToken(Login.this, accessToken, refreshToken);

                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                                        return;
                                    }

                                    // Get new FCM registration token
                                    String token = task.getResult();
                                    sendFcmToken = new SendFCMtoken();
                                    sendFcmToken.sendFcmTokenToBackend(token, accessToken);
                                });

                        AnimHelper.fadeOut(progressBar);
                        login.setEnabled(true);

                        // First-time biometric prompt
                        if (!BiometricHelper.hasBeenAsked(Login.this)
                                && BiometricHelper.isDeviceSupported(Login.this)) {
                            BiometricHelper.setAsked(Login.this);
                            new MaterialAlertDialogBuilder(Login.this)
                                    .setTitle("Enable Biometric Login?")
                                    .setMessage("Use your fingerprint or face to log in faster next time.")
                                    .setPositiveButton("Enable", (d, w) -> {
                                        BiometricHelper.setEnabled(Login.this, true);
                                        goToMain();
                                    })
                                    .setNegativeButton("Not now", (d, w) -> {
                                        Toast.makeText(Login.this,
                                                "You can enable this later in Settings",
                                                Toast.LENGTH_SHORT).show();
                                        goToMain();
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            goToMain();
                        }
                    } else {
                        AnimHelper.fadeOut(progressBar);
                        login.setEnabled(true);
                        String msg;
                        if (response.code() == 400 || response.code() == 401) {
                            msg = "Incorrect email or password";
                        } else {
                            msg = "Login failed (error " + response.code() + "), please try again";
                        }
                        Log.e("Login", "HTTP " + response.code() + " — " + response.message());
                        Toast.makeText(Login.this, msg, Toast.LENGTH_SHORT).show();
                        shakePinBoxes();
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    Log.e("Login Error", t.getMessage());
                    AnimHelper.fadeOut(progressBar);
                    login.setEnabled(true);
                    Toast.makeText(Login.this, "No connection, check your internet", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            shakePinBoxes();
            Log.e("Error", "error");
        }
    }

    private void goToMain() {
        Intent i = new Intent(Login.this, MainActivity.class);
        startActivity(i);
        finish();
    }
}