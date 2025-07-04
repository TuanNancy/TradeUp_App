package com.example.tradeup_app.models;

public class Notification {
    private String id;
    private String userId;
    private String type;
    private String title;
    private String message;
    private String relatedId; // conversationId, productId, etc.
    private long timestamp;
    private boolean isRead;
    private String imageUrl;
    private String actionUrl;

    public Notification() {
        // Required empty constructor for Firebase
    }

    public Notification(String userId, String type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getRelatedId() { return relatedId; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public String getImageUrl() { return imageUrl; }
    public String getActionUrl() { return actionUrl; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { this.isRead = read; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
}
