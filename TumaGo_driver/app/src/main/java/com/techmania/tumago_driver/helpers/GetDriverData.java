package com.techmania.tumago_driver.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.Interface.DriverCallback;
import com.techmania.tumago_driver.auth.Transport;
import com.techmania.tumago_driver.models.Driver;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetDriverData {
    public static void GetData(Context context, String accessToken, DriverCallback callback) {

        if (accessToken != null && !accessToken.isEmpty()) {
            String authHeader = "Bearer " + accessToken;

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<Driver> call = apiService.getDriverData(authHeader);

            call.enqueue(new Callback<Driver>() {
                @Override
                public void onResponse(Call<Driver> call, Response<Driver> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Driver driver = response.body();

                        callback.onDriverDataReceived(
                                driver.getName(),
                                driver.getSurname(),
                                driver.getPhoneNumber(),
                                driver.getEmail(),
                                driver.getRating(),
                                driver.getStreet(),
                                driver.getAddressLine(),
                                driver.getProvince(),
                                driver.getCity(),
                                driver.getPostalCode(),
                                driver.getRole(),
                                driver.getVerified(),
                                driver.getLicense()
                        );
                    } else {
                        callback.onFailure(new Exception(response.message()));
                        Intent intent = new Intent(context, Transport.class);
                        context.startActivity(intent);

                        if (context instanceof Activity) {
                            ((Activity) context).finish();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Driver> call, Throwable t) {
                    callback.onFailure(t);
                }
            });
        } else {
            callback.onFailure(new Exception("Access token is null or empty"));
        }
    }
}
