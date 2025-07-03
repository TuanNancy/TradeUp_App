package com.example.tradeup_app.models;

public class Message {
    private String id;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String content;
    private String messageType; // text, image, emoji, offer
    private long timestamp;
    private boolean isRead;
    private String productId;
    private double offerAmount;
    private String senderName;

    // New fields for enhanced messaging
    private String imageUrl; // For image messages
    private String imageFileName; // For image file reference
    private boolean isDeleted; // For message deletion
    private String deletedBy; // Who deleted the message
    private boolean isReported; // If message was reported
    private String reportReason; // Reason for reporting
    private boolean isEncrypted; // For future encryption support

    public Message() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.messageType = "text";
        this.isDeleted = false;
        this.isReported = false;
        this.isEncrypted = false;
    }

    public Message(String conversationId, String senderId, String receiverId, String content) {
        this();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
    }

    // Constructor for image messages
    public Message(String conversationId, String senderId, String receiverId, String imageUrl, String imageFileName) {
        this();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.imageUrl = imageUrl;
        this.imageFileName = imageFileName;
        this.messageType = "image";
        this.content = "[Image]";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public double getOfferAmount() { return offerAmount; }
    public void setOfferAmount(double offerAmount) { this.offerAmount = offerAmount; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    // New getters and setters
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageFileName() { return imageFileName; }
    public void setImageFileName(String imageFileName) { this.imageFileName = imageFileName; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    public boolean isReported() { return isReported; }
    public void setReported(boolean reported) { isReported = reported; }

    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }

    public boolean isEncrypted() { return isEncrypted; }
    public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }
}
