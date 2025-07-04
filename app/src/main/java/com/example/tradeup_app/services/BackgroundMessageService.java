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

import java.util.HashSet;
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
    private Set<String> processedMessageIds = new HashSet<>();
    private boolean isInitialLoad = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🔥 BackgroundMessageService created");

        firebaseManager = FirebaseManager.getInstance();
        notificationService = new NotificationService(this);
        currentUserId = firebaseManager.getCurrentUserId();

        Log.d(TAG, "🔍 Service initialization:");
        Log.d(TAG, "   - Current User ID: " + currentUserId);
        Log.d(TAG, "   - FirebaseManager: " + (firebaseManager != null ? "OK" : "NULL"));
        Log.d(TAG, "   - NotificationService: " + (notificationService != null ? "OK" : "NULL"));

        if (currentUserId != null) {
            Log.d(TAG, "👂 Starting message listener for user: " + currentUserId);
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

                    // Check if this is a genuinely new message
                    if (isNewMessage(message)) {
                        Log.d(TAG, "🔔 This is a NEW message - sending notification");
                        sendNotificationForMessage(message);
                    } else {
                        Log.d(TAG, "⭕ This is an old message - skipping notification");
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "🔄 Message updated: " + dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
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
     */
    private boolean isNewMessage(Message message) {
        String messageId = message.getId();
        long currentTime = System.currentTimeMillis();
        long messageTime = message.getTimestamp();
        long timeDifference = currentTime - messageTime;

        // Check if message was already processed
        boolean alreadyProcessed = processedMessageIds.contains(messageId);

        // Message is new if: not processed AND recent (within 2 minutes)
        boolean isRecent = timeDifference < 120 * 1000; // 2 minutes

        Log.d(TAG, "⏰ Message analysis:");
        Log.d(TAG, "   - Message ID: " + messageId);
        Log.d(TAG, "   - Time difference: " + timeDifference + "ms (" + (timeDifference/1000) + "s)");
        Log.d(TAG, "   - Already processed: " + alreadyProcessed);
        Log.d(TAG, "   - Is recent (< 2min): " + isRecent);
        Log.d(TAG, "   - Is initial load: " + isInitialLoad);

        // Message is new if not processed + recent + not initial load
        boolean isNew = !alreadyProcessed && isRecent && !isInitialLoad;
        Log.d(TAG, "   - 🎯 RESULT: Is new message: " + isNew);

        // Mark initial load as complete after first check
        if (isInitialLoad) {
            isInitialLoad = false;
            Log.d(TAG, "🎯 Initial load completed - future messages will be treated as new");
        }

        return isNew;
    }

    private void sendNotificationForMessage(Message message) {
        if (message.getSenderId() == null || message.getReceiverId() == null) {
            Log.w(TAG, "❌ Cannot send notification - missing sender or receiver ID");
            return;
        }

        // Don't send notification if sender is current user
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

                // Mark as processed even if name lookup failed
                processedMessageIds.add(message.getId());
            }
        });
    }

    /**
     * Static method to start the service
     */
    public static void startService(android.content.Context context) {
        Log.d(TAG, "🚀 BackgroundMessageService start requested from: " + context.getClass().getSimpleName());

        try {
            Intent intent = new Intent(context, BackgroundMessageService.class);
            context.startService(intent);
            Log.d(TAG, "✅ Service start command sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start service: " + e.getMessage());
        }
    }

    /**
     * Static method to stop the service
     */
    public static void stopService(android.content.Context context) {
        Log.d(TAG, "🛑 BackgroundMessageService stop requested from: " + context.getClass().getSimpleName());

        try {
            Intent intent = new Intent(context, BackgroundMessageService.class);
            context.stopService(intent);
            Log.d(TAG, "✅ Service stop command sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to stop service: " + e.getMessage());
        }
    }
}
