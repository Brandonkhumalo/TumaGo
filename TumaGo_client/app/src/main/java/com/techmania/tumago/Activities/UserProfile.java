package com.techmania.tumago.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Interface.UserCallback;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.GetUserData;
import com.techmania.tumago.helper.Token;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfile extends AppCompatActivity {
    TextView mainUsername, mainRating, mainName, mainPhonenumber, mainEmail, mainAddress;
    MaterialCardView delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        mainUsername = findViewById(R.id.mainUsername);
        mainRating = findViewById(R.id.mainRating);
        mainName = findViewById(R.id.mainName);
        mainPhonenumber = findViewById(R.id.mainPhonenumber);
        mainEmail = findViewById(R.id.mainEmail);
        mainAddress = findViewById(R.id.mainAddress);
        delete = findViewById(R.id.delete);

        getUserData();

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAccount();
            }
        });

        LinearLayout resetPassword = findViewById(R.id.resetPassword);
        resetPassword.setOnClickListener(v -> {
            Intent i = new Intent(UserProfile.this, ResetPassword.class);
            startActivity(i);
        });
    }

    public void getUserData(){
        String accessToken = Token.getAccessToken(this);

        GetUserData.GetData(this, accessToken, new UserCallback() {
            @Override
            public void onUserDataReceived(String name, String surname, String phoneNumber, String email,
                                           double rating, String street, String addressLine, String province,
                                           String city, String postalCode, String role) {
                // user the userData here
                mainUsername.setText(name + " " + surname);
                mainRating.setText(String.valueOf(rating));
                mainName.setText(name + " " + surname);
                mainPhonenumber.setText(phoneNumber);
                mainEmail.setText(email);
                mainAddress.setText(street + ", "+ city);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("API", "Error: " + t.getMessage());
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
                    Intent intent = new Intent(UserProfile.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // optional: clear back stack
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }
}