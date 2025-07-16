package com.techmania.tumago_driver.Interface;

import com.techmania.tumago_driver.helpers.PaginatedDeliveryResponse;
import com.techmania.tumago_driver.models.CreateDriver;
import com.techmania.tumago_driver.models.CreateVehicle;
import com.techmania.tumago_driver.models.EndTrip;
import com.techmania.tumago_driver.models.FinanceInfo;
import com.techmania.tumago_driver.models.FcmTokenRequest;
import com.techmania.tumago_driver.models.LogOutRequest;
import com.techmania.tumago_driver.models.LoginDriver;
import com.techmania.tumago_driver.models.ResetPasswordModel;
import com.techmania.tumago_driver.models.TokenResponse;
import com.techmania.tumago_driver.models.Driver;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @POST("driver/signup/")
    Call<TokenResponse> createDriver(@Body CreateDriver request);

    @POST("login/")
    Call<TokenResponse> login(@Body LoginDriver request);

    @POST("logout/")
    Call<ResponseBody> logout(@Header("Authorization") String authHeader,@Body LogOutRequest request);

    @POST("reset_password/")
    Call<ResponseBody> resetPassword(@Header("Authorization") String authHeader, @Body ResetPasswordModel request);

    @POST("driver/data/")
    Call<Driver> getDriverData(@Header("Authorization") String authHeader);

    @DELETE("delete/account/")
    Call<ResponseBody> deleteAccount(@Header("Authorization") String authHeader);

    @POST("accept/terms/")
    Call<ResponseBody> acceptTerms(@Header("Authorization") String authHeader);

    @POST("save-fcm-token/")
    Call<Void> sendFcmToken(
            @Body FcmTokenRequest tokenRequest,
            @Header("Authorization") String authToken
    );

    @POST("driver/offline/")
    Call<Void> sendOffline();

    @POST("verify_token/")
    Call<ResponseBody> verifyToken(@Header("Authorization") String authHeader);

    @POST("verifyTerms/")
    Call<ResponseBody> checkTerms(@Header("Authorization") String authHeader);

    @GET("driver/delivery_info/")
    Call<FinanceInfo> getFinances(@Header("Authorization") String authHeader);

    @POST("add/vehicle/")
    Call<ResponseBody> AddVehicle(@Header("Authorization") String authHeader, @Body CreateVehicle request);

    @POST("accept/trip/")
    Call<ResponseBody> acceptTrip(@Header("Authorization") String authHeader, @Body Map<String, String> body);

    @POST("end_trip/")
    Call<ResponseBody> endTrip(@Header("Authorization") String authHeader, @Body EndTrip request);

    @Multipart
    @PUT("add/license/")
    Call<Void> uploadLicense(
            @Header("Authorization") String authHeader,
            @Part MultipartBody.Part license
    );

    @GET("get/deliveries/")
    Call<PaginatedDeliveryResponse> getDeliveries(
            @Query("cursor") String cursor,
            @Header("Authorization") String authToken
    );
}
