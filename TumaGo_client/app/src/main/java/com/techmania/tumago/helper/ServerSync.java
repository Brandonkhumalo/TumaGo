package com.techmania.tumago.helper;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerSync {
    public static void syncServerTime(Context context) {
        String url = "http://10.0.2.2:8000/sync/time/";

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String serverUtcTime = response.getString("utc_time");
                            Log.d("SyncTime", "Server UTC Time: " + serverUtcTime);

                            // Optionally store or convert this time as needed
                            // For example: parse and compare with device time
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("SyncTime", "Failed to parse UTC time");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("SyncTime", "Error syncing with server: " + error.getMessage());
                    }
                });

        queue.add(request);
    }
}
