package com.example.tradeup_app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.PaymentHistoryAdapter;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Transaction;

import java.util.ArrayList;
import java.util.List;

public class PaymentHistoryActivity extends AppCompatActivity {
    private static final String TAG = "PaymentHistoryActivity";

    // UI Components
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ProgressBar progressBar;

    // Data
    private PaymentHistoryAdapter adapter;
    private List<Transaction> transactions;
    private FirebaseManager firebaseManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        initializeComponents();
        setupRecyclerView();
        loadPaymentHistory();
    }

    private void initializeComponents() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử thanh toán");
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        firebaseManager = FirebaseManager.getInstance();
        currentUserId = CurrentUser.getUser() != null ? CurrentUser.getUser().getUid() : null; // Sửa: getUserId() thành getUid()
        transactions = new ArrayList<>();

        swipeRefresh.setOnRefreshListener(this::loadPaymentHistory);
    }

    private void setupRecyclerView() {
        adapter = new PaymentHistoryAdapter(this, transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadPaymentHistory() {
        if (currentUserId == null) {
            showEmptyState();
            return;
        }

        showLoading(true);

        firebaseManager.getUserTransactions(currentUserId, new FirebaseManager.OnTransactionsLoadedListener() {
            @Override
            public void onSuccess(List<Transaction> userTransactions) {
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    transactions.clear();
                    transactions.addAll(userTransactions);
                    adapter.notifyDataSetChanged();

                    if (transactions.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    showEmptyState();
                });
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState() {
        emptyStateText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}
