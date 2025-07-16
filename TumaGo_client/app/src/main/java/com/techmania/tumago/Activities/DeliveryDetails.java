package com.techmania.tumago.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.Token;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryDetails extends AppCompatActivity {

    TextView driverName, driverRating, totalDriverRatings, numberPlate, carDetails;
    String vehicle_color, vehicle, vehicle_model;
    MaterialCardView cancelDelivery, goHome;
    LinearLayout layout1, layout2;
    String delivery_id = Token.getDelivery_id(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_details);

        layout1 = findViewById(R.id.Layout1);
        layout2 = findViewById(R.id.Layout2);
        layout2.setVisibility(View.GONE);
        layout1.setVisibility(View.VISIBLE);
        goHome = findViewById(R.id.goHome);

        driverName = findViewById(R.id.name);
        driverRating = findViewById(R.id.rating);
        totalDriverRatings = findViewById(R.id.totalRatings);
        numberPlate = findViewById(R.id.numberPlate);
        carDetails = findViewById(R.id.carDetails);
        ImageView vehicleImage = findViewById(R.id.vehicleImage);
        cancelDelivery = findViewById(R.id.cancelDelivery);

        Intent intent = getIntent();
        vehicle_color = intent.getStringExtra("color");
        vehicle = intent.getStringExtra("vehicle");
        vehicle_model = intent.getStringExtra("vehicle_model");

        driverName.setText(intent.getStringExtra("driver"));
        driverRating.setText(intent.getStringExtra("rating"));
        totalDriverRatings.setText(intent.getStringExtra("total_ratings"));
        numberPlate.setText(intent.getStringExtra("number_plate"));
        carDetails.setText(vehicle_color + " ~ " + vehicle + " ~ " + vehicle_model);

        System.out.println(driverName + " " + driverRating + " " + totalDriverRatings + " " + numberPlate + " " + carDetails);

        switch (vehicle.toLowerCase()) {
            case "scooter":
                vehicleImage.setImageResource(R.drawable.scooter);
                break;
            case "van":
                vehicleImage.setImageResource(R.drawable.vanm);
                break;
            case "truck":
                vehicleImage.setImageResource(R.drawable.truckm);
                break;
            default:
                vehicleImage.setImageResource(R.drawable.scooter);
                break;
        }

        cancelDelivery.setOnClickListener(v -> {
            CancelDelivery();
        });

        goHome.setOnClickListener(v -> {
            Intent i = new Intent(DeliveryDetails.this, MainActivity.class);
            startActivity(i);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check once
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }

        // Start monitoring changes
        NetworkUtils.registerNetworkCallback(this);
    }

    private void CancelDelivery(){
        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.cancel_delivery(authHeader, delivery_id);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){
                    layout1.setVisibility(View.GONE);
                    layout2.setVisibility(View.VISIBLE);
                } else {
                    //Tell user it failed
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("FAILED TO CANCEL DELIVERY", t.getMessage());
            }
        });
    }
}







