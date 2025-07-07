package com.example.tradeup_app.utils;

import android.util.Log;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DebugHelper {
    private static final String TAG = "DebugHelper";

    public interface DebugCallback {
        void onResult(String result);
    }

    /**
     * Kiểm tra cấu trúc database và tạo test data nếu cần
     */
    public static void checkAndCreateTestData(DebugCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            callback.onResult("❌ Không có user đăng nhập");
            return;
        }

        String userId = currentUser.getUid();
        String email = currentUser.getEmail();

        Log.d(TAG, "=== CHECKING DATABASE STRUCTURE ===");
        Log.d(TAG, "Current User ID: " + userId);
        Log.d(TAG, "Current User Email: " + email);

        StringBuilder result = new StringBuilder();
        result.append("🔍 KIỂM TRA DATABASE:\n");
        result.append("User ID: ").append(userId.substring(0, Math.min(userId.length(), 8))).append("...\n");
        result.append("Email: ").append(email).append("\n\n");

        // Kiểm tra root database
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                result.append("📂 DATABASE STRUCTURE:\n");
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    long count = child.getChildrenCount();
                    result.append("- ").append(key).append(": ").append(count).append(" items\n");
                }

                // Kiểm tra products
                checkProducts(userId, result, () -> {
                    // Kiểm tra transactions
                    checkTransactions(userId, result, () -> {
                        // Tạo test data nếu cần
                        createTestDataIfNeeded(userId, result, callback);
                    });
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                result.append("❌ Lỗi kiểm tra database: ").append(error.getMessage());
                callback.onResult(result.toString());
            }
        });
    }

    private static void checkProducts(String userId, StringBuilder result, Runnable next) {
        result.append("\n🛍️ PRODUCTS CHECK:\n");

        // Kiểm tra cả products và Products
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");
        productsRef.orderByChild("sellerId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        result.append("- products (lowercase): ").append(snapshot.getChildrenCount()).append(" items\n");

                        // Kiểm tra Products (uppercase)
                        DatabaseReference ProductsRef = FirebaseDatabase.getInstance().getReference("Products");
                        ProductsRef.orderByChild("sellerId").equalTo(userId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot2) {
                                        result.append("- Products (uppercase): ").append(snapshot2.getChildrenCount()).append(" items\n");
                                        next.run();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        result.append("❌ Lỗi kiểm tra Products: ").append(error.getMessage()).append("\n");
                                        next.run();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        result.append("❌ Lỗi kiểm tra products: ").append(error.getMessage()).append("\n");
                        next.run();
                    }
                });
    }

    private static void checkTransactions(String userId, StringBuilder result, Runnable next) {
        result.append("\n💳 TRANSACTIONS CHECK:\n");

        // Kiểm tra cả transactions và Transactions
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");
        transactionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int userTransactions = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Transaction transaction = child.getValue(Transaction.class);
                    if (transaction != null &&
                        (userId.equals(transaction.getBuyerId()) || userId.equals(transaction.getSellerId()))) {
                        userTransactions++;
                    }
                }
                result.append("- transactions (lowercase): ").append(userTransactions).append(" user transactions\n");

                // Kiểm tra Transactions (uppercase)
                DatabaseReference TransactionsRef = FirebaseDatabase.getInstance().getReference("Transactions");
                TransactionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot2) {
                        int userTransactions2 = 0;
                        for (DataSnapshot child : snapshot2.getChildren()) {
                            Transaction transaction = child.getValue(Transaction.class);
                            if (transaction != null &&
                                (userId.equals(transaction.getBuyerId()) || userId.equals(transaction.getSellerId()))) {
                                userTransactions2++;
                            }
                        }
                        result.append("- Transactions (uppercase): ").append(userTransactions2).append(" user transactions\n");
                        next.run();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        result.append("❌ Lỗi kiểm tra Transactions: ").append(error.getMessage()).append("\n");
                        next.run();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                result.append("❌ Lỗi kiểm tra transactions: ").append(error.getMessage()).append("\n");
                next.run();
            }
        });
    }

    private static void createTestDataIfNeeded(String userId, StringBuilder result, DebugCallback callback) {
        result.append("\n🧪 CREATING TEST DATA:\n");

        // Tạo test product
        createTestProduct(userId, result, () -> {
            // Tạo test transaction
            createTestTransaction(userId, result, () -> {
                callback.onResult(result.toString());
            });
        });
    }

    private static void createTestProduct(String userId, StringBuilder result, Runnable next) {
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");
        String productId = productsRef.push().getKey();

        Product testProduct = new Product();
        testProduct.setId(productId);
        testProduct.setTitle("📱 iPhone Test - " + System.currentTimeMillis());
        testProduct.setDescription("Đây là sản phẩm test được tạo tự động");
        testProduct.setPrice(1000000.0);
        testProduct.setSellerId(userId);
        testProduct.setSellerName("Test User");
        testProduct.setCategory("Electronics");
        testProduct.setCondition("New");
        testProduct.setLocation("Ho Chi Minh City");
        testProduct.setCreatedAt(System.currentTimeMillis());
        testProduct.setStatus("AVAILABLE");

        productsRef.child(productId).setValue(testProduct)
                .addOnSuccessListener(aVoid -> {
                    result.append("✅ Đã tạo test product: ").append(testProduct.getTitle()).append("\n");
                    next.run();
                })
                .addOnFailureListener(e -> {
                    result.append("❌ Lỗi tạo test product: ").append(e.getMessage()).append("\n");
                    next.run();
                });
    }

    private static void createTestTransaction(String userId, StringBuilder result, Runnable next) {
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");
        String transactionId = transactionsRef.push().getKey();

        Transaction testTransaction = new Transaction();
        testTransaction.setId(transactionId);
        testTransaction.setProductId("test_product_id");
        testTransaction.setProductTitle("📱 Test Transaction Product");
        testTransaction.setBuyerId(userId);
        testTransaction.setBuyerName("Test Buyer");
        testTransaction.setSellerId("test_seller_id");
        testTransaction.setSellerName("Test Seller");
        testTransaction.setFinalPrice(500000.0);
        testTransaction.setStatus("COMPLETED");
        testTransaction.setCreatedAt(System.currentTimeMillis());
        testTransaction.setCompletedAt(System.currentTimeMillis());

        transactionsRef.child(transactionId).setValue(testTransaction)
                .addOnSuccessListener(aVoid -> {
                    result.append("✅ Đã tạo test transaction: ").append(testTransaction.getProductTitle()).append("\n");
                    next.run();
                })
                .addOnFailureListener(e -> {
                    result.append("❌ Lỗi tạo test transaction: ").append(e.getMessage()).append("\n");
                    next.run();
                });
    }
}
