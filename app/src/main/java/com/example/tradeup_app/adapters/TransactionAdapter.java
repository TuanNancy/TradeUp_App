package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Transaction;
import com.example.tradeup_app.utils.VNDPriceFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public interface OnTransactionActionListener {
        void onRateTransaction(Transaction transaction);
        void onViewTransactionDetails(Transaction transaction);
        void onMarkAsCompleted(Transaction transaction);
        void onContactOtherParty(Transaction transaction);
    }

    private Context context;
    private List<Transaction> transactions;
    private OnTransactionActionListener listener;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
    }

    public void setOnTransactionActionListener(OnTransactionActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private TextView productTitleText;
        private TextView priceText;
        private TextView statusText;
        private TextView dateText;
        private TextView otherPartyText;
        private TextView roleText;
        private MaterialButton rateButton;
        private MaterialButton detailsButton;
        private MaterialButton completeButton;
        private MaterialButton contactButton;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.transactionCardView);
            productTitleText = itemView.findViewById(R.id.productTitleText);
            priceText = itemView.findViewById(R.id.priceText);
            statusText = itemView.findViewById(R.id.statusText);
            dateText = itemView.findViewById(R.id.dateText);
            otherPartyText = itemView.findViewById(R.id.otherPartyText);
            roleText = itemView.findViewById(R.id.roleText);
            rateButton = itemView.findViewById(R.id.rateButton);
            detailsButton = itemView.findViewById(R.id.detailsButton);
            completeButton = itemView.findViewById(R.id.completeButton);
            contactButton = itemView.findViewById(R.id.contactButton);
        }

        public void bind(Transaction transaction) {
            productTitleText.setText(transaction.getProductTitle());
            priceText.setText(VNDPriceFormatter.formatVND(transaction.getFinalPrice()));

            // Improved status display with icons and clear text
            setStatusWithIcon(transaction.getStatus());

            // Set status color
            setStatusColor(transaction.getStatus());

            // Set date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            if ("COMPLETED".equals(transaction.getStatus()) && transaction.getCompletedAt() > 0) {
                dateText.setText("Completed: " + sdf.format(new Date(transaction.getCompletedAt())));
            } else {
                dateText.setText("Started: " + sdf.format(new Date(transaction.getCreatedAt())));
            }

            // Determine user role and other party
            String currentUserId = com.example.tradeup_app.firebase.FirebaseManager.getInstance().getCurrentUserId();
            boolean isUserBuyer = currentUserId != null && currentUserId.equals(transaction.getBuyerId());

            if (isUserBuyer) {
                roleText.setText("You bought from:");
                otherPartyText.setText(transaction.getSellerName());
            } else {
                roleText.setText("You sold to:");
                otherPartyText.setText(transaction.getBuyerName());
            }

            // Setup buttons based on transaction status and user role
            setupButtons(transaction, isUserBuyer);

            // Set click listeners
            setupClickListeners(transaction);
        }

        private void setStatusColor(String status) {
            int colorRes;
            switch (status) {
                case "COMPLETED":
                    colorRes = R.color.offer_accepted;
                    break;
                case "CANCELLED":
                    colorRes = R.color.offer_rejected;
                    break;
                case "PENDING":
                default:
                    colorRes = R.color.offer_pending;
                    break;
            }
            statusText.setTextColor(ContextCompat.getColor(context, colorRes));
        }

        private void setStatusWithIcon(String status) {
            String statusDisplay;
            switch (status.toUpperCase()) {
                case "COMPLETED":
                case "PAID":
                case "SUCCESS":
                    statusDisplay = "✅ Giao dịch thành công";
                    break;
                case "CANCELLED":
                case "CANCELED":
                case "FAILED":
                    statusDisplay = "❌ Giao dịch thất bại";
                    break;
                case "PENDING":
                    statusDisplay = "⏳ Đang chờ xử lý";
                    break;
                default:
                    statusDisplay = "📋 " + status;
                    break;
            }

            statusText.setText(statusDisplay);
        }

        private void setupButtons(Transaction transaction, boolean isUserBuyer) {
            // Show/hide buttons based on status
            boolean isCompleted = "COMPLETED".equals(transaction.getStatus());
            boolean isPending = "PENDING".equals(transaction.getStatus());

            // Rate button - only show if completed and not yet rated
            boolean alreadyRated = isUserBuyer ? transaction.isBuyerRated() : transaction.isSellerRated();
            rateButton.setVisibility(isCompleted && !alreadyRated ? View.VISIBLE : View.GONE);

            // Complete button - only show if pending and user can complete it
            completeButton.setVisibility(isPending ? View.VISIBLE : View.GONE);

            // Contact button - always visible for communication
            contactButton.setVisibility(View.VISIBLE);

            // Details button - always visible
            detailsButton.setVisibility(View.VISIBLE);
        }

        private void setupClickListeners(Transaction transaction) {
            rateButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRateTransaction(transaction);
                }
            });

            detailsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewTransactionDetails(transaction);
                }
            });

            completeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMarkAsCompleted(transaction);
                }
            });

            contactButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactOtherParty(transaction);
                }
            });

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewTransactionDetails(transaction);
                }
            });
        }
    }
}
