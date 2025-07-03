package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.models.User;

import java.util.List;

public class BlockedUsersAdapter extends RecyclerView.Adapter<BlockedUsersAdapter.BlockedUserViewHolder> {
    private Context context;
    private List<User> blockedUsersList;
    private OnBlockedUserClickListener listener;

    public interface OnBlockedUserClickListener {
        void onUnblockUserClick(User user);
    }

    public BlockedUsersAdapter(Context context, List<User> blockedUsersList, OnBlockedUserClickListener listener) {
        this.context = context;
        this.blockedUsersList = blockedUsersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BlockedUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_user, parent, false);
        return new BlockedUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockedUserViewHolder holder, int position) {
        User user = blockedUsersList.get(position);

        // Set user name
        holder.textViewUserName.setText(user.getName() != null ? user.getName() : "Unknown User");

        // Set user email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            holder.textViewUserEmail.setText(user.getEmail());
            holder.textViewUserEmail.setVisibility(View.VISIBLE);
        } else {
            holder.textViewUserEmail.setVisibility(View.GONE);
        }

        // Load user avatar
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(holder.imageViewAvatar);
        } else {
            holder.imageViewAvatar.setImageResource(R.drawable.ic_person);
        }

        // Set unblock button click listener
        holder.buttonUnblock.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUnblockUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return blockedUsersList.size();
    }

    static class BlockedUserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewAvatar;
        TextView textViewUserName;
        TextView textViewUserEmail;
        Button buttonUnblock;

        BlockedUserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewUserEmail = itemView.findViewById(R.id.textViewUserEmail);
            buttonUnblock = itemView.findViewById(R.id.buttonUnblock);
        }
    }
}
