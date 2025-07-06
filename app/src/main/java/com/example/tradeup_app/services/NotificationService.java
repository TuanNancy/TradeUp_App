package com.example.tradeup_app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.tradeup_app.R;
import com.example.tradeup_app.activities.ChatActivity;
import com.example.tradeup_app.activities.ProductDetailActivity;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Notification;
import com.example.tradeup_app.receivers.OfferActionReceiver;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class NotificationService {
    private static final String TAG = "NotificationService";

    // Notification Channels
    private static final String CHANNEL_MESSAGES = "messages";
    private static final String CHANNEL_OFFERS = "offers";
    private static final String CHANNEL_LISTINGS = "listings";
    private static final String CHANNEL_PROMOTIONS = "promotions";
    private static final String CHANNEL_GENERAL = "general";

    // Notification Types
    public static final String TYPE_NEW_MESSAGE = "new_message";
    public static final String TYPE_PRICE_OFFER = "price_offer";
    public static final String TYPE_LISTING_UPDATE = "listing_update";
    public static final String TYPE_PROMOTION = "promotion";

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final FirebaseManager firebaseManager;

    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        this.firebaseManager = FirebaseManager.getInstance();
        createNotificationChannels();
    }

    // ✅ SỬA: Đổi tên method để logic rõ ràng hơn
    private boolean isNotificationPermissionDenied() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                   != PackageManager.PERMISSION_GRANTED;
        }
        return !notificationManager.areNotificationsEnabled();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Messages Channel
            NotificationChannel messagesChannel = new NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            );
            messagesChannel.setDescription("New chat messages");
            messagesChannel.enableVibration(true);
            messagesChannel.setShowBadge(true);

            // Offers Channel
            NotificationChannel offersChannel = new NotificationChannel(
                CHANNEL_OFFERS,
                "Price Offers",
                NotificationManager.IMPORTANCE_HIGH
            );
            offersChannel.setDescription("Price offers and negotiations");
            offersChannel.enableVibration(true);
            offersChannel.setShowBadge(true);

            // Listings Channel
            NotificationChannel listingsChannel = new NotificationChannel(
                CHANNEL_LISTINGS,
                "Listing Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            listingsChannel.setDescription("Updates to your listings");
            listingsChannel.setShowBadge(true);

            // Promotions Channel
            NotificationChannel promotionsChannel = new NotificationChannel(
                CHANNEL_PROMOTIONS,
                "Promotions",
                NotificationManager.IMPORTANCE_LOW
            );
            promotionsChannel.setDescription("Promotional offers and deals");
            promotionsChannel.setShowBadge(false);

            // General Channel
            NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General notifications");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(messagesChannel);
            manager.createNotificationChannel(offersChannel);
            manager.createNotificationChannel(listingsChannel);
            manager.createNotificationChannel(promotionsChannel);
            manager.createNotificationChannel(generalChannel);
        }
    }

    // Send notification for new message
    public void sendMessageNotification(String conversationId, String senderId, String senderName,
                                      String messageContent, String receiverId) {
        Log.d(TAG, "🔔 Processing message notification - From: " + senderName + " (" + senderId + ") To: " + receiverId);

        // ✅ SỬA: Kiểm tra quyền thông báo trước
        if (isNotificationPermissionDenied()) {
            Log.w(TAG, "❌ Notification permission not granted");
            return;
        }

        // ✅ SỬA: Kiểm tra logic gửi thông báo với logging chi tiết
        if (shouldSendNotification(receiverId, senderId)) {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("receiverId", senderId);
            intent.putExtra("receiverName", senderName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                conversationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Truncate long messages
            String displayMessage = messageContent.length() > 100 ?
                messageContent.substring(0, 97) + "..." : messageContent;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(senderName)
                .setContentText(displayMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageContent))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // ✅ Thêm để đảm bảo hiển thị
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup("messages")
                .setWhen(System.currentTimeMillis()) // ✅ Thêm timestamp
                .setShowWhen(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // ✅ Đảm bảo hiển thị
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);

            // ✅ SỬA: Thêm try-catch với logging chi tiết
            try {
                int notificationId = conversationId.hashCode();
                Log.d(TAG, "🔔 Sending notification with ID: " + notificationId + " to user: " + receiverId);
                notificationManager.notify(notificationId, builder.build());
                saveNotificationToDatabase(receiverId, TYPE_NEW_MESSAGE, senderName, messageContent, conversationId);
                Log.d(TAG, "✅ Notification sent successfully");
            } catch (SecurityException e) {
                Log.e(TAG, "❌ Failed to send notification: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "❌ Notification blocked by shouldSendNotification logic");
        }
    }

    // Send notification for price offer
    public void sendPriceOfferNotification(String productId, String productTitle, String offerAmount,
                                         String buyerName, String sellerId) {
        if (isNotificationPermissionDenied()) {
            Log.w(TAG, "Notification permission not granted");
            return;
        }

        if (shouldSendNotification(sellerId, null)) {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("productId", productId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                productId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String title = "New Price Offer";
            String message = buyerName + " offered $" + offerAmount + " for " + productTitle;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_OFFERS)
                .setSmallIcon(R.drawable.ic_offer)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup("offers")
                .addAction(R.drawable.ic_check, "Accept", createOfferActionIntent(productId, "accept"))
                .addAction(R.drawable.ic_close, "Decline", createOfferActionIntent(productId, "decline"));

            try {
                notificationManager.notify(("offer_" + productId).hashCode(), builder.build());
                saveNotificationToDatabase(sellerId, TYPE_PRICE_OFFER, title, message, productId);
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to send notification: " + e.getMessage());
            }
        }
    }

    // Send notification for successful payment to seller
    public void sendPaymentSuccessNotification(String productId, String productTitle,
                                             String buyerName, double amount, String sellerId) {
        if (isNotificationPermissionDenied()) {
            Log.w(TAG, "Notification permission not granted");
            return;
        }

        if (shouldSendNotification(sellerId, null)) {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("productId", productId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                productId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String title = "Payment Received!";
            String formattedAmount = String.format("$%.2f", amount);
            String message = buyerName + " has successfully purchased " + productTitle + " for " + formattedAmount;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_OFFERS)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup("payments");

            try {
                notificationManager.notify(("payment_" + productId).hashCode(), builder.build());
                saveNotificationToDatabase(sellerId, "PAYMENT_SUCCESS", title, message, productId);
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to send payment notification: " + e.getMessage());
            }
        }
    }

    // Send notification for listing update
    public void sendListingUpdateNotification(String productId, String productTitle, String updateType,
                                            String userId) {
        if (isNotificationPermissionDenied()) {
            Log.w(TAG, "Notification permission not granted");
            return;
        }

        if (shouldSendNotification(userId, null)) {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("productId", productId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                productId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String title = "Listing Updated";
            String message = getListingUpdateMessage(updateType, productTitle);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_LISTINGS)
                .setSmallIcon(R.drawable.ic_update)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup("listings");

            try {
                notificationManager.notify(("listing_" + productId).hashCode(), builder.build());
                saveNotificationToDatabase(userId, TYPE_LISTING_UPDATE, title, message, productId);
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to send notification: " + e.getMessage());
            }
        }
    }

    // Send promotional notification
    public void sendPromotionalNotification(String title, String message, String actionUrl, String userId) {
        if (isNotificationPermissionDenied()) {
            Log.w(TAG, "Notification permission not granted");
            return;
        }

        if (shouldSendNotification(userId, null)) {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (actionUrl != null && !actionUrl.isEmpty()) {
                    intent.putExtra("promotional_url", actionUrl);
                }
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                title.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PROMOTIONS)
                .setSmallIcon(R.drawable.ic_promotion)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup("promotions");

            try {
                notificationManager.notify(("promo_" + System.currentTimeMillis()).hashCode(), builder.build());
                saveNotificationToDatabase(userId, TYPE_PROMOTION, title, message, null);
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to send notification: " + e.getMessage());
            }
        }
    }

    private PendingIntent createOfferActionIntent(String productId, String action) {
        Intent intent = new Intent(context, OfferActionReceiver.class);
        intent.putExtra("productId", productId);
        intent.putExtra("action", action);

        return PendingIntent.getBroadcast(
            context,
            (productId + action).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private String getListingUpdateMessage(String updateType, String productTitle) {
        switch (updateType) {
            case "price_change":
                return "Price updated for " + productTitle;
            case "sold":
                return productTitle + " has been sold";
            case "expired":
                return "Your listing for " + productTitle + " has expired";
            case "featured":
                return productTitle + " is now featured";
            case "approved":
                return "Your listing for " + productTitle + " has been approved";
            case "rejected":
                return "Your listing for " + productTitle + " needs review";
            default:
                return "Update available for " + productTitle;
        }
    }

    // ✅ SỬA: Cải thiện method shouldSendNotification với logging chi tiết
    private boolean shouldSendNotification(String userId, String excludeUserId) {
        Log.d(TAG, "📋 Checking notification permissions for userId: " + userId + ", excludeUserId: " + excludeUserId);

        if (userId == null) {
            Log.w(TAG, "❌ No userId provided - skipping notification");
            return false;
        }

        // ✅ KIỂM TRA: Không gửi thông báo cho chính người gửi
        if (excludeUserId != null && excludeUserId.equals(userId)) {
            Log.d(TAG, "❌ Skipping notification - user is the sender: " + excludeUserId);
            return false;
        }

        // ✅ KIỂM TRA: Không gửi thông báo nếu user ID trống
        if (userId.trim().isEmpty()) {
            Log.w(TAG, "❌ Empty userId - skipping notification");
            return false;
        }

        // ✅ GỬI THÔNG BÁO CHO NGƯỜI NHẬN
        Log.d(TAG, "✅ Sending notification to receiver: " + userId);
        return true;
    }

    private void saveNotificationToDatabase(String userId, String type, String title, String message, String relatedId) {
        if (userId == null) return;

        DatabaseReference notificationsRef = firebaseManager.getDatabase()
                .getReference("notifications")
                .child(userId);

        String notificationId = notificationsRef.push().getKey();
        if (notificationId == null) return;

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedId(relatedId);
        notification.setTimestamp(System.currentTimeMillis());
        notification.setRead(false);

        notificationsRef.child(notificationId).setValue(notification)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification saved to database"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save notification", e));
    }

    // Handle FCM messages
    public void handleFCMMessage(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");

        if (type == null) return;

        switch (type) {
            case TYPE_NEW_MESSAGE:
                handleMessageNotification(data);
                break;
            case TYPE_PRICE_OFFER:
                handleOfferNotification(data);
                break;
            case TYPE_LISTING_UPDATE:
                handleListingNotification(data);
                break;
            case TYPE_PROMOTION:
                handlePromotionNotification(data);
                break;
            default:
                handleGeneralNotification(data);
                break;
        }
    }

    private void handleMessageNotification(Map<String, String> data) {
        String conversationId = data.get("conversationId");
        String senderId = data.get("senderId");
        String senderName = data.get("senderName");
        String message = data.get("message");
        String receiverId = data.get("receiverId");

        if (conversationId != null && senderId != null && senderName != null && message != null) {
            sendMessageNotification(conversationId, senderId, senderName, message, receiverId);
        }
    }

    private void handleOfferNotification(Map<String, String> data) {
        String productId = data.get("productId");
        String productTitle = data.get("productTitle");
        String offerAmount = data.get("offerAmount");
        String buyerName = data.get("buyerName");
        String sellerId = data.get("sellerId");

        if (productId != null && productTitle != null && offerAmount != null && buyerName != null) {
            sendPriceOfferNotification(productId, productTitle, offerAmount, buyerName, sellerId);
        }
    }

    private void handleListingNotification(Map<String, String> data) {
        String productId = data.get("productId");
        String productTitle = data.get("productTitle");
        String updateType = data.get("updateType");
        String userId = data.get("userId");

        if (productId != null && productTitle != null && updateType != null) {
            sendListingUpdateNotification(productId, productTitle, updateType, userId);
        }
    }

    private void handlePromotionNotification(Map<String, String> data) {
        String title = data.get("title");
        String message = data.get("message");
        String actionUrl = data.get("actionUrl");
        String userId = data.get("userId");

        if (title != null && message != null) {
            sendPromotionalNotification(title, message, actionUrl, userId);
        }
    }

    private void handleGeneralNotification(Map<String, String> data) {
        String title = data.get("title");
        String message = data.get("message");

        if (title != null && message != null) {
            showGeneralNotification(title, message);
        }
    }

    // Make showGeneralNotification public so FCMService can access it
    public void showGeneralNotification(String title, String message) {
        if (isNotificationPermissionDenied()) {
            Log.w(TAG, "Notification permission not granted");
            return;
        }

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to send notification: " + e.getMessage());
        }
    }

    // Clear notifications
    public void clearNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }

    public void clearAllNotifications() {
        notificationManager.cancelAll();
    }
}
