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
import com.google.android.material.progressindicator.CircularProgressIndicator;

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
        Log.d("TransactionsFragment", "loadTransactions() called");
        showLoading(true);

        String currentUserId = firebaseManager.getCurrentUserId();
        Log.d("TransactionsFragment", "Current user ID: " + currentUserId);

        if (currentUserId == null) {
            Log.e("TransactionsFragment", "User not logged in");
            showError("User not logged in");
            return;
        }

        // Use the new method that gets transactions from both data sources
        firebaseManager.getAllTransactionsForUser(currentUserId, new FirebaseManager.TransactionCallback() {
            @Override
            public void onTransactionsLoaded(List<Transaction> loadedTransactions) {
                Log.d("TransactionsFragment", "onTransactionsLoaded called with " + loadedTransactions.size() + " transactions");
                for (Transaction t : loadedTransactions) {
                    Log.d("TransactionsFragment", "Loaded transaction: " + t.getId() + " - " + t.getProductTitle());
                }

                transactions.clear();
                transactions.addAll(loadedTransactions);
                updateUI();
            }

            @Override
            public void onError(String error) {
                Log.e("TransactionsFragment", "Error loading transactions: " + error);
                showError("Error loading transactions: " + error);
            }
        });
    }

    private void updateUI() {
        Log.d("TransactionsFragment", "updateUI() called with " + transactions.size() + " transactions");
        showLoading(false);
        transactionAdapter.updateTransactions(transactions);

        if (transactions.isEmpty()) {
            Log.d("TransactionsFragment", "No transactions found, showing empty view");
            emptyView.setVisibility(View.VISIBLE);
            transactionsRecyclerView.setVisibility(View.GONE);
            emptyMessageText.setText("No transactions yet\nStart buying or selling to see your transaction history");
        } else {
            Log.d("TransactionsFragment", "Showing " + transactions.size() + " transactions");
            emptyView.setVisibility(View.GONE);
            transactionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        transactionsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        showLoading(false);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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

        // Add debug buttons for testing (remove in production)
        addDebugButtons(view);
    }

    private void addDebugButtons(View view) {
        // Add a debug button to create test transaction
        Button debugButton = new Button(getContext());
        debugButton.setText("DEBUG: Create Test Transaction");
        debugButton.setOnClickListener(v -> {
            firebaseManager.createTestTransaction(new FirebaseManager.OnTransactionSavedListener() {
                @Override
                public void onSuccess(String transactionId) {
                    Toast.makeText(getContext(), "Test transaction created: " + transactionId, Toast.LENGTH_SHORT).show();
                    loadTransactions(); // Refresh the list
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Failed to create test transaction: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Add a debug button to check database state
        Button checkButton = new Button(getContext());
        checkButton.setText("DEBUG: Check Database");
        checkButton.setOnClickListener(v -> {
            firebaseManager.checkTransactionData(result -> {
                Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
                Log.d("TransactionsFragment", "Database check result: " + result);
            });
        });

        // Add buttons to the layout programmatically (for debug only)
        if (view instanceof FrameLayout) {
            LinearLayout debugLayout = new LinearLayout(getContext());
            debugLayout.setOrientation(LinearLayout.VERTICAL);
            debugLayout.addView(debugButton);
            debugLayout.addView(checkButton);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.TOP | Gravity.END;
            params.setMargins(16, 16, 16, 16);

            ((FrameLayout) view).addView(debugLayout, params);
        }
    }
}
