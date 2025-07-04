package com.example.tradeup_app.services;

import android.util.Log;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class TokenService {
    private static final String TAG = "TokenService";
    private final FirebaseManager firebaseManager;

    public TokenService() {
        this.firebaseManager = FirebaseManager.getInstance();
    }

    public void saveTokenToDatabase(String token) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            Log.w(TAG, "User not authenticated, cannot save FCM token");
            return;
        }

        DatabaseReference tokenRef = firebaseManager.getDatabase()
                .getReference("user_tokens")
                .child(currentUserId);

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("fcm_token", token);
        tokenData.put("updated_at", System.currentTimeMillis());
        tokenData.put("device_type", "android");

        tokenRef.setValue(tokenData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
    }

    public void getCurrentToken(TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        callback.onError("Failed to get FCM token");
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Registration Token: " + token);

                    // Save token to database
                    saveTokenToDatabase(token);
                    callback.onTokenReceived(token);
                });
    }

    public void deleteToken() {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) return;

        // Delete from database
        DatabaseReference tokenRef = firebaseManager.getDatabase()
                .getReference("user_tokens")
                .child(currentUserId);

        tokenRef.removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token deleted successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete FCM token", e));

        // Delete from FCM
        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "FCM token deleted from Firebase");
                    } else {
                        Log.e(TAG, "Failed to delete FCM token from Firebase", task.getException());
                    }
                });
    }

    public interface TokenCallback {
        void onTokenReceived(String token);
        void onError(String error);
    }
}
