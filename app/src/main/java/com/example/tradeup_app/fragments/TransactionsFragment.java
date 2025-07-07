package com.example.tradeup_app.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.TransactionAdapter;
import com.example.tradeup_app.dialogs.RatingDialog;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Transaction;
import com.example.tradeup_app.models.Rating;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TransactionsFragment extends Fragment implements TransactionAdapter.OnTransactionActionListener {

    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter transactionAdapter;
    private CircularProgressIndicator progressIndicator;
    private View emptyView;
    private TextView emptyMessageText;

    private FirebaseManager firebaseManager;
    private List<Transaction> transactions = new ArrayList<>();

    private interface LoadTransactionCallback {
        void onComplete(boolean success);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        initViews(view);
        setupRecyclerView();
        loadTransactions();

        return view;
    }

    private void initViews(View view) {
        transactionsRecyclerView = view.findViewById(R.id.transactionsRecyclerView);
        progressIndicator = view.findViewById(R.id.progressIndicator);
        emptyView = view.findViewById(R.id.emptyView);
        emptyMessageText = view.findViewById(R.id.emptyMessageText);

        firebaseManager = FirebaseManager.getInstance();
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(getContext(), transactions);
        transactionAdapter.setOnTransactionActionListener(this);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsRecyclerView.setAdapter(transactionAdapter);
    }

    private void loadTransactions() {
        showLoading(true);

        // Sử dụng CurrentUser mới với callback
        CurrentUser.loadUserSynchronously(new CurrentUser.LoadUserCallback() {
            @Override
            public void onUserLoaded(UserModel user) {
                String currentUserId = user.getUid();

                if (currentUserId == null || currentUserId.isEmpty()) {
                    showError("User ID not found");
                    return;
                }

                // Load transactions directly from Firebase
                loadTransactionsFromFirebase(currentUserId);
            }

            @Override
            public void onError(String error) {
                // Fallback: thử dùng Firebase Auth trực tiếp
                com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser != null) {
                    loadTransactionsFromFirebase(firebaseUser.getUid());
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void loadTransactionsFromFirebase(String userId) {
        // Thử cả "transactions" và "Transactions" để đảm bảo tương thích
        tryLoadTransactionsFromPath("transactions", userId, success -> {
            if (!success) {
                Log.d("TransactionsFragment", "No transactions found in 'transactions', trying 'Transactions'");
                tryLoadTransactionsFromPath("Transactions", userId, success2 -> {
                    if (!success2) {
                        Log.d("TransactionsFragment", "No transactions found in both paths");
                        showEmptyState();
                    }
                });
            }
        });
    }

    private void tryLoadTransactionsFromPath(String path, String userId, LoadTransactionCallback callback) {
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference(path);

        // Query transactions where user is either buyer or seller
        transactionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("TransactionsFragment", "Query result from '" + path + "': " + snapshot.getChildrenCount() + " transactions");

                List<Transaction> userTransactions = new ArrayList<>();

                for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                    try {
                        Transaction transaction = transactionSnapshot.getValue(Transaction.class);
                        if (transaction != null) {
                            transaction.setId(transactionSnapshot.getKey());

                            // Check if user is involved in this transaction
                            if (userId.equals(transaction.getBuyerId()) || userId.equals(transaction.getSellerId())) {
                                userTransactions.add(transaction);
                                Log.d("TransactionsFragment", "Found transaction: " + transaction.getId() + " - " + transaction.getProductTitle());
                            }
                        }
                    } catch (Exception e) {
                        Log.w("TransactionsFragment", "Failed to parse transaction: " + e.getMessage());
                    }
                }

                if (!userTransactions.isEmpty()) {
                    transactions.clear();
                    transactions.addAll(userTransactions);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateUI();
                        });
                    }

                    callback.onComplete(true);
                } else {
                    callback.onComplete(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TransactionsFragment", "Database query failed for path '" + path + "': " + error.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showError("Lỗi tải dữ liệu: " + error.getMessage());
                    });
                }
                callback.onComplete(false);
            }
        });
    }

    private void updateUI() {
        Log.d("TransactionsFragment", "updateUI called with " + transactions.size() + " transactions");

        showLoading(false);

        if (transactions.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            transactionAdapter.notifyDataSetChanged();
        }
    }

    private void showEmptyState() {
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
        if (emptyMessageText != null) {
            emptyMessageText.setText("Bạn chưa có giao dịch nào.\nHãy bắt đầu mua hoặc bán sản phẩm!");
        }
        if (transactionsRecyclerView != null) {
            transactionsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
        if (transactionsRecyclerView != null) {
            transactionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        showLoading(false);
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        Log.e("TransactionsFragment", "Error: " + message);
    }

    // TransactionAdapter.OnTransactionActionListener implementations
    @Override
    public void onRateTransaction(Transaction transaction) {
        // Determine who to rate (the other party in the transaction)
        String currentUserId = firebaseManager.getCurrentUserId();
        boolean isUserBuyer = currentUserId.equals(transaction.getBuyerId());

        String otherUserName = isUserBuyer ? transaction.getSellerName() : transaction.getBuyerName();
        String otherUserId = isUserBuyer ? transaction.getSellerId() : transaction.getBuyerId();

        // Check if already rated
        String ratingField = isUserBuyer ? "buyerRated" : "sellerRated";
        boolean alreadyRated = isUserBuyer ? transaction.isBuyerRated() : transaction.isSellerRated();

        if (alreadyRated) {
            Toast.makeText(getContext(), "You have already rated this transaction", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show rating dialog
        RatingDialog ratingDialog = new RatingDialog(getContext(), otherUserName, "", new RatingDialog.OnRatingSubmitListener() {
            @Override
            public void onRatingSubmit(int stars, String review) {
                submitRating(transaction, stars, review, otherUserId, otherUserName, isUserBuyer ? "BUYER" : "SELLER");
            }

            @Override
            public void onSkip() {
                // User chose to skip rating
            }
        });
        ratingDialog.show();
    }

    @Override
    public void onViewTransactionDetails(Transaction transaction) {
        // TODO: Show transaction details dialog or navigate to details activity
        Toast.makeText(getContext(), "Transaction Details: " + transaction.getProductTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMarkAsCompleted(Transaction transaction) {
        firebaseManager.markTransactionCompleted(transaction.getId(), task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Transaction marked as completed", Toast.LENGTH_SHORT).show();
                loadTransactions(); // Refresh the list
            } else {
                Toast.makeText(getContext(), "Failed to update transaction", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onContactOtherParty(Transaction transaction) {
        // TODO: Navigate to chat with the other party
        String currentUserId = firebaseManager.getCurrentUserId();
        boolean isUserBuyer = currentUserId.equals(transaction.getBuyerId());
        String otherUserName = isUserBuyer ? transaction.getSellerName() : transaction.getBuyerName();

        Toast.makeText(getContext(), "Contact feature coming soon for " + otherUserName, Toast.LENGTH_SHORT).show();
    }

    private void submitRating(Transaction transaction, int stars, String review,
                            String ratedUserId, String ratedUserName, String userType) {
        String currentUserId = firebaseManager.getCurrentUserId();
        String currentUserName = CurrentUser.getUser() != null ? CurrentUser.getUser().getUsername() : "Anonymous";

        Rating rating = new Rating(
            transaction.getId(),
            currentUserId,
            currentUserName,
            ratedUserId,
            ratedUserName,
            stars,
            review,
            userType
        );

        firebaseManager.submitRating(rating, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Rating submitted successfully!", Toast.LENGTH_SHORT).show();
                loadTransactions(); // Refresh to update the rated status
            } else {
                Toast.makeText(getContext(), "Failed to submit rating", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void refreshTransactions() {
        loadTransactions();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Removed debug buttons - không cần debug nữa
    }
}
