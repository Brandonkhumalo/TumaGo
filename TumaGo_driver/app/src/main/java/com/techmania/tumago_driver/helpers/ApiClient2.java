package com.techmania.tumago_driver.helpers;

import com.techmania.tumago_driver.BuildConfig;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient2 {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String baseUrl) {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder()
                    .addInterceptor(logging);

            if (!BuildConfig.DEBUG) {
                CertificatePinner certPinner = new CertificatePinner.Builder()
                        .add("tumago.co.zw",
                                "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=")
                        .add("tumago.co.zw",
                                "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvPKG0=")
                        .build();
                httpBuilder.certificatePinner(certPinner);
            }

            OkHttpClient client = httpBuilder.build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
