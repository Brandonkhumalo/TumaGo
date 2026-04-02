package com.techmania.tumago_driver.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.activities.MainActivity;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.Token;
import com.techmania.tumago_driver.models.TermsResponse;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TermsAgreement extends AppCompatActivity {

    private TextView termsText;
    private Button acceptButton;
    private ProgressBar loadingSpinner;
    private View contentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_agreement);

        termsText = findViewById(R.id.terms_text);
        acceptButton = findViewById(R.id.accept_terms);
        loadingSpinner = findViewById(R.id.loading_spinner);
        contentContainer = findViewById(R.id.content_container);

        // Disable accept button until content is loaded
        acceptButton.setEnabled(false);
        acceptButton.setAlpha(0.5f);

        acceptButton.setOnClickListener(v -> acceptTerms());

        fetchTermsContent();
    }

    private void fetchTermsContent() {
        loadingSpinner.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<TermsResponse> call = apiService.getTermsContent("driver");
        call.enqueue(new Callback<TermsResponse>() {
            @Override
            public void onResponse(Call<TermsResponse> call, Response<TermsResponse> response) {
                loadingSpinner.setVisibility(View.GONE);
                contentContainer.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null) {
                    termsText.setText(response.body().getContent());
                    acceptButton.setEnabled(true);
                    acceptButton.setAlpha(1f);
                } else {
                    termsText.setText("Unable to load terms and conditions. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<TermsResponse> call, Throwable t) {
                loadingSpinner.setVisibility(View.GONE);
                contentContainer.setVisibility(View.VISIBLE);
                termsText.setText("Unable to load terms and conditions. Please check your connection.");
                Log.e("Terms", "Failed to fetch T&C", t);
            }
        });
    }

    private void acceptTerms() {
        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        if (accessToken != null && !accessToken.isEmpty()) {
            acceptButton.setEnabled(false);
            acceptButton.setText("Accepting...");

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Map<String, String> body = new HashMap<>();
            body.put("app_type", "driver");
            Call<ResponseBody> call = apiService.acceptTerms(authHeader, body);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        getSharedPreferences("app_cache", MODE_PRIVATE)
                                .edit().putBoolean("terms_accepted", true).apply();
                        Intent i = new Intent(TermsAgreement.this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    } else {
                        acceptButton.setEnabled(true);
                        acceptButton.setText("I Accept the Terms");
                        Toast.makeText(TermsAgreement.this, "Failed to accept terms. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    acceptButton.setEnabled(true);
                    acceptButton.setText("I Accept the Terms");
                    Toast.makeText(TermsAgreement.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Intent i = new Intent(TermsAgreement.this, Login.class);
            startActivity(i);
            finish();
        }
    }
}
