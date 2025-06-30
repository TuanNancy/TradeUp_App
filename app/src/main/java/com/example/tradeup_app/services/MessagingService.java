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

    public interface BlockCallback {
        void onUserBlocked(boolean success);
        void onUserUnblocked(boolean success);
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

        // Check if user is blocked
        checkIfBlocked(conversationId, currentUserId, (isBlocked) -> {
            if (isBlocked) {
                callback.onError("Cannot send message. User may have blocked you.");
                return;
            }

            Message message = new Message(conversationId, currentUserId, receiverId, content);
            sendMessage(message, callback);
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

    // Block user
    public void blockUser(String conversationId, String userIdToBlock, BlockCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        DatabaseReference conversationRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .child(conversationId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("blockedUsers/" + userIdToBlock, true);

        conversationRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onUserBlocked(true);
                    } else {
                        callback.onError("Failed to block user: " + task.getException().getMessage());
                    }
                });
    }

    // Unblock user
    public void unblockUser(String conversationId, String userIdToUnblock, BlockCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        DatabaseReference conversationRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .child(conversationId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("blockedUsers/" + userIdToUnblock, null);

        conversationRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
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
    private interface BlockCheckCallback {
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
}
