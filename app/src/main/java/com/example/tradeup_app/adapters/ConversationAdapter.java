package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Conversation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {
    private Context context;
    private List<Conversation> conversationList;
    private OnConversationClickListener listener;
    private String currentUserId;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
        void onConversationLongClick(Conversation conversation);
    }

    public ConversationAdapter(Context context, List<Conversation> conversationList, OnConversationClickListener listener) {
        this.context = context;
        this.conversationList = conversationList;
        this.listener = listener;
        this.currentUserId = FirebaseManager.getInstance().getCurrentUserId();
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

        // Set product title
        holder.textViewProductTitle.setText(conversation.getProductTitle());

        // Set other participant name
        String otherParticipantName = conversation.getOtherParticipantName(currentUserId);
        holder.textViewParticipantName.setText(otherParticipantName);

        // Set last message
        holder.textViewLastMessage.setText(conversation.getLastMessage());

        // Set timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        String timeText = sdf.format(new Date(conversation.getLastMessageTime()));
        holder.textViewTime.setText(timeText);

        // Set unread count
        if (conversation.getUnreadCount() > 0) {
            holder.textViewUnreadCount.setVisibility(View.VISIBLE);
            holder.textViewUnreadCount.setText(String.valueOf(conversation.getUnreadCount()));
        } else {
            holder.textViewUnreadCount.setVisibility(View.GONE);
        }

        // Load product image
        if (conversation.getProductImageUrl() != null && !conversation.getProductImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(conversation.getProductImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(holder.imageViewProduct);
        } else {
            holder.imageViewProduct.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conversation);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onConversationLongClick(conversation);
            }
            return true;
        });

        // Visual indication if conversation is reported
        if (conversation.isReported()) {
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.itemView.setAlpha(1.0f);
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

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProduct);
            textViewProductTitle = itemView.findViewById(R.id.textViewProductTitle);
            textViewParticipantName = itemView.findViewById(R.id.textViewParticipantName);
            textViewLastMessage = itemView.findViewById(R.id.textViewLastMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewUnreadCount = itemView.findViewById(R.id.textViewUnreadCount);
        }
    }
}
