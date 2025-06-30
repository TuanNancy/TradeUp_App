package com.example.tradeup_app.models;

public class Report {
    private String id;
    private String reporterId;
    private String reporterName;
    private String reportedUserId;
    private String reportedUserName;
    private String reportedItemId; // productId or conversationId
    private String reportType; // USER, PRODUCT, CONVERSATION
    private String reason; // SCAM, INAPPROPRIATE_CONTENT, SPAM, HARASSMENT, FAKE_LISTING
    private String description;
    private String status; // PENDING, REVIEWED, RESOLVED, DISMISSED
    private long createdAt; // Using long for Firebase compatibility
    private long reviewedAt;
    private String adminId;
    private String adminNotes;
    private String actionTaken; // WARNING, SUSPENSION, DELETION, NONE
    private String reportedItemTitle; // Added for better UI display

    public Report() {
        this.createdAt = System.currentTimeMillis();
        this.status = "PENDING";
        this.reviewedAt = 0L;
    }

    public Report(String reporterId, String reporterName, String reportedUserId,
                  String reportedUserName, String reportedItemId, String reportType,
                  String reason, String description) {
        this();
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.reportedUserId = reportedUserId;
        this.reportedUserName = reportedUserName;
        this.reportedItemId = reportedItemId;
        this.reportType = reportType;
        this.reason = reason;
        this.description = description;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(String reportedUserId) { this.reportedUserId = reportedUserId; }

    public String getReportedUserName() { return reportedUserName; }
    public void setReportedUserName(String reportedUserName) { this.reportedUserName = reportedUserName; }

    public String getReportedItemId() { return reportedItemId; }
    public void setReportedItemId(String reportedItemId) { this.reportedItemId = reportedItemId; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(long reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }

    public String getReportedItemTitle() { return reportedItemTitle; }
    public void setReportedItemTitle(String reportedItemTitle) { this.reportedItemTitle = reportedItemTitle; }
}
