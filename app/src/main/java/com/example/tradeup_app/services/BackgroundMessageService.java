package com.example.tradeup_app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Message;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Background service để listen tin nhắn mới cho user hiện tại
 * Service này sẽ chạy ngầm và gửi notification khi có tin nhắn mới
 */
public class BackgroundMessageService extends Service {
    private static final String TAG = "BackgroundMessageService";

    private FirebaseManager firebaseManager;
    private NotificationService notificationService;
    private String currentUserId;
    private ChildEventListener messageListener;
    private Map<String, Long> lastMessageTimestamps = new HashMap<>();
    private Set<String> processedMessageIds = new HashSet<>(); // ✅ Track processed messages
    private boolean isInitialLoad = true; // ✅ Track initial load state

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🔥 BackgroundMessageService created");

        firebaseManager = FirebaseManager.getInstance();
        notificationService = new NotificationService(this);
        currentUserId = firebaseManager.getCurrentUserId();

        if (currentUserId != null) {
            startListeningForMessages();
        } else {
            Log.w(TAG, "❌ No current user - stopping service");
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🚀 BackgroundMessageService started");
        return START_STICKY; // Restart service if killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is an unbound service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "💀 BackgroundMessageService destroyed");
        stopListeningForMessages();
    }

    private void startListeningForMessages() {
        if (currentUserId == null) return;

        Log.d(TAG, "👂 Starting to listen for messages for user: " + currentUserId);

        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);

        // Listen for all new messages where current user is the receiver
        Query query = messagesRef.orderByChild("receiverId").equalTo(currentUserId);

        messageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Message message = dataSnapshot.getValue(Message.class);
                if (message != null) {
                    message.setId(dataSnapshot.getKey());

                    Log.d(TAG, "📨 New message detected for user: " + currentUserId);
                    Log.d(TAG, "   - Message ID: " + message.getId());
                    Log.d(TAG, "   - From: " + message.getSenderId());
                    Log.d(TAG, "   - Content: " + message.getContent());
                    Log.d(TAG, "   - Timestamp: " + message.getTimestamp());

                    // Check if this is a genuinely new message (not from initial load)
                    if (isNewMessage(message)) {
                        Log.d(TAG, "🔔 This is a NEW message - sending notification");
                        sendNotificationForMessage(message);
                    } else {
                        Log.d(TAG, "⭕ This is an old message - skipping notification");
                    }

                    // Update last message timestamp for this conversation
                    lastMessageTimestamps.put(message.getConversationId(), message.getTimestamp());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle message updates if needed
                Log.d(TAG, "🔄 Message updated: " + dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Handle message deletion if needed
                Log.d(TAG, "🗑️ Message removed: " + dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Not typically used for messages
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "❌ Database error: " + databaseError.getMessage());
            }
        };

        query.addChildEventListener(messageListener);
        Log.d(TAG, "✅ Message listener attached successfully");
    }

    private void stopListeningForMessages() {
        if (messageListener != null) {
            DatabaseReference messagesRef = firebaseManager.getDatabase()
                    .getReference(FirebaseManager.MESSAGES_NODE);
            Query query = messagesRef.orderByChild("receiverId").equalTo(currentUserId);
            query.removeEventListener(messageListener);
            messageListener = null;
            Log.d(TAG, "🛑 Message listener removed");
        }
    }

    /**
     * Check if this is a new message (not from initial load)
     * Logic: Message is new if it's timestamp is recent (within last 30 seconds)
     */
    private boolean isNewMessage(Message message) {
        long currentTime = System.currentTimeMillis();
        long messageTime = message.getTimestamp();
        long timeDifference = currentTime - messageTime;

        // Consider message as new if it's within last 30 seconds
        boolean isRecent = timeDifference < 30 * 1000; // 30 seconds

        Log.d(TAG, "⏰ Message time check:");
        Log.d(TAG, "   - Current time: " + currentTime);
        Log.d(TAG, "   - Message time: " + messageTime);
        Log.d(TAG, "   - Time difference: " + timeDifference + "ms");
        Log.d(TAG, "   - Is recent (< 30s): " + isRecent);

        return isRecent;
    }

    private void sendNotificationForMessage(Message message) {
        if (message.getSenderId() == null || message.getReceiverId() == null) {
            Log.w(TAG, "❌ Cannot send notification - missing sender or receiver ID");
            return;
        }

        // Don't send notification if sender is current user (shouldn't happen in this service)
        if (currentUserId.equals(message.getSenderId())) {
            Log.d(TAG, "⭕ Skip notification - message from current user");
            return;
        }

        // Check if this message has already been processed
        if (processedMessageIds.contains(message.getId())) {
            Log.d(TAG, "⭕ Skip notification - message already processed: " + message.getId());
            return;
        }

        Log.d(TAG, "🔔 Sending notification for message from: " + message.getSenderId());

        // Get sender name first
        MessagingService messagingService = new MessagingService();
        messagingService.getUserProfile(message.getSenderId(), new MessagingService.UserProfileCallback() {
            @Override
            public void onSuccess(String userName, String userAvatar) {
                message.setSenderName(userName);

                // Prepare notification content
                String notificationContent = message.getContent();
                if ("image".equals(message.getMessageType())) {
                    notificationContent = "📸 Image";
                }

                Log.d(TAG, "📨 Sending notification:");
                Log.d(TAG, "   - From: " + userName + " (" + message.getSenderId() + ")");
                Log.d(TAG, "   - To: " + message.getReceiverId());
                Log.d(TAG, "   - Content: " + notificationContent);

                // Send the notification
                notificationService.sendMessageNotification(
                    message.getConversationId(),
                    message.getSenderId(),
                    userName,
                    notificationContent,
                    message.getReceiverId()
                );

                // Mark this message as processed
                processedMessageIds.add(message.getId());
                Log.d(TAG, "✅ Message marked as processed: " + message.getId());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Failed to get sender profile: " + error);
                // Send notification with generic sender name
                notificationService.sendMessageNotification(
                    message.getConversationId(),
                    message.getSenderId(),
                    "Someone",
                    message.getContent(),
                    message.getReceiverId()
                );
            }
        });
    }

    /**
     * Static method to start the service
     */
    public static void startService(android.content.Context context) {
        Intent intent = new Intent(context, BackgroundMessageService.class);
        context.startService(intent);
        Log.d(TAG, "🚀 BackgroundMessageService start requested");
    }

    /**
     * Static method to stop the service
     */
    public static void stopService(android.content.Context context) {
        Intent intent = new Intent(context, BackgroundMessageService.class);
        context.stopService(intent);
        Log.d(TAG, "🛑 BackgroundMessageService stop requested");
    }
}
