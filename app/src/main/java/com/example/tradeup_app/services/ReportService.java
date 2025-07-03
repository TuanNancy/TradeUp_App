package com.example.tradeup_app.services;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Report;
import com.example.tradeup_app.utils.Constants;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class ReportService {
    private static final String TAG = "ReportService";
    private final FirebaseManager firebaseManager;

    // Report types
    public static final String REPORT_TYPE_PRODUCT = "PRODUCT";
    public static final String REPORT_TYPE_USER = "USER";
    public static final String REPORT_TYPE_CONVERSATION = "CONVERSATION";

    // Report reasons
    public static final String REASON_SCAM_FRAUD = "Scam/fraud";
    public static final String REASON_INAPPROPRIATE_CONTENT = "Inappropriate content";
    public static final String REASON_SPAM = "Spam";
    public static final String REASON_HARASSMENT = "Harassment or bullying";
    public static final String REASON_FAKE_LISTING = "Fake listing";
    public static final String REASON_IMPERSONATION = "Impersonation";
    public static final String REASON_ABUSIVE_LANGUAGE = "Abusive language";
    public static final String REASON_THREATS = "Threats or violence";
    public static final String REASON_PRIVACY_VIOLATION = "Privacy violation";
    public static final String REASON_SEXUAL_CONTENT = "Sexual content";
    public static final String REASON_HATE_SPEECH = "Hate speech";
    public static final String REASON_MISLEADING_INFO = "Misleading information";
    public static final String REASON_STOLEN_GOODS = "Stolen goods";
    public static final String REASON_COUNTERFEIT = "Counterfeit items";
    public static final String REASON_PRICE_MANIPULATION = "Price manipulation";
    public static final String REASON_OTHER = "Other";

    public interface ReportCallback {
        void onReportSubmitted(String reportId);
        void onError(String error);
    }

    public ReportService() {
        this.firebaseManager = FirebaseManager.getInstance();
    }

    /**
     * Report a product/listing
     */
    public void reportProduct(String productId, String productTitle, String productOwnerId,
                             String productOwnerName, String reason, String description,
                             ReportCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Get current user's name first
        getUserName(currentUserId, (reporterName) -> {
            Report report = new Report(
                currentUserId, reporterName, productOwnerId, productOwnerName,
                productId, REPORT_TYPE_PRODUCT, reason, description
            );
            report.setReportedItemTitle(productTitle);

            submitReport(report, callback);
        }, callback::onError);
    }

    /**
     * Report a user profile
     */
    public void reportUser(String reportedUserId, String reportedUserName, String reason,
                          String description, ReportCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Get current user's name first
        getUserName(currentUserId, (reporterName) -> {
            Report report = new Report(
                currentUserId, reporterName, reportedUserId, reportedUserName,
                reportedUserId, REPORT_TYPE_USER, reason, description
            );
            report.setReportedItemTitle("User Profile: " + reportedUserName);

            submitReport(report, callback);
        }, callback::onError);
    }

    /**
     * Report a conversation/chat
     */
    public void reportConversation(String conversationId, String otherUserId, String otherUserName,
                                  String reason, String description, ReportCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Get current user's name first
        getUserName(currentUserId, (reporterName) -> {
            Report report = new Report(
                currentUserId, reporterName, otherUserId, otherUserName,
                conversationId, REPORT_TYPE_CONVERSATION, reason, description
            );
            report.setReportedItemTitle("Conversation with " + otherUserName);

            submitReport(report, callback);
        }, callback::onError);
    }

    /**
     * Submit the report to Firebase
     */
    private void submitReport(Report report, ReportCallback callback) {
        DatabaseReference reportsRef = firebaseManager.getDatabase()
                .getReference(Constants.REPORTS_NODE);

        String reportId = reportsRef.push().getKey();
        if (reportId == null) {
            callback.onError("Failed to generate report ID");
            return;
        }

        report.setId(reportId);

        reportsRef.child(reportId).setValue(report)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Also update the reported item to mark it as reported
                        markItemAsReported(report);
                        callback.onReportSubmitted(reportId);
                    } else {
                        String error = task.getException() != null ?
                            task.getException().getMessage() : "Unknown error";
                        callback.onError("Failed to submit report: " + error);
                    }
                });
    }

    /**
     * Mark the reported item as reported in its respective node
     */
    private void markItemAsReported(Report report) {
        DatabaseReference itemRef;
        String nodeType;

        switch (report.getReportType()) {
            case REPORT_TYPE_PRODUCT:
                nodeType = FirebaseManager.PRODUCTS_NODE;
                break;
            case REPORT_TYPE_USER:
                nodeType = FirebaseManager.USERS_NODE;
                break;
            case REPORT_TYPE_CONVERSATION:
                nodeType = FirebaseManager.CONVERSATIONS_NODE;
                break;
            default:
                return; // Unknown type
        }

        itemRef = firebaseManager.getDatabase()
                .getReference(nodeType)
                .child(report.getReportedItemId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("isReported", true);
        updates.put("reportCount", com.google.firebase.database.ServerValue.increment(1));
        updates.put("lastReportedAt", System.currentTimeMillis());

        itemRef.updateChildren(updates);
    }

    /**
     * Get user name for the reporter
     */
    private void getUserName(String userId, OnUserNameCallback callback, OnErrorCallback errorCallback) {
        firebaseManager.getDatabase()
                .getReference(FirebaseManager.USERS_NODE)
                .child(userId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                        String userName = "Unknown User";
                        if (dataSnapshot.exists()) {
                            String name = dataSnapshot.child("name").getValue(String.class);
                            String fullName = dataSnapshot.child("fullName").getValue(String.class);
                            String username = dataSnapshot.child("username").getValue(String.class);

                            if (name != null && !name.isEmpty()) {
                                userName = name;
                            } else if (fullName != null && !fullName.isEmpty()) {
                                userName = fullName;
                            } else if (username != null && !username.isEmpty()) {
                                userName = username;
                            }
                        }
                        callback.onUserName(userName);
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                        errorCallback.onError("Failed to load user name: " + databaseError.getMessage());
                    }
                });
    }

    /**
     * Check if user has already reported an item
     */
    public void hasUserReportedItem(String itemId, String reportType, OnCheckCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onCheckComplete(false);
            return;
        }

        firebaseManager.getDatabase()
                .getReference(Constants.REPORTS_NODE)
                .orderByChild("reporterId")
                .equalTo(currentUserId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                        boolean hasReported = false;
                        for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Report report = snapshot.getValue(Report.class);
                            if (report != null &&
                                report.getReportedItemId().equals(itemId) &&
                                report.getReportType().equals(reportType)) {
                                hasReported = true;
                                break;
                            }
                        }
                        callback.onCheckComplete(hasReported);
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                        callback.onCheckComplete(false);
                    }
                });
    }

    /**
     * Get predefined report reasons based on type
     */
    public static String[] getReportReasons(String reportType) {
        // Simplified report reasons - same for all content types
        return new String[]{
            REASON_SCAM_FRAUD,
            REASON_INAPPROPRIATE_CONTENT,
            REASON_SPAM
        };
    }

    // Callback interfaces
    private interface OnUserNameCallback {
        void onUserName(String userName);
    }

    private interface OnErrorCallback {
        void onError(String error);
    }

    public interface OnCheckCallback {
        void onCheckComplete(boolean hasReported);
    }
}
