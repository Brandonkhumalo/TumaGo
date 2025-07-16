package com.techmania.tumago.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.NetworkUtils;

public class NoDriverFound extends AppCompatActivity {

    MaterialCardView goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_driver_found);

        goBack = findViewById(R.id.goback);

        goBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
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
}