package com.techmania.tumago.auth;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import androidx.appcompat.app.AppCompatActivity;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.CreateUser;
import com.techmania.tumago.Model.TokenResponse;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.Token;

import java.util.Properties;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUp extends AppCompatActivity {
    TextView signin;
    LinearLayout btnLayout;
    EditText email, password;
    CheckBox checked;
    ProgressBar progressBar;
    private Spinner spinnerCountryCode;
    private EditText phoneNumberInput;
    String countryCode;
    int VerificationCode;
    private final String DEFAULT_COUNTRY = "🇿🇼 Zimbabwe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        signin = findViewById(R.id.signin);
        btnLayout = findViewById(R.id.regBtn);
        email = findViewById(R.id.emailInput);
        password = findViewById(R.id.passwordInput);
        checked = findViewById(R.id.remember);
        progressBar = findViewById(R.id.progressSign);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        phoneNumberInput = findViewById(R.id.phoneNumberInput);

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

        //PHONE NUMBER
        // Spinner Adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item
                ,CountryCodes.getCountryNames());
        spinnerCountryCode.setAdapter(adapter);

        int defaultPosition = CountryCodes.getCountryNames().indexOf(DEFAULT_COUNTRY);
        spinnerCountryCode.setSelection(defaultPosition);
        phoneNumberInput.setText(CountryCodes.getCode(DEFAULT_COUNTRY) + " "); // Pre-fill South Africa's code
        phoneNumberInput.setSelection(phoneNumberInput.getText().length()); // Move cursor to end


        // Handle Country Selection
        spinnerCountryCode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCountry = parent.getItemAtPosition(position).toString();
                countryCode = CountryCodes.getCode(selectedCountry);

                if (countryCode != null) {
                    phoneNumberInput.setText(countryCode + " ");
                    phoneNumberInput.setSelection(phoneNumberInput.getText().length()); // Move cursor to end
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterUser();
            }
        });

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SignUp.this, Login.class);
                startActivity(i);
            }
        });

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

    public void RegisterUser(){
        String email = this.email.getText().toString();
        String password = this.password.getText().toString();

        progressBar.setVisibility(View.VISIBLE);

        if(!email.isEmpty() && !password.isEmpty() && !getFullPhoneNumber().isEmpty()) {
            CreateUser createUser = new CreateUser(email, password, getFullPhoneNumber());

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<TokenResponse> call = apiService.signup(createUser);

            call.enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    if (response.isSuccessful()) {

                        String accessToken = response.body().getAccess();
                        String refreshToken = response.body().getRefresh();

                        if(checked.isChecked()){
                            Token.storeToken(SignUp.this, accessToken, refreshToken);
                        }

                        String text = "Account Verification Email";
                        generateCode();
                        sendEmail(email, text, VerificationCode);

                        progressBar.setVisibility(View.GONE);
                        Intent i = new Intent(SignUp.this, EmailVerification.class);
                        i.putExtra("email", email);
                        startActivity(i);
                        finish();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SignUp.this, "Registration failed, try again later", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    Toast.makeText(SignUp.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(SignUp.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
        }
    }

    private String getRawPhoneNumber() {
        String text = phoneNumberInput.getText().toString();
        String raw = text.replace(countryCode + " ", "").trim();

        // Remove leading 0 if present
        if (raw.startsWith("0")) {
            raw = raw.substring(1);
        }

        return raw;
    }

    public String getFullPhoneNumber() {
        return countryCode + getRawPhoneNumber();
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
                progressBar.setVisibility(View.GONE);
                Intent i = new Intent();
                i.putExtra("VerificationCode", VerificationCode);
            } else {
                Log.e("SendEmail", "Failed to send email!");
                progressBar.setVisibility(View.GONE);
            }
        }
    }
}








