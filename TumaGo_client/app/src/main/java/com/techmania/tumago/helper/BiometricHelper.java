package com.techmania.tumago.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Manages biometric authentication state and prompts.
 * Stores preferences in EncryptedSharedPreferences alongside tokens.
 */
public class BiometricHelper {

    private static final String PREF_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String PREF_BIOMETRIC_ASKED = "biometric_asked";

    // Check if the device supports biometric authentication
    public static boolean isDeviceSupported(Context context) {
        BiometricManager manager = BiometricManager.from(context);
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    // Check if user has enabled biometric login
    public static boolean isEnabled(Context context) {
        return getPrefs(context).getBoolean(PREF_BIOMETRIC_ENABLED, false);
    }

    // Set biometric login on or off
    public static void setEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply();
    }

    // Check if we already asked the user about biometrics (first-time prompt)
    public static boolean hasBeenAsked(Context context) {
        return getPrefs(context).getBoolean(PREF_BIOMETRIC_ASKED, false);
    }

    // Mark that we have asked
    public static void setAsked(Context context) {
        getPrefs(context).edit().putBoolean(PREF_BIOMETRIC_ASKED, true).apply();
    }

    /**
     * Show the system biometric prompt. Calls onSuccess or onFailure on the main thread.
     */
    public static void authenticate(FragmentActivity activity,
                                    Runnable onSuccess,
                                    Runnable onFailure) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Verify your identity to continue")
                .setNegativeButtonText("Use password")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
                ContextCompat.getMainExecutor(activity),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        onSuccess.run();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        // User pressed "Use password" or cancelled
                        onFailure.run();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // Fingerprint not recognized — prompt stays open for retry
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private static SharedPreferences getPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to regular prefs if encryption fails
            return context.getSharedPreferences("secure_prefs_fallback", Context.MODE_PRIVATE);
        }
    }
}
