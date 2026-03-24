package com.techmania.tumago.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.Token;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchingForDriver extends AppCompatActivity {

    private static final String TAG = "SearchingForDriver";
    private String tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching_for_driver);

        tripId = getIntent().getStringExtra("trip_id");

        // Set the vehicle image based on what was selected
        String vehicleImageName = getIntent().getStringExtra("transportImage");
        if (vehicleImageName != null) {
            ImageView vehicleImage = findViewById(R.id.searchVehicleImage);
            int resId = getResources().getIdentifier(vehicleImageName, "drawable", getPackageName());
            if (resId != 0) {
                vehicleImage.setImageResource(resId);
            }
        }

        Button cancelSearch = findViewById(R.id.cancelSearch);
        cancelSearch.setOnClickListener(v -> {
            cancelTripRequest();
        });
    }

    private void cancelTripRequest() {
        if (tripId != null) {
            String accessToken = Token.getAccessToken(this);
            if (accessToken != null && !accessToken.isEmpty()) {
                String authHeader = "Bearer " + accessToken;
                Map<String, String> body = new HashMap<>();
                body.put("trip_id", tripId);

                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<ResponseBody> call = apiService.cancelTripRequest(authHeader, body);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Cancel trip request failed: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "Cancel trip request error: " + t.getMessage());
                    }
                });
            }
        }

        // Navigate home regardless of cancel result
        Intent intent = new Intent(SearchingForDriver.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_down, R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }
        NetworkUtils.registerNetworkCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        NetworkUtils.unregisterNetworkCallback(this);
    }
}
