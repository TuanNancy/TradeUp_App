package com.example.tradeup_app.models;

import java.io.Serializable;

public class ChatOffer implements Serializable {
    private String id;
    private String conversationId;
    private String productId;
    private String productTitle;
    private String senderId;
    private String senderName;
    private String receiverId;
    private double originalPrice;
    private double offerPrice;
    private String message;
    private String status; // PENDING, ACCEPTED, DECLINED, COUNTERED
    private long timestamp;
    private String counterOfferId; // If this is a counter offer

    public ChatOffer() {
        this.timestamp = System.currentTimeMillis();
        this.status = "PENDING";
    }

    public ChatOffer(String conversationId, String productId, String productTitle,
                    String senderId, String senderName, String receiverId,
                    double originalPrice, double offerPrice, String message) {
        this();
        this.conversationId = conversationId;
        this.productId = productId;
        this.productTitle = productTitle;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.originalPrice = originalPrice;
        this.offerPrice = offerPrice;
        this.message = message;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public double getOfferPrice() { return offerPrice; }
    public void setOfferPrice(double offerPrice) { this.offerPrice = offerPrice; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCounterOfferId() { return counterOfferId; }
    public void setCounterOfferId(String counterOfferId) { this.counterOfferId = counterOfferId; }

    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isAccepted() { return "ACCEPTED".equals(status); }
    public boolean isDeclined() { return "DECLINED".equals(status); }
    public boolean isCountered() { return "COUNTERED".equals(status); }
}
