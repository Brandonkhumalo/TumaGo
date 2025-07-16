package com.techmania.tumago_driver.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.techmania.tumago_driver.R;

import java.util.Properties;
import java.util.Random;

import com.google.android.material.textfield.TextInputEditText;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender extends AppCompatActivity {
    Button ConfirmEmail;
    TextInputEditText emailReg;
    ProgressBar progressBar;
    LinearLayout goToLogin;
    int VerificationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_sender);

        ConfirmEmail = findViewById(R.id.continueBtn);
        emailReg = findViewById(R.id.emailReg);
        progressBar = findViewById(R.id.progressBar);
        goToLogin = findViewById(R.id.goToLogin);

        ConfirmEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String to = emailReg.getText().toString();
                String text = "Account Verification Email";
                generateCode();

                sendEmail(to, text, VerificationCode);
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

    public void generateCode(){
        Random rand = new Random();
        // Generate a 4-digit random number (between 1000 and 9999)
        int randomNumber = rand.nextInt(90000) + 9000; // Range: 1000 to 9999
        VerificationCode = randomNumber;
    }

    public void sendEmail(final String to, final String subject, final int body) {
        new SendEmailTask(to, subject, body).execute();
    }

    private class SendEmailTask extends AsyncTask<Void, Void, Boolean> {
        private final String to, subject;
        private final int body;
        private static final String FROM_EMAIL = "apex2.0predator@gmail.com";  // Sender email
        private static final String PASSWORD = "hrobguwmsqgjkswf";  // Use App Password from Google

        public SendEmailTask(String to, String subject, int body) {
            this.to = to;
            this.subject = subject;
            this.body = body;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Properties properties = new Properties();
                properties.put("mail.smtp.host", "smtp.gmail.com");
                properties.put("mail.smtp.port", "587");
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setText(String.valueOf(body)); // Convert int to string

                Transport.send(message);
                return true;
            } catch (MessagingException e) {
                Log.e("SendEmail", "Failed to send email", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.VISIBLE);
            if (success) {
                Intent i = new Intent(EmailSender.this, verification.class);
                i.putExtra("VerificationCode", VerificationCode);
                i.putExtra("Email", emailReg.getText().toString());
                storeEmail(EmailSender.this, emailReg.getText().toString());
                startActivity(i);
                finish();

                progressBar.setVisibility(View.GONE);
            } else {
                Log.e("SendEmail", "Failed to send email!");
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EmailSender.this, "Failed to send email!", Toast.LENGTH_SHORT).show();
            }
        }
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
}