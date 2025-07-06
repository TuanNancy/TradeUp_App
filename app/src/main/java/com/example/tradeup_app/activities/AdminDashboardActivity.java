package com.example.tradeup_app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.FlaggedUserAdapter;
import com.example.tradeup_app.adapters.ReportAdapter;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Report;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity implements ReportAdapter.OnReportActionListener, FlaggedUserAdapter.OnFlaggedUserActionListener {

    private RecyclerView reportsRecyclerView;
    private ReportAdapter reportAdapter;
    private CircularProgressIndicator progressIndicator;
    private View emptyView;

    private FirebaseManager firebaseManager;
    private List<Report> reports = new ArrayList<>();

    private RecyclerView flaggedUsersRecyclerView;
    private FlaggedUserAdapter flaggedUserAdapter;
    private List<UserModel> flaggedUsers = new ArrayList<>();
    private TabLayout adminTabLayout;

    private TextView emptyTitleText, emptyDescriptionText;
    private ImageView emptyImageView;
    private boolean isShowingReports = true; // Track current tab

    // Real-time listeners
    private ValueEventListener reportsListener;
    private ValueEventListener flaggedUsersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initViews();
        setupRecyclerView();
        setupTabs();

        // Initialize with Reports tab by default
        initializeDefaultTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("AdminDashboard", "🔄 onResume called - setting up listeners");
        // Set up real-time listeners when activity becomes visible
        setupRealtimeListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.d("AdminDashboard", "⏸️ onPause called - removing listeners");
        // Remove listeners when activity is not visible to save resources
        removeRealtimeListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.d("AdminDashboard", "🗑️ onDestroy called - cleanup");
        // Ensure listeners are removed
        removeRealtimeListeners();
    }

    private void setupRealtimeListeners() {
        android.util.Log.d("AdminDashboard", "🚀 Setting up real-time listeners");
        // Force remove any existing listeners first
        removeRealtimeListeners();

        // Add a small delay to ensure proper cleanup
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            setupReportsListener();
            setupFlaggedUsersListener();
        }, 100);
    }

    private void setupReportsListener() {
        android.util.Log.d("AdminDashboard", "🔧 Setting up reports listener - removing any existing listener first");

        if (reportsListener != null) {
            // Remove existing listener first
            firebaseManager.getDatabase().getReference(FirebaseManager.REPORTS_NODE)
                .removeEventListener(reportsListener);
            android.util.Log.d("AdminDashboard", "🗑️ Removed existing reports listener");
        }

        android.util.Log.d("AdminDashboard", "📡 Creating new ValueEventListener for reports");

        reportsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                android.util.Log.d("AdminDashboard", "🔔 Reports data changed! Total snapshots: " + dataSnapshot.getChildrenCount());

                List<Report> loadedReports = new ArrayList<>();
                int pendingCount = 0;
                int totalCount = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    totalCount++;
                    Report report = snapshot.getValue(Report.class);
                    if (report != null) {
                        report.setId(snapshot.getKey());
                        android.util.Log.d("AdminDashboard", "📄 Report " + totalCount + ": ID=" + report.getId() +
                            ", Status=" + report.getStatus() +
                            ", CreatedAt=" + report.getCreatedAt() +
                            ", Reason=" + report.getReason());

                        // Only show pending reports to admin
                        if ("PENDING".equals(report.getStatus())) {
                            loadedReports.add(report);
                            pendingCount++;
                        }
                    }
                }

                android.util.Log.d("AdminDashboard", "📊 Summary: Total reports=" + totalCount + ", Pending reports=" + pendingCount);

                // Sort reports by creation time (newest first) - CRITICAL FIX
                loadedReports.sort((r1, r2) -> {
                    // Handle potential null/zero timestamps
                    long time1 = r1.getCreatedAt() > 0 ? r1.getCreatedAt() : System.currentTimeMillis();
                    long time2 = r2.getCreatedAt() > 0 ? r2.getCreatedAt() : System.currentTimeMillis();
                    return Long.compare(time2, time1); // Descending order (newest first)
                });

                android.util.Log.d("AdminDashboard", "🔄 Sorted " + loadedReports.size() + " pending reports by creation time");

                // Update reports on UI thread
                runOnUiThread(() -> {
                    int previousSize = reports.size();
                    android.util.Log.d("AdminDashboard", "📱 Updating UI - Previous size: " + previousSize + ", New size: " + loadedReports.size());

                    reports.clear();
                    reports.addAll(loadedReports);

                    if (isShowingReports) {
                        updateReportsUI();
                        android.util.Log.d("AdminDashboard", "✅ UI updated with reports");

                        // Show notification if new reports arrived
                        if (loadedReports.size() > previousSize) {
                            int newReports = loadedReports.size() - previousSize;
                            android.util.Log.d("AdminDashboard", "🚨 NEW REPORTS DETECTED: " + newReports + " new report(s)!");

                            // Show toast notification
                            Toast.makeText(AdminDashboardActivity.this,
                                "📩 " + newReports + " new report(s) received!",
                                Toast.LENGTH_LONG).show();

                            // Also show system notification if app is in background
                            if (loadedReports.size() > 0) {
                                Report latestReport = loadedReports.get(0);
                                com.example.tradeup_app.utils.AdminNotificationHelper.showNewReportNotification(
                                    AdminDashboardActivity.this,
                                    latestReport.getReportedItemTitle() != null ? latestReport.getReportedItemTitle() : "Unknown Product",
                                    latestReport.getReason()
                                );
                            }
                        }
                    } else {
                        android.util.Log.d("AdminDashboard", "ℹ️ Not showing reports tab, skipping UI update");
                    }

                    // Log final state
                    android.util.Log.d("AdminDashboard", "🏁 Final state: " + reports.size() + " reports in UI");
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                android.util.Log.e("AdminDashboard", "❌ Reports listener cancelled: " + databaseError.getMessage());
                android.util.Log.e("AdminDashboard", "❌ Error code: " + databaseError.getCode());
                android.util.Log.e("AdminDashboard", "❌ Error details: " + databaseError.getDetails());

                runOnUiThread(() -> {
                    Toast.makeText(AdminDashboardActivity.this,
                        "⚠️ Real-time updates failed: " + databaseError.getMessage(),
                        Toast.LENGTH_LONG).show();

                    // Fallback to manual loading if listener fails
                    android.util.Log.d("AdminDashboard", "🔄 Falling back to manual loading");
                    loadReportsManually();
                });
            }
        };

        // Listen to all reports - NO server-side filtering for better real-time performance
        android.util.Log.d("AdminDashboard", "🚀 Attaching listener to Firebase path: " + FirebaseManager.REPORTS_NODE);
        firebaseManager.getDatabase().getReference(FirebaseManager.REPORTS_NODE)
            .addValueEventListener(reportsListener);

        android.util.Log.d("AdminDashboard", "✅ Reports listener setup complete - listening for real-time changes");
    }

    private void setupFlaggedUsersListener() {
        if (flaggedUsersListener != null) return; // Already set up

        flaggedUsersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<UserModel> loadedUsers = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserModel user = snapshot.getValue(UserModel.class);
                    if (user != null) {
                        // Only include flagged users
                        if (user.isFlagged()) {
                            loadedUsers.add(user);
                        }
                    }
                }

                // Update flagged users on UI thread
                runOnUiThread(() -> {
                    flaggedUsers.clear();
                    flaggedUsers.addAll(loadedUsers);

                    if (!isShowingReports) {
                        updateFlaggedUsersUI();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminDashboardActivity.this,
                        "Error loading flagged users: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            }
        };

        firebaseManager.getDatabase().getReference("Users")
            .orderByChild("isFlagged").equalTo(true)
            .addValueEventListener(flaggedUsersListener);
    }

    private void removeRealtimeListeners() {
        if (reportsListener != null) {
            firebaseManager.getDatabase().getReference(FirebaseManager.REPORTS_NODE)
                .removeEventListener(reportsListener);
            reportsListener = null;
        }

        if (flaggedUsersListener != null) {
            firebaseManager.getDatabase().getReference("Users")
                .removeEventListener(flaggedUsersListener);
            flaggedUsersListener = null;
        }
    }

    private void initViews() {
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        flaggedUsersRecyclerView = findViewById(R.id.flaggedUsersRecyclerView);
        progressIndicator = findViewById(R.id.progressIndicator);
        emptyView = findViewById(R.id.emptyView);
        adminTabLayout = findViewById(R.id.admin_tab_layout);

        // Initialize empty state views
        emptyTitleText = findViewById(R.id.emptyTitleText);
        emptyDescriptionText = findViewById(R.id.emptyDescriptionText);
        emptyImageView = findViewById(R.id.emptyImageView);

        firebaseManager = FirebaseManager.getInstance();
        setTitle("Admin Dashboard");

        // Setup toolbar with back navigation
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Handle back button click
        toolbar.setNavigationOnClickListener(v -> {
            finish(); // Close this activity and return to previous screen
        });
    }

    private void setupRecyclerView() {
        reportAdapter = new ReportAdapter(this, reports);
        reportAdapter.setOnReportActionListener(this);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportsRecyclerView.setAdapter(reportAdapter);

        flaggedUserAdapter = new FlaggedUserAdapter(this, flaggedUsers);
        flaggedUserAdapter.setOnFlaggedUserActionListener(this);
        flaggedUsersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        flaggedUsersRecyclerView.setAdapter(flaggedUserAdapter);
    }

    private void setupTabs() {
        adminTabLayout.addTab(adminTabLayout.newTab().setText("Reports"));
        adminTabLayout.addTab(adminTabLayout.newTab().setText("Flagged Users"));

        adminTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchToTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void initializeDefaultTab() {
        // Set default to Reports tab
        isShowingReports = true;
        reportsRecyclerView.setVisibility(View.VISIBLE);
        flaggedUsersRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        // Show loading state while setting up listeners
        showLoading(true);

        // Add a small delay to ensure Firebase is ready
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                setupRealtimeListeners();

                // Fallback: Load data manually after 3 seconds if listener hasn't triggered
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (reports.isEmpty() && isShowingReports) {
                        android.util.Log.w("AdminDashboard", "Fallback: Loading reports manually");
                        loadReportsManually();
                    }
                }, 3000);
            }
        }, 500);
    }

    private void loadReportsManually() {
        android.util.Log.d("AdminDashboard", "Loading reports manually as fallback");
        firebaseManager.getReportsForAdmin(new FirebaseManager.ReportCallback() {
            @Override
            public void onReportsLoaded(List<Report> loadedReports) {
                android.util.Log.d("AdminDashboard", "Manual load successful: " + loadedReports.size() + " reports");
                reports.clear();
                reports.addAll(loadedReports);
                if (isShowingReports) {
                    updateReportsUI();
                }
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("AdminDashboard", "Manual load failed: " + error);
                showLoading(false);
                Toast.makeText(AdminDashboardActivity.this, "Error loading reports: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchToTab(int position) {
        if (position == 0) {
            // Reports tab
            isShowingReports = true;
            showReportsTab();
            updateReportsUI(); // Use cached data from real-time listener
        } else {
            // Flagged Users tab
            isShowingReports = false;
            showFlaggedUsersTab();
            updateFlaggedUsersUI(); // Use cached data from real-time listener
        }
    }

    private void showReportsTab() {
        reportsRecyclerView.setVisibility(View.VISIBLE);
        flaggedUsersRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        updateEmptyStateForReports();
    }

    private void showFlaggedUsersTab() {
        reportsRecyclerView.setVisibility(View.GONE);
        flaggedUsersRecyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        updateEmptyStateForFlaggedUsers();
    }

    private void loadReports() {
        // ✅ DEPRECATED: Replace with real-time listener
        // This method is now only used as fallback if real-time listener fails
        showLoading(true);

        firebaseManager.getReportsForAdmin(new FirebaseManager.ReportCallback() {
            @Override
            public void onReportsLoaded(List<Report> loadedReports) {
                reports.clear();
                reports.addAll(loadedReports);
                updateReportsUI();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(AdminDashboardActivity.this, "Error loading reports: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyStateForReports() {
        emptyTitleText.setText("No pending reports");
        emptyDescriptionText.setText("All reports have been reviewed");
        emptyImageView.setImageResource(R.drawable.ic_flag);
    }

    private void updateEmptyStateForFlaggedUsers() {
        emptyTitleText.setText("No flagged users");
        emptyDescriptionText.setText("No users have been flagged");
        emptyImageView.setImageResource(R.drawable.ic_user_placeholder);
    }

    private void loadFlaggedUsers() {
        // ✅ DEPRECATED: Replace with real-time listener
        // This method is now only used as fallback if real-time listener fails
        showLoading(true);

        firebaseManager.getFlaggedUsers(new FirebaseManager.FlaggedUsersCallback() {
            @Override
            public void onFlaggedUsersLoaded(List<UserModel> users) {
                flaggedUsers.clear();
                flaggedUsers.addAll(users);
                updateFlaggedUsersUI();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(AdminDashboardActivity.this, "Error loading flagged users: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateReportsUI() {
        showLoading(false);
        reportAdapter.updateReports(reports);

        if (reports.isEmpty()) {
            updateEmptyStateForReports();
            emptyView.setVisibility(View.VISIBLE);
            reportsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            reportsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateFlaggedUsersUI() {
        showLoading(false);
        flaggedUserAdapter.updateUsers(flaggedUsers);

        if (flaggedUsers.isEmpty()) {
            updateEmptyStateForFlaggedUsers();
            emptyView.setVisibility(View.VISIBLE);
            flaggedUsersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            flaggedUsersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateUI() {
        // ✅ DEPRECATED: Split into updateReportsUI() and updateFlaggedUsersUI()
        if (isShowingReports) {
            updateReportsUI();
        } else {
            updateFlaggedUsersUI();
        }
    }

    private void showLoading(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show) {
            // Hide both RecyclerViews and empty view when loading
            reportsRecyclerView.setVisibility(View.GONE);
            flaggedUsersRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            // Show appropriate views based on current tab after loading
            if (isShowingReports) {
                flaggedUsersRecyclerView.setVisibility(View.GONE);
            } else {
                reportsRecyclerView.setVisibility(View.GONE);
            }
        }
    }

    // ReportAdapter.OnReportActionListener implementations
    @Override
    public void onResolveReport(Report report, String action, String notes) {
        String adminId = firebaseManager.getCurrentUserId();

        // Show action selection dialog for resolving report
        showReportActionDialog(report, adminId, notes);
    }

    private void showReportActionDialog(Report report, String adminId, String notes) {
        String[] actions = {"Warning", "Temporary Suspension", "Permanent Ban", "Remove Content Only"};

        new AlertDialog.Builder(this)
            .setTitle("Resolve Report: " + report.getReason())
            .setMessage("Reported User: " + report.getReportedUserName() + "\nReason: " + report.getDescription())
            .setSingleChoiceItems(actions, 0, null)
            .setPositiveButton("Apply Action", (dialog, which) -> {
                int selectedAction = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                String actionTaken = actions[selectedAction];

                // Update report status
                firebaseManager.updateReportStatus(
                    report.getId(),
                    "RESOLVED",
                    adminId,
                    notes,
                    actionTaken,
                    task -> {
                        if (task.isSuccessful()) {
                            // Auto-flag user if action is taken against them
                            if (selectedAction < 3) { // Warning, Suspension, or Ban
                                flagUserFromReport(report, actionTaken);
                            }
                            Toast.makeText(this, "Report resolved with action: " + actionTaken, Toast.LENGTH_SHORT).show();
                            loadReports(); // Refresh the list
                        } else {
                            Toast.makeText(this, "Failed to resolve report", Toast.LENGTH_SHORT).show();
                        }
                    }
                );
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void flagUserFromReport(Report report, String actionTaken) {
        // Flag the reported user and add reason
        Map<String, Object> updates = new HashMap<>();
        updates.put("isFlagged", true);
        updates.put("flaggedReason", "Report resolved: " + report.getReason() + " - Action: " + actionTaken);

        firebaseManager.getDatabase().getReference("Users")
            .child(report.getReportedUserId())
            .updateChildren(updates);
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

    // FlaggedUserAdapter.OnFlaggedUserActionListener implementations
    @Override
    public void onDeleteUser(UserModel user) {
        new AlertDialog.Builder(this)
            .setTitle("Delete User Account")
            .setMessage("Are you sure you want to permanently delete user: " + user.getUsername() + "?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                performUserAction(user, "DELETE", "Account permanently deleted by admin");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onSuspendUser(UserModel user) {
        showSuspensionDialog(user);
    }

    @Override
    public void onWarnUser(UserModel user) {
        showWarningDialog(user);
    }

    private void showSuspensionDialog(UserModel user) {
        String[] durations = {"1 day", "3 days", "1 week", "1 month", "Permanent"};

        new AlertDialog.Builder(this)
            .setTitle("Suspend User: " + user.getUsername())
            .setSingleChoiceItems(durations, 2, null) // Default to 1 week
            .setPositiveButton("Suspend", (dialog, which) -> {
                int selectedDuration = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                String duration = durations[selectedDuration];
                performUserAction(user, "SUSPEND", "Account suspended for: " + duration);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showWarningDialog(UserModel user) {
        String[] warningTypes = {"Inappropriate Content", "Spam/Harassment", "Fake Listings", "Terms Violation", "Custom Warning"};

        new AlertDialog.Builder(this)
            .setTitle("Warn User: " + user.getUsername())
            .setSingleChoiceItems(warningTypes, 0, null)
            .setPositiveButton("Send Warning", (dialog, which) -> {
                int selectedWarning = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                String warningType = warningTypes[selectedWarning];
                performUserAction(user, "WARN", "Warning issued for: " + warningType);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performUserAction(UserModel user, String action, String reason) {
        Map<String, Object> updates = new HashMap<>();

        switch (action) {
            case "DELETE":
                // Mark user as deleted (keep data for audit but disable account)
                updates.put("deactivated", true);
                updates.put("deletedByAdmin", true);
                updates.put("deletionReason", reason);
                updates.put("deletedAt", System.currentTimeMillis());
                break;

            case "SUSPEND":
                updates.put("deactivated", true);
                updates.put("suspendedByAdmin", true);
                updates.put("suspensionReason", reason);
                updates.put("suspendedAt", System.currentTimeMillis());
                break;

            case "WARN":
                // Keep user active but add warning
                updates.put("warningCount", user.getRating() + 1); // Using rating field temporarily
                updates.put("lastWarningReason", reason);
                updates.put("lastWarningAt", System.currentTimeMillis());
                break;
        }

        // Update user in Firebase
        firebaseManager.getDatabase().getReference("Users")
            .child(user.getUid())
            .updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, action + " applied to user: " + user.getUsername(), Toast.LENGTH_SHORT).show();

                // If not just a warning, remove from flagged list
                if (!action.equals("WARN")) {
                    unflagUser(user);
                }

                // Refresh flagged users list
                loadFlaggedUsers();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to apply " + action + " to user", Toast.LENGTH_SHORT).show();
            });
    }

    private void unflagUser(UserModel user) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isFlagged", false);
        updates.put("flaggedReason", "");

        firebaseManager.getDatabase().getReference("Users")
            .child(user.getUid())
            .updateChildren(updates);
    }
}
