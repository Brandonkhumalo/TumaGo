package com.techmania.tumago_driver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago_driver.R;

public class DeliveryCancelled extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_cancelled);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String title = getIntent().getStringExtra("title");
        String body = getIntent().getStringExtra("body");

        TextView titleView = findViewById(R.id.trip_cancelled_title);
        TextView bodyView = findViewById(R.id.trip_cancelled_body);

        titleView.setText(title != null ? title : "Trip Cancelled");
        bodyView.setText(body != null ? body : "Your trip was cancelled.");

        MaterialCardView home_page = findViewById(R.id.homePage);
        home_page.setOnClickListener(v -> {
            Intent i = new Intent(DeliveryCancelled.this, MainActivity.class);
            startActivity(i);
            finish();
        });
    }
}