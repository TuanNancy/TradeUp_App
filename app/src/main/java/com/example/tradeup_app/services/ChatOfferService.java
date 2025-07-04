package com.example.tradeup_app.services;

import android.util.Log;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.ChatOffer;
import com.example.tradeup_app.models.Message;
import com.example.tradeup_app.utils.NotificationManager;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to handle chat-integrated offers
 */
public class ChatOfferService {
    private static final String TAG = "ChatOfferService";
    private static final String CHAT_OFFERS_NODE = "chat_offers";

    private FirebaseManager firebaseManager;
    private MessagingService messagingService;
    private NotificationManager notificationManager;

    public ChatOfferService() {
        this.firebaseManager = FirebaseManager.getInstance();
        this.messagingService = new MessagingService();
        this.notificationManager = NotificationManager.getInstance(null);
    }

    /**
     * Send an offer through chat
     */
    public void sendOfferInChat(String conversationId, String productId, String productTitle,
                               String senderId, String senderName, String receiverId,
                               double originalPrice, double offerPrice, String message,
                               ChatOfferCallback callback) {

        Log.d(TAG, "Sending offer in chat: " + offerPrice + " for " + productTitle);

        // Create ChatOffer object
        ChatOffer chatOffer = new ChatOffer(conversationId, productId, productTitle,
                senderId, senderName, receiverId, originalPrice, offerPrice, message);

        // Generate offer ID
        String offerId = firebaseManager.getDatabase()
                .getReference(CHAT_OFFERS_NODE).push().getKey();

        if (offerId == null) {
            callback.onError("Failed to generate offer ID");
            return;
        }

        chatOffer.setId(offerId);

        // Save offer to Firebase
        firebaseManager.getDatabase()
                .getReference(CHAT_OFFERS_NODE)
                .child(offerId)
                .setValue(chatOffer)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Chat offer saved successfully");

                    // Create and send offer message
                    sendOfferMessage(conversationId, senderId, receiverId, chatOffer, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save chat offer", e);
                    callback.onError("Failed to save offer: " + e.getMessage());
                });
    }

    /**
     * Send offer message in chat
     */
    private void sendOfferMessage(String conversationId, String senderId, String receiverId,
                                 ChatOffer chatOffer, ChatOfferCallback callback) {

        // Create special offer message with proper fields
        Message offerMessage = new Message();
        offerMessage.setConversationId(conversationId);
        offerMessage.setSenderId(senderId);
        offerMessage.setReceiverId(receiverId);
        offerMessage.setMessageType("CHAT_OFFER"); // Set proper message type for recognition
        offerMessage.setOfferId(chatOffer.getId());
        offerMessage.setOfferAmount(chatOffer.getOfferPrice());
        offerMessage.setOriginalPrice(chatOffer.getOriginalPrice());
        offerMessage.setOfferStatus("PENDING");
        offerMessage.setOfferMessage(chatOffer.getMessage());
        offerMessage.setProductId(chatOffer.getProductId());

        // Format offer content with structured data for easy parsing
        String offerContent = String.format("💰 Offer: %s VND (Original: %s VND)\n📦 %s\n💬 %s",
                formatVNDPrice(chatOffer.getOfferPrice()),
                formatVNDPrice(chatOffer.getOriginalPrice()),
                chatOffer.getProductTitle(),
                chatOffer.getMessage() != null ? chatOffer.getMessage() : "No additional message");
        offerMessage.setContent(offerContent);

        // Generate message ID - FIX: Use correct path
        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);
        String messageId = messagesRef.push().getKey();

        if (messageId == null) {
            callback.onError("Failed to generate message ID");
            return;
        }

        offerMessage.setId(messageId);

        // Save offer message directly to Firebase - FIX: Use correct path
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", messageId);
        messageData.put("conversationId", conversationId);
        messageData.put("senderId", senderId);
        messageData.put("receiverId", receiverId);
        messageData.put("content", offerContent);
        messageData.put("messageType", "CHAT_OFFER");
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("isRead", false);
        messageData.put("offerId", chatOffer.getId());
        messageData.put("offerAmount", chatOffer.getOfferPrice());
        messageData.put("originalPrice", chatOffer.getOriginalPrice());
        messageData.put("offerStatus", "PENDING");
        messageData.put("offerMessage", chatOffer.getMessage());
        messageData.put("productId", chatOffer.getProductId());

        // Save to correct Firebase path
        messagesRef.child(messageId).setValue(messageData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Offer message sent successfully");
                    callback.onOfferSent(chatOffer);

                    // Update conversation's last message
                    updateConversationLastMessage(conversationId, offerContent);

                    // Send notification to receiver
                    sendOfferNotification(receiverId, chatOffer);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send offer message", e);
                    callback.onError("Failed to send offer message: " + e.getMessage());
                });
    }

    // Helper method to format VND price
    private String formatVNDPrice(double price) {
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        return formatter.format(price);
    }

    /**
     * Respond to an offer (Accept/Decline/Counter)
     */
    public void respondToOffer(String offerId, String response, double counterOfferPrice,
                              String counterMessage, String responderId, ChatOfferCallback callback) {

        Log.d(TAG, "Responding to offer " + offerId + " with: " + response);

        DatabaseReference offerRef = firebaseManager.getDatabase()
                .getReference(CHAT_OFFERS_NODE)
                .child(offerId);

        // Get the original offer first
        offerRef.get().addOnSuccessListener(snapshot -> {
            ChatOffer originalOffer = snapshot.getValue(ChatOffer.class);
            if (originalOffer == null) {
                callback.onError("Offer not found");
                return;
            }

            // Update offer status
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", response);
            updates.put("timestamp", System.currentTimeMillis());

            offerRef.updateChildren(updates).addOnSuccessListener(aVoid -> {

                if ("COUNTERED".equals(response)) {
                    // Create counter offer
                    createCounterOffer(originalOffer, counterOfferPrice, counterMessage, responderId, callback);
                } else {
                    // Send response message
                    sendOfferResponseMessage(originalOffer, response, callback);
                }

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to update offer status", e);
                callback.onError("Failed to update offer: " + e.getMessage());
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get original offer", e);
            callback.onError("Failed to get offer: " + e.getMessage());
        });
    }

    /**
     * Create counter offer
     */
    private void createCounterOffer(ChatOffer originalOffer, double counterPrice,
                                   String counterMessage, String responderId, ChatOfferCallback callback) {

        // Create counter offer
        ChatOffer counterOffer = new ChatOffer(
                originalOffer.getConversationId(),
                originalOffer.getProductId(),
                originalOffer.getProductTitle(),
                responderId, // Counter offer sender
                "Seller", // Will be updated with actual name
                originalOffer.getSenderId(), // Original offer sender becomes receiver
                originalOffer.getOriginalPrice(),
                counterPrice,
                counterMessage
        );

        counterOffer.setCounterOfferId(originalOffer.getId());

        String counterOfferId = firebaseManager.getDatabase()
                .getReference(CHAT_OFFERS_NODE).push().getKey();

        if (counterOfferId == null) {
            callback.onError("Failed to generate counter offer ID");
            return;
        }

        counterOffer.setId(counterOfferId);

        // Save counter offer
        firebaseManager.getDatabase()
                .getReference(CHAT_OFFERS_NODE)
                .child(counterOfferId)
                .setValue(counterOffer)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Counter offer created successfully");

                    // Send counter offer message
                    sendOfferMessage(originalOffer.getConversationId(), responderId,
                                   originalOffer.getSenderId(), counterOffer, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create counter offer", e);
                    callback.onError("Failed to create counter offer: " + e.getMessage());
                });
    }

    /**
     * Send offer response message
     */
    private void sendOfferResponseMessage(ChatOffer originalOffer, String response, ChatOfferCallback callback) {
        String responseText;
        String emoji;

        switch (response) {
            case "ACCEPTED":
                responseText = "Offer Accepted! 🎉";
                emoji = "✅";
                break;
            case "DECLINED":
                responseText = "Offer Declined";
                emoji = "❌";
                break;
            default:
                responseText = "Offer Updated";
                emoji = "📝";
                break;
        }

        Message responseMessage = new Message();
        responseMessage.setConversationId(originalOffer.getConversationId());
        responseMessage.setSenderId(originalOffer.getReceiverId());
        responseMessage.setReceiverId(originalOffer.getSenderId());
        responseMessage.setMessageType("offer_response");
        responseMessage.setOfferId(originalOffer.getId());
        responseMessage.setOfferStatus(response);
        responseMessage.setContent(emoji + " " + responseText + " for " + originalOffer.getProductTitle());

        // Send response message using public method
        String responseContent = emoji + " " + responseText + " for " + originalOffer.getProductTitle();
        messagingService.sendTextMessage(originalOffer.getConversationId(),
                                       originalOffer.getSenderId(), responseContent,
            new MessagingService.MessageCallback() {
                @Override
                public void onMessagesLoaded(List<Message> messages) {
                    // Not used
                }

                @Override
                public void onMessageSent(String messageId) {
                    Log.d(TAG, "Offer response message sent");

                    // Send notification
                    sendOfferResponseNotification(originalOffer, response);

                    callback.onOfferResponded(originalOffer, response);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to send offer response message: " + error);
                    callback.onError("Failed to send response message: " + error);
                }
            });
    }

    /**
     * Send offer notification
     */
    private void sendOfferNotification(String receiverId, ChatOffer chatOffer) {
        try {
            if (notificationManager != null) {
                notificationManager.sendPriceOfferNotification(
                        chatOffer.getProductId(),
                        chatOffer.getProductTitle(),
                        String.valueOf(chatOffer.getOfferPrice()),
                        chatOffer.getSenderName(),
                        receiverId
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send offer notification", e);
        }
    }

    /**
     * Update conversation's last message
     */
    private void updateConversationLastMessage(String conversationId, String lastMessage) {
        Map<String, Object> conversationUpdates = new HashMap<>();
        conversationUpdates.put("lastMessage", lastMessage);
        conversationUpdates.put("lastMessageTime", System.currentTimeMillis());

        firebaseManager.getDatabase()
                .getReference("conversations")
                .child(conversationId)
                .updateChildren(conversationUpdates)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update conversation", e));
    }

    /**
     * Send offer response notification
     */
    private void sendOfferResponseNotification(ChatOffer originalOffer, String response) {
        try {
            String message = "Your offer for " + originalOffer.getProductTitle() + " was " + response.toLowerCase();
            notificationManager.sendMessageNotification(
                    originalOffer.getConversationId(),
                    originalOffer.getReceiverId(),
                    "Seller",
                    message,
                    originalOffer.getSenderId()
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to send offer response notification", e);
        }
    }

    /**
     * Callback interface for chat offer operations
     */
    public interface ChatOfferCallback {
        void onOfferSent(ChatOffer chatOffer);
        void onOfferResponded(ChatOffer chatOffer, String response);
        void onError(String error);
    }
}
