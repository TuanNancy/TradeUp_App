package com.example.tradeup_app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.BlockedUsersAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.User;
import com.example.tradeup_app.services.MessagingService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BlockedUsersActivity extends AppCompatActivity {
    private static final String TAG = "BlockedUsersActivity";

    // UI Components
    private RecyclerView recyclerViewBlockedUsers;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textViewEmptyState;
    private Toolbar toolbar;

    // Data
    private BlockedUsersAdapter blockedUsersAdapter;
    private List<User> blockedUsersList;
    private MessagingService messagingService;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_users);

        initializeUI();
        setupRecyclerView();
        setupListeners();
        loadBlockedUsers();
    }

    private void initializeUI() {
        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Blocked Users");

        // Initialize views
        recyclerViewBlockedUsers = findViewById(R.id.recyclerViewBlockedUsers);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        textViewEmptyState = findViewById(R.id.textViewEmptyState);

        // Initialize services
        messagingService = new MessagingService();
        firebaseManager = FirebaseManager.getInstance();

        // Initially hide empty state
        textViewEmptyState.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        blockedUsersList = new ArrayList<>();
        blockedUsersAdapter = new BlockedUsersAdapter(this, blockedUsersList, new BlockedUsersAdapter.OnBlockedUserClickListener() {
            @Override
            public void onUnblockUserClick(User user) {
                showUnblockConfirmationDialog(user);
            }
        });

        recyclerViewBlockedUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBlockedUsers.setAdapter(blockedUsersAdapter);
    }

    private void setupListeners() {
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadBlockedUsers);

        // Back button
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadBlockedUsers() {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        // Load blocked users from user's profile
        firebaseManager.getDatabase()
                .getReference(FirebaseManager.USERS_NODE)
                .child(currentUserId)
                .child("blockedUsers")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<String> blockedUserIds = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Boolean isBlocked = snapshot.getValue(Boolean.class);
                            if (isBlocked != null && isBlocked) {
                                blockedUserIds.add(snapshot.getKey());
                            }
                        }

                        loadUserDetails(blockedUserIds);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        runOnUiThread(() -> {
                            Toast.makeText(BlockedUsersActivity.this,
                                "Error loading blocked users: " + databaseError.getMessage(),
                                Toast.LENGTH_LONG).show();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                });
    }

    private void loadUserDetails(List<String> blockedUserIds) {
        if (blockedUserIds.isEmpty()) {
            updateBlockedUsersList(new ArrayList<>());
            return;
        }

        List<User> users = new ArrayList<>();
        int[] loadedCount = {0};

        for (String userId : blockedUserIds) {
            firebaseManager.getDatabase()
                    .getReference(FirebaseManager.USERS_NODE)
                    .child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            loadedCount[0]++;

                            if (dataSnapshot.exists()) {
                                User user = dataSnapshot.getValue(User.class);
                                if (user != null) {
                                    user.setId(userId);
                                    users.add(user);
                                }
                            }

                            // Check if all users are loaded
                            if (loadedCount[0] == blockedUserIds.size()) {
                                updateBlockedUsersList(users);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            loadedCount[0]++;
                            if (loadedCount[0] == blockedUserIds.size()) {
                                updateBlockedUsersList(users);
                            }
                        }
                    });
        }
    }

    private void updateBlockedUsersList(List<User> users) {
        runOnUiThread(() -> {
            blockedUsersList.clear();
            blockedUsersList.addAll(users);
            blockedUsersAdapter.notifyDataSetChanged();

            // Show/hide empty state
            if (blockedUsersList.isEmpty()) {
                recyclerViewBlockedUsers.setVisibility(View.GONE);
                textViewEmptyState.setVisibility(View.VISIBLE);
                textViewEmptyState.setText("No blocked users.\nYou haven't blocked anyone yet.");
            } else {
                recyclerViewBlockedUsers.setVisibility(View.VISIBLE);
                textViewEmptyState.setVisibility(View.GONE);
            }

            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void showUnblockConfirmationDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Unblock User")
                .setMessage("Are you sure you want to unblock " + user.getName() + "? You will be able to receive messages from this user again.")
                .setPositiveButton("Unblock", (dialog, which) -> {
                    unblockUser(user);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unblockUser(User user) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove from blocked users list in user profile
        firebaseManager.getDatabase()
                .getReference(FirebaseManager.USERS_NODE)
                .child(currentUserId)
                .child("blockedUsers")
                .child(user.getId())
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, user.getName() + " has been unblocked", Toast.LENGTH_SHORT).show();
                        loadBlockedUsers(); // Refresh the list
                    } else {
                        Toast.makeText(this, "Failed to unblock user", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBlockedUsers();
    }
}
