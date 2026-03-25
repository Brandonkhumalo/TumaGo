package com.techmania.tumago.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;

import com.techmania.tumago.R;

import java.util.Map;

public class PackagePickedUpHelper {

    public static final String ACTION_PACKAGE_PICKED_UP = "com.techmania.tumago.PACKAGE_PICKED_UP";

    public static void handle(Context context, Map<String, String> data) {
        String deliveryId = data.get("delivery_id");

        if (deliveryId == null) {
            Log.e("PackagePickedUpHelper", "Missing delivery_id in FCM data");
            return;
        }

        // Broadcast to any active DeliveryDetails activity to hide cancel button
        Intent broadcastIntent = new Intent(ACTION_PACKAGE_PICKED_UP);
        broadcastIntent.putExtra("delivery_id", deliveryId);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

        showNotification(context, "Package Picked Up",
                "Your driver has picked up the package and is on the way!");
    }

    private static void showNotification(Context context, String title, String message) {
        String channelId = "TUMAGO_CHANNEL";
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "TumaGo Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.tuma_go_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1005, builder.build());
    }
}
