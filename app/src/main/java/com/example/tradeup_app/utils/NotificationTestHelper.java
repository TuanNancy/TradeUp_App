package com.example.tradeup_app.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Offer;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.services.MessagingService;
import com.example.tradeup_app.services.PromotionService;

/**
 * Helper class to test and debug notification functionality
 */
public class NotificationTestHelper {
    private static final String TAG = "NotificationTestHelper";
    private final Context context;
    private final FirebaseManager firebaseManager;

    public NotificationTestHelper(Context context) {
        this.context = context;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    /**
     * Test message notification
     */
    public void testMessageNotification() {
        Log.d(TAG, "Testing message notification...");

        try {
            NotificationManager notificationManager = NotificationManager.getInstance(context);
            notificationManager.sendMessageNotification(
                "test_conversation_123",
                "sender_user_id",
                "Test User",
                "This is a test message notification",
                firebaseManager.getCurrentUserId()
            );

            Toast.makeText(context, "Test message notification sent!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Message notification test completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Message notification test failed", e);
            Toast.makeText(context, "Message notification test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Test price offer notification
     */
    public void testPriceOfferNotification() {
        Log.d(TAG, "Testing price offer notification...");

        try {
            NotificationManager notificationManager = NotificationManager.getInstance(context);
            notificationManager.sendPriceOfferNotification(
                "test_product_123",
                "Test Product",
                "100000",
                "Test Buyer",
                firebaseManager.getCurrentUserId()
            );

            Toast.makeText(context, "Test price offer notification sent!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Price offer notification test completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Price offer notification test failed", e);
            Toast.makeText(context, "Price offer notification test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Test listing update notification
     */
    public void testListingUpdateNotification() {
        Log.d(TAG, "Testing listing update notification...");

        try {
            NotificationManager notificationManager = NotificationManager.getInstance(context);
            notificationManager.sendListingUpdateNotification(
                "test_product_123",
                "Test Product",
                "sold",
                firebaseManager.getCurrentUserId()
            );

            Toast.makeText(context, "Test listing update notification sent!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Listing update notification test completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Listing update notification test failed", e);
            Toast.makeText(context, "Listing update notification test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Test promotional notification
     */
    public void testPromotionalNotification() {
        Log.d(TAG, "Testing promotional notification...");

        try {
            PromotionService promotionService = new PromotionService(context);
            promotionService.sendPromotionToUser(
                firebaseManager.getCurrentUserId(),
                "🎉 Test Promotion",
                "This is a test promotional notification from TradeUp!",
                "test_promotion"
            );

            Toast.makeText(context, "Test promotional notification sent!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Promotional notification test completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Promotional notification test failed", e);
            Toast.makeText(context, "Promotional notification test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Test all notification types
     */
    public void testAllNotifications() {
        Log.d(TAG, "Testing all notification types...");

        testMessageNotification();

        // Delay between tests
        new android.os.Handler().postDelayed(() -> {
            testPriceOfferNotification();
        }, 2000);

        new android.os.Handler().postDelayed(() -> {
            testListingUpdateNotification();
        }, 4000);

        new android.os.Handler().postDelayed(() -> {
            testPromotionalNotification();
        }, 6000);
    }

    /**
     * Debug messaging service
     */
    public void debugMessagingService() {
        Log.d(TAG, "Debugging messaging service...");

        try {
            MessagingService messagingService = new MessagingService(context);
            String currentUserId = firebaseManager.getCurrentUserId();

            if (currentUserId == null) {
                Log.e(TAG, "Current user ID is null - user not authenticated");
                Toast.makeText(context, "Error: User not authenticated", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d(TAG, "Current user ID: " + currentUserId);

            // Test getUserProfile
            messagingService.getUserProfile(currentUserId, new MessagingService.UserProfileCallback() {
                @Override
                public void onSuccess(String userName, String userAvatar) {
                    Log.d(TAG, "User profile loaded successfully: " + userName);
                    Toast.makeText(context, "Messaging service is working. User: " + userName, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to load user profile: " + error);
                    Toast.makeText(context, "Messaging service error: " + error, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Messaging service debug failed", e);
            Toast.makeText(context, "Messaging service debug failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Debug Firebase connection
     */
    public void debugFirebaseConnection() {
        Log.d(TAG, "Debugging Firebase connection...");

        try {
            String currentUserId = firebaseManager.getCurrentUserId();

            if (currentUserId == null) {
                Log.e(TAG, "Firebase: User not authenticated");
                Toast.makeText(context, "Firebase: User not authenticated", Toast.LENGTH_LONG).show();
                return;
            }

            // Test Firebase database connection
            firebaseManager.getDatabase().getReference("test")
                .setValue("test_value_" + System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firebase database connection successful");
                    Toast.makeText(context, "Firebase database connection successful", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase database connection failed", e);
                    Toast.makeText(context, "Firebase database connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        } catch (Exception e) {
            Log.e(TAG, "Firebase connection debug failed", e);
            Toast.makeText(context, "Firebase connection debug failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
