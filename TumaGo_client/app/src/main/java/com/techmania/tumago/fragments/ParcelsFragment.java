package com.techmania.tumago.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.Deliveries;
import com.techmania.tumago.R;
import com.techmania.tumago.adapter.DeliveryAdapter;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.PaginatedDeliveryResponse;
import com.techmania.tumago.helper.Token;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParcelsFragment extends Fragment {

    private String nextCursor = null;
    private String prevCursor = null;
    private int currentPage = 1;

    private RecyclerView recyclerView;
    private DeliveryAdapter adapter;
    private ArrayList<Deliveries> arrayList;
    private LinearLayout noParcels;
    private TextView pageNumberTextView;
    private ImageView nextPageBtn, prevPageBtn;
    private boolean dataLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parcels, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        noParcels = view.findViewById(R.id.noParcel);
        recyclerView = view.findViewById(R.id.DeliveryRecycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        arrayList = new ArrayList<>();
        adapter = new DeliveryAdapter(arrayList, requireContext());
        recyclerView.setAdapter(adapter);

        pageNumberTextView = view.findViewById(R.id.pageNumberTextView);
        pageNumberTextView.setText(String.valueOf(currentPage));

        nextPageBtn = view.findViewById(R.id.nextPage);
        nextPageBtn.setOnClickListener(v -> {
            if (nextCursor != null) {
                currentPage++;
                getDeliveries(nextCursor);
                pageNumberTextView.setText(String.valueOf(currentPage));
            }
        });

        prevPageBtn = view.findViewById(R.id.prevPage);
        prevPageBtn.setOnClickListener(v -> {
            if (prevCursor != null && currentPage > 1) {
                currentPage--;
                getDeliveries(prevCursor);
                pageNumberTextView.setText(String.valueOf(currentPage));
            }
        });

        getDeliveries(null);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Refresh parcels when tab becomes visible again
        if (!hidden && dataLoaded) {
            currentPage = 1;
            nextCursor = null;
            prevCursor = null;
            getDeliveries(null);
            if (pageNumberTextView != null) {
                pageNumberTextView.setText(String.valueOf(currentPage));
            }
        }
    }

    private void getDeliveries(String cursor) {
        if (!isAdded()) return;
        String accessToken = Token.getAccessToken(requireContext());
        String authHeader = "Bearer " + accessToken;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<PaginatedDeliveryResponse> call = apiService.getDeliveries(cursor, authHeader);

        call.enqueue(new Callback<PaginatedDeliveryResponse>() {
            @Override
            public void onResponse(Call<PaginatedDeliveryResponse> call, Response<PaginatedDeliveryResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Deliveries> deliveries = response.body().getResults();
                    dataLoaded = true;

                    if (deliveries != null) {
                        arrayList.clear();

                        if (!deliveries.isEmpty()) {
                            arrayList.addAll(deliveries);
                            adapter.notifyDataSetChanged();
                            recyclerView.scheduleLayoutAnimation();
                            recyclerView.setVisibility(View.VISIBLE);
                            noParcels.setVisibility(View.GONE);
                        } else {
                            recyclerView.setVisibility(View.GONE);
                            noParcels.setVisibility(View.VISIBLE);
                        }

                        nextCursor = extractCursorFromUrl(response.body().getNext());
                        prevCursor = extractCursorFromUrl(response.body().getPrevious());

                        nextPageBtn.setEnabled(nextCursor != null);
                        prevPageBtn.setEnabled(prevCursor != null && currentPage > 1);
                    }
                } else {
                    Log.e("API", "Response failed or body is null");
                }
            }

            @Override
            public void onFailure(Call<PaginatedDeliveryResponse> call, Throwable t) {
                if (!isAdded()) return;
                Log.d("FAILED", t.getMessage());
                Toast.makeText(requireContext(), "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
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
