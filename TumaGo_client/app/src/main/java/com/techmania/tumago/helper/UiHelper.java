package com.techmania.tumago.helper;

import android.view.View;
import android.widget.ProgressBar;

import com.google.android.material.snackbar.Snackbar;

/**
 * Centralises loading-state and error-retry UI so every Activity
 * behaves the same way during network calls.
 */
public final class UiHelper {

    private UiHelper() {}

    /** Show an indeterminate ProgressBar and disable one or more views (buttons, inputs). */
    public static void showLoading(ProgressBar pb, View... disable) {
        if (pb != null) pb.setVisibility(View.VISIBLE);
        for (View v : disable) {
            if (v != null) v.setEnabled(false);
        }
    }

    /** Hide the ProgressBar and re-enable the views. */
    public static void hideLoading(ProgressBar pb, View... enable) {
        if (pb != null) pb.setVisibility(View.GONE);
        for (View v : enable) {
            if (v != null) v.setEnabled(true);
        }
    }

    /** Show a Snackbar with a Retry action. */
    public static void showRetry(View root, String message, Runnable retry) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> retry.run())
                .show();
    }
}
