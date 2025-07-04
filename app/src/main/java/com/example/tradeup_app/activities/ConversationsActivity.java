package com.example.tradeup_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ConversationAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ConversationsActivity extends AppCompatActivity {
    private static final String TAG = "ConversationsActivity";

    // UI Components
    private RecyclerView recyclerViewConversations;
    private TextView textViewEmptyState;
    private Toolbar toolbar;

    // Data
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;
    private FirebaseManager firebaseManager;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        // Initialize UI
        initializeUI();

        // Initialize services
        firebaseManager = FirebaseManager.getInstance();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup listeners
        setupListeners();

        // Load conversations
        loadConversations();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_conversations, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadConversations();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeUI() {
        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Null check for ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Messages");
        }

        // Initialize views
        recyclerViewConversations = findViewById(R.id.recyclerViewConversations);
        textViewEmptyState = findViewById(R.id.textViewEmptyState);

        // Initially hide empty state
        textViewEmptyState.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        conversationList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(this, conversationList, new ConversationAdapter.OnConversationClickListener() {
            @Override
            public void onConversationClick(Conversation conversation) {
                openChat(conversation);
            }

            @Override
            public void onConversationLongClick(Conversation conversation) {
                // Handled by adapter internally now
            }

            @Override
            public void onConversationDeleted(Conversation conversation) {
                // Refresh the conversation list after deletion
                loadConversations();
                Toast.makeText(ConversationsActivity.this, "Conversation deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConversationBlocked(Conversation conversation) {
                // Refresh conversations after blocking user
                loadConversations();
                String otherUserName = conversation.getOtherParticipantName(firebaseManager.getCurrentUserId());
                Toast.makeText(ConversationsActivity.this, otherUserName + " has been blocked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConversationReported(Conversation conversation) {
                // Show confirmation and optionally refresh
                Toast.makeText(ConversationsActivity.this, "Conversation reported successfully", Toast.LENGTH_SHORT).show();
                // Notify specific item changed instead of entire dataset
                int position = conversationList.indexOf(conversation);
                if (position != -1) {
                    conversationAdapter.notifyItemChanged(position);
                }
            }
        });

        recyclerViewConversations.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewConversations.setAdapter(conversationAdapter);
    }

    private void setupListeners() {
        // Back button
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadConversations() {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLoading) {
            return; // Prevent multiple simultaneous loads
        }

        isLoading = true;
        Log.d(TAG, "Loading conversations...");

        // Load conversations from Firebase
        firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                        List<Conversation> conversations = new ArrayList<>();

                        for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Conversation conversation = snapshot.getValue(Conversation.class);
                            if (conversation != null) {
                                conversation.setId(snapshot.getKey());

                                // Only include conversations where current user is a participant
                                if (conversation.getBuyerId().equals(currentUserId) ||
                                    conversation.getSellerId().equals(currentUserId)) {

                                    // Check if user is not blocked
                                    if (!conversation.isUserBlocked(currentUserId)) {
                                        conversations.add(conversation);
                                    }
                                }
                            }
                        }

                        // Sort by last message time (newest first) - use List.sort instead of Collections.sort
                        conversations.sort((c1, c2) ->
                            Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

                        updateConversationsList(conversations);
                        isLoading = false;
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                        Log.e(TAG, "Error loading conversations", databaseError.toException());
                        runOnUiThread(() -> {
                            Toast.makeText(ConversationsActivity.this,
                                "Error loading conversations: " + databaseError.getMessage(),
                                Toast.LENGTH_LONG).show();
                            isLoading = false;
                        });
                    }
                });
    }

    private void updateConversationsList(List<Conversation> conversations) {
        runOnUiThread(() -> {
            // Calculate differences for better performance
            int oldSize = conversationList.size();
            conversationList.clear();
            conversationList.addAll(conversations);

            // Use more specific notifications
            if (oldSize == 0 && !conversations.isEmpty()) {
                conversationAdapter.notifyItemRangeInserted(0, conversations.size());
            } else if (conversations.isEmpty() && oldSize > 0) {
                conversationAdapter.notifyItemRangeRemoved(0, oldSize);
            } else {
                conversationAdapter.notifyDataSetChanged(); // Fallback for complex changes
            }

            // Show/hide empty state
            if (conversationList.isEmpty()) {
                recyclerViewConversations.setVisibility(View.GONE);
                textViewEmptyState.setVisibility(View.VISIBLE);
                textViewEmptyState.setText("No conversations yet.\nStart chatting with sellers and buyers!");
            } else {
                recyclerViewConversations.setVisibility(View.VISIBLE);
                textViewEmptyState.setVisibility(View.GONE);
            }
        });
    }

    private void openChat(Conversation conversation) {
        String currentUserId = firebaseManager.getCurrentUserId();
        String otherUserId = conversation.getOtherParticipantId(currentUserId);
        String otherUserName = conversation.getOtherParticipantName(currentUserId);

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationId", conversation.getId());
        intent.putExtra("receiverId", otherUserId);
        intent.putExtra("receiverName", otherUserName);
        intent.putExtra("productTitle", conversation.getProductTitle());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh conversations when returning to this activity
        loadConversations();
    }
}
