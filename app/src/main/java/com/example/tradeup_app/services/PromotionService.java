package com.example.tradeup_app.services;

import android.content.Context;
import android.util.Log;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.utils.NotificationManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class PromotionService {
    private static final String TAG = "PromotionService";
    private final Context context;
    private final FirebaseManager firebaseManager;

    public PromotionService(Context context) {
        this.context = context;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    /**
     * Send promotional notification to all users
     */
    public void sendPromotionToAllUsers(String title, String message, String actionUrl) {
        Log.d(TAG, "Sending promotion to all users: " + title);

        // Get all users from database
        DatabaseReference usersRef = firebaseManager.getDatabase().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                NotificationManager notificationManager = NotificationManager.getInstance(context);

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null) {
                        notificationManager.sendPromotionalNotification(title, message, actionUrl, userId);
                    }
                }

                Log.d(TAG, "Promotional notifications sent to " + dataSnapshot.getChildrenCount() + " users");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to get users for promotion: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Send promotional notification to specific user
     */
    public void sendPromotionToUser(String userId, String title, String message, String actionUrl) {
        Log.d(TAG, "Sending promotion to user: " + userId);

        NotificationManager notificationManager = NotificationManager.getInstance(context);
        notificationManager.sendPromotionalNotification(title, message, actionUrl, userId);
    }

    /**
     * Send new feature announcement
     */
    public void sendNewFeatureAnnouncement() {
        String title = "🎉 New Features Available!";
        String message = "Check out the latest updates in TradeUp: Enhanced chat, better notifications, and improved search!";
        String actionUrl = "feature_updates";

        sendPromotionToAllUsers(title, message, actionUrl);
    }

    /**
     * Send seasonal promotion
     */
    public void sendSeasonalPromotion() {
        String title = "🛍️ Special Sale Event!";
        String message = "Don't miss out on amazing deals! Browse featured products with special discounts.";
        String actionUrl = "featured_products";

        sendPromotionToAllUsers(title, message, actionUrl);
    }

    /**
     * Send welcome notification to new users
     */
    public void sendWelcomeNotification(String userId, String userName) {
        String title = "Welcome to TradeUp! 👋";
        String message = "Hi " + userName + "! Start exploring amazing products and connect with other traders.";
        String actionUrl = "explore";

        sendPromotionToUser(userId, title, message, actionUrl);
    }

    /**
     * Send reminder to inactive users
     */
    public void sendInactiveUserReminder(String userId) {
        String title = "We miss you! 💙";
        String message = "Discover new products and great deals waiting for you on TradeUp.";
        String actionUrl = "browse_products";

        sendPromotionToUser(userId, title, message, actionUrl);
    }
}
