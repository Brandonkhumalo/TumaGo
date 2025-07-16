package com.techmania.tumago_driver.helpers;

import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;

public class WebSocketLocationClient extends WebSocketClient {

    public WebSocketLocationClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d("WebSocket", "Connected to server");
    }

    @Override
    public void onMessage(String message) {
        Log.d("WebSocket", "Message from server: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("WebSocket", "Closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.e("WebSocket", "Error: ", ex);
    }

    public void sendLocation(double latitude, double longitude) {
        try {
            JSONObject locationJson = new JSONObject();
            locationJson.put("latitude", latitude);
            locationJson.put("longitude", longitude);
            this.send(locationJson.toString());
        } catch (Exception e) {
            Log.e("WebSocket", "Failed to send location", e);
        }
    }
}
