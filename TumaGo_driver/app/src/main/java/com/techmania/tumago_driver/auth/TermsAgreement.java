package com.techmania.tumago_driver.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.activities.MainActivity;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.Token;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TermsAgreement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_agreement);

        Button agreeTerms = findViewById(R.id.accept_terms);

        agreeTerms.setOnClickListener(v -> {
            AcceptTerms();
        });
    }

    private void AcceptTerms(){
        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        if (accessToken != null && !accessToken.isEmpty()){
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call <ResponseBody> call = apiService.acceptTerms(authHeader);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()){
                        Intent i = new Intent(TermsAgreement.this, MainActivity.class);
                        startActivity(i);
                        finish();
                    } else {
                        Log.d("Terms", response.message());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {

                }
            });
        } else {
            Intent i = new Intent(TermsAgreement.this, Login.class);
            startActivity(i);
            finish();
        }
    }
}








