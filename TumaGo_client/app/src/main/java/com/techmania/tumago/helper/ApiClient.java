package com.techmania.tumago.helper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Update this to your production base URL before release.
    // Development:  http://10.0.2.2:8000/   (emulator → localhost)
    // Local device: http://192.168.x.x:8000/
    // Production:   https://api.tumago.com/
    private static final String BASE_URL = "http://13.246.35.254/";

    private static final int MAX_RETRIES    = 3;
    private static final int CONNECT_TIMEOUT_S = 15;
    private static final int READ_TIMEOUT_S    = 30;
    private static final int WRITE_TIMEOUT_S   = 30;

    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(buildOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Invalidate the cached Retrofit instance (e.g. after a base URL change).
     */
    public static void reset() {
        retrofit = null;
    }

    private static OkHttpClient buildOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // Use BODY in debug builds only; use NONE in release to protect user data.
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_S, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(new RetryInterceptor(MAX_RETRIES))
                .build();
    }

    /**
     * OkHttp interceptor that retries failed requests with exponential backoff.
     * Retries on:  IO failures, 429 Too Many Requests, 503 Service Unavailable.
     * Does NOT retry:  4xx client errors (except 429), successful responses.
     */
    private static class RetryInterceptor implements Interceptor {

        private final int maxRetries;

        RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;

            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                if (attempt > 0) {
                    // Exponential backoff: 1s, 2s, 4s
                    long backoffMs = (1L << (attempt - 1)) * 1_000L;
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // Close previous response body before retrying
                    if (response != null) {
                        response.close();
                    }
                }

                try {
                    response = chain.proceed(request);
                } catch (IOException e) {
                    lastException = e;
                    continue;  // Network error — retry
                }

                // Retry on transient server errors
                if (response.code() == 429 || response.code() == 503) {
                    continue;
                }

                return response;  // Success or non-retryable error
            }

            if (lastException != null) {
                throw lastException;
            }
            return response;
        }
    }
}
