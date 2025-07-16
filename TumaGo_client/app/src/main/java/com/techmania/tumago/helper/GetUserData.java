package com.techmania.tumago.helper;

import android.content.Context;

import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Interface.UserCallback;
import com.techmania.tumago.Model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetUserData {
    public static void GetData(Context context, String accessToken, UserCallback callback) {

        if (accessToken != null && !accessToken.isEmpty()) {
            String authHeader = "Bearer " + accessToken;

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<User> call = apiService.getUserData(authHeader);

            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();

                        callback.onUserDataReceived(
                                user.getName(),
                                user.getSurname(),
                                user.getPhoneNumber(),
                                user.getEmail(),
                                user.getRating(),
                                user.getStreet(),
                                user.getAddressLine(),
                                user.getProvince(),
                                user.getCity(),
                                user.getPostalCode(),
                                user.getRole()
                        );
                    } else {
                        callback.onFailure(new Exception(response.message()));
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    callback.onFailure(t);
                }
            });
        } else {
            callback.onFailure(new Exception("Access token is null or empty"));
        }
    }
}
