package com.example.tradeup_app.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.models.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

    // Send image message
    public void sendImageMessage(String conversationId, String receiverId, Uri imageUri,
                               Context context, MessageCallback callback) {
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

            // Upload image first
            uploadImage(imageUri, new ImageUploadCallback() {
                @Override
                public void onImageUploaded(String imageUrl) {
                    String fileName = "image_" + System.currentTimeMillis() + ".jpg";
                    Message message = new Message(conversationId, currentUserId, receiverId, imageUrl, fileName);
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
                        callback.onMessageSent(messageId);
                    } else {
                        callback.onError("Failed to send message: " + task.getException().getMessage());
                    }
                });
    }

    private void updateConversationLastMessage(Message message) {
        DatabaseReference conversationRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .child(message.getConversationId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message.getContent());
        updates.put("lastMessageTime", message.getTimestamp());
        updates.put("updatedAt", System.currentTimeMillis());

        conversationRef.updateChildren(updates);
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
        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);

        Query query = messagesRef.orderByChild("conversationId").equalTo(conversationId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Message> messages = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null) {
                        message.setId(snapshot.getKey());
                        messages.add(message);
                    }
                }
                callback.onMessagesLoaded(messages);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
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

    // Send image message overload without context
    public void sendImageMessage(String conversationId, String receiverId, Uri imageUri,
                               ImageUploadCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Check if user is blocked
        checkIfBlocked(conversationId, currentUserId, (isBlocked) -> {
            if (isBlocked) {
                callback.onError("Cannot send message. User may have blocked you.");
                return;
            }

            // Upload image first
            uploadImage(imageUri, new ImageUploadCallback() {
                @Override
                public void onImageUploaded(String imageUrl) {
                    String fileName = "image_" + System.currentTimeMillis() + ".jpg";
                    Message message = new Message(conversationId, currentUserId, receiverId, imageUrl, fileName);
                    sendMessage(message, new MessageCallback() {
                        @Override
                        public void onMessagesLoaded(List<Message> messages) {}

                        @Override
                        public void onMessageSent(String messageId) {
                            callback.onImageUploaded(imageUrl);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                }

                @Override
                public void onUploadProgress(int progress) {
                    callback.onUploadProgress(progress);
                }

                @Override
                public void onError(String error) {
                    callback.onError("Failed to upload image: " + error);
                }
            });
        });
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

                // If it's a text message, replace content
                if ("text".equals(message.getMessageType()) || "emoji".equals(message.getMessageType())) {
                    updates.put("content", "This message was deleted");
                }

                messageRef.updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                callback.onMessageDeleted(messageId);
                            } else {
                                callback.onError("Failed to delete message: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Database error: " + databaseError.getMessage());
            }
        });
    }

    // Delete message for everyone (only if sent within last 7 days and user is sender)
    public void deleteMessageForEveryone(String messageId, MessageDeletionCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        DatabaseReference messageRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE)
                .child(messageId);

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

                // Check if user is the sender
                if (!currentUserId.equals(message.getSenderId())) {
                    callback.onError("You can only delete your own messages for everyone");
                    return;
                }

                // Check if message is within 7 days (7 * 24 * 60 * 60 * 1000 milliseconds)
                long sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L;
                long currentTime = System.currentTimeMillis();
                if (currentTime - message.getTimestamp() > sevenDaysInMillis) {
                    callback.onError("Messages older than 7 days cannot be deleted for everyone");
                    return;
                }

                // If it's an image message, delete from storage first
                if ("image".equals(message.getMessageType()) && message.getImageUrl() != null) {
                    deleteImageFromStorage(message.getImageUrl(), new ImageDeletionCallback() {
                        @Override
                        public void onImageDeleted() {
                            performCompleteMessageDeletion(messageId, message, callback);
                        }

                        @Override
                        public void onError(String error) {
                            Log.w(TAG, "Failed to delete image from storage: " + error);
                            // Continue with message deletion even if image deletion fails
                            performCompleteMessageDeletion(messageId, message, callback);
                        }
                    });
                } else {
                    performCompleteMessageDeletion(messageId, message, callback);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Database error: " + databaseError.getMessage());
            }
        });
    }

    // Helper method to perform complete message deletion
    private void performCompleteMessageDeletion(String messageId, Message message, MessageDeletionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("deletedBy", message.getSenderId());
        updates.put("deletedAt", System.currentTimeMillis());
        updates.put("deletedForEveryone", true);
        updates.put("content", "This message was deleted");

        // Clear image data if it was an image message
        if ("image".equals(message.getMessageType())) {
            updates.put("imageUrl", null);
            updates.put("imageFileName", null);
        }

        DatabaseReference messageRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE)
                .child(messageId);

        messageRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onMessageDeleted(messageId);
                    } else {
                        callback.onError("Failed to delete message: " +
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    // Interface for image deletion callback
    private interface ImageDeletionCallback {
        void onImageDeleted();
        void onError(String error);
    }

    // Delete image from Firebase Storage
    private void deleteImageFromStorage(String imageUrl, ImageDeletionCallback callback) {
        try {
            StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
            imageRef.delete()
                    .addOnSuccessListener(aVoid -> callback.onImageDeleted())
                    .addOnFailureListener(exception ->
                        callback.onError("Failed to delete image: " + exception.getMessage()));
        } catch (Exception e) {
            callback.onError("Invalid image URL: " + e.getMessage());
        }
    }

    // Delete multiple messages (bulk deletion)
    public void deleteMultipleMessages(List<String> messageIds, boolean deleteForEveryone, MessageDeletionCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        if (messageIds.isEmpty()) {
            callback.onError("No messages selected for deletion");
            return;
        }

        // Track successful deletions
        final int[] successCount = {0};
        final int[] failureCount = {0};
        final int totalMessages = messageIds.size();

        for (String messageId : messageIds) {
            MessageDeletionCallback individualCallback = new MessageDeletionCallback() {
                @Override
                public void onMessageDeleted(String deletedMessageId) {
                    successCount[0]++;
                    checkBulkDeletionComplete(successCount[0], failureCount[0], totalMessages, callback);
                }

                @Override
                public void onError(String error) {
                    failureCount[0]++;
                    Log.w(TAG, "Failed to delete message " + messageId + ": " + error);
                    checkBulkDeletionComplete(successCount[0], failureCount[0], totalMessages, callback);
                }
            };

            if (deleteForEveryone) {
                deleteMessageForEveryone(messageId, individualCallback);
            } else {
                deleteMessageForMe(messageId, individualCallback);
            }
        }
    }

    // Helper method to check if bulk deletion is complete
    private void checkBulkDeletionComplete(int successCount, int failureCount, int totalMessages, MessageDeletionCallback callback) {
        if (successCount + failureCount == totalMessages) {
            if (successCount == totalMessages) {
                callback.onMessageDeleted("All messages deleted successfully");
            } else if (successCount > 0) {
                callback.onError(successCount + " out of " + totalMessages + " messages deleted successfully");
            } else {
                callback.onError("Failed to delete all messages");
            }
        }
    }

    // Check if message can be deleted for everyone
    public boolean canDeleteForEveryone(Message message, String currentUserId) {
        if (message == null || currentUserId == null) {
            return false;
        }

        // Must be sender
        if (!currentUserId.equals(message.getSenderId())) {
            return false;
        }

        // Must be within 7 days
        long sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L;
        long currentTime = System.currentTimeMillis();
        return currentTime - message.getTimestamp() <= sevenDaysInMillis;
    }

    // Enhanced block checking - checks both users' blocked lists
    public void checkIfUserBlocked(String senderId, String receiverId, BlockCheckCallback callback) {
        // Check if sender is blocked by receiver
        firebaseManager.getDatabase()
                .getReference(FirebaseManager.USERS_NODE)
                .child(receiverId)
                .child("blockedUsers")
                .child(senderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Boolean isBlockedByReceiver = dataSnapshot.getValue(Boolean.class);
                        if (isBlockedByReceiver != null && isBlockedByReceiver) {
                            callback.onBlockCheckComplete(true);
                            return;
                        }

                        // Check if receiver is blocked by sender
                        firebaseManager.getDatabase()
                                .getReference(FirebaseManager.USERS_NODE)
                                .child(senderId)
                                .child("blockedUsers")
                                .child(receiverId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Boolean isBlockedBySender = dataSnapshot.getValue(Boolean.class);
                                        callback.onBlockCheckComplete(isBlockedBySender != null && isBlockedBySender);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        callback.onBlockCheckComplete(false);
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onBlockCheckComplete(false);
                    }
                });
    }
}
