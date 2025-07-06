package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeup_app.R;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class FlaggedUserAdapter extends RecyclerView.Adapter<FlaggedUserAdapter.FlaggedUserViewHolder> {
    public interface OnFlaggedUserActionListener {
        void onDeleteUser(UserModel user);
        void onSuspendUser(UserModel user);
        void onWarnUser(UserModel user);
    }

    private Context context;
    private List<UserModel> users;
    private OnFlaggedUserActionListener listener;

    public FlaggedUserAdapter(Context context, List<UserModel> users) {
        this.context = context;
        this.users = users;
    }

    public void setOnFlaggedUserActionListener(OnFlaggedUserActionListener listener) {
        this.listener = listener;
    }

    public void updateUsers(List<UserModel> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FlaggedUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_flagged_user, parent, false);
        return new FlaggedUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlaggedUserViewHolder holder, int position) {
        UserModel user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class FlaggedUserViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, emailText, reasonText;
        MaterialButton btnDelete, btnSuspend, btnWarn;
        public FlaggedUserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.flagged_user_username);
            emailText = itemView.findViewById(R.id.flagged_user_email);
            reasonText = itemView.findViewById(R.id.flagged_user_reason);
            btnDelete = itemView.findViewById(R.id.btn_delete_user);
            btnSuspend = itemView.findViewById(R.id.btn_suspend_user);
            btnWarn = itemView.findViewById(R.id.btn_warn_user);
        }
        void bind(UserModel user) {
            usernameText.setText(user.getUsername());
            emailText.setText(user.getEmail());
            reasonText.setText(user.getFlaggedReason() != null ? user.getFlaggedReason() : "");
            btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDeleteUser(user); });
            btnSuspend.setOnClickListener(v -> { if (listener != null) listener.onSuspendUser(user); });
            btnWarn.setOnClickListener(v -> { if (listener != null) listener.onWarnUser(user); });
        }
    }
}

