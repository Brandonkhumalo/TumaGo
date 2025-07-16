package com.techmania.tumago_driver.helpers;

import android.util.Log;

import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.models.FcmTokenRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SendFCMtoken {
    public void sendFcmTokenToBackend(String fcmToken, String jwtToken) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.8.147:8000/") // Make sure to end with /
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        FcmTokenRequest request = new FcmTokenRequest(fcmToken);

        Call<Void> call = apiService.sendFcmToken(request, "Bearer " + jwtToken);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("FCM", "Token sent successfully");
                } else {
                    Log.e("FCM", "Failed to send token: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("FCM", "Error sending token", t);
            }
        });
    }

}
