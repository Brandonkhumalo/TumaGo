package com.techmania.tumago_driver.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.helpers.ApiClient;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Email OTP verification screen for drivers.
 * Sends a 6-digit code to the driver's email, verifies it before proceeding to Register.
 */
public class verification extends AppCompatActivity {

    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private EditText[] otpFields;
    private Button verifyBtn;
    private TextView resendOtp, emailOtpText;

    private String email;
    private boolean canResend = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        email = getIntent().getStringExtra("email");
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Email not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
        otpFields = new EditText[]{otp1, otp2, otp3, otp4, otp5, otp6};

        verifyBtn = findViewById(R.id.verify);
        resendOtp = findViewById(R.id.resendOtp);
        emailOtpText = findViewById(R.id.emailOtp);

        emailOtpText.setText("We sent a verification code to\n" + email);

        setupOtpInputs();
        sendOtpToEmail();

        verifyBtn.setOnClickListener(v -> verifyOtp());
        resendOtp.setOnClickListener(v -> {
            if (canResend) sendOtpToEmail();
        });
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    }
                }
            });
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpFields[index].getText().toString().isEmpty() && index > 0) {
                        otpFields[index - 1].requestFocus();
                        otpFields[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private String getOtpFromFields() {
        StringBuilder otp = new StringBuilder();
        for (EditText field : otpFields) {
            otp.append(field.getText().toString().trim());
        }
        return otp.toString();
    }

    private void sendOtpToEmail() {
        canResend = false;
        resendOtp.setEnabled(false);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        api.sendEmailOtp(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(verification.this, "Verification code sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(verification.this, "Failed to send code. Try again.", Toast.LENGTH_SHORT).show();
                    canResend = true;
                    resendOtp.setEnabled(true);
                    return;
                }
                startResendCooldown();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(verification.this, "Connection error", Toast.LENGTH_SHORT).show();
                canResend = true;
                resendOtp.setEnabled(true);
            }
        });
    }

    private void startResendCooldown() {
        new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendOtp.setText("Resend in " + (millisUntilFinished / 1000) + "s");
                resendOtp.setEnabled(false);
            }
            @Override
            public void onFinish() {
                resendOtp.setText("Resend Code");
                resendOtp.setEnabled(true);
                canResend = true;
            }
        }.start();
    }

    private void verifyOtp() {
        String otp = getOtpFromFields();
        if (otp.length() != 6) {
            Toast.makeText(this, "Please enter the full code", Toast.LENGTH_SHORT).show();
            return;
        }

        verifyBtn.setEnabled(false);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("otp", otp);

        api.verifyEmailOtp(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                verifyBtn.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(verification.this, "Email verified!", Toast.LENGTH_SHORT).show();
                    Intent result = new Intent();
                    result.putExtra("email_verified", true);
                    setResult(RESULT_OK, result);
                    finish();
                } else {
                    Toast.makeText(verification.this, "Invalid code. Try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                verifyBtn.setEnabled(true);
                Toast.makeText(verification.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }
}
