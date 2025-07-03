package com.example.tradeup_app.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.example.tradeup_app.services.ReportService;

public class ReportUtils {

    /**
     * Show a report dialog for any type of content
     */
    public static void showReportDialog(Context context, String reportType, String itemId,
                                       String itemTitle, String reportedUserId, String reportedUserName,
                                       ReportService.ReportCallback callback) {

        ReportService reportService = new ReportService();

        // First check if user has already reported this item
        reportService.hasUserReportedItem(itemId, reportType, hasReported -> {
            if (hasReported) {
                Toast.makeText(context, "You have already reported this " +
                    getItemTypeName(reportType).toLowerCase(), Toast.LENGTH_SHORT).show();
                return;
            }

            // Show the report dialog
            showReportReasonDialog(context, reportType, itemId, itemTitle,
                                 reportedUserId, reportedUserName, callback);
        });
    }

    /**
     * Show dialog to select report reason
     */
    private static void showReportReasonDialog(Context context, String reportType, String itemId,
                                             String itemTitle, String reportedUserId, String reportedUserName,
                                             ReportService.ReportCallback callback) {

        try {
            String[] reasons = ReportService.getReportReasons(reportType);
            String itemTypeName = getItemTypeName(reportType);

            // Debug log to check if reasons are loaded
            android.util.Log.d("ReportUtils", "Report reasons count: " + reasons.length);
            for (int i = 0; i < reasons.length; i++) {
                android.util.Log.d("ReportUtils", "Reason " + i + ": " + reasons[i]);
            }

            // Use a variable to track selected item
            final int[] selectedPosition = {-1};

            // Create a custom layout for better control
            android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 20);

            // Add message text
            android.widget.TextView messageView = new android.widget.TextView(context);
            messageView.setText("Why are you reporting this " + itemTypeName.toLowerCase() + "?");
            messageView.setPadding(0, 0, 0, 30);
            messageView.setTextSize(16);
            layout.addView(messageView);

            // Create RadioGroup for better control
            android.widget.RadioGroup radioGroup = new android.widget.RadioGroup(context);
            radioGroup.setOrientation(android.widget.RadioGroup.VERTICAL);

            // Add radio buttons for each reason
            for (int i = 0; i < reasons.length; i++) {
                android.widget.RadioButton radioButton = new android.widget.RadioButton(context);
                radioButton.setText(reasons[i]);
                radioButton.setId(i);
                radioButton.setPadding(10, 15, 10, 15);
                radioButton.setTextSize(14);
                
                final int position = i;
                radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedPosition[0] = position;
                        android.util.Log.d("ReportUtils", "Radio button selected - position: " + position + ", reason: " + reasons[position]);
                    }
                });
                
                radioGroup.addView(radioButton);
            }

            layout.addView(radioGroup);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Report " + itemTypeName);
            builder.setView(layout);

            builder.setPositiveButton("Continue", (dialog, which) -> {
                android.util.Log.d("ReportUtils", "Continue clicked, selected position: " + selectedPosition[0]);
                if (selectedPosition[0] >= 0 && selectedPosition[0] < reasons.length) {
                    String selectedReason = reasons[selectedPosition[0]];
                    dialog.dismiss();

                    // Show description dialog
                    showReportDescriptionDialog(context, reportType, itemId, itemTitle,
                                              reportedUserId, reportedUserName, selectedReason, callback);
                } else {
                    Toast.makeText(context, "Please select a reason", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> {
                android.util.Log.d("ReportUtils", "Report dialog cancelled");
                dialog.dismiss();
            });

            // Create and show dialog
            AlertDialog dialog = builder.create();

            // Ensure dialog is shown on UI thread
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (!((android.app.Activity) context).isFinishing()) {
                        dialog.show();
                        android.util.Log.d("ReportUtils", "Custom report dialog shown successfully");
                    }
                });
            } else {
                dialog.show();
                android.util.Log.d("ReportUtils", "Custom report dialog shown successfully");
            }

        } catch (Exception e) {
            android.util.Log.e("ReportUtils", "Error showing report dialog", e);
            Toast.makeText(context, "Error showing report dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show dialog to enter additional description
     */
    private static void showReportDescriptionDialog(Context context, String reportType, String itemId,
                                                   String itemTitle, String reportedUserId, String reportedUserName,
                                                   String reason, ReportService.ReportCallback callback) {

        EditText editText = new EditText(context);
        editText.setHint("Please provide additional details (optional)");
        editText.setMinLines(3);
        editText.setMaxLines(5);

        new AlertDialog.Builder(context)
            .setTitle("Additional Details")
            .setMessage("Reason: " + reason + "\n\nPlease provide any additional information that might help us review this report:")
            .setView(editText)
            .setPositiveButton("Submit Report", (dialog, which) -> {
                String description = editText.getText().toString().trim();

                // Submit the report
                submitReport(context, reportType, itemId, itemTitle, reportedUserId,
                           reportedUserName, reason, description, callback);
            })
            .setNegativeButton("Back", (dialog, which) -> {
                // Go back to reason selection
                showReportReasonDialog(context, reportType, itemId, itemTitle,
                                     reportedUserId, reportedUserName, callback);
            })
            .show();
    }

    /**
     * Submit the report using the appropriate service method
     */
    private static void submitReport(Context context, String reportType, String itemId,
                                   String itemTitle, String reportedUserId, String reportedUserName,
                                   String reason, String description, ReportService.ReportCallback callback) {

        ReportService reportService = new ReportService();

        // Show loading toast
        Toast.makeText(context, "Submitting report...", Toast.LENGTH_SHORT).show();

        ReportService.ReportCallback internalCallback = new ReportService.ReportCallback() {
            @Override
            public void onReportSubmitted(String reportId) {
                Toast.makeText(context, "Report submitted successfully. Thank you for keeping our community safe.",
                             Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onReportSubmitted(reportId);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Failed to submit report: " + error, Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onError(error);
                }
            }
        };

        switch (reportType) {
            case ReportService.REPORT_TYPE_PRODUCT:
                reportService.reportProduct(itemId, itemTitle, reportedUserId, reportedUserName,
                                           reason, description, internalCallback);
                break;
            case ReportService.REPORT_TYPE_USER:
                reportService.reportUser(reportedUserId, reportedUserName, reason, description, internalCallback);
                break;
            case ReportService.REPORT_TYPE_CONVERSATION:
                reportService.reportConversation(itemId, reportedUserId, reportedUserName,
                                                reason, description, internalCallback);
                break;
            default:
                Toast.makeText(context, "Unknown report type", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Get human-readable name for report type
     */
    private static String getItemTypeName(String reportType) {
        switch (reportType) {
            case ReportService.REPORT_TYPE_PRODUCT:
                return "Listing";
            case ReportService.REPORT_TYPE_USER:
                return "Profile";
            case ReportService.REPORT_TYPE_CONVERSATION:
                return "Conversation";
            default:
                return "Item";
        }
    }

    /**
     * Quick method to report a product/listing
     */
    public static void reportProduct(Context context, String productId, String productTitle,
                                   String ownerId, String ownerName) {
        // Skip the check and show dialog directly for testing
        showReportReasonDialog(context, ReportService.REPORT_TYPE_PRODUCT, productId, productTitle,
                        ownerId, ownerName, null);
    }

    /**
     * Quick method to report a user profile
     */
    public static void reportUser(Context context, String userId, String userName) {
        // Skip the check and show dialog directly for testing
        showReportReasonDialog(context, ReportService.REPORT_TYPE_USER, userId, null, userId, userName, null);
    }

    /**
     * Quick method to report a conversation
     */
    public static void reportConversation(Context context, String conversationId, String otherUserId,
                                        String otherUserName) {
        // Skip the check and show dialog directly for testing
        showReportReasonDialog(context, ReportService.REPORT_TYPE_CONVERSATION, conversationId, null,
                        otherUserId, otherUserName, null);
    }
}
