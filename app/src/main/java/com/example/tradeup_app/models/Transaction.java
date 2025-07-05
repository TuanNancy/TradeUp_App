package com.example.tradeup_app.models;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String id;
    private String productId;
    private String productTitle;
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String sellerName;
    private double finalPrice;
    private String status; // PENDING, COMPLETED, CANCELLED, PAID
    private long createdAt;
    private long completedAt;
    private String offerId; // If created from an offer
    private String notes;
    private boolean buyerRated;
    private boolean sellerRated;

    // Payment related fields
    private String paymentId;
    private String paymentStatus; // PENDING, SUCCEEDED, FAILED, CANCELED
    private String stripePaymentIntentId;
    private String paymentMethod;
    private long paidAt;
    private String receiptUrl;

    public Transaction() {
        this.createdAt = System.currentTimeMillis();
        this.status = "PENDING";
        this.paymentStatus = "PENDING";
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

    // Payment related getters and setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(long paidAt) {
        this.paidAt = paidAt;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
}
