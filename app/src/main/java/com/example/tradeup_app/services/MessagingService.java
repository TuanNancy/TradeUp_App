package com.example.tradeup_app.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.models.Message;
import com.example.tradeup_app.utils.ImageUploadManager;
import com.example.tradeup_app.utils.NotificationManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessagingService {
    private static final String TAG = "MessagingService";
    private final FirebaseManager firebaseManager;
    private final FirebaseStorage storage;
    private Context context; // Add context for notifications

    // Node references
    private static final String BLOCKED_USERS_NODE = "blocked_users";
    private static final String MESSAGE_IMAGES_PATH = "message_images";

    public interface MessageCallback {
        void onMessagesLoaded(List<Message> messages);
        void onMessageSent(String messageId);
        void onError(String error);
    }

    public interface ConversationCallback {
        void onConversationsLoaded(List<Conversation> conversations);
        void onConversationCreated(String conversationId);
        void onError(String error);
    }

    public interface ImageUploadCallback {
        void onImageUploaded(String imageUrl);
        void onUploadProgress(int progress);
        void onError(String error);
    }

    public interface UserProfileCallback {
        void onSuccess(String userName, String userAvatar);
        void onError(String error);
    }

    public interface BlockCallback {
        void onUserBlocked(boolean success);
        void onUserUnblocked(boolean success);
        void onError(String error);
    }

    // Enhanced Message Deletion Interface
    public interface MessageDeletionCallback {
        void onMessageDeleted(String messageId);
        void onError(String error);
    }

    public MessagingService() {
        this.firebaseManager = FirebaseManager.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    // Add constructor that accepts context for notifications
    public MessagingService(Context context) {
        this.firebaseManager = FirebaseManager.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.context = context;
    }

    // Create or get existing conversation
    public void createOrGetConversation(String productId, String buyerId, String sellerId,
                                      String productTitle, String productImageUrl, ConversationCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Check if conversation already exists
        DatabaseReference conversationsRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE);

        Query query = conversationsRef.orderByChild("productId").equalTo(productId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Conversation existingConversation = null;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Conversation conversation = snapshot.getValue(Conversation.class);
                    if (conversation != null &&
                        ((conversation.getBuyerId().equals(buyerId) && conversation.getSellerId().equals(sellerId)) ||
                         (conversation.getBuyerId().equals(sellerId) && conversation.getSellerId().equals(buyerId)))) {
                        existingConversation = conversation;
                        existingConversation.setId(snapshot.getKey());
                        break;
                    }
                }

                if (existingConversation != null) {
                    callback.onConversationCreated(existingConversation.getId());
                } else {
                    // Create new conversation
                    createNewConversation(productId, buyerId, sellerId, productTitle, productImageUrl, callback);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }

    private void createNewConversation(String productId, String buyerId, String sellerId,
                                     String productTitle, String productImageUrl, ConversationCallback callback) {
        DatabaseReference conversationsRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE);

        String conversationId = conversationsRef.push().getKey();

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setProductId(productId);
        conversation.setProductTitle(productTitle);
        conversation.setProductImageUrl(productImageUrl);
        conversation.setBuyerId(buyerId);
        conversation.setSellerId(sellerId);
        conversation.setLastMessage("Conversation started");
        conversation.setLastMessageTime(System.currentTimeMillis());

        conversationsRef.child(conversationId).setValue(conversation)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onConversationCreated(conversationId);
                    } else {
                        callback.onError("Failed to create conversation: " + task.getException().getMessage());
                    }
                });
    }

    // Send text message
    public void sendTextMessage(String conversationId, String receiverId, String content, MessageCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Check if current user is blocked by receiver or if receiver is blocked by current user
        checkIfUserBlocked(currentUserId, receiverId, (isBlocked) -> {
            if (isBlocked) {
                callback.onError("Cannot send message. This user has been blocked.");
                return;
            }

            // Load sender name first, then create message
            loadSenderNameAndCreateMessage(conversationId, currentUserId, receiverId, content, null, null, callback);
        });
    }


    // Send image message using Cloudinary
    public void sendImageMessage(String conversationId, String receiverId, Uri imageUri,
                               ImageUploadCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        Log.d(TAG, "Starting image message send for conversation: " + conversationId);

        // Check if user is blocked
        checkIfUserBlocked(currentUserId, receiverId, (isBlocked) -> {
            if (isBlocked) {
                callback.onError("Cannot send message. This user has been blocked.");
                return;
            }

            // Upload image to Cloudinary first
            ImageUploadManager.uploadChatImage(imageUri, null, new ImageUploadManager.ChatImageUploadCallback() {
                @Override
                public void onStart() {
                    Log.d(TAG, "Image upload started");
                }

                @Override
                public void onProgress(int progress) {
                    Log.d(TAG, "Image upload progress: " + progress + "%");
                    callback.onUploadProgress(progress);
                }

                @Override
                public void onSuccess(String imageUrl) {
                    Log.d(TAG, "Image uploaded successfully: " + imageUrl);

                    // Create image message using the correct constructor
                    String fileName = "chat_image_" + System.currentTimeMillis() + ".jpg";
                    Message message = new Message(conversationId, currentUserId, receiverId, imageUrl, fileName);

                    // Get sender name and send message
                    getUserProfile(currentUserId, new UserProfileCallback() {
                        @Override
                        public void onSuccess(String userName, String userAvatar) {
                            message.setSenderName(userName);

                            sendMessage(message, new MessageCallback() {
                                @Override
                                public void onMessagesLoaded(List<Message> messages) {}

                                @Override
                                public void onMessageSent(String messageId) {
                                    Log.d(TAG, "Image message sent successfully with ID: " + messageId);
                                    callback.onImageUploaded(imageUrl);
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Failed to send image message: " + error);
                                    callback.onError("Failed to send message: " + error);
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Failed to load sender profile: " + error);
                            callback.onError("Failed to load sender profile: " + error);
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Image upload failed: " + e.getMessage(), e);
                    callback.onError("Failed to upload image: " + e.getMessage());
                }
            });
        });
    }

    private void sendMessage(Message message, MessageCallback callback) {
        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);

        String messageId = messagesRef.push().getKey();
        message.setId(messageId);

        messagesRef.child(messageId).setValue(message)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update conversation's last message
                        updateConversationLastMessage(message);

                        // ✅ SỬA: KHÔNG gửi notification ở đây nữa
                        // Notification sẽ được gửi từ listenForMessages khi detect tin nhắn mới
                        Log.d(TAG, "📤 Message sent successfully, notification will be handled by listeners");

                        callback.onMessageSent(messageId);
                    } else {
                        callback.onError("Failed to send message: " + task.getException().getMessage());
                    }
                });
    }

    // Send notification for new message
    private void sendMessageNotification(Message message) {
        if (message.getReceiverId() == null || message.getSenderId() == null) {
            Log.w(TAG, "Cannot send notification - missing receiver or sender ID");
            return;
        }

        // ✅ SỬA: Logic hoàn toàn mới - kiểm tra và gửi thông báo chính xác
        NotificationService notificationService = new NotificationService(context);

        // Get sender name for notification
        String senderName = message.getSenderName();
        if (senderName == null || senderName.isEmpty()) {
            senderName = "Someone";
        }

        // Prepare message content for notification
        String notificationContent = message.getContent();
        if ("image".equals(message.getMessageType())) {
            notificationContent = "📸 Image";
        }

        Log.d(TAG, "📨 NOTIFICATION LOGIC CHECK:");
        Log.d(TAG, "   - Sender ID: " + message.getSenderId() + " (Name: " + senderName + ")");
        Log.d(TAG, "   - Receiver ID: " + message.getReceiverId());
        Log.d(TAG, "   - Message: " + notificationContent);
        Log.d(TAG, "   - Conversation ID: " + message.getConversationId());

        // ✅ GỬI THÔNG BÁO CHO NGƯỜI NHẬN - không gửi cho người gửi
        // Đây là logic chính: senderId là excludeUserId để tránh tự gửi cho mình
        notificationService.sendMessageNotification(
            message.getConversationId(),      // conversationId
            message.getSenderId(),            // senderId (người gửi)
            senderName,                       // senderName
            notificationContent,              // messageContent
            message.getReceiverId()           // receiverId (người nhận - sẽ nhận thông báo)
        );

        Log.d(TAG, "📨 Notification request sent to NotificationService");
    }

    private void updateConversationLastMessage(Message message) {
        DatabaseReference conversationRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .child(message.getConversationId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message.getContent());
        updates.put("lastMessageTime", message.getTimestamp());
        updates.put("lastMessageSenderId", message.getSenderId()); // Thêm thông tin người gửi
        updates.put("updatedAt", System.currentTimeMillis());

        // QUAN TRỌNG: Tự động cập nhật lastReadTimes cho người gửi
        // Điều này đảm bảo người gửi luôn thấy conversation là "đã đọc"
        String senderLastReadPath = "lastReadTimes/" + message.getSenderId();
        updates.put(senderLastReadPath, System.currentTimeMillis());

        conversationRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Conversation updated successfully. Sender marked as read.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update conversation", e);
                });
    }

    // Upload image to Firebase Storage
    private void uploadImage(Uri imageUri, ImageUploadCallback callback) {
        String fileName = MESSAGE_IMAGES_PATH + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storage.getReference().child(fileName);

        UploadTask uploadTask = imageRef.putFile(imageUri);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onUploadProgress((int) progress);
        }).addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                callback.onImageUploaded(uri.toString());
            }).addOnFailureListener(e -> {
                callback.onError("Failed to get download URL: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            callback.onError("Upload failed: " + e.getMessage());
        });
    }

    // Block user - Enhanced version
    public void blockUser(String conversationId, String userIdToBlock, BlockCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Store block information in current user's profile
        DatabaseReference userRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.USERS_NODE)
                .child(currentUserId)
                .child("blockedUsers")
                .child(userIdToBlock);

        userRef.setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Also update conversation to maintain compatibility
                        DatabaseReference conversationRef = firebaseManager.getDatabase()
                                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                                .child(conversationId);

                        Map<String, Object> conversationUpdates = new HashMap<>();
                        conversationUpdates.put("blockedUsers/" + userIdToBlock, true);

                        conversationRef.updateChildren(conversationUpdates)
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        callback.onUserBlocked(true);
                                    } else {
                                        // Even if conversation update fails, user is still blocked
                                        callback.onUserBlocked(true);
                                    }
                                });
                    } else {
                        callback.onError("Failed to block user: " + task.getException().getMessage());
                    }
                });
    }

    // Unblock user - Enhanced version
    public void unblockUser(String conversationId, String userIdToUnblock, BlockCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Remove from current user's blocked list
        DatabaseReference userRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.USERS_NODE)
                .child(currentUserId)
                .child("blockedUsers")
                .child(userIdToUnblock);

        userRef.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Also update conversation to maintain compatibility
                        if (conversationId != null) {
                            DatabaseReference conversationRef = firebaseManager.getDatabase()
                                    .getReference(FirebaseManager.CONVERSATIONS_NODE)
                                    .child(conversationId);

                            Map<String, Object> conversationUpdates = new HashMap<>();
                            conversationUpdates.put("blockedUsers/" + userIdToUnblock, null);

                            conversationRef.updateChildren(conversationUpdates);
                        }
                        callback.onUserUnblocked(true);
                    } else {
                        callback.onError("Failed to unblock user: " + task.getException().getMessage());
                    }
                });
    }

    // Report conversation
    public void reportConversation(String conversationId, String reason, OnCompleteListener<Void> callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            // Create a failed task to pass to the callback
            Exception exception = new Exception("User not authenticated");
            Task<Void> failedTask = com.google.android.gms.tasks.Tasks.forException(exception);
            callback.onComplete(failedTask);
            return;
        }

        DatabaseReference conversationRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .child(conversationId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isReported", true);
        updates.put("reportedBy", currentUserId);
        updates.put("reportReason", reason);
        updates.put("reportedAt", System.currentTimeMillis());

        conversationRef.updateChildren(updates).addOnCompleteListener(callback);
    }

    // Report message
    public void reportMessage(String messageId, String reason, OnCompleteListener<Void> callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }

        DatabaseReference messageRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE)
                .child(messageId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isReported", true);
        updates.put("reportReason", reason);

        messageRef.updateChildren(updates).addOnCompleteListener(callback);
    }

    // Check if user is blocked
    private void checkIfBlocked(String conversationId, String userId, BlockCheckCallback callback) {
        DatabaseReference conversationRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .child(conversationId);

        conversationRef.child("blockedUsers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isBlocked = dataSnapshot.child(userId).exists();
                callback.onBlockCheckComplete(isBlocked);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onBlockCheckComplete(false); // Assume not blocked on error
            }
        });
    }

    // Listen for messages in real-time
    public void listenForMessages(String conversationId, MessageCallback callback) {
        android.util.Log.d("MessagingService", "🔍 listenForMessages called for conversation: " + conversationId);

        if (conversationId == null || conversationId.isEmpty()) {
            android.util.Log.e("MessagingService", "❌ Invalid conversationId: " + conversationId);
            callback.onError("Invalid conversation ID");
            return;
        }

        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);

        android.util.Log.d("MessagingService", "📡 Setting up query for messages at path: " + FirebaseManager.MESSAGES_NODE);
        android.util.Log.d("MessagingService", "🔎 Query filter: conversationId == " + conversationId);

        Query query = messagesRef.orderByChild("conversationId").equalTo(conversationId);

        // ✅ SỬA: Sử dụng ValueEventListener để load initial messages, sau đó ChildEventListener cho real-time
        query.addValueEventListener(new ValueEventListener() {
            private boolean isFirstLoad = true;
            private List<String> loadedMessageIds = new ArrayList<>();

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                android.util.Log.d("MessagingService", "📨 onDataChange triggered - isFirstLoad: " + isFirstLoad);

                List<Message> messages = new ArrayList<>();
                List<String> currentMessageIds = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null) {
                        message.setId(snapshot.getKey());
                        messages.add(message);
                        currentMessageIds.add(snapshot.getKey());

                        android.util.Log.d("MessagingService", "✅ Message loaded: " + message.getMessageType() +
                                          ", ConvId: " + message.getConversationId() +
                                          ", OfferId: " + message.getOfferId());

                        // ✅ SỬA: Chỉ gửi thông báo cho tin nhắn MỚI (không có trong loadedMessageIds)
                        if (!isFirstLoad && context != null && !loadedMessageIds.contains(snapshot.getKey())) {
                            String currentUserId = firebaseManager.getCurrentUserId();

                            android.util.Log.d("MessagingService", "🔔 NEW MESSAGE DETECTED: " + snapshot.getKey());
                            android.util.Log.d("MessagingService", "   - Current User: " + currentUserId);
                            android.util.Log.d("MessagingService", "   - Message Sender: " + message.getSenderId());

                            // Chỉ gửi thông báo nếu tin nhắn không phải từ user hiện tại
                            if (currentUserId != null && !currentUserId.equals(message.getSenderId())) {
                                android.util.Log.d("MessagingService", "🔔 Sending notification for new message from: " + message.getSenderId());
                                sendMessageNotification(message);
                            } else {
                                android.util.Log.d("MessagingService", "⭕ Skip notification - message from current user: " + message.getSenderId());
                            }
                        }
                    }
                }

                // Sort messages by timestamp
                messages.sort((m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                if (isFirstLoad) {
                    android.util.Log.d("MessagingService", "🎯 Initial load completed with " + messages.size() + " messages");
                    isFirstLoad = false;
                } else {
                    android.util.Log.d("MessagingService", "🔄 Real-time update with " + messages.size() + " messages");
                }

                // Update loaded message IDs for next comparison
                loadedMessageIds.clear();
                loadedMessageIds.addAll(currentMessageIds);

                // Callback with updated list
                callback.onMessagesLoaded(new ArrayList<>(messages));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                android.util.Log.e("MessagingService", "❌ Database error in listenForMessages: " + databaseError.getMessage());
                callback.onError(databaseError.getMessage());
            }
        });

        android.util.Log.d("MessagingService", "🎪 ValueEventListener attached successfully");
    }

    // Load user conversations
    public void loadUserConversations(ConversationCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        DatabaseReference conversationsRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE);

        conversationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Conversation> conversations = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Conversation conversation = snapshot.getValue(Conversation.class);
                    if (conversation != null &&
                        (conversation.getBuyerId().equals(currentUserId) ||
                         conversation.getSellerId().equals(currentUserId))) {
                        conversation.setId(snapshot.getKey());
                        conversations.add(conversation);
                    }
                }
                callback.onConversationsLoaded(conversations);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }

    // Mark messages as read
    public void markMessagesAsRead(String conversationId, String senderId) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) return;

        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);

        Query query = messagesRef.orderByChild("conversationId").equalTo(conversationId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null &&
                        message.getSenderId().equals(senderId) &&
                        message.getReceiverId().equals(currentUserId) &&
                        !message.isRead()) {

                        snapshot.getRef().child("read").setValue(true);
                        snapshot.getRef().child("readAt").setValue(System.currentTimeMillis());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to mark messages as read: " + databaseError.getMessage());
            }
        });
    }

    // Delete conversation
    public void deleteConversation(String conversationId, ConversationCallback callback) {
        DatabaseReference conversationRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .child(conversationId);

        conversationRef.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Also delete all messages in this conversation
                        deleteConversationMessages(conversationId);
                        callback.onConversationCreated("deleted"); // Reusing callback
                    } else {
                        callback.onError("Failed to delete conversation: " + task.getException().getMessage());
                    }
                });
    }

    private void deleteConversationMessages(String conversationId) {
        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);

        Query query = messagesRef.orderByChild("conversationId").equalTo(conversationId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to delete conversation messages: " + databaseError.getMessage());
            }
        });
    }

    // Cleanup method
    public void cleanup() {
        // Remove any listeners if needed
        Log.d(TAG, "MessagingService cleanup completed");
    }

    // Interface for block checking
    public interface BlockCheckCallback {
        void onBlockCheckComplete(boolean isBlocked);
    }

    // Get user profile information
    public void getUserProfile(String userId, UserProfileCallback callback) {
        if (userId == null) {
            callback.onError("User ID is null");
            return;
        }

        DatabaseReference userRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.USERS_NODE)
                .child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String userName = dataSnapshot.child("name").getValue(String.class);
                    String userAvatar = dataSnapshot.child("profileImageUrl").getValue(String.class);

                    if (userName == null) {
                        userName = dataSnapshot.child("fullName").getValue(String.class);
                    }
                    if (userName == null) {
                        userName = dataSnapshot.child("username").getValue(String.class);
                    }
                    if (userName == null) {
                        userName = "User";
                    }

                    callback.onSuccess(userName, userAvatar);
                } else {
                    callback.onError("User not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Database error: " + databaseError.getMessage());
            }
        });
    }

    // Load sender name and create message
    private void loadSenderNameAndCreateMessage(String conversationId, String senderId, String receiverId,
                                               String content, Uri imageUri, ImageUploadCallback imageCallback,
                                               MessageCallback callback) {
        // First, get the sender's name
        getUserProfile(senderId, new UserProfileCallback() {
            @Override
            public void onSuccess(String userName, String userAvatar) {
                // Create the message with the sender's name
                Message message = new Message(conversationId, senderId, receiverId, content);
                message.setSenderName(userName);

                // If there's an image, upload it
                if (imageUri != null) {
                    uploadImage(imageUri, new ImageUploadCallback() {
                        @Override
                        public void onImageUploaded(String imageUrl) {
                            String fileName = "image_" + System.currentTimeMillis() + ".jpg";
                            message.setContent(imageUrl);
                            message.setImageFileName(fileName);
                            message.setMessageType("image");
                            sendMessage(message, callback);
                        }

                        @Override
                        public void onUploadProgress(int progress) {
                            // Could show progress to user if needed
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError("Failed to upload image: " + error);
                        }
                    });
                } else {
                    // No image, just send the message
                    sendMessage(message, callback);
                }
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to load sender profile: " + error);
            }
        });
    }

    // Delete message for current user only (soft delete)
    public void deleteMessageForMe(String messageId, MessageDeletionCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        DatabaseReference messageRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE)
                .child(messageId);

        // First get the message to check ownership and update appropriately
        messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    callback.onError("Message not found");
                    return;
                }

                Message message = dataSnapshot.getValue(Message.class);
                if (message == null) {
                    callback.onError("Failed to load message data");
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("isDeleted", true);
                updates.put("deletedBy", currentUserId);
                updates.put("deletedAt", System.currentTimeMillis());

                messageRef.updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                callback.onMessageDeleted(messageId);
                            } else {
                                callback.onError("Failed to delete message: " + task.getException().getMessage());
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Database error: " + databaseError.getMessage());
            }
        });
    }

    // Check if user can delete message for everyone
    public boolean canDeleteForEveryone(Message message, String currentUserId) {
        if (message == null || currentUserId == null) {
            return false;
        }

        // Only the sender can delete for everyone
        if (!currentUserId.equals(message.getSenderId())) {
            return false;
        }

        // Check if message is too old (e.g., more than 24 hours)
        long messageAge = System.currentTimeMillis() - message.getTimestamp();
        long maxDeleteAge = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

        return messageAge <= maxDeleteAge;
    }

    // Delete message for everyone
    public void deleteMessageForEveryone(String messageId, MessageDeletionCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        DatabaseReference messageRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE)
                .child(messageId);

        // First get the message to check if user can delete it
        messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    callback.onError("Message not found");
                    return;
                }

                Message message = dataSnapshot.getValue(Message.class);
                if (message == null) {
                    callback.onError("Failed to load message data");
                    return;
                }

                // Check if current user is the sender
                if (!currentUserId.equals(message.getSenderId())) {
                    callback.onError("You can only delete your own messages");
                    return;
                }

                // Check if message is not too old
                if (!canDeleteForEveryone(message, currentUserId)) {
                    callback.onError("This message is too old to delete for everyone");
                    return;
                }

                // Update message to show it's deleted for everyone
                Map<String, Object> updates = new HashMap<>();
                updates.put("isDeleted", true);
                updates.put("deletedForEveryone", true);
                updates.put("deletedBy", currentUserId);
                updates.put("deletedAt", System.currentTimeMillis());
                updates.put("content", "This message was deleted");

                // If it's an image message, remove image data
                if ("image".equals(message.getMessageType())) {
                    updates.put("imageUrl", null);
                    updates.put("imageFileName", null);
                }

                messageRef.updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                callback.onMessageDeleted(messageId);
                            } else {
                                callback.onError("Failed to delete message: " + task.getException().getMessage());
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Database error: " + databaseError.getMessage());
            }
        });
    }

    // Check if user is blocked - Enhanced version that checks both directions
    public void checkIfUserBlocked(String senderId, String receiverId, BlockCheckCallback callback) {
        if (senderId == null || receiverId == null) {
            callback.onBlockCheckComplete(false);
            return;
        }

        // Check if senderId has blocked receiverId
        DatabaseReference senderBlockedRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.USERS_NODE)
                .child(senderId)
                .child("blockedUsers")
                .child(receiverId);

        senderBlockedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Sender has blocked receiver
                    callback.onBlockCheckComplete(true);
                    return;
                }

                // Check if receiverId has blocked senderId
                DatabaseReference receiverBlockedRef = firebaseManager.getDatabase()
                        .getReference(FirebaseManager.USERS_NODE)
                        .child(receiverId)
                        .child("blockedUsers")
                        .child(senderId);

                receiverBlockedRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean isBlocked = dataSnapshot.exists();
                        callback.onBlockCheckComplete(isBlocked);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onBlockCheckComplete(false); // Assume not blocked on error
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onBlockCheckComplete(false); // Assume not blocked on error
            }
        });
    }
}
