package com.techmania.tumago.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.techmania.tumago.Activities.DeliveryDetails;
import com.techmania.tumago.R;

import java.util.Map;

public class DriverFoundHelper {

    public static void handle(Context context, Map<String, String> data) {
        String driver = data.get("driver_name");
        String vehicle = data.get("vehicle");
        String vehicle_name = data.get("vehicle_name");
        String number_plate = data.get("number_plate");
        String vehicle_model = data.get("vehicle_model");
        String color = data.get("color");
        String delivery_id = data.get("delivery_id");
        String total_ratings = data.get("total_ratings");
        String rating = data.get("rating");

        String title = data.get("title");
        String body = data.get("body");

        if (delivery_id != null) {
            storeTripId(context, delivery_id);
        }

        showDeliveryDetailsActivity(context, driver, vehicle, vehicle_name, number_plate,
                vehicle_model, color, total_ratings, rating);

        showNotification(context, title, body);
    }

    private static void showDeliveryDetailsActivity(Context context, String driver, String vehicle, String vehicle_name,
                                                    String number_plate, String vehicle_model, String color,
                                                    String total_ratings, String rating) {

        Intent intent = new Intent(context, DeliveryDetails.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("driver", driver);
        intent.putExtra("vehicle", vehicle);
        intent.putExtra("vehicle_name", vehicle_name);
        intent.putExtra("number_plate", number_plate);
        intent.putExtra("vehicle_model", vehicle_model);
        intent.putExtra("color", color);
        intent.putExtra("rating", rating);
        intent.putExtra("total_ratings", total_ratings);
        context.startActivity(intent);
    }

    private static void showNotification(Context context, String title, String message) {
        String channelId = "TUMAGO_CHANNEL";
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "TumaGo Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Delivery alerts and updates");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, DeliveryDetails.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("title", title);
        intent.putExtra("body", message);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.tuma_go_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 250, 250, 250});

        notificationManager.notify(1002, builder.build());
    }

    private static void storeTripId(Context context, String delivery_id) {
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
            editor.putString("delivery_id", delivery_id);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
