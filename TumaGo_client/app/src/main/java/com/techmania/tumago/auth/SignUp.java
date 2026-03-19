package com.techmania.tumago.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.CreateUser;
import com.techmania.tumago.Model.TokenResponse;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.Token;
import com.techmania.tumago.helper.UiHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUp extends AppCompatActivity {
    TextView signin;
    Button btnLayout, nextPage, prevPage;
    EditText email, password, usernameInput, surnameInput;
    ProgressBar progressBar;
    private Spinner spinnerCountryCode;
    private EditText phoneNumberInput;
    String countryCode;

    // Page containers
    LinearLayout infoRegister, dataForm;
    TextView pageNumberTextView;
    int currentPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        signin = findViewById(R.id.signin);
        btnLayout = findViewById(R.id.regBtn);
        nextPage = findViewById(R.id.nextPage);
        prevPage = findViewById(R.id.prevPage);
        email = findViewById(R.id.emailInput);
        password = findViewById(R.id.passwordInput);
        usernameInput = findViewById(R.id.usernameInput);
        surnameInput = findViewById(R.id.surnameInput);
        progressBar = findViewById(R.id.progressSign);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        infoRegister = findViewById(R.id.infoRegister);
        dataForm = findViewById(R.id.dataForm);
        pageNumberTextView = findViewById(R.id.pageNumberTextView);

        EditText passwordInput = findViewById(R.id.passwordInput);
        ImageView togglePassword = findViewById(R.id.togglePassword);
        EditText passwordConfirm = findViewById(R.id.passwordConfirm);
        ImageView togglePasswordConfirm = findViewById(R.id.togglePasswordConfirm);

        togglePassword.setOnClickListener(new View.OnClickListener() {
            boolean isPasswordVisible = false;

            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }
                isPasswordVisible = !isPasswordVisible;
                passwordInput.setSelection(passwordInput.length());
            }
        });

        togglePasswordConfirm.setOnClickListener(new View.OnClickListener() {
            boolean isPasswordVisible = false;

            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    passwordConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    passwordConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }
                isPasswordVisible = !isPasswordVisible;
                passwordConfirm.setSelection(passwordConfirm.length());
            }
        });

        // PHONE NUMBER — custom adapter shows "ZW +263" collapsed, full name in dropdown
        CountrySpinnerAdapter adapter = new CountrySpinnerAdapter(
                this, CountryCodes.getShortLabels(), CountryCodes.getDropdownNames());
        spinnerCountryCode.setAdapter(adapter);

        int defaultPos = CountryCodes.getDefaultPosition();
        spinnerCountryCode.setSelection(defaultPos);
        countryCode = CountryCodes.getCodeByPosition(defaultPos);

        // Handle Country Selection
        spinnerCountryCode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                countryCode = CountryCodes.getCodeByPosition(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Page navigation
        nextPage.setOnClickListener(v -> {
            if (currentPage == 1) {
                String firstName = usernameInput.getText().toString().trim();
                String surname = surnameInput.getText().toString().trim();
                String phone = phoneNumberInput.getText().toString().trim();

                if (firstName.isEmpty()) {
                    Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (surname.isEmpty()) {
                    Toast.makeText(this, "Please enter your surname", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (phone.isEmpty()) {
                    Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Move to page 2
                currentPage = 2;
                infoRegister.setVisibility(View.GONE);
                dataForm.setVisibility(View.VISIBLE);
                prevPage.setVisibility(View.VISIBLE);
                nextPage.setVisibility(View.GONE);
                btnLayout.setVisibility(View.VISIBLE);
                pageNumberTextView.setText("Step 2 of 2");
            }
        });

        prevPage.setOnClickListener(v -> {
            if (currentPage == 2) {
                currentPage = 1;
                dataForm.setVisibility(View.GONE);
                infoRegister.setVisibility(View.VISIBLE);
                prevPage.setVisibility(View.GONE);
                nextPage.setVisibility(View.VISIBLE);
                btnLayout.setVisibility(View.GONE);
                pageNumberTextView.setText("Step 1 of 2");
            }
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
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

    }

    public void RegisterUser(){
        String emailText = email.getText().toString().trim();
        String passwordText = password.getText().toString().trim();
        String firstName = usernameInput.getText().toString().trim();
        String surname = surnameInput.getText().toString().trim();
        String phone = getFullPhoneNumber();

        progressBar.setVisibility(View.VISIBLE);

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(SignUp.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (passwordText.length() < 6) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(SignUp.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!emailText.isEmpty() && !passwordText.isEmpty() && !phone.isEmpty()) {
            CreateUser createUser = new CreateUser(emailText, passwordText, phone, firstName, surname);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<TokenResponse> call = apiService.signup(createUser);

            btnLayout.setEnabled(false);
            call.enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    if (response.isSuccessful()) {
                        if (response.body() == null) {
                            progressBar.setVisibility(View.GONE);
                            btnLayout.setEnabled(true);
                            Toast.makeText(SignUp.this, "Registration failed, try again later", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String accessToken = response.body().getAccess();
                        String refreshToken = response.body().getRefresh();

                        Token.storeToken(SignUp.this, accessToken, refreshToken);

                        progressBar.setVisibility(View.GONE);
                        btnLayout.setEnabled(true);
                        Intent i = new Intent(SignUp.this, UserInfo.class);
                        startActivity(i);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLayout.setEnabled(true);
                        Toast.makeText(SignUp.this, "Registration failed, try again later", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    UiHelper.hideLoading(progressBar, btnLayout);
                    UiHelper.showRetry(findViewById(android.R.id.content), "Connection error", () -> RegisterUser());
                }
            });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(SignUp.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
        }
    }

    private String getRawPhoneNumber() {
        String raw = phoneNumberInput.getText().toString().trim();

        // Remove leading 0 if present
        if (raw.startsWith("0")) {
            raw = raw.substring(1);
        }

        return raw;
    }

    public String getFullPhoneNumber() {
        return countryCode + getRawPhoneNumber();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
