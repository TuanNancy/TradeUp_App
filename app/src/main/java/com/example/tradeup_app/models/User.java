package com.example.tradeup_app.models;

import java.util.Map;

public class User {
    private String id;
    private String name;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String address;
    private String bio;
    private String profileImageUrl;
    private long createdAt;
    private long updatedAt;
    private boolean isActive;
    private boolean isVerified;
    private String userType; // "buyer", "seller", "admin"

    // User preferences and settings
    private boolean emailNotifications;
    private boolean pushNotifications;
    private String language;
    private String currency;

    // User statistics
    private int totalProducts;
    private int totalSales;
    private int totalPurchases;
    private double averageRating;
    private int totalRatings;

    // Privacy and security
    private Map<String, Boolean> blockedUsers; // userId -> blocked status
    private Map<String, Boolean> privacySettings; // setting -> enabled status
    private long lastLoginAt;
    private String deviceToken; // For push notifications

    public User() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isActive = true;
        this.isVerified = false;
        this.userType = "buyer";
        this.emailNotifications = true;
        this.pushNotifications = true;
        this.language = "en";
        this.currency = "USD";
        this.totalProducts = 0;
        this.totalSales = 0;
        this.totalPurchases = 0;
        this.averageRating = 0.0;
        this.totalRatings = 0;
    }

    public User(String email, String name) {
        this();
        this.email = email;
        this.name = name;
        this.fullName = name;
        this.username = generateUsernameFromEmail(email);
    }

    private String generateUsernameFromEmail(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@")).toLowerCase();
        }
        return "user" + System.currentTimeMillis();
    }

    // Utility methods for blocking
    public boolean isUserBlocked(String userId) {
        return blockedUsers != null && blockedUsers.containsKey(userId) && blockedUsers.get(userId);
    }

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

    // Update user rating
    public void updateRating(double newRating) {
        double totalScore = averageRating * totalRatings + newRating;
        totalRatings++;
        averageRating = totalScore / totalRatings;
        this.updatedAt = System.currentTimeMillis();
    }

    // Check if user can be contacted
    public boolean canBeContacted() {
        return isActive && isVerified;
    }

    // Get display name (prefer fullName, fallback to name, then username)
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        return "User";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAddress() { return address; }
    public void setAddress(String address) {
        this.address = address;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getBio() { return bio; }
    public void setBio(String bio) {
        this.bio = bio;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        this.isActive = active;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) {
        this.isVerified = verified;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getUserType() { return userType; }
    public void setUserType(String userType) {
        this.userType = userType;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isPushNotifications() { return pushNotifications; }
    public void setPushNotifications(boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) {
        this.language = language;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) {
        this.currency = currency;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }

    public int getTotalSales() { return totalSales; }
    public void setTotalSales(int totalSales) { this.totalSales = totalSales; }

    public int getTotalPurchases() { return totalPurchases; }
    public void setTotalPurchases(int totalPurchases) { this.totalPurchases = totalPurchases; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }

    public Map<String, Boolean> getBlockedUsers() { return blockedUsers; }
    public void setBlockedUsers(Map<String, Boolean> blockedUsers) { this.blockedUsers = blockedUsers; }

    public Map<String, Boolean> getPrivacySettings() { return privacySettings; }
    public void setPrivacySettings(Map<String, Boolean> privacySettings) { this.privacySettings = privacySettings; }

    public long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(long lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
}
