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
        if (currentUser == null) {
            Log.e(TAG, "CurrentUser is null!");
            Toast.makeText(this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Lấy UID từ Firebase Auth thay vì từ UserModel
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.e(TAG, "Firebase user is null!");
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = firebaseUser.getUid();
        String userModelUid = currentUser.getUid();

        Log.d(TAG, "Firebase Auth UID: " + userId);
        Log.d(TAG, "UserModel UID: " + userModelUid);
        Log.d(TAG, "Username: " + currentUser.getUsername());
        Log.d(TAG, "Email from UserModel: " + currentUser.getEmail());
        Log.d(TAG, "Email from Firebase Auth: " + firebaseUser.getEmail());

        showLoading(true);

        // DEBUG: Kiểm tra database structure
        Log.d(TAG, "=== DEBUG: Checking database structure ===");
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Root database children:");
                for (DataSnapshot child : snapshot.getChildren()) {
                    Log.d(TAG, "- " + child.getKey() + " (count: " + child.getChildrenCount() + ")");
                }

                // Kiểm tra các path có thể có
                checkMultiplePaths(userId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Root database check failed: " + error.getMessage());
                checkMultiplePaths(userId);
            }
        });
    }

    private void checkMultiplePaths(String userId) {
        Log.d(TAG, "=== Checking multiple possible paths ===");

        // Danh sách các path có thể có
        String[] possiblePaths = {"Products", "products", "Product", "Items", "items", "Listings"};

        for (String path : possiblePaths) {
            DatabaseReference pathRef = FirebaseDatabase.getInstance().getReference(path);
            pathRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "Path '" + path + "' has " + snapshot.getChildrenCount() + " children");

                    if (snapshot.getChildrenCount() > 0) {
                        // Nếu tìm thấy data, log vài sample
                        int count = 0;
                        for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                            if (count < 3) { // Log 3 sản phẩm đầu tiên
                                Log.d(TAG, "Sample from '" + path + "': " + productSnapshot.getKey());
                                // Thử parse as Product
                                try {
                                    Product product = productSnapshot.getValue(Product.class);
                                    if (product != null) {
                                        Log.d(TAG, "  - Title: " + product.getTitle() + ", SellerId: " + product.getSellerId());
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "  - Cannot parse as Product: " + e.getMessage());
                                }
                                count++;
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "Failed to check path '" + path + "': " + error.getMessage());
                }
            });
        }

        // Vẫn thử query với path gốc
        performActualQuery(userId);
    }

    private void performActualQuery(String userId) {
        Log.d(TAG, "=== Performing actual query for userId: " + userId + " ===");

        productsRef.orderByChild("sellerId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Firebase query completed. Snapshot exists: " + snapshot.exists());
                        Log.d(TAG, "Number of children: " + snapshot.getChildrenCount());

                        myProductsList.clear();
                        int productCount = 0;

                        for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                            String productId = productSnapshot.getKey();
                            Log.d(TAG, "Processing product ID: " + productId);

                            Product product = productSnapshot.getValue(Product.class);
                            if (product != null) {
                                Log.d(TAG, "Product found - Title: " + product.getTitle() +
                                        ", SellerId: " + product.getSellerId() +
                                        ", Status: " + product.getStatus());

                                product.setId(productId);
                                myProductsList.add(product);
                                productCount++;
                            } else {
                                Log.w(TAG, "Product is null for snapshot: " + productId);
                            }
                        }

                        Log.d(TAG, "Total products loaded: " + productCount);
                        Log.d(TAG, "myProductsList size before adapter update: " + myProductsList.size());

                        showLoading(false);
                        updateUI();

                        // Create a copy of the list to avoid reference issues
                        List<Product> productsCopy = new ArrayList<>(myProductsList);
                        Log.d(TAG, "productsCopy size: " + productsCopy.size());
                        myProductsAdapter.updateProducts(productsCopy);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase query cancelled: " + error.getMessage());
                        Log.e(TAG, "Error code: " + error.getCode());
                        Log.e(TAG, "Error details: " + error.getDetails());

                        showLoading(false);
                        Toast.makeText(MyProductsActivity.this,
                                "Lỗi khi tải sản phẩm: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void updateUI() {
        Log.d(TAG, "Updating UI. Products list size: " + myProductsList.size());

        if (myProductsList.isEmpty()) {
            Log.d(TAG, "No products found - showing empty state");
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText("Bạn chưa có sản phẩm nào.\nHãy đăng sản phẩm đầu tiên của bạn!");
            recyclerView.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "Products found - showing RecyclerView with " + myProductsList.size() + " items");
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners if needed
    }
}
