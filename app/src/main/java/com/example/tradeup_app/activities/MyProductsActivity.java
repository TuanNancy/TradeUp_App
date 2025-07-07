package com.example.tradeup_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.MyProductsAdapter;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyProductsActivity extends AppCompatActivity {

    private static final String TAG = "MyProductsActivity";

    private RecyclerView recyclerView;
    private MyProductsAdapter myProductsAdapter;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private List<Product> myProductsList;
    private DatabaseReference productsRef;
    private UserModel currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_products);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadMyProducts();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.my_products_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateText = findViewById(R.id.empty_state_text);

        myProductsList = new ArrayList<>();
        // FIX: Sử dụng "products" (chữ thường) thay vì "Products" (chữ hoa)
        productsRef = FirebaseDatabase.getInstance().getReference("products");
        currentUser = CurrentUser.getUser();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sản phẩm của tôi");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        myProductsAdapter = new MyProductsAdapter(this, myProductsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(myProductsAdapter);

        // Set click listener for my products
        myProductsAdapter.setOnMyProductClickListener(new MyProductsAdapter.OnMyProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                // Handle product click - open detail view
                Intent intent = new Intent(MyProductsActivity.this,
                        com.example.tradeup_app.activities.ProductDetailActivity.class);
                intent.putExtra("product_id", product.getId());
                startActivity(intent);
            }

            @Override
            public void onEditProduct(Product product) {
                // Handle edit product
                Toast.makeText(MyProductsActivity.this,
                        "Chỉnh sửa sản phẩm: " + product.getTitle(), Toast.LENGTH_SHORT).show();
                // TODO: Navigate to edit product activity
            }

            @Override
            public void onDeleteProduct(Product product) {
                // Handle delete product
                showDeleteConfirmDialog(product);
            }

            @Override
            public void onViewAnalytics(Product product) {
                // Handle view analytics
                Toast.makeText(MyProductsActivity.this,
                        "Xem thống kê cho: " + product.getTitle(), Toast.LENGTH_SHORT).show();
                // TODO: Navigate to analytics activity
            }
        });
    }

    private void showDeleteConfirmDialog(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa sản phẩm")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm \"" + product.getTitle() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteProduct(product);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteProduct(Product product) {
        if (product.getId() == null || product.getId().isEmpty()) {
            Toast.makeText(this, "Lỗi: Không thể xóa sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        productsRef.child(product.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm thành công", Toast.LENGTH_SHORT).show();
                    // Remove from local list
                    myProductsList.remove(product);
                    myProductsAdapter.updateProducts(myProductsList);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi xóa sản phẩm: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to delete product", e);
                });
    }

    private void loadMyProducts() {
        // Bỏ debug log và toast thừa
        showLoading(true);

        // Sử dụng CurrentUser mới với callback
        CurrentUser.loadUserSynchronously(new CurrentUser.LoadUserCallback() {
            @Override
            public void onUserLoaded(UserModel user) {
                currentUser = user;
                String userId = user.getUid();

                if (userId == null || userId.isEmpty()) {
                    Toast.makeText(MyProductsActivity.this, "Lỗi: User ID không hợp lệ", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    return;
                }

                performProductQuery(userId);
            }

            @Override
            public void onError(String error) {
                // Fallback: thử dùng Firebase Auth trực tiếp
                com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser != null) {
                    performProductQuery(firebaseUser.getUid());
                } else {
                    Toast.makeText(MyProductsActivity.this, "Vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
                    showLoading(false);
                    finish();
                }
            }
        });
    }

    private void performProductQuery(String userId) {
        showLoading(true);

        // Thử cả "products" và "Products" để đảm bảo tương thích
        tryLoadFromPath("products", userId, success -> {
            if (!success) {
                tryLoadFromPath("Products", userId, success2 -> {
                    if (!success2) {
                        showEmptyState();
                    }
                });
            }
        });
    }

    private void tryLoadFromPath(String path, String userId, LoadCallback callback) {
        DatabaseReference pathRef = FirebaseDatabase.getInstance().getReference(path);

        pathRef.orderByChild("sellerId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getChildrenCount() > 0) {
                            List<Product> products = new ArrayList<>();

                            for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                                try {
                                    Product product = productSnapshot.getValue(Product.class);
                                    if (product != null) {
                                        product.setId(productSnapshot.getKey());
                                        products.add(product);
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Failed to parse product: " + e.getMessage());
                                }
                            }

                            myProductsList.clear();
                            myProductsList.addAll(products);

                            runOnUiThread(() -> {
                                myProductsAdapter.updateProducts(new ArrayList<>(products));
                                showLoading(false);

                                if (products.isEmpty()) {
                                    showEmptyState();
                                } else {
                                    hideEmptyState();
                                }
                            });

                            callback.onComplete(true);
                        } else {
                            callback.onComplete(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(MyProductsActivity.this,
                                    "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                        callback.onComplete(false);
                    }
                });
    }

    private void showEmptyState() {
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText("Bạn chưa có sản phẩm nào.\nHãy đăng sản phẩm đầu tiên của bạn!");
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private interface LoadCallback {
        void onComplete(boolean success);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners if needed
    }
}
