package com.techmania.tumago_driver.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.techmania.tumago_driver.R;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class EmailSender extends AppCompatActivity {
    Button ConfirmEmail;
    EditText emailReg;
    LinearLayout goToLogin;

    // Launcher for email verification — on success, proceed to Register
    private final ActivityResultLauncher<Intent> emailVerificationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    boolean verified = result.getData().getBooleanExtra("email_verified", false);
                    if (verified) {
                        String email = emailReg.getText().toString().trim();
                        storeEmail(EmailSender.this, email);
                        storeVerified(EmailSender.this, true);

                        Intent i = new Intent(EmailSender.this, Register.class);
                        startActivity(i);
                        finish();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_sender);

        ConfirmEmail = findViewById(R.id.continueBtn);
        emailReg = findViewById(R.id.emailReg);
        goToLogin = findViewById(R.id.goToLogin);

        ConfirmEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailReg.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(EmailSender.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(EmailSender.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Launch email verification activity
                Intent i = new Intent(EmailSender.this, verification.class);
                i.putExtra("email", email);
                emailVerificationLauncher.launch(i);
            }
        });

        goToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(EmailSender.this, Login.class);
                startActivity(i);
                finish();
            }
        });
    }

    public void storeEmail(Context context, String Email) {
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
            editor.putString("Email", Email);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void storeVerified(Context context, Boolean verifiedEmail) {
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
}
