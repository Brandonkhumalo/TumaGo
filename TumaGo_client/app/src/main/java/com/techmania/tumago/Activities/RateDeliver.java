package com.techmania.tumago.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.RateTrip;
import com.techmania.tumago.R;
import com.techmania.tumago.auth.Login;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.Token;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RateDeliver extends AppCompatActivity {

    private ImageView[] stars = new ImageView[5];
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_deliver);

        stars[0] = findViewById(R.id.star1);
        stars[1] = findViewById(R.id.star2);
        stars[2] = findViewById(R.id.star3);
        stars[3] = findViewById(R.id.star4);
        stars[4] = findViewById(R.id.star5);

        for (int i = 0; i < stars.length; i++) {
            final int index = i;
            stars[i].setOnClickListener(v -> {
                selectedRating = index + 1;
                updateStarColors();
            });
        }

        Button rateButton = findViewById(R.id.rateButton);
        rateButton.setOnClickListener(v -> {
            RateDelivery();
        });
    }

    private void updateStarColors() {
        for (int i = 0; i < stars.length; i++) {
            if (i < selectedRating) {
                stars[i].setImageResource(R.drawable.ic_star_filled); // blue star
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline); // empty star
            }
        }
    }

    private void RateDelivery(){
        String accessToken = Token.getAccessToken(this);

        if (accessToken != null && !accessToken.isEmpty()){
            String authHeader = "Bearer " + accessToken;
            String delivery_id = Token.getDelivery_id(this);

            RateTrip rate_trip = new RateTrip(delivery_id, selectedRating);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.rateTrip(authHeader, rate_trip);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()){
                        Intent i = new Intent(RateDeliver.this, MainActivity.class);
                        startActivity(i);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("FAILED", t.getMessage());
                }
            });
        } else {
            Intent i = new Intent(RateDeliver.this, Login.class);
            startActivity(i);
            finish();
        }
    }
}