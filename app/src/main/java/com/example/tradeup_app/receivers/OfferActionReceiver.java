package com.example.tradeup_app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.services.NotificationService;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class OfferActionReceiver extends BroadcastReceiver {
    private static final String TAG = "OfferActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String productId = intent.getStringExtra("productId");
        String action = intent.getStringExtra("action");

        if (productId == null || action == null) {
            Log.e(TAG, "Missing required data in intent");
            return;
        }

        Log.d(TAG, "Handling offer action: " + action + " for product: " + productId);

        switch (action) {
            case "accept":
                handleAcceptOffer(context, productId);
                break;
            case "decline":
                handleDeclineOffer(context, productId);
                break;
            default:
                Log.w(TAG, "Unknown action: " + action);
                break;
        }

        // Clear the notification after action
        NotificationService notificationService = new NotificationService(context);
        notificationService.clearNotification(("offer_" + productId).hashCode());
    }

    private void handleAcceptOffer(Context context, String productId) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        String currentUserId = firebaseManager.getCurrentUserId();

        if (currentUserId == null) {
            Toast.makeText(context, "Please log in to accept offers", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update offer status in database
        DatabaseReference offerRef = firebaseManager.getDatabase()
                .getReference("offers")
                .child(productId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "accepted");
        updates.put("acceptedAt", System.currentTimeMillis());
        updates.put("acceptedBy", currentUserId);

        offerRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Offer accepted successfully");
                    Toast.makeText(context, "Offer accepted!", Toast.LENGTH_SHORT).show();

                    // Optionally send notification to buyer
                    sendOfferResponseNotification(context, productId, "accepted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept offer", e);
                    Toast.makeText(context, "Failed to accept offer", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleDeclineOffer(Context context, String productId) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        String currentUserId = firebaseManager.getCurrentUserId();

        if (currentUserId == null) {
            Toast.makeText(context, "Please log in to decline offers", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update offer status in database
        DatabaseReference offerRef = firebaseManager.getDatabase()
                .getReference("offers")
                .child(productId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "declined");
        updates.put("declinedAt", System.currentTimeMillis());
        updates.put("declinedBy", currentUserId);

        offerRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Offer declined successfully");
                    Toast.makeText(context, "Offer declined", Toast.LENGTH_SHORT).show();

                    // Optionally send notification to buyer
                    sendOfferResponseNotification(context, productId, "declined");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to decline offer", e);
                    Toast.makeText(context, "Failed to decline offer", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendOfferResponseNotification(Context context, String productId, String response) {
        // This would send a notification back to the buyer about the response
        // Implementation depends on your notification system architecture
        Log.d(TAG, "Offer " + response + " for product: " + productId);
    }
}
