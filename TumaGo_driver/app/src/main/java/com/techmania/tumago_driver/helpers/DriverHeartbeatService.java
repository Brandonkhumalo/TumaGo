package com.techmania.tumago_driver.helpers;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.techmania.tumago_driver.BuildConfig;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.activities.MainActivity;

import java.net.URI;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Foreground service that keeps GPS location streaming alive when the app is
 * backgrounded or the screen is off. Uses FusedLocationProvider for battery-
 * efficient updates and streams coordinates over WebSocket.
 */
public class DriverHeartbeatService extends Service {

    private static final String TAG = "DriverHeartbeatService";
    private static final String CHANNEL_ID = "driver_location_channel";
    private static final int NOTIFICATION_ID = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private WebSocketLocationClient webSocketClient;
    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        apiService = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service with the persistent notification
        Notification notification = buildNotification();

        // ServiceCompat handles the API-level differences for foregroundServiceType
        ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        );

        connectWebSocket();
        startLocationUpdates();

        // If the system kills this service, restart it automatically
        return START_STICKY;
    }

    private void connectWebSocket() {
        try {
            String token = Token.getAccessToken(this);
            if (token == null || token.isEmpty()) {
                Log.w(TAG, "No access token — cannot connect WebSocket");
                return;
            }
            // Build WS URL from BASE_URL (strip http/https, use ws/wss)
            String baseUrl = BuildConfig.BASE_URL;
            String wsScheme = baseUrl.startsWith("https") ? "wss" : "ws";
            String host = baseUrl.replaceFirst("https?://", "").replaceFirst("/$", "");
            String wsUrl = wsScheme + "://" + host + "/ws/driver_location/";
            URI uri = new URI(wsUrl);
            webSocketClient = new WebSocketLocationClient(uri, token);
            webSocketClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "WebSocket connection failed", e);
        }
    }

    private void startLocationUpdates() {
        // Check permission before requesting updates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing ACCESS_FINE_LOCATION — cannot start updates");
            stopSelf();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(60_000)        // 1 minute between updates
                .setFastestInterval(30_000)  // 30s fastest
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || webSocketClient == null) return;

                double lat = locationResult.getLastLocation().getLatitude();
                double lon = locationResult.getLastLocation().getLongitude();
                webSocketClient.sendLocation(lat, lon);
                Log.d(TAG, "Location sent: " + lat + ", " + lon);
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    /**
     * Called when the user swipes the app away from recents.
     * Notifies the backend that the driver is offline.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        sendOffline();
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (webSocketClient != null) {
            webSocketClient.closeConnection();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void sendOffline() {
        Call<Void> call = apiService.sendOffline();
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, "Notified offline: " + response.code());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed offline notify: " + t.getMessage());
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW  // no sound, sits quietly
            );
            channel.setDescription("Shows while TumaGo is tracking your location for deliveries");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        // Tapping the notification opens MainActivity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TumaGo Driver")
                .setContentText("Location tracking active")
                .setSmallIcon(R.drawable.tuma_go_logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
