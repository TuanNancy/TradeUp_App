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
import com.example.tradeup_app.models.Conversation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<Conversation> conversations;
    private Context context;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(Context context, List<Conversation> conversations) {
        this.context = context;
        this.conversations = conversations != null ? conversations : new ArrayList<>();
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void updateConversations(List<Conversation> newConversations) {
        this.conversations.clear();
        if (newConversations != null) {
            this.conversations.addAll(newConversations);
        }
        notifyDataSetChanged();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage, userAvatar;
        private TextView productTitle, userName, lastMessage, timeText, unreadBadge;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            userAvatar = itemView.findViewById(R.id.user_avatar);
            productTitle = itemView.findViewById(R.id.product_title);
            userName = itemView.findViewById(R.id.user_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            timeText = itemView.findViewById(R.id.time_text);
            unreadBadge = itemView.findViewById(R.id.unread_badge);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onConversationClick(conversations.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Conversation conversation) {
            productTitle.setText(conversation.getProductTitle());
            userName.setText(conversation.getBuyerName());
            lastMessage.setText(conversation.getLastMessage());

            // Format time - Fixed: check for timestamp > 0 instead of null
            if (conversation.getLastMessageTime() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                timeText.setText(sdf.format(new Date(conversation.getLastMessageTime())));
            }

            // Show unread badge
            if (conversation.getUnreadCount() > 0) {
                unreadBadge.setVisibility(View.VISIBLE);
                unreadBadge.setText(String.valueOf(conversation.getUnreadCount()));
            } else {
                unreadBadge.setVisibility(View.GONE);
            }

            // Load product image
            if (conversation.getProductImageUrl() != null && !conversation.getProductImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(conversation.getProductImageUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.ic_launcher_background);
            }

            // Load user avatar
            userAvatar.setImageResource(R.drawable.ic_user);
        }
    }
}
