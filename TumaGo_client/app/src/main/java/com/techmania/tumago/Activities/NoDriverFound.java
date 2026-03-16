package com.techmania.tumago.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.NetworkUtils;

public class NoDriverFound extends AppCompatActivity {

    Button goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_driver_found);

        goBack = findViewById(R.id.goHome);

        goBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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

    @Override
    protected void onStop() {
        super.onStop();
        NetworkUtils.unregisterNetworkCallback(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}