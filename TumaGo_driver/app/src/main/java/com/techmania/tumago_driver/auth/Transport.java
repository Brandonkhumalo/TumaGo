package com.techmania.tumago_driver.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.activities.MainActivity;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.models.CreateVehicle;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Transport extends AppCompatActivity {
    MaterialCardView scooter, van, truck;
    MaterialCardView[] position;
    String vehicleName;
    Button continueButton, finishReg;
    LinearLayout firstLayout, secondLayout;

    EditText vehicleBrand, modelName, colorName, numberPlateName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport);

        scooter = findViewById(R.id.scooter);
        van = findViewById(R.id.van);
        truck = findViewById(R.id.truck);
        continueButton = findViewById(R.id.Continue);
        finishReg = findViewById(R.id.finishReg);
        firstLayout = findViewById(R.id.chooseVehicle);
        secondLayout = findViewById(R.id.vehicleRegistration);
        vehicleBrand = findViewById(R.id.vehicle);
        modelName = findViewById(R.id.model);
        colorName = findViewById(R.id.color);
        numberPlateName = findViewById(R.id.numberPlate);

        position = new MaterialCardView[]{scooter, van, truck};

        for(MaterialCardView view : position){
            view.setOnClickListener(v -> {
                int pos = getPosition(view);

                for (MaterialCardView card : position) {
                    card.setStrokeColor(Color.GRAY);  // default border color
                    card.setStrokeWidth(1);
                }

                // Highlight the clicked card
                view.setStrokeColor(Color.GREEN);
                view.setStrokeWidth(4);

                switch(pos) {
                    case 0:
                        vehicleName = "Scooter";
                        break;
                    case 1:
                        vehicleName = "Van";
                        break;
                    case 2:
                        vehicleName = "Truck";
                        break;
                }
            });
        }

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firstLayout.setVisibility(View.GONE);
                secondLayout.setVisibility(View.VISIBLE);
            }
        });

        finishReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterVehicle();
            }
        });
    }

    public int getPosition(MaterialCardView card){
        for(int i = 0; i < position.length; i++){
            if(position[i] == card){
                return i;
            }
        }
        return -1;
    }

    public void RegisterVehicle(){
        String vehicle = vehicleBrand.getText().toString();
        String model = modelName.getText().toString();
        String color = colorName.getText().toString();
        String numberPlate = numberPlateName.getText().toString();

        if (!vehicleName.isEmpty() && !vehicle.isEmpty() && !model.isEmpty() && !color.isEmpty() && !numberPlate.isEmpty()) {

            CreateVehicle delivery_vehicle = new CreateVehicle(model, color, numberPlate, vehicle, vehicleName);

            String token = Token.getAccessToken(this);
            String authHeader = "Bearer " + token;

            if (token == null) {
                Intent intent = new Intent(Transport.this, Login.class);
                startActivity(intent);
                finish();
            } else {
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<ResponseBody> call = apiService.AddVehicle(authHeader ,delivery_vehicle);

                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Intent intent = new Intent(Transport.this, TermsAgreement.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(Transport.this, "Error", Toast.LENGTH_SHORT).show();
                            Log.d("ERROR", response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(Transport.this, "Error", Toast.LENGTH_SHORT).show();
                        Log.d("ERROR", t.getMessage());
                    }
                });
            }
        } else {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
        }
    }
}











