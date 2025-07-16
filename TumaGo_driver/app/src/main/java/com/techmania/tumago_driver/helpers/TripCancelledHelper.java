package com.techmania.tumago_driver.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.activities.DeliveryCancelled;

import java.util.Map;

public class TripCancelledHelper {

    public static void handle(Context context, Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");

        if (title == null) title = "Trip Cancelled";
        if (body == null) body = "The client has cancelled the delivery request.";

        showTripCancelledScreen(context, title, body);
        showNotification(context, title, body);
    }

    private static void showTripCancelledScreen(Context context, String title, String message) {
        Intent intent = new Intent(context, DeliveryCancelled.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("title", title);
        intent.putExtra("body", message);
        context.startActivity(intent);
    }

    private static void showNotification(Context context, String title, String message) {
        String channelId = "cancelled_trip_channel";
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Trip Cancelled Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts for cancelled delivery trips");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, DeliveryCancelled.class);
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
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 250, 250, 250});

        notificationManager.notify(1004, builder.build());
    }
}
