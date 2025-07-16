package com.techmania.tumago.Interface;

import com.techmania.tumago.Model.CreateUser;
import com.techmania.tumago.Model.DeliveryRequest;
import com.techmania.tumago.Model.Expense;
import com.techmania.tumago.Model.FcmTokenRequest;
import com.techmania.tumago.Model.LoginUser;
import com.techmania.tumago.Model.RateTrip;
import com.techmania.tumago.Model.ResetPasswordModel;
import com.techmania.tumago.Model.TokenResponse;
import com.techmania.tumago.Model.User;
import com.techmania.tumago.Model.UserProfileData;
import com.techmania.tumago.helper.PaginatedDeliveryResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("signup/")
    Call<TokenResponse> signup(@Body CreateUser request);

    @POST("login/")
    Call<TokenResponse> login(@Body LoginUser request);

    @POST("accept/terms/")
    Call<ResponseBody> acceptTerms(@Header("Authorization") String authHeader);

    @POST("verifyTerms/")
    Call<ResponseBody> checkTerms(@Header("Authorization") String authHeader);

    @POST("logout/")
    Call<ResponseBody> logout(@Header("Authorization") String authHeader,@Query("refreshToken") String refreshToken);

    @POST("reset_password/")
    Call<ResponseBody> resetPassword(@Header("Authorization") String authHeader, @Body ResetPasswordModel request);

    @DELETE("delete/account/")
    Call<ResponseBody> deleteAccount(@Header("Authorization") String authHeader);

    @POST("update/user/profile/")
    Call<ResponseBody> updateProfile(@Header("Authorization") String authHeader, @Body UserProfileData request);

    @POST("user/Data/")
    Call<User> getUserData(@Header("Authorization") String authHeader);

    @GET("trip_Expense/")
    Call<Expense> getExpense(@Query("distance") double distance);

    @POST("verify_token/")
    Call<ResponseBody> verifyToken(@Header("Authorization") String authHeader);

    @POST("delivery/request/")
    Call<ResponseBody> requestDelivery(@Header("Authorization") String authHeader, @Body DeliveryRequest request);

    @POST("save-fcm-token/")
    Call<Void> sendFcmToken(
            @Body FcmTokenRequest tokenRequest,
            @Header("Authorization") String authToken
    );

    @GET("get/deliveries/")
    Call<PaginatedDeliveryResponse> getDeliveries(
            @Query("cursor") String cursor,
            @Header("Authorization") String authToken
    );

    @POST("cancel/delivery/")
    Call<ResponseBody> cancel_delivery(
            @Header("Authorization") String authHeader,
            @Query("delivery_id") String delivery_id
    );

    @POST("rate/driver/")
    Call<ResponseBody> rateTrip(
            @Header("Authorization") String authHeader,
            @Body RateTrip request
    );
}

