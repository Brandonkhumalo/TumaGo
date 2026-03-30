package com.techmania.tumago_driver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.auth.Login;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.RefundRequestAdapter;
import com.techmania.tumago_driver.helpers.Token;
import com.techmania.tumago_driver.helpers.UiHelper;
import com.techmania.tumago_driver.models.RefundRequest;
import com.techmania.tumago_driver.models.RefundRequestResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RefundRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;
    private final List<RefundRequest> requestList = new ArrayList<>();
    private RefundRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refund_requests);

        Toolbar toolbar = findViewById(R.id.toolbarRefunds);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.refundRecycler);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RefundRequestAdapter(requestList);
        recyclerView.setAdapter(adapter);

        String accessToken = Token.getAccessToken(this);
        if (accessToken == null || accessToken.isEmpty()) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        loadRefundRequests("Bearer " + accessToken);
    }

    private void loadRefundRequests(String authHeader) {
        UiHelper.showLoading(progressBar);
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.getRefundRequests(authHeader).enqueue(new Callback<RefundRequestResponse>() {
            @Override
            public void onResponse(Call<RefundRequestResponse> call, Response<RefundRequestResponse> response) {
                UiHelper.hideLoading(progressBar);
                if (response.isSuccessful() && response.body() != null) {
                    List<RefundRequest> list = response.body().getRefundRequests();
                    requestList.clear();
                    if (list != null && !list.isEmpty()) {
                        requestList.addAll(list);
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    } else {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<RefundRequestResponse> call, Throwable t) {
                UiHelper.hideLoading(progressBar);
                Log.e("RefundRequests", "Failed to load", t);
                UiHelper.showRetry(findViewById(android.R.id.content), "Failed to load",
                        () -> loadRefundRequests(authHeader));
            }
        });
    }
}
