package com.techmania.tumago_driver.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverHeartbeatService extends Service {
    private Handler handler = new Handler();
    private Runnable runnable;
    private static final long INTERVAL = 30 * 1000; // 30 seconds

    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    private void sendOffline() {
        Call<Void> call = apiService.sendOffline();
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("Offline", "Notified offline: " + response.code());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("Offline", "Failed offline notify: " + t.getMessage());
            }
        });
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Called when user swipes app from recent tasks
        sendOffline();
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        String channelId = "driver_status_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Driver Status",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Driver Online")
                .setContentText("Sending heartbeat to server")
                .setSmallIcon(R.drawable.tuma_go_logo)
                .build();
    }
}