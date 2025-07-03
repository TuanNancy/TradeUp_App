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

    private Context context;
    private List<Message> messageList;
    private String currentUserId;
    private MessagingService messagingService;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        this.messagingService = new MessagingService();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder instanceof SentMessageViewHolder) {
            bindSentMessage((SentMessageViewHolder) holder, message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            bindReceivedMessage((ReceivedMessageViewHolder) holder, message);
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

    private void bindCommonMessageData(TextView textViewMessage, TextView textViewTime,
                                     ImageView imageViewMessage, Message message) {
        // Handle text messages
        if ("text".equals(message.getMessageType()) || "emoji".equals(message.getMessageType())) {
            textViewMessage.setVisibility(View.VISIBLE);
            textViewMessage.setText(message.getContent());
            if (imageViewMessage != null) {
                imageViewMessage.setVisibility(View.GONE);
            }
        }
        // Handle image messages
        else if ("image".equals(message.getMessageType())) {
            textViewMessage.setVisibility(View.GONE);
            if (imageViewMessage != null) {
                imageViewMessage.setVisibility(View.VISIBLE);

                // Use imageUrl for image messages, fallback to content
                String imageUrl = message.getImageUrl() != null ? message.getImageUrl() : message.getContent();

                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(imageViewMessage);

                // Click to view full image
                imageViewMessage.setOnClickListener(v -> {
                    // You can implement full screen image viewer here
                    Toast.makeText(context, "Image viewer not implemented yet", Toast.LENGTH_SHORT).show();
                });
            }
        }

        // Set timestamp
        String timeText = formatTimestamp(message.getTimestamp());
        textViewTime.setText(timeText);
    }

    private void showMessageOptions(Message message, boolean isSentByMe) {
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
        new AlertDialog.Builder(context)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Delete from Firebase
                FirebaseManager.getInstance().getDatabase()
                    .getReference(FirebaseManager.MESSAGES_NODE)
                    .child(message.getId())
                    .removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to delete message", Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
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
        return messageList.size();
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
