package com.techmania.tumago.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.techmania.tumago.R;

/**
 * Opens the Paynow checkout page in a WebView for card payments.
 *
 * Expects extras:
 *   - "url"        — the Paynow redirect URL
 *   - "payment_id" — the TumaGo payment UUID
 *
 * When the user finishes (or navigates to the return URL), the activity
 * sets the payment_id as a result and finishes so ConfirmDelivery can
 * poll the status.
 */
public class PaynowWebViewActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private String paymentId;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paynow_webview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Complete Payment");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> cancelAndFinish());

        progressBar = findViewById(R.id.progressBar);
        webView = findViewById(R.id.webView);

        String url = getIntent().getStringExtra("url");
        paymentId = getIntent().getStringExtra("payment_id");

        if (url == null || url.isEmpty()) {
            cancelAndFinish();
            return;
        }

        // Configure WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // When Paynow redirects to our return URL, the payment is done.
                // The return URL is a dummy — the WebView intercepts it before
                // it actually loads, so it doesn't need to resolve to a real page.
                if (url.contains("payment-return") || url.contains("tumago.co.zw")) {
                    finishWithResult();
                    return true;
                }
                return false;
            }
        });

        webView.loadUrl(url);
    }

    private void finishWithResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("payment_id", paymentId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void cancelAndFinish() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            cancelAndFinish();
        }
    }
}
