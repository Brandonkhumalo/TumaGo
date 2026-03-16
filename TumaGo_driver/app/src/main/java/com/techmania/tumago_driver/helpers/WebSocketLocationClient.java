package com.techmania.tumago_driver.helpers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;

/**
 * WebSocket client for streaming driver GPS location to the backend.
 *
 * Improvements for 50k-scale deployment:
 *   - Exponential backoff reconnection (prevents thundering herd after server restart)
 *   - Unlimited reconnect attempts (resets backoff after hitting cap)
 *   - Location queuing: if socket is not open, the last known location is cached
 *     and sent as soon as reconnection succeeds (no silent drops)
 */
public class WebSocketLocationClient extends WebSocketClient {

    private static final String TAG = "WebSocketLocation";

    // Backoff: starts at 1s, doubles each attempt, caps at 60s, then stays at 60s indefinitely
    private static final long BACKOFF_BASE_MS    = 1_000L;
    private static final long BACKOFF_MAX_MS     = 60_000L;

    private final URI serverUri;
    private final String authToken;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int  reconnectAttempts = 0;
    private boolean manualClose   = false;

    // Pending location to flush once reconnected
    private double pendingLat = Double.NaN;
    private double pendingLng = Double.NaN;

    public WebSocketLocationClient(URI serverUri, String authToken) {
        super(serverUri);
        this.serverUri = serverUri;
        this.authToken = authToken;
    }

    // ── WebSocketClient callbacks ───────────────────────────────────────────

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG, "Connected to server");
        reconnectAttempts = 0;

        // Send auth token as the first message (server expects {"token":"<jwt>"})
        try {
            JSONObject authJson = new JSONObject();
            authJson.put("token", authToken);
            send(authJson.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to send auth token", e);
            close();
            return;
        }

        // Flush any location that was buffered while disconnected
        if (!Double.isNaN(pendingLat) && !Double.isNaN(pendingLng)) {
            sendLocation(pendingLat, pendingLng);
            pendingLat = Double.NaN;
            pendingLng = Double.NaN;
        }
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, "Message from server: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Closed: code=" + code + " reason=" + reason + " remote=" + remote);
        if (!manualClose) {
            scheduleReconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "WebSocket error", ex);
        // onClose will be called after onError for connection errors; reconnect handled there
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Send GPS coordinates. If the socket is not currently open the location is
     * buffered and flushed on the next successful reconnect.
     */
    public void sendLocation(double latitude, double longitude) {
        if (!isOpen()) {
            // Buffer the latest location; only keep the most recent
            pendingLat = latitude;
            pendingLng = longitude;
            Log.d(TAG, "Socket not open — buffering location (" + latitude + ", " + longitude + ")");
            return;
        }
        try {
            JSONObject locationJson = new JSONObject();
            locationJson.put("latitude", latitude);
            locationJson.put("longitude", longitude);
            send(locationJson.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to send location", e);
        }
    }

    /**
     * Permanently close the connection (no reconnect will be attempted).
     */
    public void closeConnection() {
        manualClose = true;
        handler.removeCallbacksAndMessages(null);
        close();
    }

    // ── Reconnection ────────────────────────────────────────────────────────

    private void scheduleReconnect() {
        // Cap the exponent to avoid overflow — once at max, delay stays at BACKOFF_MAX_MS
        int exponent = Math.min(reconnectAttempts, 20);
        long delay = Math.min(BACKOFF_BASE_MS * (1L << exponent), BACKOFF_MAX_MS);
        reconnectAttempts++;

        Log.d(TAG, "Reconnect attempt " + reconnectAttempts + " in " + delay + "ms");

        handler.postDelayed(() -> {
            if (!manualClose) {
                try {
                    reconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Reconnect failed", e);
                    scheduleReconnect();
                }
            }
        }, delay);
    }
}
