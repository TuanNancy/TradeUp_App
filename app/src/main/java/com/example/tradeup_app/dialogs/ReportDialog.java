package com.example.tradeup_app.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.tradeup_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ReportDialog extends Dialog {

    public interface OnReportSubmitListener {
        void onReportSubmit(String reason, String description);
    }

    private OnReportSubmitListener listener;

    private RadioGroup reportTypeGroup;
    private TextInputEditText reportDescriptionEditText;
    private TextInputLayout reportDescriptionInputLayout;
    private MaterialButton cancelReportButton;
    private MaterialButton submitReportButton;

    public ReportDialog(@NonNull Context context, OnReportSubmitListener listener) {
        super(context);
        this.listener = listener;

        initDialog();
    }

    private void initDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_report, null);
        setContentView(view);

        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        reportTypeGroup = view.findViewById(R.id.reportTypeGroup);
        reportDescriptionEditText = view.findViewById(R.id.reportDescriptionEditText);
        reportDescriptionInputLayout = view.findViewById(R.id.reportDescriptionInputLayout);
        cancelReportButton = view.findViewById(R.id.cancelReportButton);
        submitReportButton = view.findViewById(R.id.submitReportButton);

        // Disable submit button initially
        submitReportButton.setEnabled(false);
    }

    private void setupListeners() {
        cancelReportButton.setOnClickListener(v -> dismiss());

        submitReportButton.setOnClickListener(v -> {
            if (validateInput()) {
                String reason = getSelectedReason();
                String description = reportDescriptionEditText.getText() != null ?
                    reportDescriptionEditText.getText().toString().trim() : "";

                if (listener != null) {
                    listener.onReportSubmit(reason, description);
                }
                dismiss();
            }
        });

        // Enable submit button when reason is selected
        reportTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            submitReportButton.setEnabled(checkedId != -1);
        });
    }

    private boolean validateInput() {
        if (reportTypeGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), "Please select a reason for reporting", Toast.LENGTH_SHORT).show();
            return false;
        }

        String description = reportDescriptionEditText.getText() != null ?
            reportDescriptionEditText.getText().toString().trim() : "";

        if (description.isEmpty()) {
            reportDescriptionInputLayout.setError("Please describe the issue");
            return false;
        }

        if (description.length() < 10) {
            reportDescriptionInputLayout.setError("Description must be at least 10 characters");
            return false;
        }

        reportDescriptionInputLayout.setError(null);
        return true;
    }

    private String getSelectedReason() {
        int checkedId = reportTypeGroup.getCheckedRadioButtonId();

        if (checkedId == R.id.reportScamRadio) {
            return "SCAM";
        } else if (checkedId == R.id.reportInappropriateRadio) {
            return "INAPPROPRIATE_CONTENT";
        } else if (checkedId == R.id.reportSpamRadio) {
            return "SPAM";
        } else if (checkedId == R.id.reportHarassmentRadio) {
            return "HARASSMENT";
        } else if (checkedId == R.id.reportFakeListingRadio) {
            return "FAKE_LISTING";
        }

        return "OTHER";
    }
}
