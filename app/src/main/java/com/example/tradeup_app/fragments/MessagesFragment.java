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
import java.util.List;

public class MessagesFragment extends Fragment {

    private RecyclerView conversationsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ConversationAdapter conversationAdapter;
    private FirebaseManager firebaseManager;

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
    }

    private void setupRecyclerView() {
        conversationAdapter = new ConversationAdapter(getContext(), new ArrayList<>());
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        conversationsRecyclerView.setAdapter(conversationAdapter);

        conversationAdapter.setOnConversationClickListener(conversation -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("conversationId", conversation.getId());
            intent.putExtra("productId", conversation.getProductId());
            intent.putExtra("sellerId", conversation.getSellerId());
            startActivity(intent);
        });
    }

    private void loadConversations() {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) return;

        firebaseManager.getConversationsForUser(currentUserId, new FirebaseManager.ConversationCallback() {
            @Override
            public void onConversationsLoaded(List<Conversation> conversations) {
                conversationAdapter.updateConversations(conversations);

                // Update empty state visibility
                updateEmptyState();
            }

            @Override
            public void onError(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                }
                updateEmptyState();
            }
        });
    }

    private void showEmptyState() {
        conversationsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        conversationsRecyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void updateEmptyState() {
        if (conversationAdapter.getItemCount() == 0) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh conversations when fragment resumes
        loadConversations();
    }
}
