package com.techmania.tumago.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.model.LatLng;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.DeliveryRequest;
import com.techmania.tumago.Model.PaymentRequest;
import com.techmania.tumago.Model.PaymentResponse;
import com.techmania.tumago.Model.PaymentStatusResponse;
import com.techmania.tumago.R;
import com.techmania.tumago.auth.Login;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.NetworkUtils;
import com.techmania.tumago.helper.Token;
import com.techmania.tumago.helper.UiHelper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfirmDelivery extends AppCompatActivity {
    private static final String TAG = "ConfirmDelivery";
    private static final int PAYMENT_POLL_INTERVAL_MS = 3000; // Poll every 3 seconds
    private static final int PAYMENT_POLL_MAX_ATTEMPTS = 40;  // Max 2 minutes of polling

    TextView fare, vehicle, pickup, dropoff;
    LinearLayout confirmDelivery;
    ProgressBar look_for_driver;
    Spinner paymentSpinner;
    private LatLng originLatLng;
    private LatLng destLatLng;

    double fareValue;
    String vehicleValue;

    private boolean isRequesting = false;
    private Handler pollHandler;
    private int pollAttempts = 0;
    private AlertDialog paymentDialog;

    // Launcher for the card payment WebView — when it finishes, poll once and proceed
    private final ActivityResultLauncher<Intent> cardPaymentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                String paymentId = null;
                if (result.getData() != null) {
                    paymentId = result.getData().getStringExtra("payment_id");
                }
                if (paymentId != null) {
                    // User returned from Paynow checkout — poll to confirm
                    pollPaymentStatus(paymentId);
                } else {
                    resetRequestState();
                    Toast.makeText(this, "Card payment was not completed", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_delivery);

        pollHandler = new Handler(Looper.getMainLooper());

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        look_for_driver = findViewById(R.id.LookForDriver);
        look_for_driver.setVisibility(View.GONE);

        fare = findViewById(R.id.fare);
        vehicle = findViewById(R.id.vehicle);
        pickup = findViewById(R.id.pickup);
        dropoff = findViewById(R.id.dropoff);
        confirmDelivery = findViewById(R.id.confirmDelivery);

        Intent intent = getIntent();
        fareValue = intent.getDoubleExtra("price", 0);
        vehicleValue = intent.getStringExtra("transportName");
        String vehicleImageName = intent.getStringExtra("transportImage");
        fare.setText("$" + fareValue);
        vehicle.setText(vehicleValue);

        // Set vehicle image
        if (vehicleImageName != null) {
            ImageView vehicleImage = findViewById(R.id.vehicleImage);
            int resId = getResources().getIdentifier(vehicleImageName, "drawable", getPackageName());
            if (resId != 0) {
                vehicleImage.setImageResource(resId);
            }
        }

        // Set up payment spinner with all supported methods
        paymentSpinner = findViewById(R.id.payment);
        String[] paymentOptions = {"Cash", "Card", "EcoCash", "OneMoney"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, paymentOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentSpinner.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        long originLatBits = prefs.getLong("originLat", 0);
        long originLngBits = prefs.getLong("originLng", 0);
        long destLatBits = prefs.getLong("destLat", 0);
        long destLngBits = prefs.getLong("destLng", 0);

        // Only use coords if they were actually saved (avoid 0,0 default)
        if (originLatBits != 0 && originLngBits != 0) {
            double originLat = Double.longBitsToDouble(originLatBits);
            double originLng = Double.longBitsToDouble(originLngBits);
            originLatLng = new LatLng(originLat, originLng);
            getAddressFromLatLng(originLatLng, true);
        }
        if (destLatBits != 0 && destLngBits != 0) {
            double destLat = Double.longBitsToDouble(destLatBits);
            double destLng = Double.longBitsToDouble(destLngBits);
            destLatLng = new LatLng(destLat, destLng);
            getAddressFromLatLng(destLatLng, false);
        }

        findViewById(R.id.lookForTrip).setOnClickListener(view -> {
            onConfirmDelivery();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }
        NetworkUtils.registerNetworkCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        NetworkUtils.unregisterNetworkCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop any pending polls
        if (pollHandler != null) pollHandler.removeCallbacksAndMessages(null);
        dismissPaymentDialog();
    }

    private void getAddressFromLatLng(LatLng latLng, boolean isOrigin) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                // Use the full formatted address line so nothing is cut off
                String result = address.getAddressLine(0);
                if (result == null || result.isEmpty()) {
                    // Fallback: build manually if getAddressLine returns nothing
                    String street = address.getThoroughfare();
                    String number = address.getSubThoroughfare();
                    String city = address.getLocality();
                    String streetFull = (number != null ? number + " " : "") + (street != null ? street : "");
                    result = streetFull + ", " + (city != null ? city : "Unknown City");
                }

                if (isOrigin) {
                    pickup.setText(result);
                } else {
                    dropoff.setText(result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Entry point: decides whether to pay first or go straight to delivery
    // -------------------------------------------------------------------------
    private void onConfirmDelivery() {
        if (isRequesting) return;

        String accessToken = Token.getAccessToken(this);
        if (accessToken == null || accessToken.isEmpty()) {
            startActivity(new Intent(this, Login.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }

        String selectedPayment = paymentSpinner.getSelectedItem().toString();
        String paymentMethod = selectedPayment.toLowerCase();

        if (paymentMethod.equals("cash")) {
            // Cash — go straight to looking for a driver
            lookForDriver(paymentMethod);
        } else {
            // Card / EcoCash / OneMoney — pay via Paynow first
            initiatePaynowPayment(paymentMethod);
        }
    }

    // -------------------------------------------------------------------------
    // Paynow payment initiation
    // -------------------------------------------------------------------------
    private void initiatePaynowPayment(String paymentMethod) {
        isRequesting = true;
        look_for_driver.setVisibility(View.VISIBLE);
        findViewById(R.id.lookForTrip).setEnabled(false);

        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        // For EcoCash/OneMoney we need the user's phone number from their profile
        String phone = Token.getPhoneNumber(this);

        PaymentRequest paymentRequest = new PaymentRequest(fareValue, paymentMethod, phone);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<PaymentResponse> call = apiService.initiatePayment(authHeader, paymentRequest);

        call.enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PaymentResponse paymentResponse = response.body();
                    String paymentId = paymentResponse.getPaymentId();

                    if (paymentMethod.equals("card")) {
                        // Card: open Paynow checkout in a WebView
                        look_for_driver.setVisibility(View.GONE);
                        String redirectUrl = paymentResponse.getRedirectUrl();
                        if (redirectUrl != null && !redirectUrl.isEmpty()) {
                            Intent webIntent = new Intent(ConfirmDelivery.this, PaynowWebViewActivity.class);
                            webIntent.putExtra("url", redirectUrl);
                            webIntent.putExtra("payment_id", paymentId);
                            cardPaymentLauncher.launch(webIntent);
                        } else {
                            resetRequestState();
                            Toast.makeText(ConfirmDelivery.this, "Could not open payment page", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // EcoCash / OneMoney: USSD prompt sent to phone — show dialog and poll
                        look_for_driver.setVisibility(View.GONE);
                        showMobilePaymentDialog(paymentId, paymentResponse.getInstructions());
                    }
                } else {
                    resetRequestState();
                    Log.e(TAG, "Payment initiation failed: " + response.message());
                    UiHelper.showRetry(findViewById(android.R.id.content),
                            "Payment failed. Please try again.",
                            () -> initiatePaynowPayment(paymentMethod));
                }
            }

            @Override
            public void onFailure(Call<PaymentResponse> call, Throwable t) {
                resetRequestState();
                Log.e(TAG, "Payment initiation error: " + t.getMessage());
                UiHelper.showRetry(findViewById(android.R.id.content),
                        "Connection error. Please try again.",
                        () -> initiatePaynowPayment(paymentMethod));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Mobile money dialog — shown while waiting for EcoCash/OneMoney confirmation
    // -------------------------------------------------------------------------
    private void showMobilePaymentDialog(String paymentId, String instructions) {
        if (instructions == null || instructions.isEmpty()) {
            instructions = "A payment prompt has been sent to your phone. Please confirm to continue.";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Payment");
        builder.setMessage(instructions);
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            pollHandler.removeCallbacksAndMessages(null);
            resetRequestState();
            dialog.dismiss();
        });

        paymentDialog = builder.create();
        paymentDialog.show();

        // Start polling for payment confirmation
        pollAttempts = 0;
        pollPaymentStatus(paymentId);
    }

    // -------------------------------------------------------------------------
    // Poll Paynow payment status until paid, failed, or timeout
    // -------------------------------------------------------------------------
    private void pollPaymentStatus(String paymentId) {
        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<PaymentStatusResponse> call = apiService.checkPaymentStatus(authHeader, paymentId);

        call.enqueue(new Callback<PaymentStatusResponse>() {
            @Override
            public void onResponse(Call<PaymentStatusResponse> call, Response<PaymentStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PaymentStatusResponse statusResponse = response.body();

                    if (statusResponse.isPaid()) {
                        // Payment confirmed — proceed to look for a driver
                        dismissPaymentDialog();
                        String paymentMethod = paymentSpinner.getSelectedItem().toString().toLowerCase();
                        lookForDriver(paymentMethod);
                        return;
                    }

                    String status = statusResponse.getStatus();
                    if ("failed".equals(status) || "cancelled".equals(status)) {
                        dismissPaymentDialog();
                        resetRequestState();
                        Toast.makeText(ConfirmDelivery.this,
                                "Payment " + status + ". Please try again.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                // Still pending — poll again after delay
                pollAttempts++;
                if (pollAttempts < PAYMENT_POLL_MAX_ATTEMPTS) {
                    pollHandler.postDelayed(() -> pollPaymentStatus(paymentId), PAYMENT_POLL_INTERVAL_MS);
                } else {
                    dismissPaymentDialog();
                    resetRequestState();
                    Toast.makeText(ConfirmDelivery.this,
                            "Payment timed out. Please try again.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PaymentStatusResponse> call, Throwable t) {
                // Network error during poll — retry
                pollAttempts++;
                if (pollAttempts < PAYMENT_POLL_MAX_ATTEMPTS) {
                    pollHandler.postDelayed(() -> pollPaymentStatus(paymentId), PAYMENT_POLL_INTERVAL_MS);
                } else {
                    dismissPaymentDialog();
                    resetRequestState();
                    Toast.makeText(ConfirmDelivery.this,
                            "Connection lost. Please check your payment and try again.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Request a delivery (called after payment is confirmed or for cash)
    // -------------------------------------------------------------------------
    private void lookForDriver(String paymentMethod) {
        isRequesting = true;
        look_for_driver.setVisibility(View.VISIBLE);
        findViewById(R.id.lookForTrip).setEnabled(false);

        String accessToken = Token.getAccessToken(this);
        String authHeader = "Bearer " + accessToken;

        DeliveryRequest request = new DeliveryRequest(originLatLng, destLatLng, vehicleValue, fareValue, paymentMethod);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.requestDelivery(authHeader, request);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                resetRequestState();

                if (response.isSuccessful() && response.body() != null) {
                    Intent searchIntent = new Intent(ConfirmDelivery.this, SearchingForDriver.class);
                    searchIntent.putExtra("transportImage", getIntent().getStringExtra("transportImage"));
                    startActivity(searchIntent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                } else {
                    Log.d(TAG, "Delivery request failed: " + response.message());
                    UiHelper.showRetry(findViewById(android.R.id.content),
                            "Failed to request delivery", () -> lookForDriver(paymentMethod));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                resetRequestState();
                Log.e(TAG, "Delivery request error: " + t.getMessage());
                UiHelper.showRetry(findViewById(android.R.id.content),
                        "Connection error. Please try again.", () -> lookForDriver(paymentMethod));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private void resetRequestState() {
        isRequesting = false;
        look_for_driver.setVisibility(View.GONE);
        findViewById(R.id.lookForTrip).setEnabled(true);
    }

    private void dismissPaymentDialog() {
        if (paymentDialog != null && paymentDialog.isShowing()) {
            paymentDialog.dismiss();
            paymentDialog = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (isRequesting) {
            Toast.makeText(this, "Please wait while we process your request.", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
