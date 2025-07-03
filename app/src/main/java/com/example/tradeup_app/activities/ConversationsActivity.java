package com.example.tradeup_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ConversationAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.services.MessagingService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConversationsActivity extends AppCompatActivity {
    private static final String TAG = "ConversationsActivity";

    // UI Components
    private RecyclerView recyclerViewConversations;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textViewEmptyState;
    private Toolbar toolbar;

    // Data
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;
    private MessagingService messagingService;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        // Initialize UI
        initializeUI();

        // Initialize services
        messagingService = new MessagingService();
        firebaseManager = FirebaseManager.getInstance();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup listeners
        setupListeners();

        // Load conversations
        loadConversations();
    }

    private void initializeUI() {
        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Messages");

        // Initialize views
        recyclerViewConversations = findViewById(R.id.recyclerViewConversations);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
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
                // Refresh to show visual indication
                conversationAdapter.notifyDataSetChanged();
            }
        });

        recyclerViewConversations.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewConversations.setAdapter(conversationAdapter);
    }

    private void setupListeners() {
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadConversations);

        // Back button
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadConversations() {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        // Load conversations from Firebase
        firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
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

                        // Sort by last message time (newest first)
                        Collections.sort(conversations, (c1, c2) ->
                            Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

                        updateConversationsList(conversations);
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                        runOnUiThread(() -> {
                            Toast.makeText(ConversationsActivity.this,
                                "Error loading conversations: " + databaseError.getMessage(),
                                Toast.LENGTH_LONG).show();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                });
    }

    private void updateConversationsList(List<Conversation> conversations) {
        runOnUiThread(() -> {
            conversationList.clear();
            conversationList.addAll(conversations);
            conversationAdapter.notifyDataSetChanged();

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
