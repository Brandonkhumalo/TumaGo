package com.techmania.tumago.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.auth.Login;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogOutUser {
    public static void LogOut(Context context, String accessToken){
        String refreshToken = Token.getRefreshToken(context);

        if (refreshToken != null && !refreshToken.isEmpty()) {
            String authHeader = "Bearer " + accessToken;

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<ResponseBody> call = apiService.logout(authHeader, refreshToken);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()){
                        Token.clearToken(context);
                        Intent intent = new Intent(context, Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // optional: clear back stack
                        context.startActivity(intent);
                        if (context instanceof Activity) {
                            ((Activity) context).finish(); //close current activity
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("Failed", t.getMessage());
                }
            });
        } else {
            Log.d("TokenNULL", "refreshToken is null");
        }
    }
}
