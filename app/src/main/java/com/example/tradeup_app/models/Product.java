package com.example.tradeup_app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String description;
    private double price;
    private boolean isNegotiable;
    private String category;
    private String condition;
    private String location;
    private String sellerId;
    private String sellerName;
    private List<String> imageUrls;
    private List<String> tags;
    private long createdAt;
    private long updatedAt;
    private String status; // Available, Sold, Paused
    private int viewCount;
    private int likeCount;
    private double latitude;
    private double longitude;
    private String itemBehavior;
    private int interactionCount;
    private long lastViewedAt;
    private List<String> interactionHistory;

    public Product() {
        this.imageUrls = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.status = "Available";
        this.viewCount = 0;
        this.likeCount = 0;
        this.interactionCount = 0;
        this.interactionHistory = new ArrayList<>();
        this.lastViewedAt = 0L;
    }

    public Product(String title, String description, double price, String category, String condition, String location, String sellerId) {
        this();
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.condition = condition;
        this.location = location;
        this.sellerId = sellerId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isNegotiable() { return isNegotiable; }
    public void setNegotiable(boolean negotiable) { isNegotiable = negotiable; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // Alias method for custom tags (same as getTags)
    public List<String> getCustomTags() { return tags; }
    public void setCustomTags(List<String> customTags) { this.tags = customTags; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getItemBehavior() { return itemBehavior; }
    public void setItemBehavior(String itemBehavior) { this.itemBehavior = itemBehavior; }

    public int getInteractionCount() { return interactionCount; }
    public void setInteractionCount(int interactionCount) { this.interactionCount = interactionCount; }

    public long getLastViewedAt() { return lastViewedAt; }
    public void setLastViewedAt(long lastViewedAt) { this.lastViewedAt = lastViewedAt; }

    public List<String> getInteractionHistory() { return interactionHistory; }
    public void setInteractionHistory(List<String> interactionHistory) { this.interactionHistory = interactionHistory; }
}
