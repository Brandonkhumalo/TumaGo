package com.techmania.tumago.helper;

public class GetRefreshToken {
    /**private void refreshAccessToken() {
        String refreshToken = Token.getRefreshToken(this);

        // Make a network call to refresh the access token using the refresh token
        ApiService.refreshToken(refreshToken, new TokenCallback() {
            @Override
            public void onSuccess(String newAccessToken) {
                // Store the new access token securely
                Token.storeToken(MainActivity.this, newAccessToken, refreshToken);
                // Retry the original request
                getUserData();  // Call the function that fetches user data
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("Token Refresh", "Failed to refresh token: " + t.getMessage());
            }
        });
    }*/
}
