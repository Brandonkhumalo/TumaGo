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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.techmania.tumago.Activities.MainActivity;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.LoginUser;
import com.techmania.tumago.Model.TokenResponse;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.Token;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Login extends AppCompatActivity {
    TextView signup;
    EditText userName, pass;
    LinearLayout loginBtn;
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
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginUser();
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

    public void LoginUser(){
        String email = userName.getText().toString();
        String password = pass.getText().toString();
        Boolean remember = this.checked.isChecked();

        progressBar.setVisibility(View.VISIBLE);

        if (!email.isEmpty() && !password.isEmpty()) {
            LoginUser loginUser = new LoginUser(email, password);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<TokenResponse> call = apiService.login(loginUser);

            call.enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    if(response.isSuccessful()){
                        String accessToken = response.body().getAccess();
                        String refreshToken = response.body().getRefresh();

                        if (remember) {
                            Token.storeToken(Login.this, accessToken, refreshToken);
                        }

                        Intent i = new Intent(Login.this, MainActivity.class);
                        startActivity(i);
                        finish();

                        progressBar.setVisibility(View.GONE);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(Login.this, "Login failed, try again later", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    Toast.makeText(Login.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    Log.d("Failure", t.getMessage());
                }
            });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(Login.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
        }
    }
}









