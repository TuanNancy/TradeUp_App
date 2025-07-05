package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Transaction;
import com.google.android.material.card.MaterialCardView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.TransactionViewHolder> {
    private Context context;
    private List<Transaction> transactions;
    private DecimalFormat priceFormat;
    private SimpleDateFormat dateFormat;

    public PaymentHistoryAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
        this.priceFormat = new DecimalFormat("#,###.##");
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment_history, parent, false);
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

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private TextView productTitle;
        private TextView transactionId;
        private TextView amount;
        private TextView status;
        private TextView date;
        private TextView paymentMethod;
        private ImageView statusIcon;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            productTitle = itemView.findViewById(R.id.productTitle);
            transactionId = itemView.findViewById(R.id.transactionId);
            amount = itemView.findViewById(R.id.amount);
            status = itemView.findViewById(R.id.status);
            date = itemView.findViewById(R.id.date);
            paymentMethod = itemView.findViewById(R.id.paymentMethod);
            statusIcon = itemView.findViewById(R.id.statusIcon);
        }

        public void bind(Transaction transaction) {
            productTitle.setText(transaction.getProductTitle());
            transactionId.setText("Mã GD: " + (transaction.getId() != null ?
                transaction.getId().substring(0, Math.min(8, transaction.getId().length())) : "N/A"));
            amount.setText("$" + priceFormat.format(transaction.getFinalPrice()));
            date.setText(dateFormat.format(new Date(transaction.getCreatedAt())));

            // Set payment method
            String method = transaction.getPaymentMethod();
            if (method != null && method.equals("card")) {
                paymentMethod.setText("Thẻ tín dụng");
            } else {
                paymentMethod.setText("Khác");
            }

            // Set status and styling
            setStatusAppearance(transaction);

            // Set click listener for transaction details
            cardView.setOnClickListener(v -> {
                // TODO: Open transaction details
            });
        }

        private void setStatusAppearance(Transaction transaction) {
            String paymentStatus = transaction.getPaymentStatus();

            switch (paymentStatus) {
                case "SUCCEEDED":
                    status.setText("Thành công");
                    status.setTextColor(ContextCompat.getColor(context, R.color.success_color));
                    statusIcon.setImageResource(R.drawable.ic_check_circle);
                    statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_color));
                    break;
                case "FAILED":
                    status.setText("Thất bại");
                    status.setTextColor(ContextCompat.getColor(context, R.color.error_color));
                    statusIcon.setImageResource(R.drawable.ic_error);
                    statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.error_color));
                    break;
                case "PENDING":
                    status.setText("Đang xử lý");
                    status.setTextColor(ContextCompat.getColor(context, R.color.warning_color));
                    statusIcon.setImageResource(R.drawable.ic_pending);
                    statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.warning_color));
                    break;
                case "CANCELED":
                    status.setText("Đã hủy");
                    status.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                    statusIcon.setImageResource(R.drawable.ic_cancel);
                    statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
                    break;
                default:
                    status.setText("Không xác định");
                    status.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                    statusIcon.setImageResource(R.drawable.ic_help);
                    statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
                    break;
            }
        }
    }
}
