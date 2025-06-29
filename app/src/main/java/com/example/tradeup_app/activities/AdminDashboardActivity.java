package com.example.tradeup_app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ReportAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Report;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity implements ReportAdapter.OnReportActionListener {

    private RecyclerView reportsRecyclerView;
    private ReportAdapter reportAdapter;
    private CircularProgressIndicator progressIndicator;
    private View emptyView;

    private FirebaseManager firebaseManager;
    private List<Report> reports = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initViews();
        setupRecyclerView();
        loadReports();
    }

    private void initViews() {
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        progressIndicator = findViewById(R.id.progressIndicator);
        emptyView = findViewById(R.id.emptyView);

        firebaseManager = FirebaseManager.getInstance();
        setTitle("Admin Dashboard - Reports");
    }

    private void setupRecyclerView() {
        reportAdapter = new ReportAdapter(this, reports);
        reportAdapter.setOnReportActionListener(this);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportsRecyclerView.setAdapter(reportAdapter);
    }

    private void loadReports() {
        showLoading(true);

        firebaseManager.getReportsForAdmin(new FirebaseManager.ReportCallback() {
            @Override
            public void onReportsLoaded(List<Report> loadedReports) {
                reports.clear();
                reports.addAll(loadedReports);
                updateUI();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(AdminDashboardActivity.this, "Error loading reports: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        showLoading(false);
        reportAdapter.updateReports(reports);

        if (reports.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            reportsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            reportsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        reportsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // ReportAdapter.OnReportActionListener implementations
    @Override
    public void onResolveReport(Report report, String action, String notes) {
        String adminId = firebaseManager.getCurrentUserId();

        firebaseManager.updateReportStatus(
            report.getId(),
            "RESOLVED",
            adminId,
            notes,
            action,
            task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Report resolved successfully", Toast.LENGTH_SHORT).show();
                    loadReports(); // Refresh the list
                } else {
                    Toast.makeText(this, "Failed to resolve report", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    @Override
    public void onDismissReport(Report report, String notes) {
        String adminId = firebaseManager.getCurrentUserId();

        firebaseManager.updateReportStatus(
            report.getId(),
            "DISMISSED",
            adminId,
            notes,
            "NONE",
            task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Report dismissed", Toast.LENGTH_SHORT).show();
                    loadReports(); // Refresh the list
                } else {
                    Toast.makeText(this, "Failed to dismiss report", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    @Override
    public void onViewReportDetails(Report report) {
        // TODO: Show detailed report information dialog
        Toast.makeText(this, "Report Details: " + report.getReason(), Toast.LENGTH_SHORT).show();
    }
}
