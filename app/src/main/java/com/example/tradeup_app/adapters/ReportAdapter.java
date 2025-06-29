package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Report;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    public interface OnReportActionListener {
        void onResolveReport(Report report, String action, String notes);
        void onDismissReport(Report report, String notes);
        void onViewReportDetails(Report report);
    }

    private Context context;
    private List<Report> reports;
    private OnReportActionListener listener;

    public ReportAdapter(Context context, List<Report> reports) {
        this.context = context;
        this.reports = reports;
    }

    public void setOnReportActionListener(OnReportActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reports.get(position);
        holder.bind(report);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public void updateReports(List<Report> newReports) {
        this.reports = newReports;
        notifyDataSetChanged();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private TextView reporterText;
        private TextView reportedUserText;
        private TextView reasonText;
        private TextView typeText;
        private TextView descriptionText;
        private TextView dateText;
        private TextView statusText;
        private MaterialButton resolveButton;
        private MaterialButton dismissButton;
        private MaterialButton detailsButton;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.reportCardView);
            reporterText = itemView.findViewById(R.id.reporterText);
            reportedUserText = itemView.findViewById(R.id.reportedUserText);
            reasonText = itemView.findViewById(R.id.reasonText);
            typeText = itemView.findViewById(R.id.typeText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            dateText = itemView.findViewById(R.id.dateText);
            statusText = itemView.findViewById(R.id.statusText);
            resolveButton = itemView.findViewById(R.id.resolveButton);
            dismissButton = itemView.findViewById(R.id.dismissButton);
            detailsButton = itemView.findViewById(R.id.detailsButton);
        }

        public void bind(Report report) {
            reporterText.setText("Reporter: " + report.getReporterName());
            reportedUserText.setText("Reported: " + report.getReportedUserName());
            reasonText.setText(formatReason(report.getReason()));
            typeText.setText(report.getReportType());
            descriptionText.setText(report.getDescription());
            statusText.setText(report.getStatus());

            // Set status color
            setStatusColor(report.getStatus());

            // Set date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            dateText.setText(sdf.format(new Date(report.getCreatedAt())));

            // Show/hide buttons based on status
            boolean isPending = "PENDING".equals(report.getStatus());
            resolveButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
            dismissButton.setVisibility(isPending ? View.VISIBLE : View.GONE);

            // Set click listeners
            setupClickListeners(report);
        }

        private String formatReason(String reason) {
            switch (reason) {
                case "SCAM":
                    return "Scam/Fraud";
                case "INAPPROPRIATE_CONTENT":
                    return "Inappropriate Content";
                case "SPAM":
                    return "Spam";
                case "HARASSMENT":
                    return "Harassment";
                case "FAKE_LISTING":
                    return "Fake Listing";
                default:
                    return reason;
            }
        }

        private void setStatusColor(String status) {
            int colorRes;
            switch (status) {
                case "RESOLVED":
                    colorRes = R.color.offer_accepted;
                    break;
                case "DISMISSED":
                    colorRes = R.color.secondary_text;
                    break;
                case "PENDING":
                default:
                    colorRes = R.color.offer_pending;
                    break;
            }
            statusText.setTextColor(ContextCompat.getColor(context, colorRes));
        }

        private void setupClickListeners(Report report) {
            resolveButton.setOnClickListener(v -> showResolveDialog(report));
            dismissButton.setOnClickListener(v -> showDismissDialog(report));
            detailsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewReportDetails(report);
                }
            });

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewReportDetails(report);
                }
            });
        }

        private void showResolveDialog(Report report) {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_admin_action, null);
            TextInputEditText notesEditText = dialogView.findViewById(R.id.notesEditText);

            // Action buttons in dialog
            MaterialButton warningButton = dialogView.findViewById(R.id.warningButton);
            MaterialButton suspensionButton = dialogView.findViewById(R.id.suspensionButton);
            MaterialButton deletionButton = dialogView.findViewById(R.id.deletionButton);

            AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Resolve Report")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .create();

            warningButton.setOnClickListener(v -> {
                String notes = notesEditText.getText() != null ? notesEditText.getText().toString().trim() : "";
                if (listener != null) {
                    listener.onResolveReport(report, "WARNING", notes);
                }
                dialog.dismiss();
            });

            suspensionButton.setOnClickListener(v -> {
                String notes = notesEditText.getText() != null ? notesEditText.getText().toString().trim() : "";
                if (listener != null) {
                    listener.onResolveReport(report, "SUSPENSION", notes);
                }
                dialog.dismiss();
            });

            deletionButton.setOnClickListener(v -> {
                String notes = notesEditText.getText() != null ? notesEditText.getText().toString().trim() : "";
                if (listener != null) {
                    listener.onResolveReport(report, "DELETION", notes);
                }
                dialog.dismiss();
            });

            dialog.show();
        }

        private void showDismissDialog(Report report) {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_simple_input, null);
            TextInputEditText notesEditText = dialogView.findViewById(R.id.inputEditText);
            notesEditText.setHint("Reason for dismissal (optional)");

            new AlertDialog.Builder(context)
                .setTitle("Dismiss Report")
                .setMessage("Are you sure you want to dismiss this report?")
                .setView(dialogView)
                .setPositiveButton("Dismiss", (dialog, which) -> {
                    String notes = notesEditText.getText() != null ? notesEditText.getText().toString().trim() : "";
                    if (listener != null) {
                        listener.onDismissReport(report, notes);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }
}
