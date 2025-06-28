package com.example.tradeup_app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.tradeup_app.R;
import com.example.tradeup_app.activities.ChatActivity;
import com.example.tradeup_app.auth.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class TradeUpFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "tradeup_notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Send token to server if needed
        sendRegistrationToServer(token);
    }

    private void handleDataMessage(java.util.Map<String, String> data) {
        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");

        if (type != null) {
            switch (type) {
                case "new_message":
                    showNotification(title, body, data);
                    break;
                case "new_offer":
                    showNotification("💰 " + title, body, data);
                    break;
                case "product_sold":
                    showNotification("🎉 " + title, body, data);
                    break;
                default:
                    showNotification(title, body, data);
                    break;
            }
        }
    }

    private void showNotification(String title, String body, java.util.Map<String, String> data) {
        Intent intent = getNotificationIntent(data);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }

    private Intent getNotificationIntent(java.util.Map<String, String> data) {
        String type = data.get("type");
        String conversationId = data.get("conversationId");
        String productId = data.get("productId");
        String sellerId = data.get("sellerId");

        if ("new_message".equals(type) || "new_offer".equals(type)) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("productId", productId);
            intent.putExtra("sellerId", sellerId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TradeUp Notifications";
            String description = "Notifications for TradeUp app";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void sendRegistrationToServer(String token) {
        // TODO: Send token to your server to store for sending notifications
        Log.d(TAG, "Token sent to server: " + token);
    }
}
