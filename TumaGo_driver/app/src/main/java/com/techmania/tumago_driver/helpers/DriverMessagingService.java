package com.techmania.tumago_driver.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class DriverMessagingService extends FirebaseMessagingService {
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            handleDataMessage(remoteMessage.getData());
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        String type = data.get("type");

        if (type == null) {
            Log.w("FCM", "No type specified in message");
            return;
        }

        switch (type) {
            case "new_request":
                ClientFoundHelper.handle(getApplicationContext(), data);
                break;

            case "delivery_cancelled":
                TripCancelledHelper.handle(getApplicationContext(), data);
                break;

            default:
                Log.w("FCM", "Unknown message type: " + type);
                break;
        }
    }
}
