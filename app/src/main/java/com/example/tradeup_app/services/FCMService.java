package com.example.tradeup_app.services;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Handle FCM data message
            NotificationService notificationService = new NotificationService(this);
            notificationService.handleFCMMessage(remoteMessage);
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

            // For display notification, we can also create a local notification
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            NotificationService notificationService = new NotificationService(this);
            notificationService.showGeneralNotification(title != null ? title : "TradeUp",
                                                       body != null ? body : "You have a new notification");
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Send token to your server or save it to Firebase Realtime Database
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Save the FCM token to Firebase Realtime Database
        TokenService tokenService = new TokenService();
        tokenService.saveTokenToDatabase(token);
    }
}
