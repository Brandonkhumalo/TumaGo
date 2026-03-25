package com.techmania.tumago.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.techmania.tumago.Activities.RateDeliver;
import com.techmania.tumago.R;

import java.util.Map;

public class DeliveryCompleteHelper {

    public static void handle(Context context, Map<String, String> data) {
        String deliveryId = data.get("delivery_id");
        String fare = data.get("fare");
        String driverName = data.get("driver_name");

        if (deliveryId == null) {
            Log.e("DeliveryCompleteHelper", "Missing delivery_id in FCM data");
            return;
        }

        // Launch rating activity
        Intent intent = new Intent(context, RateDeliver.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("delivery_id", deliveryId);
        intent.putExtra("fare", fare);
        intent.putExtra("driver_name", driverName);
        context.startActivity(intent);

        showNotification(context, "Delivery Complete",
                "Your package has been delivered! Rate your driver.");
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

        notificationManager.notify(1004, builder.build());
    }
}
