package com.example.tradeup_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ConversationAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.activities.ChatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessagesFragment extends Fragment {

    private RecyclerView conversationsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ConversationAdapter conversationAdapter;
    private FirebaseManager firebaseManager;
    private List<Conversation> conversationList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        initViews(view);
        setupRecyclerView();
        loadConversations();

        return view;
    }

    private void initViews(View view) {
        conversationsRecyclerView = view.findViewById(R.id.conversations_recycler_view);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        firebaseManager = FirebaseManager.getInstance();
        conversationList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        // Create adapter with the correct constructor signature
        conversationAdapter = new ConversationAdapter(getContext(), conversationList, new ConversationAdapter.OnConversationClickListener() {
            @Override
            public void onConversationClick(Conversation conversation) {
                String currentUserId = firebaseManager.getCurrentUserId();
                String otherUserId = conversation.getOtherParticipantId(currentUserId);
                String otherUserName = conversation.getOtherParticipantName(currentUserId);

                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("conversationId", conversation.getId());
                intent.putExtra("receiverId", otherUserId);
                intent.putExtra("receiverName", otherUserName);
                intent.putExtra("productTitle", conversation.getProductTitle());
                intent.putExtra("productId", conversation.getProductId()); // Add missing productId
                startActivity(intent);
            }

            @Override
            public void onConversationLongClick(Conversation conversation) {
                // Handle long click if needed (handled by adapter now)
            }

            @Override
            public void onConversationDeleted(Conversation conversation) {
                // Refresh the conversation list after deletion
                loadConversations();
                android.util.Log.d("MessagesFragment", "Conversation deleted: " + conversation.getId());
            }

            @Override
            public void onConversationBlocked(Conversation conversation) {
                // Optionally refresh or show a message
                String otherUserName = conversation.getOtherParticipantName(firebaseManager.getCurrentUserId());
                android.util.Log.d("MessagesFragment", "User blocked: " + otherUserName);

                // You might want to filter out blocked conversations or mark them differently
                // For now, we'll just refresh the list
                loadConversations();
            }

            @Override
            public void onConversationReported(Conversation conversation) {
                // Log the report and optionally refresh
                android.util.Log.d("MessagesFragment", "Conversation reported: " + conversation.getId());

                // Refresh to show visual indication of reported conversation
                conversationAdapter.notifyDataSetChanged();
            }
        });

        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        conversationsRecyclerView.setAdapter(conversationAdapter);
    }

    private void loadConversations() {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            android.util.Log.w("MessagesFragment", "User not authenticated, cannot load conversations");
            return;
        }

        android.util.Log.d("MessagesFragment", "Loading conversations for user: " + currentUserId);

        // Load conversations from Firebase
        firebaseManager.getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                        android.util.Log.d("MessagesFragment", "Received " + dataSnapshot.getChildrenCount() + " conversation snapshots");

                        List<Conversation> conversations = new ArrayList<>();

                        for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            try {
                                Conversation conversation = snapshot.getValue(Conversation.class);
                                if (conversation != null) {
                                    conversation.setId(snapshot.getKey());

                                    // Only include conversations where current user is a participant
                                    if (conversation.getBuyerId().equals(currentUserId) ||
                                            conversation.getSellerId().equals(currentUserId)) {

                                        // Check if user is not blocked
                                        if (!conversation.isUserBlocked(currentUserId)) {
                                            conversations.add(conversation);
                                            android.util.Log.d("MessagesFragment", "Added conversation: " + conversation.getId());
                                        } else {
                                            android.util.Log.d("MessagesFragment", "User blocked in conversation: " + conversation.getId());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                android.util.Log.e("MessagesFragment", "Error parsing conversation", e);
                            }
                        }

                        android.util.Log.d("MessagesFragment", "Final conversation count: " + conversations.size());
                        updateConversations(conversations);
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                        android.util.Log.e("MessagesFragment", "Firebase error: " + databaseError.getMessage());
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading conversations: " + databaseError.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void updateConversations(List<Conversation> conversations) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Sắp xếp conversations theo thời gian tin nhắn mới nhất (giảm dần)
                conversations.sort((c1, c2) -> Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

                conversationList.clear();
                conversationList.addAll(conversations);
                conversationAdapter.notifyDataSetChanged();

                // Update empty state visibility
                updateEmptyState();
            });
        }
    }

    private void updateEmptyState() {
        if (conversationList.isEmpty()) {
            conversationsRecyclerView.setVisibility(View.GONE);
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
        } else {
            conversationsRecyclerView.setVisibility(View.VISIBLE);
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh conversations when fragment becomes visible
        loadConversations();
    }
}
