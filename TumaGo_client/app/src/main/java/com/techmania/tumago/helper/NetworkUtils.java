package com.techmania.tumago.helper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class NetworkUtils {

    // Per-context callbacks to avoid overwriting when multiple activities register
    private static final Map<Context, ConnectivityManager.NetworkCallback> callbackMap = new HashMap<>();

    // Method to check internet availability
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        }
        return false;
    }

    public static void registerNetworkCallback(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Unregister existing callback for this context first to avoid leaks
            unregisterNetworkCallback(context);

            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    Toast.makeText(context, "Internet connected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    Toast.makeText(context, "Internet disconnected", Toast.LENGTH_SHORT).show();
                }
            };
            callbackMap.put(context, callback);
            connectivityManager.registerDefaultNetworkCallback(callback);
        }
    }

    public static void unregisterNetworkCallback(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConnectivityManager.NetworkCallback callback = callbackMap.remove(context);
            if (callback != null) {
                ConnectivityManager connectivityManager =
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                connectivityManager.unregisterNetworkCallback(callback);
            }
        }
    }
}
