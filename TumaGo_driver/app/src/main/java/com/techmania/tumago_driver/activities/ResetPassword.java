package com.techmania.tumago_driver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.auth.Login;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.Token;
import com.techmania.tumago_driver.models.ResetPasswordModel;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPassword extends AppCompatActivity {

    EditText password, passwordConfirm, oldPassword;
    MaterialCardView changePass;
    TextView passwordChanged, passwordUnchanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        passwordChanged = findViewById(R.id.passwordChanged);
        passwordUnchanged = findViewById(R.id.passwordUnchanged);

        oldPassword = findViewById(R.id.oldPassword);
        password = findViewById(R.id.passwordInput);
        passwordConfirm = findViewById(R.id.passwordConfirm);
        changePass = findViewById(R.id.changePassword);

        ImageView togglePassword = findViewById(R.id.togglePassword);
        ImageView togglePasswordConfirm = findViewById(R.id.togglePasswordConfirm);
        ImageView toggleOldPassword = findViewById(R.id.toggleOldPassword);

        togglePassword.setOnClickListener(new View.OnClickListener() {
            boolean isPasswordVisible = false;

            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    togglePassword.setImageResource(R.drawable.pass); // closed eye icon
                } else {
                    password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    togglePassword.setImageResource(R.drawable.pass); // open eye icon
                }
                isPasswordVisible = !isPasswordVisible;
                password.setSelection(password.length()); // Keep cursor at the end
            }
        });

        togglePasswordConfirm.setOnClickListener(new View.OnClickListener() {
            boolean isPasswordVisible = false;

            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    passwordConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    togglePasswordConfirm.setImageResource(R.drawable.pass); // closed eye icon
                } else {
                    passwordConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    togglePasswordConfirm.setImageResource(R.drawable.pass); // open eye icon
                }
                isPasswordVisible = !isPasswordVisible;
                passwordConfirm.setSelection(passwordConfirm.length()); // Keep cursor at the end
            }
        });

        toggleOldPassword.setOnClickListener(new View.OnClickListener() {
            boolean isPasswordVisible = false;

            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    oldPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    toggleOldPassword.setImageResource(R.drawable.pass); // closed eye icon
                } else {
                    oldPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    toggleOldPassword.setImageResource(R.drawable.pass); // open eye icon
                }
                isPasswordVisible = !isPasswordVisible;
                oldPassword.setSelection( oldPassword.length()); // Keep cursor at the end
            }
        });

        changePass.setOnClickListener(v -> {
            ChangePassword();
        });
    }

    private void ChangePassword(){
        String accessToken = Token.getAccessToken(this);
        String authBearer = "Bearer " + accessToken;

        String old_password = oldPassword.getText().toString();
        String new_password = password.getText().toString();
        String confirm_password = passwordConfirm.getText().toString();

        if (!accessToken.isEmpty() && !old_password.isEmpty() && !new_password.isEmpty() && !confirm_password.isEmpty()) {
            ResetPasswordModel resetPasswordModel = new ResetPasswordModel(confirm_password, new_password, old_password);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.resetPassword(authBearer, resetPasswordModel);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()){
                        passwordChanged.setVisibility(View.VISIBLE);
                    } else {
                        passwordUnchanged.setVisibility(View.VISIBLE);
                        Log.d("PASSWORD NOT CHANGED", response.message());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {

                }
            });

        } else {
            Intent intent = new Intent(ResetPassword.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}








