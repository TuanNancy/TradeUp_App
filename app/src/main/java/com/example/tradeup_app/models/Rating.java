package com.example.tradeup_app.models;

public class Rating {
    private String id;
    private String transactionId;
    private String raterId;
    private String raterName;
    private String ratedUserId;
    private String ratedUserName;
    private int stars; // 1-5
    private String review;
    private long createdAt; // Changed to long for Firebase compatibility
    private String userType; // BUYER or SELLER
    private String productId; // Added for better tracking
    private String productTitle; // Added for better UI display

    public Rating() {
        this.createdAt = System.currentTimeMillis();
    }

    public Rating(String transactionId, String raterId, String raterName,
                  String ratedUserId, String ratedUserName, int stars, String review, String userType) {
        this();
        this.transactionId = transactionId;
        this.raterId = raterId;
        this.raterName = raterName;
        this.ratedUserId = ratedUserId;
        this.ratedUserName = ratedUserName;
        this.stars = Math.max(1, Math.min(5, stars)); // Ensure stars is between 1-5
        this.review = review;
        this.userType = userType;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getRaterId() { return raterId; }
    public void setRaterId(String raterId) { this.raterId = raterId; }

    public String getRaterName() { return raterName; }
    public void setRaterName(String raterName) { this.raterName = raterName; }

    public String getRatedUserId() { return ratedUserId; }
    public void setRatedUserId(String ratedUserId) { this.ratedUserId = ratedUserId; }

    public String getRatedUserName() { return ratedUserName; }
    public void setRatedUserName(String ratedUserName) { this.ratedUserName = ratedUserName; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = Math.max(1, Math.min(5, stars)); }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
}
