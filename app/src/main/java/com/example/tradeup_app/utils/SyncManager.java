package com.example.tradeup_app.utils;

import android.content.Context;
import android.util.Log;

import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Conversation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronization manager to handle data sync between local cache and Firebase
 */
public class SyncManager {
    private static final String TAG = "SyncManager";

    private final FirebaseManager firebaseManager;
    private final CacheManager cacheManager;
    private final NetworkUtils networkUtils;
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);

    public interface SyncCallback {
        void onSyncStarted();
        void onSyncCompleted(boolean success);
        void onSyncError(String error);
    }

    public SyncManager(Context context) {
        this.firebaseManager = FirebaseManager.getInstance();
        this.cacheManager = new CacheManager(context);
        this.networkUtils = new NetworkUtils(context);
    }

    /**
     * Sync products data with Firebase
     */
    public void syncProducts(SyncCallback callback) {
        if (!networkUtils.isNetworkAvailable()) {
            if (callback != null) {
                callback.onSyncError("Không có kết nối mạng");
            }
            return;
        }

        if (isSyncing.get()) {
            Log.d(TAG, "Sync already in progress");
            return;
        }

        isSyncing.set(true);
        if (callback != null) callback.onSyncStarted();

        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                try {
                    // Cache the fresh data
                    cacheManager.cacheProducts(products);

                    // Apply data validation and sanitization
                    for (Product product : products) {
                        sanitizeProduct(product);
                    }

                    Log.d(TAG, "Products synced successfully: " + products.size() + " items");
                    isSyncing.set(false);
                    if (callback != null) callback.onSyncCompleted(true);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing synced products: " + e.getMessage());
                    isSyncing.set(false);
                    if (callback != null) callback.onSyncError("Lỗi xử lý dữ liệu: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error syncing products: " + error);
                isSyncing.set(false);
                if (callback != null) callback.onSyncError("Lỗi đồng bộ: " + error);
            }
        });
    }

    /**
     * Sync conversations data with Firebase
     */
    public void syncConversations(String userId, SyncCallback callback) {
        if (!networkUtils.isNetworkAvailable()) {
            if (callback != null) {
                callback.onSyncError("Không có kết nối mạng");
            }
            return;
        }

        if (isSyncing.get()) {
            Log.d(TAG, "Sync already in progress");
            return;
        }

        isSyncing.set(true);
        if (callback != null) callback.onSyncStarted();

        firebaseManager.getConversationsForUser(userId, new FirebaseManager.ConversationCallback() {
            @Override
            public void onConversationsLoaded(List<Conversation> conversations) {
                try {
                    // Cache the fresh data
                    cacheManager.cacheConversations(conversations);

                    Log.d(TAG, "Conversations synced successfully: " + conversations.size() + " items");
                    isSyncing.set(false);
                    if (callback != null) callback.onSyncCompleted(true);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing synced conversations: " + e.getMessage());
                    isSyncing.set(false);
                    if (callback != null) callback.onSyncError("Lỗi xử lý dữ liệu: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error syncing conversations: " + error);
                isSyncing.set(false);
                if (callback != null) callback.onSyncError("Lỗi đồng bộ: " + error);
            }
        });
    }

    /**
     * Get products with cache-first strategy
     */
    public void getProducts(FirebaseManager.ProductCallback callback) {
        // Try cache first if network is slow or unavailable
        if (!networkUtils.isNetworkAvailable() || !networkUtils.isGoodTimeForLargeSync()) {
            List<Product> cachedProducts = cacheManager.getCachedProducts();
            if (cachedProducts != null && !cachedProducts.isEmpty()) {
                Log.d(TAG, "Returning cached products: " + cachedProducts.size());
                callback.onProductsLoaded(cachedProducts);
                return;
            }
        }

        // If cache is empty or expired, sync with Firebase
        syncProducts(new SyncCallback() {
            @Override
            public void onSyncStarted() {
                Log.d(TAG, "Started syncing products from Firebase");
            }

            @Override
            public void onSyncCompleted(boolean success) {
                if (success) {
                    List<Product> products = cacheManager.getCachedProducts();
                    if (products != null) {
                        callback.onProductsLoaded(products);
                    } else {
                        callback.onError("Không thể tải dữ liệu");
                    }
                } else {
                    callback.onError("Đồng bộ thất bại");
                }
            }

            @Override
            public void onSyncError(String error) {
                // Fallback to cache even if sync failed
                List<Product> cachedProducts = cacheManager.getCachedProducts();
                if (cachedProducts != null && !cachedProducts.isEmpty()) {
                    Log.d(TAG, "Sync failed, returning cached products");
                    callback.onProductsLoaded(cachedProducts);
                } else {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Get conversations with cache-first strategy
     */
    public void getConversations(String userId, FirebaseManager.ConversationCallback callback) {
        // Try cache first if network is slow or unavailable
        if (!networkUtils.isNetworkAvailable()) {
            List<Conversation> cachedConversations = cacheManager.getCachedConversations();
            if (cachedConversations != null && !cachedConversations.isEmpty()) {
                Log.d(TAG, "Returning cached conversations: " + cachedConversations.size());
                callback.onConversationsLoaded(cachedConversations);
                return;
            }
        }

        // If cache is empty or expired, sync with Firebase
        syncConversations(userId, new SyncCallback() {
            @Override
            public void onSyncStarted() {
                Log.d(TAG, "Started syncing conversations from Firebase");
            }

            @Override
            public void onSyncCompleted(boolean success) {
                if (success) {
                    List<Conversation> conversations = cacheManager.getCachedConversations();
                    if (conversations != null) {
                        callback.onConversationsLoaded(conversations);
                    } else {
                        callback.onError("Không thể tải dữ liệu");
                    }
                } else {
                    callback.onError("Đồng bộ thất bại");
                }
            }

            @Override
            public void onSyncError(String error) {
                // Fallback to cache even if sync failed
                List<Conversation> cachedConversations = cacheManager.getCachedConversations();
                if (cachedConversations != null && !cachedConversations.isEmpty()) {
                    Log.d(TAG, "Sync failed, returning cached conversations");
                    callback.onConversationsLoaded(cachedConversations);
                } else {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Force refresh data from Firebase
     */
    public void forceRefresh(String userId, SyncCallback callback) {
        if (!networkUtils.isNetworkAvailable()) {
            if (callback != null) {
                callback.onSyncError("Không có kết nối mạng");
            }
            return;
        }

        // Clear cache first
        cacheManager.clearAllCache();

        // Sync products and conversations
        syncProducts(new SyncCallback() {
            @Override
            public void onSyncStarted() {
                if (callback != null) callback.onSyncStarted();
            }

            @Override
            public void onSyncCompleted(boolean success) {
                if (success && userId != null) {
                    // Also sync conversations if user is logged in
                    syncConversations(userId, callback);
                } else {
                    if (callback != null) callback.onSyncCompleted(success);
                }
            }

            @Override
            public void onSyncError(String error) {
                if (callback != null) callback.onSyncError(error);
            }
        });
    }

    /**
     * Sanitize product data to ensure consistency
     */
    private void sanitizeProduct(Product product) {
        if (product == null) return;

        // Sanitize text fields
        if (product.getTitle() != null) {
            product.setTitle(DataValidator.sanitizeInput(product.getTitle()));
        }
        if (product.getDescription() != null) {
            product.setDescription(DataValidator.sanitizeInput(product.getDescription()));
        }
        if (product.getLocation() != null) {
            product.setLocation(DataValidator.sanitizeInput(product.getLocation()));
        }

        // Ensure status is valid
        if (product.getStatus() == null || product.getStatus().isEmpty()) {
            product.setStatus(Constants.PRODUCT_STATUS_AVAILABLE);
        }

        // Ensure timestamps are valid
        if (product.getCreatedAt() <= 0) {
            product.setCreatedAt(System.currentTimeMillis());
        }
        if (product.getUpdatedAt() <= 0) {
            product.setUpdatedAt(product.getCreatedAt());
        }

        // Ensure counters are non-negative
        if (product.getViewCount() < 0) {
            product.setViewCount(0);
        }
        if (product.getLikeCount() < 0) {
            product.setLikeCount(0);
        }
    }

    /**
     * Check if sync is currently in progress
     */
    public boolean isSyncing() {
        return isSyncing.get();
    }

    /**
     * Cancel current sync operation
     */
    public void cancelSync() {
        isSyncing.set(false);
        Log.d(TAG, "Sync operation cancelled");
    }

    /**
     * Get cache manager instance
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Get network utils instance
     */
    public NetworkUtils getNetworkUtils() {
        return networkUtils;
    }
}
