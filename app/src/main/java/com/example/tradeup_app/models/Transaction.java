package com.example.tradeup_app.models;

public class Transaction {
    private String id;
    private String productId;
    private String productTitle;
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String sellerName;
    private double finalPrice;
    private String status; // PENDING, COMPLETED, CANCELLED
    private long createdAt;
    private long completedAt;
    private String offerId; // If created from an offer
    private String notes;
    private boolean buyerRated;
    private boolean sellerRated;

    public Transaction() {
        this.createdAt = System.currentTimeMillis();
        this.status = "PENDING";
        this.buyerRated = false;
        this.sellerRated = false;
    }

    public Transaction(String productId, String productTitle, String buyerId, String buyerName,
                      String sellerId, String sellerName, double finalPrice) {
        this();
        this.productId = productId;
        this.productTitle = productTitle;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.finalPrice = finalPrice;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if ("COMPLETED".equals(status)) {
            this.completedAt = System.currentTimeMillis();
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isBuyerRated() {
        return buyerRated;
    }

    public void setBuyerRated(boolean buyerRated) {
        this.buyerRated = buyerRated;
    }

    public boolean isSellerRated() {
        return sellerRated;
    }

    public void setSellerRated(boolean sellerRated) {
        this.sellerRated = sellerRated;
    }
}
