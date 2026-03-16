package com.techmania.tumago.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.techmania.tumago.Activities.TrackDelivery;
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

public class HomeFragment extends Fragment {

    private TextView userName;
    private RecyclerView recentOrdersRecycler;
    private LinearLayout noRecentOrders;
    private ArrayList<Deliveries> recentList;
    private DeliveryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = view.findViewById(R.id.userName);
        recentOrdersRecycler = view.findViewById(R.id.recentOrdersRecycler);
        noRecentOrders = view.findViewById(R.id.noRecentOrders);
        LinearLayout btnOrderDelivery = view.findViewById(R.id.btnOrderDelivery);
        LinearLayout btnTrackDelivery = view.findViewById(R.id.btnTrackDelivery);
        TextView viewAll = view.findViewById(R.id.viewAll);

        // Set up recent orders RecyclerView
        recentList = new ArrayList<>();
        adapter = new DeliveryAdapter(recentList, requireContext());
        recentOrdersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentOrdersRecycler.setAdapter(adapter);

        // Load user name from cache
        loadUserName();

        // Fetch recent 3 deliveries
        fetchRecentOrders();

        // Order Delivery button -> switch to Delivery tab
        btnOrderDelivery.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_delivery);
            }
        });

        // Track Delivery button -> open TrackDelivery activity
        btnTrackDelivery.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TrackDelivery.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // View All -> switch to Parcels tab
        viewAll.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_parcels);
            }
        });
    }

    private void loadUserName() {
        if (getContext() == null) return;
        SharedPreferences cache = requireContext().getSharedPreferences("app_cache", Context.MODE_PRIVATE);
        String name = cache.getString("user_name", "");
        String surname = cache.getString("user_surname", "");

        if (!name.isEmpty() || !surname.isEmpty()) {
            userName.setText(String.format("%s %s", name, surname).trim());
        }
    }

    private void fetchRecentOrders() {
        if (getContext() == null) return;
        String accessToken = Token.getAccessToken(requireContext());
        if (accessToken == null || accessToken.isEmpty()) return;

        String authHeader = "Bearer " + accessToken;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        // Fetch first page (no cursor) — the API returns deliveries ordered by start_time desc
        Call<PaginatedDeliveryResponse> call = apiService.getDeliveries(null, authHeader);
        call.enqueue(new Callback<PaginatedDeliveryResponse>() {
            @Override
            public void onResponse(Call<PaginatedDeliveryResponse> call, Response<PaginatedDeliveryResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Deliveries> deliveries = response.body().getResults();

                    if (deliveries != null && !deliveries.isEmpty()) {
                        recentList.clear();
                        // Only take the first 3
                        int limit = Math.min(deliveries.size(), 3);
                        for (int i = 0; i < limit; i++) {
                            recentList.add(deliveries.get(i));
                        }
                        adapter.notifyDataSetChanged();
                        recentOrdersRecycler.scheduleLayoutAnimation();
                        recentOrdersRecycler.setVisibility(View.VISIBLE);
                        noRecentOrders.setVisibility(View.GONE);
                    } else {
                        recentOrdersRecycler.setVisibility(View.GONE);
                        noRecentOrders.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<PaginatedDeliveryResponse> call, Throwable t) {
                Log.e("HomeFragment", "Failed to fetch recent orders: " + t.getMessage());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserName();
    }
}
