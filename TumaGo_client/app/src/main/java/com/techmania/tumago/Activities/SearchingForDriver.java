package com.techmania.tumago.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.techmania.tumago.R;
import com.techmania.tumago.helper.NetworkUtils;

public class SearchingForDriver extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching_for_driver);

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
            Intent intent = new Intent(SearchingForDriver.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_down, R.anim.fade_out);
            finish();
        });
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
