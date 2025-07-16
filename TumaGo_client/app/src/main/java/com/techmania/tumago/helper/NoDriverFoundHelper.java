package com.techmania.tumago.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.techmania.tumago.Activities.NoDriverFound;
import com.techmania.tumago.R;

import java.util.Map;

public class NoDriverFoundHelper {

    public static void handle(Context context, Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");

        if (title == null) title = "No Driver Found";
        if (body == null) body = "We couldn’t find a driver at the moment.";

        showDeliveryDetailsActivity(context, title, body);
        showNotification(context, title, body);
    }

    private static void showDeliveryDetailsActivity(Context context, String title, String body) {
        Intent intent = new Intent(context, NoDriverFound.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        context.startActivity(intent);
    }

    private static void showNotification(Context context, String title, String message) {
        String channelId = "TUMAGO_CHANNEL";
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8+ channel setup
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

        Intent intent = new Intent(context, NoDriverFound.class);
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

        notificationManager.notify(1001, builder.build());
    }
}
