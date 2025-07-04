package com.example.tradeup_app.utils;

import android.content.Context;
import android.util.Log;

import com.example.tradeup_app.services.NotificationService;
import com.example.tradeup_app.services.TokenService;

public class NotificationManager {
    private static final String TAG = "NotificationManager";
    private static NotificationManager instance;
    private NotificationService notificationService;
    private TokenService tokenService;
    private Context context;

    private NotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationService = new NotificationService(this.context);
        this.tokenService = new TokenService();
    }

    public static synchronized NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager(context);
        }
        return instance;
    }

    public static NotificationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NotificationManager not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    /**
     * Initialize notification system - call this in Application class or main activity
     */
    public void initialize() {
        Log.d(TAG, "Initializing notification system...");

        // Get and save FCM token
        tokenService.getCurrentToken(new TokenService.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                Log.d(TAG, "FCM token initialized successfully");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to initialize FCM token: " + error);
            }
        });
    }

    /**
     * Send notification for new message
     */
    public void sendMessageNotification(String conversationId, String senderId, String senderName,
                                      String messageContent, String receiverId) {
        notificationService.sendMessageNotification(conversationId, senderId, senderName,
                                                   messageContent, receiverId);
    }

    /**
     * Send notification for price offer
     */
    public void sendPriceOfferNotification(String productId, String productTitle, String offerAmount,
                                         String buyerName, String sellerId) {
        notificationService.sendPriceOfferNotification(productId, productTitle, offerAmount,
                                                      buyerName, sellerId);
    }

    /**
     * Send notification for listing update
     */
    public void sendListingUpdateNotification(String productId, String productTitle, String updateType,
                                            String userId) {
        notificationService.sendListingUpdateNotification(productId, productTitle, updateType, userId);
    }

    /**
     * Send promotional notification
     */
    public void sendPromotionalNotification(String title, String message, String actionUrl, String userId) {
        notificationService.sendPromotionalNotification(title, message, actionUrl, userId);
    }

    /**
     * Clear specific notification
     */
    public void clearNotification(int notificationId) {
        notificationService.clearNotification(notificationId);
    }

    /**
     * Clear all notifications
     */
    public void clearAllNotifications() {
        notificationService.clearAllNotifications();
    }

    /**
     * Clean up when user logs out
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up notification system...");
        tokenService.deleteToken();
    }

    /**
     * Get the notification service instance
     */
    public NotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * Get the token service instance
     */
    public TokenService getTokenService() {
        return tokenService;
    }
}
