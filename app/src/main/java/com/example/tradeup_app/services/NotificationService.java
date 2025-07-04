package com.example.tradeup_app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

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
import java.util.HashMap;
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
    public static final String TYPE_GENERAL = "general";

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final FirebaseManager firebaseManager;

    public interface NotificationCallback {
        void onNotificationSent(boolean success);
        void onError(String error);
    }

    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        this.firebaseManager = FirebaseManager.getInstance();
        createNotificationChannels();
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
        if (shouldSendNotification(receiverId, TYPE_NEW_MESSAGE)) {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("receiverId", senderId);
            intent.putExtra("receiverName", senderName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

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
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup("messages")
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);

            notificationManager.notify(conversationId.hashCode(), builder.build());

            // Save notification to database
            saveNotificationToDatabase(receiverId, TYPE_NEW_MESSAGE, senderName, messageContent, conversationId);
        }
    }

    // Send notification for price offer
    public void sendPriceOfferNotification(String productId, String productTitle, String offerAmount,
                                         String buyerName, String sellerId) {
        if (shouldSendNotification(sellerId, TYPE_PRICE_OFFER)) {
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

            notificationManager.notify(("offer_" + productId).hashCode(), builder.build());

            // Save notification to database
            saveNotificationToDatabase(sellerId, TYPE_PRICE_OFFER, title, message, productId);
        }
    }

    // Send notification for listing update
    public void sendListingUpdateNotification(String productId, String productTitle, String updateType,
                                            String userId) {
        if (shouldSendNotification(userId, TYPE_LISTING_UPDATE)) {
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

            notificationManager.notify(("listing_" + productId).hashCode(), builder.build());

            // Save notification to database
            saveNotificationToDatabase(userId, TYPE_LISTING_UPDATE, title, message, productId);
        }
    }

    // Send promotional notification
    public void sendPromotionalNotification(String title, String message, String actionUrl, String userId) {
        if (shouldSendNotification(userId, TYPE_PROMOTION)) {
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

            notificationManager.notify(("promo_" + System.currentTimeMillis()).hashCode(), builder.build());

            // Save notification to database
            saveNotificationToDatabase(userId, TYPE_PROMOTION, title, message, null);
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

    private boolean shouldSendNotification(String userId, String notificationType) {
        // Check user notification preferences
        // This can be enhanced to check user settings from Firebase
        String currentUserId = firebaseManager.getCurrentUserId();

        // Don't send notification to self
        if (currentUserId != null && currentUserId.equals(userId)) {
            return false;
        }

        // Check if user has disabled this type of notification
        // For now, return true, but this should check user preferences
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

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // Clear notifications
    public void clearNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }

    public void clearAllNotifications() {
        notificationManager.cancelAll();
    }

    // Load image from URL for rich notifications
    private Bitmap loadImageFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Log.e(TAG, "Error loading notification image", e);
            return null;
        }
    }
}
