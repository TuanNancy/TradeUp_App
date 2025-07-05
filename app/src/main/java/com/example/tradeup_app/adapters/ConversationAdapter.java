package com.example.tradeup_app.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.services.MessagingService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {
    private Context context;
    private List<Conversation> conversationList;
    private OnConversationClickListener listener;
    private String currentUserId;
    private MessagingService messagingService;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
        void onConversationLongClick(Conversation conversation);
        void onConversationDeleted(Conversation conversation);
        void onConversationBlocked(Conversation conversation);
        void onConversationReported(Conversation conversation);
    }

    public ConversationAdapter(Context context, List<Conversation> conversationList, OnConversationClickListener listener) {
        this.context = context;
        this.conversationList = conversationList;
        this.listener = listener;
        this.currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        this.messagingService = new MessagingService();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);

        // ✅ SỬA: Hiển thị title thông minh dựa trên conversation type
        String displayTitle = conversation.getDisplayTitle();
        if (displayTitle == null || displayTitle.isEmpty()) {
            if (conversation.getProductTitle() != null && !conversation.getProductTitle().isEmpty()) {
                displayTitle = "Về: " + conversation.getProductTitle();
            } else {
                displayTitle = "Chat chung";
            }
        }
        holder.textViewProductTitle.setText(displayTitle);

        // Set other participant name
        String otherParticipantName = conversation.getOtherParticipantName(currentUserId);
        if (otherParticipantName == null || otherParticipantName.equals("Unknown")) {
            // Fallback: try to get name from Firebase
            loadParticipantName(conversation, holder.textViewParticipantName);
        } else {
            holder.textViewParticipantName.setText(otherParticipantName);
        }

        // Set last message
        String lastMessage = conversation.getLastMessage();
        if (lastMessage == null || lastMessage.isEmpty()) {
            lastMessage = "Cuộc trò chuyện bắt đầu";
        }
        holder.textViewLastMessage.setText(lastMessage);

        // Set timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        String timeText = sdf.format(new Date(conversation.getLastMessageTime()));
        holder.textViewTime.setText(timeText);

        // ✅ SỬA: Logic unread đơn giản và chính xác hơn
        boolean hasUnread = checkIfConversationHasUnread(conversation);
        final boolean isUnread = hasUnread;

        // Debug log
        android.util.Log.d("ConversationAdapter", "Conversation " + conversation.getId() +
            " - hasUnread: " + isUnread +
            " - lastMessage: " + conversation.getLastMessage() +
            " - otherParticipant: " + otherParticipantName);

        // Set visual indicators for read/unread status
        if (isUnread) {
            // Unread conversation - make text bold and show unread indicator
            holder.textViewProductTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.textViewLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.textViewLastMessage.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.textViewParticipantName.setTextColor(context.getResources().getColor(android.R.color.black));

            // Show unread indicator
            holder.imageViewReadStatus.setVisibility(View.VISIBLE);
            holder.imageViewReadStatus.setImageResource(R.drawable.ic_message_unread);

            // Set background to slightly highlighted
            holder.itemView.setBackgroundColor(0xFFE3F2FD); // Light blue background
        } else {
            // Read conversation - normal text style
            holder.textViewProductTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.textViewLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.textViewLastMessage.setTextColor(0xFF757575); // Gray color
            holder.textViewParticipantName.setTextColor(0xFF757575); // Gray color

            // Hide read indicator
            holder.imageViewReadStatus.setVisibility(View.GONE);

            // Normal background
            holder.itemView.setBackgroundColor(0xFFFFFFFF); // White background
        }

        // Set unread count (legacy support)
        if (conversation.getUnreadCount() > 0) {
            holder.textViewUnreadCount.setVisibility(View.VISIBLE);
            holder.textViewUnreadCount.setText(String.valueOf(conversation.getUnreadCount()));
        } else {
            holder.textViewUnreadCount.setVisibility(View.GONE);
        }

        // ✅ SỬA: Load product image thông minh
        loadConversationImage(conversation, holder.imageViewProduct);

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Mark conversation as read when clicked
                if (isUnread) {
                    updateConversationReadStatus(conversation);
                    // Refresh the item to update UI
                    notifyItemChanged(position);
                }
                listener.onConversationClick(conversation);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                showConversationOptionsDialog(conversation, adapterPosition);
            }
            return true;
        });

        // Visual indication if conversation is reported or blocked
        if (conversation.isReported()) {
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }
    }

    private void showConversationOptionsDialog(Conversation conversation, int position) {
        String[] options = {"Delete Conversation", "Block User", "Report Conversation"};

        new AlertDialog.Builder(context)
            .setTitle("Conversation Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Delete Conversation
                        showDeleteConfirmationDialog(conversation, position);
                        break;
                    case 1: // Block User
                        showBlockConfirmationDialog(conversation, position);
                        break;
                    case 2: // Report Conversation
                        showReportDialog(conversation, position);
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showDeleteConfirmationDialog(Conversation conversation, int position) {
        new AlertDialog.Builder(context)
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete this conversation? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteConversation(conversation, position);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showBlockConfirmationDialog(Conversation conversation, int position) {
        String otherUserName = conversation.getOtherParticipantName(currentUserId);
        new AlertDialog.Builder(context)
            .setTitle("Block User")
            .setMessage("Are you sure you want to block " + otherUserName + "? You will no longer receive messages from this user.")
            .setPositiveButton("Block", (dialog, which) -> {
                blockConversation(conversation, position);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showReportDialog(Conversation conversation, int position) {
        String[] reasons = {
            "Spam or unwanted messages",
            "Inappropriate content",
            "Harassment or bullying",
            "Scam or fraud",
            "Other"
        };

        new AlertDialog.Builder(context)
            .setTitle("Report Conversation")
            .setMessage("Why are you reporting this conversation?")
            .setSingleChoiceItems(reasons, -1, null)
            .setPositiveButton("Report", (dialog, which) -> {
                int selectedIndex = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                if (selectedIndex >= 0) {
                    String reason = reasons[selectedIndex];
                    reportConversation(conversation, reason, position);
                } else {
                    Toast.makeText(context, "Please select a reason", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteConversation(Conversation conversation, int position) {
        showLoadingDialog("Deleting conversation...");

        messagingService.deleteConversation(conversation.getId(), new MessagingService.ConversationCallback() {
            @Override
            public void onConversationsLoaded(List<Conversation> conversations) {}

            @Override
            public void onConversationCreated(String conversationId) {
                hideLoadingDialog();
                conversationList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, conversationList.size());

                if (listener != null) {
                    listener.onConversationDeleted(conversation);
                }
                Toast.makeText(context, "Conversation deleted successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                Toast.makeText(context, "Failed to delete conversation: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void blockConversation(Conversation conversation, int position) {
        showLoadingDialog("Blocking user...");

        String otherUserId = conversation.getOtherParticipantId(currentUserId);
        messagingService.blockUser(conversation.getId(), otherUserId, new MessagingService.BlockCallback() {
            @Override
            public void onUserBlocked(boolean success) {
                hideLoadingDialog();
                if (success) {
                    // Optionally remove from list or mark as blocked
                    conversation.setBlocked(true);
                    notifyItemChanged(position);

                    if (listener != null) {
                        listener.onConversationBlocked(conversation);
                    }

                    String otherUserName = conversation.getOtherParticipantName(currentUserId);
                    Toast.makeText(context, otherUserName + " has been blocked", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to block user", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUserUnblocked(boolean success) {}

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                Toast.makeText(context, "Failed to block user: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void reportConversation(Conversation conversation, String reason, int position) {
        showLoadingDialog("Reporting conversation...");

        messagingService.reportConversation(conversation.getId(), reason, task -> {
            hideLoadingDialog();
            if (task.isSuccessful()) {
                conversation.setReported(true);
                conversation.setReportReason(reason);
                notifyItemChanged(position);

                if (listener != null) {
                    listener.onConversationReported(conversation);
                }
                Toast.makeText(context, "Conversation reported successfully. Thank you for keeping our community safe.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Failed to report conversation", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private AlertDialog loadingDialog;

    private void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(false);
            loadingDialog = builder.create();
        }
        loadingDialog.setMessage(message);
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    // Method to update conversation read status in Firebase
    private void updateConversationReadStatus(Conversation conversation) {
        FirebaseManager.getInstance().getDatabase()
                .getReference(FirebaseManager.CONVERSATIONS_NODE)
                .child(conversation.getId())
                .child("lastReadTimes")
                .child(currentUserId)
                .setValue(System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ConversationAdapter", "Read status updated successfully");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ConversationAdapter", "Failed to update read status", e);
                });
    }

    // NEW: Check if conversation has unread messages
    private boolean checkIfConversationHasUnread(Conversation conversation) {
        // Check if there's a last message
        if (conversation.getLastMessage() == null || conversation.getLastMessage().trim().isEmpty()) {
            return false;
        }

        // Check lastReadTimes
        if (conversation.getLastReadTimes() == null ||
            !conversation.getLastReadTimes().containsKey(currentUserId)) {
            // User has never read this conversation -> unread
            return true;
        }

        // User has read before, check if there are new messages
        Long userLastReadTime = conversation.getLastReadTimes().get(currentUserId);
        if (userLastReadTime != null && conversation.getLastMessageTime() > userLastReadTime) {
            // There's a new message after user's last read time -> unread
            return true;
        }

        // Check if current user is the last message sender
        if (conversation.getLastMessageSenderId() != null &&
            conversation.getLastMessageSenderId().equals(currentUserId)) {
            // User sent the last message -> mark as read for them
            return false;
        }

        // Fallback: check unreadCount
        return conversation.getUnreadCount() > 0;
    }

    // NEW: Load participant name from Firebase if not available
    private void loadParticipantName(Conversation conversation, TextView nameTextView) {
        String otherUserId = conversation.getOtherParticipantId(currentUserId);
        if (otherUserId == null) {
            nameTextView.setText("Unknown User");
            return;
        }

        // Load name from Firebase
        messagingService.getUserProfile(otherUserId, new MessagingService.UserProfileCallback() {
            @Override
            public void onSuccess(String userName, String userAvatar) {
                nameTextView.setText(userName);
                // Update conversation object for future use
                if (conversation.getBuyerId().equals(currentUserId)) {
                    conversation.setSellerName(userName);
                } else {
                    conversation.setBuyerName(userName);
                }
            }

            @Override
            public void onError(String error) {
                nameTextView.setText("Unknown User");
            }
        });
    }

    // NEW: Load conversation image intelligently
    private void loadConversationImage(Conversation conversation, ImageView imageView) {
        String imageUrl = null;

        // Priority 1: Main product image
        if (conversation.getProductImageUrl() != null && !conversation.getProductImageUrl().isEmpty()) {
            imageUrl = conversation.getProductImageUrl();
        }
        // Priority 2: First product in products list
        else if (conversation.getProducts() != null && !conversation.getProducts().isEmpty()) {
            for (Object productObj : conversation.getProducts().values()) {
                if (productObj instanceof Map) {
                    Map<String, Object> productInfo = (Map<String, Object>) productObj;
                    String productImageUrl = (String) productInfo.get("productImageUrl");
                    if (productImageUrl != null && !productImageUrl.isEmpty()) {
                        imageUrl = productImageUrl;
                        break;
                    }
                }
            }
        }

        // Load image or use default
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(imageView);
        } else {
            // Use default chat icon for general conversations
            imageView.setImageResource(R.drawable.ic_user_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProduct;
        TextView textViewProductTitle;
        TextView textViewParticipantName;
        TextView textViewLastMessage;
        TextView textViewTime;
        TextView textViewUnreadCount;
        ImageView imageViewReadStatus;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProduct);
            textViewProductTitle = itemView.findViewById(R.id.textViewProductTitle);
            textViewParticipantName = itemView.findViewById(R.id.textViewParticipantName);
            textViewLastMessage = itemView.findViewById(R.id.textViewLastMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewUnreadCount = itemView.findViewById(R.id.textViewUnreadCount);
            imageViewReadStatus = itemView.findViewById(R.id.imageViewReadStatus);
        }
    }
}
