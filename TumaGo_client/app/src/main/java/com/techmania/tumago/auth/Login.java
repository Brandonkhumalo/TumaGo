package com.techmania.tumago.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.techmania.tumago.Activities.HomeActivity;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.LoginUser;
import com.techmania.tumago.Model.TokenResponse;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.BiometricHelper;
import com.techmania.tumago.helper.Token;
import com.techmania.tumago.helper.UiHelper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Login extends AppCompatActivity {
    TextView signup, forgetPass;
    EditText userName, pass;
    Button loginBtn;
    CheckBox checked;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        signup = findViewById(R.id.signup);
        userName = findViewById(R.id.usernameInput);
        pass = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        checked = findViewById(R.id.remember);
        progressBar = findViewById(R.id.progressLogin);

        EditText passwordInput = findViewById(R.id.passwordInput);
        ImageView togglePassword = findViewById(R.id.togglePassword);

        togglePassword.setOnClickListener(new View.OnClickListener() {
            boolean isPasswordVisible = false;

            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    togglePassword.setImageResource(R.drawable.pass); // closed eye icon
                } else {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    togglePassword.setImageResource(R.drawable.pass); // open eye icon
                }
                isPasswordVisible = !isPasswordVisible;
                passwordInput.setSelection(passwordInput.length()); // Keep cursor at the end
            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Login.this, SignUp.class);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginUser();
            }
        });

        forgetPass = findViewById(R.id.forgetPass);
        forgetPass.setOnClickListener(v -> showForgotPasswordDialog());

        checked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Checkbox is checked
                    checked.setChecked(true);
                } else {
                    // Checkbox is unchecked
                    checked.setChecked(false);
                }
            }
        });
    }

    public void LoginUser(){
        String email = userName.getText().toString();
        String password = pass.getText().toString();
        Boolean remember = this.checked.isChecked();

        progressBar.setVisibility(View.VISIBLE);

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(Login.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.isEmpty() && !password.isEmpty()) {
            LoginUser loginUser = new LoginUser(email, password);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<TokenResponse> call = apiService.login(loginUser);

            loginBtn.setEnabled(false);
            call.enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    if(response.isSuccessful()){
                        if (response.body() == null) {
                            progressBar.setVisibility(View.GONE);
                            loginBtn.setEnabled(true);
                            Toast.makeText(Login.this, "Login failed, please try again", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String accessToken = response.body().getAccess();
                        String refreshToken = response.body().getRefresh();

                        if (remember) {
                            Token.storeToken(Login.this, accessToken, refreshToken);
                        }

                        progressBar.setVisibility(View.GONE);
                        loginBtn.setEnabled(true);

                        // First-time biometric prompt
                        if (!BiometricHelper.hasBeenAsked(Login.this)
                                && BiometricHelper.isDeviceSupported(Login.this)) {
                            BiometricHelper.setAsked(Login.this);
                            new MaterialAlertDialogBuilder(Login.this)
                                    .setTitle("Enable Biometric Login?")
                                    .setMessage("Use your fingerprint or face to log in faster next time.")
                                    .setPositiveButton("Enable", (d, w) -> {
                                        BiometricHelper.setEnabled(Login.this, true);
                                        goToHome();
                                    })
                                    .setNegativeButton("Not now", (d, w) -> {
                                        Toast.makeText(Login.this,
                                                "You can enable this later in Settings",
                                                Toast.LENGTH_SHORT).show();
                                        goToHome();
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            goToHome();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        loginBtn.setEnabled(true);
                        String msg;
                        if (response.code() == 400 || response.code() == 401) {
                            msg = "Incorrect email or password";
                        } else {
                            msg = "Login failed, please try again";
                        }
                        Toast.makeText(Login.this, msg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    UiHelper.hideLoading(progressBar, loginBtn);
                    Log.d("Failure", t.getMessage());
                    UiHelper.showRetry(findViewById(android.R.id.content), "No connection, check your internet", () -> LoginUser());
                }
            });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(Login.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHome() {
        Intent i = new Intent(Login.this, HomeActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void showForgotPasswordDialog() {
        final EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email address");
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setPadding(48, 32, 48, 32);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email and we'll send you a link to reset your password.")
                .setView(emailInput)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendForgotPasswordRequest(email);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendForgotPasswordRequest(String email) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        apiService.forgotPassword(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Toast.makeText(Login.this, "If an account exists with that email, a reset link has been sent.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(Login.this, "Connection error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}









