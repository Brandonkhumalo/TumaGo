package com.techmania.tumago_driver.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.auth.Login;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.Token;
import com.techmania.tumago_driver.helpers.UiHelper;
import com.techmania.tumago_driver.helpers.WalletTransactionAdapter;
import com.techmania.tumago_driver.models.WalletBalance;
import com.techmania.tumago_driver.models.WalletTopupResponse;
import com.techmania.tumago_driver.models.WalletTopupStatus;
import com.techmania.tumago_driver.models.WalletTransaction;
import com.techmania.tumago_driver.models.WalletTransactionResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private static final String TAG = "WalletActivity";

    private TextView walletBalance, owedBalance, totalPaid, emptyState;
    private ProgressBar progressBar;
    private RecyclerView transactionsRecycler;
    private MaterialButton btnTopUp, btnTransfer;
    private TextView btnRefundRequests;

    private ApiService apiService;
    private String authHeader;
    private final List<WalletTransaction> transactionList = new ArrayList<>();
    private WalletTransactionAdapter adapter;

    private Handler pollHandler;
    private Runnable pollRunnable;
    private int pollCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        Toolbar toolbar = findViewById(R.id.toolbarWallet);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        walletBalance = findViewById(R.id.walletBalance);
        owedBalance = findViewById(R.id.owedBalance);
        totalPaid = findViewById(R.id.totalPaid);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        transactionsRecycler = findViewById(R.id.transactionsRecycler);
        btnTopUp = findViewById(R.id.btnTopUp);
        btnTransfer = findViewById(R.id.btnTransfer);
        btnRefundRequests = findViewById(R.id.btnRefundRequests);

        transactionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WalletTransactionAdapter(transactionList);
        transactionsRecycler.setAdapter(adapter);

        String accessToken = Token.getAccessToken(this);
        if (accessToken == null || accessToken.isEmpty()) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }
        authHeader = "Bearer " + accessToken;
        apiService = ApiClient.getClient().create(ApiService.class);

        btnTopUp.setOnClickListener(v -> showTopUpDialog());
        btnTransfer.setOnClickListener(v -> showTransferDialog());
        btnRefundRequests.setOnClickListener(v ->
                startActivity(new Intent(this, RefundRequestsActivity.class)));

        loadData();
    }

    private void loadData() {
        UiHelper.showLoading(progressBar);
        loadBalance();
        loadTransactions();
    }

    private void loadBalance() {
        apiService.getWalletBalance(authHeader).enqueue(new Callback<WalletBalance>() {
            @Override
            public void onResponse(Call<WalletBalance> call, Response<WalletBalance> response) {
                UiHelper.hideLoading(progressBar);
                if (response.isSuccessful() && response.body() != null) {
                    WalletBalance b = response.body();
                    walletBalance.setText("$" + formatMoney(b.getWalletBalance()));
                    owedBalance.setText("$" + formatMoney(b.getOwedBalance()));
                    totalPaid.setText("$" + formatMoney(b.getTotalPaid()));
                }
            }

            @Override
            public void onFailure(Call<WalletBalance> call, Throwable t) {
                UiHelper.hideLoading(progressBar);
                Log.e(TAG, "Balance load failed", t);
            }
        });
    }

    private void loadTransactions() {
        apiService.getWalletTransactions(authHeader, 0).enqueue(new Callback<WalletTransactionResponse>() {
            @Override
            public void onResponse(Call<WalletTransactionResponse> call, Response<WalletTransactionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    transactionList.clear();
                    List<WalletTransaction> txns = response.body().getTransactions();
                    if (txns != null && !txns.isEmpty()) {
                        transactionList.addAll(txns);
                        emptyState.setVisibility(View.GONE);
                        transactionsRecycler.setVisibility(View.VISIBLE);
                    } else {
                        emptyState.setVisibility(View.VISIBLE);
                        transactionsRecycler.setVisibility(View.GONE);
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<WalletTransactionResponse> call, Throwable t) {
                Log.e(TAG, "Transactions load failed", t);
            }
        });
    }

    // ---- Top Up Dialog ----
    private void showTopUpDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_topup, null);
        EditText amountInput = dialogView.findViewById(R.id.topupAmount);
        EditText phoneInput = dialogView.findViewById(R.id.topupPhone);
        RadioGroup methodGroup = dialogView.findViewById(R.id.paymentMethodGroup);

        new AlertDialog.Builder(this)
                .setTitle("Top Up Wallet")
                .setView(dialogView)
                .setPositiveButton("Pay", (dialog, which) -> {
                    String amountStr = amountInput.getText().toString().trim();
                    String phone = phoneInput.getText().toString().trim();

                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedId = methodGroup.getCheckedRadioButtonId();
                    String method;
                    if (selectedId == R.id.radioEcocash) method = "ecocash";
                    else if (selectedId == R.id.radioOnemoney) method = "onemoney";
                    else method = "card";

                    if (!method.equals("card") && phone.isEmpty()) {
                        Toast.makeText(this, "Enter your phone number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    initiateTopUp(amountStr, method, phone);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void initiateTopUp(String amount, String method, String phone) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", Double.parseDouble(amount));
        body.put("payment_method", method);
        if (!phone.isEmpty()) body.put("phone", phone);

        UiHelper.showLoading(progressBar);
        apiService.walletTopup(authHeader, body).enqueue(new Callback<WalletTopupResponse>() {
            @Override
            public void onResponse(Call<WalletTopupResponse> call, Response<WalletTopupResponse> response) {
                UiHelper.hideLoading(progressBar);
                if (response.isSuccessful() && response.body() != null) {
                    String paymentId = response.body().getPaymentId();
                    String instructions = response.body().getInstructions();
                    Toast.makeText(WalletActivity.this, instructions, Toast.LENGTH_LONG).show();
                    startPollingTopup(paymentId);
                } else {
                    Toast.makeText(WalletActivity.this, "Top-up failed. Try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WalletTopupResponse> call, Throwable t) {
                UiHelper.hideLoading(progressBar);
                Toast.makeText(WalletActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startPollingTopup(String paymentId) {
        pollCount = 0;
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = () -> {
            if (pollCount >= 30) { // Stop after ~2 minutes
                Toast.makeText(this, "Payment timed out. Check your balance later.", Toast.LENGTH_LONG).show();
                return;
            }
            pollCount++;

            apiService.walletTopupStatus(authHeader, paymentId).enqueue(new Callback<WalletTopupStatus>() {
                @Override
                public void onResponse(Call<WalletTopupStatus> call, Response<WalletTopupStatus> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        WalletTopupStatus status = response.body();
                        if (status.isPaid()) {
                            Toast.makeText(WalletActivity.this, "Wallet topped up!", Toast.LENGTH_SHORT).show();
                            loadData();
                            return;
                        }
                        String s = status.getStatus();
                        if ("failed".equals(s) || "cancelled".equals(s)) {
                            Toast.makeText(WalletActivity.this, "Payment " + s, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // Still pending — poll again
                    pollHandler.postDelayed(pollRunnable, 4000);
                }

                @Override
                public void onFailure(Call<WalletTopupStatus> call, Throwable t) {
                    pollHandler.postDelayed(pollRunnable, 4000);
                }
            });
        };
        pollHandler.postDelayed(pollRunnable, 4000);
    }

    // ---- Transfer Dialog ----
    private void showTransferDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_transfer, null);
        EditText amountInput = dialogView.findViewById(R.id.transferAmount);
        RadioGroup directionGroup = dialogView.findViewById(R.id.transferDirectionGroup);

        new AlertDialog.Builder(this)
                .setTitle("Transfer Between Accounts")
                .setView(dialogView)
                .setPositiveButton("Transfer", (dialog, which) -> {
                    String amountStr = amountInput.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedId = directionGroup.getCheckedRadioButtonId();
                    String direction = (selectedId == R.id.radioOwedToWallet)
                            ? "owed_to_wallet" : "wallet_to_owed";

                    executeTransfer(amountStr, direction);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeTransfer(String amount, String direction) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", Double.parseDouble(amount));
        body.put("direction", direction);

        UiHelper.showLoading(progressBar);
        apiService.walletTransfer(authHeader, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                UiHelper.hideLoading(progressBar);
                if (response.isSuccessful()) {
                    Toast.makeText(WalletActivity.this, "Transfer successful!", Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    Toast.makeText(WalletActivity.this, "Transfer failed. Check your balance.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                UiHelper.hideLoading(progressBar);
                Toast.makeText(WalletActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatMoney(String value) {
        if (value == null || value.isEmpty()) return "0.00";
        try {
            return String.format("%.2f", Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return value;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }
}
