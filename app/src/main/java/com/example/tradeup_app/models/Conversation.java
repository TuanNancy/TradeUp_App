package com.example.tradeup_app.models;

import java.util.List;
import java.util.Map;

public class Conversation {
    private String id;
    private String productId;
    private String productTitle;
    private String productImageUrl;
    private String buyerId;
    private String sellerId;
    private String buyerName;
    private String sellerName;
    private String lastMessage;
    private long lastMessageTime; // Changed from Date to long for Firebase compatibility
    private int unreadCount;
    private boolean isActive;
    private long createdAt; // Added for better tracking
    private long updatedAt; // Added for better tracking

    // New fields for blocking and reporting
    private Map<String, Boolean> blockedUsers; // userId -> blocked status
    private Map<String, Long> lastReadTimes; // userId -> timestamp of last read message
    private String lastMessageSenderId; // ID of user who sent the last message
    private boolean isReported;
    private String reportedBy;
    private String reportReason;
    private long reportedAt;
    private long lastReportedAt; // Add this missing field
    private int reportCount; // Add this missing field
    private boolean isEncrypted; // For secure messaging
    private int messageCount; // Total messages in conversation

    public Conversation() {
        this.isActive = true;
        this.unreadCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isReported = false;
        this.isEncrypted = true; // Enable encryption by default
        this.messageCount = 0;
        this.reportCount = 0; // Initialize report count
        this.lastReportedAt = 0; // Initialize last reported timestamp
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

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }

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
        this.updatedAt = System.currentTimeMillis(); // Auto-update timestamp
    }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // New getters and setters
    public Map<String, Boolean> getBlockedUsers() { return blockedUsers; }
    public void setBlockedUsers(Map<String, Boolean> blockedUsers) { this.blockedUsers = blockedUsers; }

    public boolean isReported() { return isReported; }
    public void setReported(boolean reported) {
        this.isReported = reported;
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

    // Additional utility methods for blocking functionality
    public void blockUser(String userId) {
        if (blockedUsers == null) {
            blockedUsers = new java.util.HashMap<>();
        }
        blockedUsers.put(userId, true);
        this.updatedAt = System.currentTimeMillis();
    }

    public void unblockUser(String userId) {
        if (blockedUsers != null) {
            blockedUsers.remove(userId);
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public boolean isBlocked() {
        return blockedUsers != null && !blockedUsers.isEmpty();
    }

    public void setBlocked(boolean blocked) {
        // This is a convenience method for UI updates
        // The actual blocking logic should use blockUser/unblockUser methods
    }

    public Map<String, Long> getLastReadTimes() {
        return lastReadTimes;
    }

    public void setLastReadTimes(Map<String, Long> lastReadTimes) {
        this.lastReadTimes = lastReadTimes;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    // Methods for read/unread status
    public boolean hasUnreadMessages(String userId) {
        // Nếu không có thời gian đọc cuối hoặc thời gian tin nhắn cuối mới hơn thời gian đọc cuối
        if (lastReadTimes == null || !lastReadTimes.containsKey(userId)) {
            return lastMessageSenderId != null && !lastMessageSenderId.equals(userId);
        }

        Long lastReadTime = lastReadTimes.get(userId);
        return lastMessageTime > lastReadTime && !userId.equals(lastMessageSenderId);
    }

    public void markAsRead(String userId) {
        if (lastReadTimes == null) {
            lastReadTimes = new java.util.HashMap<>();
        }
        lastReadTimes.put(userId, System.currentTimeMillis());
        this.updatedAt = System.currentTimeMillis();
    }

    public long getLastReadTime(String userId) {
        if (lastReadTimes == null || !lastReadTimes.containsKey(userId)) {
            return 0;
        }
        return lastReadTimes.get(userId);
    }

    // Thêm các setter cho Firebase compatibility
    public void setRead(boolean read) {
        // Setter cho field "read" để tương thích với Firebase
        // Có thể để trống hoặc xử lý logic tùy theo nhu cầu
    }

    public void setMessages(Object messages) {
        // Setter cho field "messages" để tương thích với Firebase
        // Có thể để trống vì không cần xử lý
    }
}
