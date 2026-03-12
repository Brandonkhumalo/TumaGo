package com.techmania.tumago.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.ResetPasswordModel;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.Token;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPassword extends AppCompatActivity {

    EditText oldPassword, newPassword, confirmPassword;
    Button resetPasswordBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        oldPassword = findViewById(R.id.oldPassword);
        newPassword = findViewById(R.id.passwordChanged);
        confirmPassword = findViewById(R.id.passwordUnchanged);
        resetPasswordBtn = findViewById(R.id.resetPassword);

        ImageView toggleOldPassword = findViewById(R.id.toggleOldPassword);

        toggleOldPassword.setOnClickListener(new View.OnClickListener() {
            boolean isPasswordVisible = false;

            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    oldPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    toggleOldPassword.setImageResource(R.drawable.pass);
                } else {
                    oldPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    toggleOldPassword.setImageResource(R.drawable.pass);
                }
                isPasswordVisible = !isPasswordVisible;
                oldPassword.setSelection(oldPassword.length());
            }
        });

        resetPasswordBtn.setOnClickListener(v -> {
            ChangePassword();
        });
    }

    private void ChangePassword(){
        String accessToken = Token.getAccessToken(this);
        String authBearer = "Bearer " + accessToken;

        String old_password = oldPassword.getText().toString();
        String new_password = newPassword.getText().toString();
        String confirm_password = confirmPassword.getText().toString();

        if (!accessToken.isEmpty() && !old_password.isEmpty() && !new_password.isEmpty() && !confirm_password.isEmpty()) {
            ResetPasswordModel resetPasswordModel = new ResetPasswordModel(confirm_password, new_password, old_password);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.resetPassword(authBearer, resetPasswordModel);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()){
                        Toast.makeText(ResetPassword.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ResetPassword.this, "Failed to change password", Toast.LENGTH_SHORT).show();
                        Log.d("PASSWORD NOT CHANGED", response.message());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(ResetPassword.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        }
    }
}