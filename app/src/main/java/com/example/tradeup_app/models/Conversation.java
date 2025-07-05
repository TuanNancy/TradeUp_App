package com.example.tradeup_app.models;

import java.util.List;
import java.util.Map;

public class Conversation {
    private String id;
    private String productId; // Keep for backward compatibility
    private String productTitle; // Keep for backward compatibility
    private String productImageUrl; // Keep for backward compatibility
    private String buyerId;
    private String sellerId;
    private String buyerName;
    private String sellerName;
    private String lastMessage;
    private long lastMessageTime;
    private int unreadCount;
    private boolean isActive;
    private long createdAt;
    private long updatedAt;

    // NEW: Support for multiple products in one conversation
    private Map<String, Object> products; // productId -> product info (title, imageUrl, addedTime)

    // Fields for blocking and reporting
    private Map<String, Boolean> blockedUsers; // userId -> blocked status
    private Map<String, Long> lastReadTimes; // userId -> timestamp of last read message
    private String lastMessageSenderId; // ID of user who sent the last message
    private boolean isReported;
    private String reportedBy;
    private String reportReason;
    private long reportedAt;
    private long lastReportedAt;
    private int reportCount;
    private boolean isEncrypted; // For secure messaging
    private int messageCount; // Total messages in conversation

    public Conversation() {
        this.isActive = true;
        this.unreadCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isReported = false;
        this.isEncrypted = true;
        this.messageCount = 0;
        this.reportCount = 0;
        this.lastReportedAt = 0;
    }

    // Utility methods for blocking
    public boolean isUserBlocked(String userId) {
        return blockedUsers != null && blockedUsers.containsKey(userId) && blockedUsers.get(userId);
    }

    public boolean canUserSendMessage(String userId) {
        return isActive && !isUserBlocked(userId);
    }

    // Get the other participant in the conversation
    public String getOtherParticipantId(String currentUserId) {
        if (currentUserId.equals(buyerId)) {
            return sellerId;
        } else if (currentUserId.equals(sellerId)) {
            return buyerId;
        }
        return null;
    }

    public String getOtherParticipantName(String currentUserId) {
        if (currentUserId.equals(buyerId)) {
            return sellerName;
        } else if (currentUserId.equals(sellerId)) {
            return buyerName;
        }
        return "Unknown";
    }

    // NEW: Methods for handling multiple products
    public boolean hasProduct(String productId) {
        return products != null && products.containsKey(productId);
    }

    public int getProductCount() {
        return products != null ? products.size() : (productId != null ? 1 : 0);
    }

    public String getDisplayTitle() {
        if (products != null && products.size() > 1) {
            return "Chat về " + products.size() + " sản phẩm";
        } else if (productTitle != null) {
            return "Về: " + productTitle;
        } else {
            return "Chat chung";
        }
    }

    // NEW: Blocked status management
    private boolean isBlocked = false;

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // NEW: Products getter/setter
    public Map<String, Object> getProducts() { return products; }
    public void setProducts(Map<String, Object> products) {
        this.products = products;
        this.updatedAt = System.currentTimeMillis();
    }

    // Blocking and reporting getters/setters
    public Map<String, Boolean> getBlockedUsers() { return blockedUsers; }
    public void setBlockedUsers(Map<String, Boolean> blockedUsers) { this.blockedUsers = blockedUsers; }

    public Map<String, Long> getLastReadTimes() { return lastReadTimes; }
    public void setLastReadTimes(Map<String, Long> lastReadTimes) { this.lastReadTimes = lastReadTimes; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public boolean isReported() { return isReported; }
    public void setReported(boolean reported) {
        isReported = reported;
        if (reported) {
            this.lastReportedAt = System.currentTimeMillis();
            this.reportCount++;
        }
    }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }

    public long getReportedAt() { return reportedAt; }
    public void setReportedAt(long reportedAt) { this.reportedAt = reportedAt; }

    public long getLastReportedAt() { return lastReportedAt; }
    public void setLastReportedAt(long lastReportedAt) { this.lastReportedAt = lastReportedAt; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public boolean isEncrypted() { return isEncrypted; }
    public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
}
