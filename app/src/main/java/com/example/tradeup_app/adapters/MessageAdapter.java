package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Message;
import com.example.tradeup_app.services.MessagingService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_OFFER_SENT = 3;
    private static final int VIEW_TYPE_OFFER_RECEIVED = 4;

    private Context context;
    private List<Message> messageList;
    private String currentUserId;
    private MessagingService messagingService;
    private OnOfferActionListener offerActionListener;

    public interface OnOfferActionListener {
        void onAcceptOffer(Message offerMessage);
        void onRejectOffer(Message offerMessage);
        void onCounterOffer(Message offerMessage);
    }

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        this.messagingService = new MessagingService();
    }

    public void setOnOfferActionListener(OnOfferActionListener listener) {
        this.offerActionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);

        // Check if message is an offer
        if (isOfferMessage(message)) {
            if (message.getSenderId().equals(currentUserId)) {
                return VIEW_TYPE_OFFER_SENT;
            } else {
                return VIEW_TYPE_OFFER_RECEIVED;
            }
        }

        // Regular messages
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    private boolean isOfferMessage(Message message) {
        return message.getMessageType() != null &&
               ("OFFER".equals(message.getMessageType()) ||
                "CHAT_OFFER".equals(message.getMessageType()) ||
                "offer".equals(message.getMessageType().toLowerCase()));
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_OFFER_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_offer_sent, parent, false);
            return new SentOfferViewHolder(view);
        } else if (viewType == VIEW_TYPE_OFFER_RECEIVED) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_offer_received, parent, false);
            return new ReceivedOfferViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < 0 || position >= messageList.size()) {
            return; // Prevent out of bounds access
        }

        Message message = messageList.get(position);
        if (message == null) {
            return; // Prevent null message binding
        }

        if (holder instanceof SentMessageViewHolder) {
            bindSentMessage((SentMessageViewHolder) holder, message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            bindReceivedMessage((ReceivedMessageViewHolder) holder, message);
        } else if (holder instanceof SentOfferViewHolder) {
            bindSentOffer((SentOfferViewHolder) holder, message);
        } else if (holder instanceof ReceivedOfferViewHolder) {
            bindReceivedOffer((ReceivedOfferViewHolder) holder, message);
        }
    }

    private void bindSentMessage(SentMessageViewHolder holder, Message message) {
        bindCommonMessageData(holder.textViewMessage, holder.textViewTime,
                             holder.imageViewMessage, message);

        // Set read status
        holder.textViewReadStatus.setText(message.isRead() ? "Read" : "Delivered");

        // Long click for message options
        holder.itemView.setOnLongClickListener(v -> {
            showMessageOptions(message, true);
            return true;
        });
    }

    private void bindReceivedMessage(ReceivedMessageViewHolder holder, Message message) {
        bindCommonMessageData(holder.textViewMessage, holder.textViewTime,
                             holder.imageViewMessage, message);

        // Set sender name if available
        if (holder.textViewSenderName != null && message.getSenderName() != null) {
            holder.textViewSenderName.setText(message.getSenderName());
        }

        // Long click for message options
        holder.itemView.setOnLongClickListener(v -> {
            showMessageOptions(message, false);
            return true;
        });
    }

    private void bindSentOffer(SentOfferViewHolder holder, Message message) {
        // Bind common data
        bindCommonMessageData(holder.textViewMessage, holder.textViewTime,
                             holder.imageViewMessage, message);

        // Set offer-specific UI elements
        if (holder.textViewOfferStatus != null) {
            holder.textViewOfferStatus.setText("Offer Sent");
        }

        // Hide action buttons with null checks (sent offers don't need action buttons)
        if (holder.buttonAccept != null) {
            holder.buttonAccept.setVisibility(View.GONE);
        }
        if (holder.buttonReject != null) {
            holder.buttonReject.setVisibility(View.GONE);
        }
        if (holder.buttonCounter != null) {
            holder.buttonCounter.setVisibility(View.GONE);
        }

        // Long click for message options
        holder.itemView.setOnLongClickListener(v -> {
            showMessageOptions(message, true);
            return true;
        });
    }

    private void bindReceivedOffer(ReceivedOfferViewHolder holder, Message message) {
        android.util.Log.d("MessageAdapter", "Binding received offer message: " + message.getContent());

        // Bind common data
        bindCommonMessageData(holder.textViewMessage, holder.textViewTime,
                             holder.imageViewMessage, message);

        // Set offer-specific UI elements
        holder.textViewOfferStatus.setText("New Offer Received");
        holder.textViewOfferStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.offer_pending));

        // Show action buttons for received offers with PENDING status
        String offerStatus = message.getOfferStatus();
        android.util.Log.d("MessageAdapter", "Offer status: " + offerStatus);

        if ("PENDING".equals(offerStatus)) {
            android.util.Log.d("MessageAdapter", "Showing action buttons for PENDING offer");
            holder.buttonAccept.setVisibility(View.VISIBLE);
            holder.buttonReject.setVisibility(View.VISIBLE);
            holder.buttonCounter.setVisibility(View.VISIBLE);

            // Set up button click listeners
            holder.buttonAccept.setOnClickListener(v -> {
                android.util.Log.d("MessageAdapter", "Accept button clicked");
                if (offerActionListener != null) {
                    offerActionListener.onAcceptOffer(message);
                }
            });

            holder.buttonReject.setOnClickListener(v -> {
                android.util.Log.d("MessageAdapter", "Reject button clicked");
                if (offerActionListener != null) {
                    offerActionListener.onRejectOffer(message);
                }
            });

            holder.buttonCounter.setOnClickListener(v -> {
                android.util.Log.d("MessageAdapter", "Counter button clicked");
                if (offerActionListener != null) {
                    offerActionListener.onCounterOffer(message);
                }
            });
        } else {
            android.util.Log.d("MessageAdapter", "Hiding action buttons for status: " + offerStatus);
            holder.buttonAccept.setVisibility(View.GONE);
            holder.buttonReject.setVisibility(View.GONE);
            holder.buttonCounter.setVisibility(View.GONE);
        }

        // Set sender name if available
        if (holder.textViewSenderName != null && message.getSenderName() != null) {
            holder.textViewSenderName.setText(message.getSenderName());
        }

        // Long click for message options
        holder.itemView.setOnLongClickListener(v -> {
            showMessageOptions(message, false);
            return true;
        });
    }

    private void bindCommonMessageData(TextView textViewMessage, TextView textViewTime,
                                       ImageView imageViewMessage, Message message) {
        // Handle different message types
        if ("image".equals(message.getMessageType()) && message.getImageUrl() != null) {
            // Show image message
            textViewMessage.setVisibility(View.GONE);
            imageViewMessage.setVisibility(View.VISIBLE);

            // Load image using Glide
            Glide.with(context)
                .load(message.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .centerCrop()
                .into(imageViewMessage);

            // Click to view full image
            imageViewMessage.setOnClickListener(v -> {
                showFullScreenImage(message.getImageUrl());
            });
        } else {
            // Show text message
            textViewMessage.setVisibility(View.VISIBLE);
            imageViewMessage.setVisibility(View.GONE);

            // Handle deleted messages
            if (message.isDeleted()) {
                textViewMessage.setText("This message was deleted");
                textViewMessage.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
                textViewMessage.setTypeface(null, android.graphics.Typeface.ITALIC);
            } else {
                textViewMessage.setText(message.getContent());
                textViewMessage.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                textViewMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }

        // Set time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        textViewTime.setText(sdf.format(new Date(message.getTimestamp())));
    }

    private void showFullScreenImage(String imageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Create a simple ImageView for full screen display
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        imageView.setBackgroundColor(android.graphics.Color.BLACK);

        Glide.with(context)
            .load(imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_dialog_alert)
            .into(imageView);

        builder.setView(imageView);
        builder.setNegativeButton("Close", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showMessageOptions(Message message, boolean isSentByMe) {
        // Don't show options for deleted messages
        if (message.isDeleted()) {
            Toast.makeText(context, "This message was deleted", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options;
        if (isSentByMe) {
            options = new String[]{"Delete Message", "Copy Text"};
        } else {
            options = new String[]{"Copy Text", "Report Message"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Message Options");
        builder.setItems(options, (dialog, which) -> {
            if (isSentByMe) {
                switch (which) {
                    case 0: // Delete Message
                        deleteMessage(message);
                        break;
                    case 1: // Copy Text
                        copyMessageText(message);
                        break;
                }
            } else {
                switch (which) {
                    case 0: // Copy Text
                        copyMessageText(message);
                        break;
                    case 1: // Report Message
                        reportMessage(message);
                        break;
                }
            }
        });
        builder.show();
    }

    private void deleteMessage(Message message) {
        String currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isSender = currentUserId.equals(message.getSenderId());
        boolean canDeleteForEveryone = messagingService.canDeleteForEveryone(message, currentUserId);

        // Create options based on permissions
        String[] options;
        if (isSender && canDeleteForEveryone) {
            options = new String[]{"Delete for me", "Delete for everyone"};
        } else if (isSender) {
            options = new String[]{"Delete for me"};
        } else {
            // Non-sender can only delete for themselves
            options = new String[]{"Delete for me"};
        }

        new AlertDialog.Builder(context)
            .setTitle("Delete Message")
            .setItems(options, (dialog, which) -> {
                if (isSender && canDeleteForEveryone && which == 1) {
                    // Delete for everyone
                    showDeleteConfirmationDialog(message, true);
                } else {
                    // Delete for me
                    showDeleteConfirmationDialog(message, false);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showDeleteConfirmationDialog(Message message, boolean deleteForEveryone) {
        String title = deleteForEveryone ? "Delete for Everyone" : "Delete for Me";
        String messageText = deleteForEveryone ?
            "This message will be deleted for everyone in this chat. This action cannot be undone." :
            "This message will be deleted for you only. Other participants will still see it.";

        new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(messageText)
            .setPositiveButton("Delete", (dialog, which) -> {
                performMessageDeletion(message, deleteForEveryone);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performMessageDeletion(Message message, boolean deleteForEveryone) {
        // Show loading indicator
        showLoadingDialog();

        MessagingService.MessageDeletionCallback callback = new MessagingService.MessageDeletionCallback() {
            @Override
            public void onMessageDeleted(String messageId) {
                hideLoadingDialog();

                // Update local message object for immediate UI update
                message.setDeleted(true);
                message.setDeletedBy(FirebaseManager.getInstance().getCurrentUserId());
                message.setDeletedAt(System.currentTimeMillis());

                if (deleteForEveryone) {
                    message.setDeletedForEveryone(true);
                    message.setContent("This message was deleted");
                    // Clear image data for everyone
                    if ("image".equals(message.getMessageType())) {
                        message.setImageUrl(null);
                        message.setImageFileName(null);
                    }
                } else {
                    // For "delete for me", just mark as deleted locally
                    message.setContent("You deleted this message");
                }

                // Notify adapter to refresh this specific item
                int position = messageList.indexOf(message);
                if (position != -1) {
                    notifyItemChanged(position);
                }

                String successMessage = deleteForEveryone ?
                    "Message deleted for everyone" : "Message deleted for you";
                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                Toast.makeText(context, "Failed to delete message: " + error, Toast.LENGTH_LONG).show();
            }
        };

        if (deleteForEveryone) {
            messagingService.deleteMessageForEveryone(message.getId(), callback);
        } else {
            messagingService.deleteMessageForMe(message.getId(), callback);
        }
    }

    private AlertDialog loadingDialog;

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Deleting message...")
                   .setCancelable(false);
            loadingDialog = builder.create();
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void copyMessageText(Message message) {
        if ("text".equals(message.getMessageType()) || "emoji".equals(message.getMessageType())) {
            android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Message", message.getContent());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Cannot copy image message", Toast.LENGTH_SHORT).show();
        }
    }

    private void reportMessage(Message message) {
        new AlertDialog.Builder(context)
            .setTitle("Report Message")
            .setMessage("Report this message for inappropriate content?")
            .setPositiveButton("Report", (dialog, which) -> {
                messagingService.reportMessage(message.getId(), "Inappropriate content", task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Message reported. Thank you for keeping our community safe.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, "Failed to report message", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    // ViewHolder for sent messages
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewTime;
        TextView textViewReadStatus;
        ImageView imageViewMessage;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewReadStatus = itemView.findViewById(R.id.textViewReadStatus);
            imageViewMessage = itemView.findViewById(R.id.imageViewMessage);
        }
    }

    // ViewHolder for received messages
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewTime;
        TextView textViewSenderName;
        ImageView imageViewMessage;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            imageViewMessage = itemView.findViewById(R.id.imageViewMessage);
        }
    }

    // ViewHolder for sent offer messages
    static class SentOfferViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewTime;
        TextView textViewOfferStatus;
        ImageView imageViewMessage;
        View buttonAccept;
        View buttonReject;
        View buttonCounter;

        public SentOfferViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewOfferStatus = itemView.findViewById(R.id.textViewOfferStatus);
            imageViewMessage = itemView.findViewById(R.id.imageViewMessage);
            buttonAccept = itemView.findViewById(R.id.buttonAccept);
            buttonReject = itemView.findViewById(R.id.buttonReject);
            buttonCounter = itemView.findViewById(R.id.buttonCounter);
        }
    }

    // ViewHolder for received offer messages
    static class ReceivedOfferViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewTime;
        TextView textViewOfferStatus;
        TextView textViewSenderName; // Added missing field
        ImageView imageViewMessage;
        View buttonAccept;
        View buttonReject;
        View buttonCounter;

        public ReceivedOfferViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewOfferStatus = itemView.findViewById(R.id.textViewOfferStatus);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName); // Added missing initialization
            imageViewMessage = itemView.findViewById(R.id.imageViewMessage);
            buttonAccept = itemView.findViewById(R.id.buttonAccept);
            buttonReject = itemView.findViewById(R.id.buttonReject);
            buttonCounter = itemView.findViewById(R.id.buttonCounter);
        }
    }

    // Method to update messages list
    public void updateMessages(List<Message> newMessages) {
        this.messageList.clear();
        this.messageList.addAll(newMessages);
        notifyDataSetChanged();
    }

    // Method to add new message
    public void addMessage(Message message) {
        this.messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }
}
