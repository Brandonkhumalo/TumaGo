package com.techmania.tumago.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.techmania.tumago.R;
import com.techmania.tumago.helper.EmailSenderHelper;

import java.util.Random;

public class EmailVerification extends AppCompatActivity {
    EditText[] otpBoxes;
    TextView userEmail, resendOTP;
    Intent intent;
    int VerificationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        intent = getIntent();
        userEmail = findViewById(R.id.emailOtp);
        userEmail.setText(intent.getStringExtra("email"));

        setUpOtpInputs();

        resendOTP = findViewById(R.id.resendOtp);
        resendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = intent.getStringExtra("email");
                String text = "Account Verification Email";
                generateCode();

                EmailSenderHelper.sendEmail(email, text, VerificationCode);
            }
        });
    }

    private void setUpOtpInputs() {
        otpBoxes = new EditText[]{
                findViewById(R.id.otp1),
                findViewById(R.id.otp2),
                findViewById(R.id.otp3),
                findViewById(R.id.otp4),
                findViewById(R.id.otp5)
        };

        for (int i = 0; i < otpBoxes.length; i++) {
            int currentIndex = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!s.toString().isEmpty()) {
                        // Move to next box if not the last
                        if (currentIndex < otpBoxes.length - 1) {
                            otpBoxes[currentIndex + 1].requestFocus();
                        } else {
                            // Last box filled — show OTP
                            getOtpValue();
                            VerifyCode();
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void shakeOtpBoxes() {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake_otp);
        for (EditText box : otpBoxes) {
            box.startAnimation(shake);
        }
    }

    private String getOtpValue() {
        StringBuilder otp = new StringBuilder();
        for (EditText box : otpBoxes) {
            otp.append(box.getText().toString());
        }
        return otp.toString();
    }

    public void VerifyCode(){
        String OTP = getOtpValue().trim();
        String sentOTP = String.valueOf(intent.getIntExtra("VerificationCode", -1));

        if(OTP.equals(sentOTP)){
            Intent intent = new Intent(EmailVerification.this, UserInfo.class);
            storeBoolean(this, true);
            startActivity(intent);
            finish();
        }else{
            shakeOtpBoxes();
        }
    }

    public void storeBoolean(Context context, Boolean verifiedEmail) {
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

            SharedPreferences.Editor editor = securePrefs.edit();
            editor.putBoolean("verifiedEmail", verifiedEmail);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateCode(){
        Random rand = new Random();
        // Generate a 4-digit random number (between 1000 and 9999)
        int randomNumber = rand.nextInt(90000) + 9000; // Range: 1000 to 9999
        VerificationCode = randomNumber;
    }
}