package com.techmania.tumago.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.widget.Button;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Interface.UserCallback;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.GetUserData;
import com.techmania.tumago.helper.Token;
import com.techmania.tumago.helper.UiHelper;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfile extends AppCompatActivity {
    TextView mainUsername, mainRating, mainName, mainPhonenumber, mainEmail, mainAddress;
    Button delete;
    ProgressBar progressBar;
    ScrollView contentScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        mainUsername = findViewById(R.id.mainUsername);
        mainRating = findViewById(R.id.mainRating);
        mainName = findViewById(R.id.mainName);
        mainPhonenumber = findViewById(R.id.mainPhonenumber);
        mainEmail = findViewById(R.id.mainEmail);
        mainAddress = findViewById(R.id.mainAddress);
        delete = findViewById(R.id.delete);
        progressBar = findViewById(R.id.progressBar);
        contentScroll = findViewById(R.id.contentScroll);
        contentScroll.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        getUserData();

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(UserProfile.this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });

        LinearLayout resetPassword = findViewById(R.id.changePassword);
        resetPassword.setOnClickListener(v -> {
            Intent i = new Intent(UserProfile.this, ResetPassword.class);
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    public void getUserData(){
        String accessToken = Token.getAccessToken(this);

        GetUserData.GetData(this, accessToken, new UserCallback() {
            @Override
            public void onUserDataReceived(String name, String surname, String phoneNumber, String email,
                                           double rating, String street, String addressLine, String province,
                                           String city, String postalCode, String role) {
                progressBar.setVisibility(View.GONE);
                contentScroll.setVisibility(View.VISIBLE);
                mainUsername.setText(name + " " + surname);
                mainRating.setText(String.valueOf(rating));
                mainName.setText(name + " " + surname);
                mainPhonenumber.setText(phoneNumber);
                mainEmail.setText(email);
                mainAddress.setText(street + ", "+ city);
            }

            @Override
            public void onFailure(Throwable t) {
                progressBar.setVisibility(View.GONE);
                contentScroll.setVisibility(View.VISIBLE);
                Log.e("API", "Error: " + t.getMessage());
                UiHelper.showRetry(findViewById(android.R.id.content), "Failed to load profile", () -> getUserData());
            }
        });
    }

    public void deleteAccount(){
        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.deleteAccount(authHeader);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()){
                    Token.clearToken(UserProfile.this);
                    Intent intent = new Intent(UserProfile.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                UiHelper.showRetry(findViewById(android.R.id.content), "Failed to delete account", () -> deleteAccount());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}