package com.techmania.tumago_driver.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.activities.DeliveryDetails;

import java.util.Map;

public class ClientFoundHelper {

    public static void handle(Context context, Map<String, String> data) {
        String requesterName = data.get("requester_name");
        String destinationLat = data.get("destination_lat");
        String destinationLng = data.get("destination_lng");
        String requesterLat = data.get("requester_lat");
        String requesterLng = data.get("requester_lng");
        String distance = data.get("distance_meters");
        String cost = data.get("cost");
        String tripId = data.get("trip_id");

        if (tripId != null) {
            storeTripId(context, tripId);
        }

        showDeliveryDetailsActivity(context, requesterName, destinationLat, destinationLng, distance, cost, requesterLat, requesterLng);
        showNotification(context, "New Delivery Request", requesterName + " needs a ride nearby!");
    }

    private static void showDeliveryDetailsActivity(Context context, String name, String lat, String lng, String distance, String cost, String userLat, String userLng) {
        Intent intent = new Intent(context, DeliveryDetails.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("requester_name", name);
        intent.putExtra("destination_lat", lat);
        intent.putExtra("destination_lng", lng);
        intent.putExtra("userLatitude", userLat);
        intent.putExtra("userLongi", userLng);
        intent.putExtra("distance_meters", distance);
        intent.putExtra("cost", cost);
        context.startActivity(intent);
    }

    private static void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "default_channel_id";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Delivery Requests", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1003, builder.build()); // Use a different ID to prevent overwrite
    }

    private static void storeTripId(Context context, String tripId) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor editor = securePrefs.edit();
            editor.putString("trip_id", tripId);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
