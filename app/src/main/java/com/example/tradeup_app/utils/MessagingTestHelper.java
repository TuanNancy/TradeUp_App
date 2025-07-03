package com.example.tradeup_app.utils;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.models.Message;
import com.google.firebase.database.DatabaseReference;

public class MessagingTestHelper {

    private static final String TAG = "MessagingTestHelper";
    private final FirebaseManager firebaseManager;

    public MessagingTestHelper() {
        this.firebaseManager = FirebaseManager.getInstance();
    }

    /**
     * Tạo một conversation test để kiểm tra messaging system
     */
    public void createTestConversation(String productId, String productTitle, String buyerId, String sellerId) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            android.util.Log.e(TAG, "User not authenticated");
            return;
        }

        // Tạo conversation ID
        DatabaseReference conversationsRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE);
        String conversationId = conversationsRef.push().getKey();

        // Tạo conversation object
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setProductId(productId);
        conversation.setProductTitle(productTitle);
        conversation.setProductImageUrl("https://via.placeholder.com/150"); // Placeholder image
        conversation.setBuyerId(buyerId);
        conversation.setSellerId(sellerId);
        conversation.setBuyerName("Test Buyer");
        conversation.setSellerName("Test Seller");
        conversation.setLastMessage("Hello! I'm interested in this product.");
        conversation.setLastMessageTime(System.currentTimeMillis());
        conversation.setUnreadCount(1);
        conversation.setActive(true);

        // Lưu conversation
        conversationsRef.child(conversationId).setValue(conversation)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d(TAG, "Test conversation created successfully: " + conversationId);

                    // Tạo message đầu tiên
                    createTestMessage(conversationId, buyerId, sellerId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to create test conversation", e);
                });
    }

    private void createTestMessage(String conversationId, String senderId, String receiverId) {
        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);

        String messageId = messagesRef.push().getKey();

        Message message = new Message(conversationId, senderId, receiverId, "Hello! I'm interested in this product.");
        message.setId(messageId);
        message.setSenderName("Test User");

        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d(TAG, "Test message created successfully: " + messageId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to create test message", e);
                });
    }

    /**
     * Xóa tất cả test data
     */
    public void clearTestData() {
        DatabaseReference conversationsRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE);
        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);

        // Note: Trong thực tế, bạn nên query specific test data thay vì clear all
        android.util.Log.d(TAG, "Clear test data method called - implement specific logic as needed");
    }
}
