package com.techmania.tumago.Activities;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.Deliveries;
import com.techmania.tumago.R;
import com.techmania.tumago.adapter.DeliveryAdapter;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.PaginatedDeliveryResponse;
import com.techmania.tumago.helper.Token;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetParcels extends AppCompatActivity {

    private String nextCursor = null;
    private String prevCursor = null;
    private int currentPage = 1;
    TextView pageNumberTextView;
    ImageView nextPagebtn, prevPagebtn;

    RecyclerView recyclerView;
    DeliveryAdapter adapter;
    ArrayList<Deliveries> arrayList;
    LinearLayout noParcels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_parcels);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (currentPage < 1) {
            currentPage = 1;
        }

        noParcels = findViewById(R.id.noParcel);

        recyclerView = findViewById(R.id.DeliveryRecycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        arrayList = new ArrayList<>();
        adapter = new DeliveryAdapter(arrayList, this);
        recyclerView.setAdapter(adapter);

        getDeliveries(nextCursor);

        pageNumberTextView = findViewById(R.id.pageNumberTextView);
        pageNumberTextView.setText(String.valueOf(currentPage));

        nextPagebtn = findViewById(R.id.nextPage);
        nextPagebtn.setOnClickListener(v -> {
            if (nextCursor != null) {
                currentPage++;
                getDeliveries(nextCursor);
                pageNumberTextView.setText(String.valueOf(currentPage));
            }
        });

        prevPagebtn = findViewById(R.id.prevPage);
        prevPagebtn.setOnClickListener(v -> {
            if (prevCursor != null) {
                if (prevCursor != null && currentPage > 1) {
                    currentPage--;
                    getDeliveries(prevCursor);
                    pageNumberTextView.setText(String.valueOf(currentPage));
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check once
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }

        // Start monitoring changes
        NetworkUtils.registerNetworkCallback(this);
    }

    private void getDeliveries(String cursor){
        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<PaginatedDeliveryResponse> call = apiService.getDeliveries(cursor, authHeader);

        call.enqueue(new Callback<PaginatedDeliveryResponse>() {
            @Override
            public void onResponse(Call<PaginatedDeliveryResponse> call, Response<PaginatedDeliveryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Deliveries> deliveries = response.body().getResults();

                    if (deliveries != null) {
                            arrayList.clear();

                        if (!deliveries.isEmpty()) {
                            arrayList.addAll(deliveries);
                            adapter.notifyDataSetChanged();

                            recyclerView.setVisibility(View.VISIBLE);
                            noParcels.setVisibility(View.GONE);
                        } else {
                            Log.d("API", "No more deliveries to display");
                            recyclerView.setVisibility(View.GONE);
                            noParcels.setVisibility(View.VISIBLE);
                        }

                        // Cursor handling
                        nextCursor = extractCursorFromUrl(response.body().getNext());
                        prevCursor = extractCursorFromUrl(response.body().getPrevious());

                        nextPagebtn.setEnabled(nextCursor != null);
                        prevPagebtn.setEnabled(prevCursor != null && currentPage > 1);
                    } else {
                        Log.w("API", "Deliveries list is null");
                        // Show empty state UI if needed
                    }
                } else {
                    Log.e("API", "Response failed or body is null");
                    // Optionally show error message to the user
                }
            }

            @Override
            public void onFailure(Call<PaginatedDeliveryResponse> call, Throwable t) {
                Log.d("FAILED", t.getMessage());
            }
        });
    }

    private String extractCursorFromUrl(String url) {
        if (url == null) return null;
        try {
            Uri uri = Uri.parse(url);
            return uri.getQueryParameter("cursor");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}













