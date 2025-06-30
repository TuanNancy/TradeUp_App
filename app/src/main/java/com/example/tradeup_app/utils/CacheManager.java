package com.example.tradeup_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Conversation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Cache manager to handle offline data and improve app performance
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String CACHE_PREFS = "TradeUpCache";

    // Cache keys
    private static final String KEY_PRODUCTS_CACHE = "products_cache";
    private static final String KEY_PRODUCTS_CACHE_TIME = "products_cache_time";
    private static final String KEY_CONVERSATIONS_CACHE = "conversations_cache";
    private static final String KEY_CONVERSATIONS_CACHE_TIME = "conversations_cache_time";
    private static final String KEY_USER_CACHE = "user_cache_";
    private static final String KEY_USER_CACHE_TIME = "user_cache_time_";

    private final SharedPreferences cachePrefs;
    private final Gson gson;

    public CacheManager(Context context) {
        this.cachePrefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Cache products list
     */
    public void cacheProducts(List<Product> products) {
        try {
            String productsJson = gson.toJson(products);
            cachePrefs.edit()
                .putString(KEY_PRODUCTS_CACHE, productsJson)
                .putLong(KEY_PRODUCTS_CACHE_TIME, System.currentTimeMillis())
                .apply();
            Log.d(TAG, "Cached " + products.size() + " products");
        } catch (Exception e) {
            Log.e(TAG, "Error caching products: " + e.getMessage());
        }
    }

    /**
     * Get cached products if still valid
     */
    public List<Product> getCachedProducts() {
        try {
            long cacheTime = cachePrefs.getLong(KEY_PRODUCTS_CACHE_TIME, 0);
            long currentTime = System.currentTimeMillis();

            // Check if cache is still valid (within cache duration)
            if (currentTime - cacheTime > Constants.PRODUCT_CACHE_DURATION) {
                Log.d(TAG, "Products cache expired");
                return null;
            }

            String productsJson = cachePrefs.getString(KEY_PRODUCTS_CACHE, null);
            if (productsJson != null) {
                Type listType = new TypeToken<List<Product>>(){}.getType();
                List<Product> products = gson.fromJson(productsJson, listType);
                Log.d(TAG, "Retrieved " + products.size() + " products from cache");
                return products;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached products: " + e.getMessage());
        }
        return null;
    }

    /**
     * Cache conversations list
     */
    public void cacheConversations(List<Conversation> conversations) {
        try {
            String conversationsJson = gson.toJson(conversations);
            cachePrefs.edit()
                .putString(KEY_CONVERSATIONS_CACHE, conversationsJson)
                .putLong(KEY_CONVERSATIONS_CACHE_TIME, System.currentTimeMillis())
                .apply();
            Log.d(TAG, "Cached " + conversations.size() + " conversations");
        } catch (Exception e) {
            Log.e(TAG, "Error caching conversations: " + e.getMessage());
        }
    }

    /**
     * Get cached conversations if still valid
     */
    public List<Conversation> getCachedConversations() {
        try {
            long cacheTime = cachePrefs.getLong(KEY_CONVERSATIONS_CACHE_TIME, 0);
            long currentTime = System.currentTimeMillis();

            if (currentTime - cacheTime > Constants.PRODUCT_CACHE_DURATION) {
                Log.d(TAG, "Conversations cache expired");
                return null;
            }

            String conversationsJson = cachePrefs.getString(KEY_CONVERSATIONS_CACHE, null);
            if (conversationsJson != null) {
                Type listType = new TypeToken<List<Conversation>>(){}.getType();
                List<Conversation> conversations = gson.fromJson(conversationsJson, listType);
                Log.d(TAG, "Retrieved " + conversations.size() + " conversations from cache");
                return conversations;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached conversations: " + e.getMessage());
        }
        return null;
    }

    /**
     * Cache user data
     */
    public void cacheUserData(String userId, String userData) {
        try {
            cachePrefs.edit()
                .putString(KEY_USER_CACHE + userId, userData)
                .putLong(KEY_USER_CACHE_TIME + userId, System.currentTimeMillis())
                .apply();
            Log.d(TAG, "Cached user data for: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error caching user data: " + e.getMessage());
        }
    }

    /**
     * Get cached user data if still valid
     */
    public String getCachedUserData(String userId) {
        try {
            long cacheTime = cachePrefs.getLong(KEY_USER_CACHE_TIME + userId, 0);
            long currentTime = System.currentTimeMillis();

            if (currentTime - cacheTime > Constants.USER_CACHE_DURATION) {
                Log.d(TAG, "User cache expired for: " + userId);
                return null;
            }

            String userData = cachePrefs.getString(KEY_USER_CACHE + userId, null);
            if (userData != null) {
                Log.d(TAG, "Retrieved user data from cache for: " + userId);
            }
            return userData;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached user data: " + e.getMessage());
        }
        return null;
    }

    /**
     * Clear all cache
     */
    public void clearAllCache() {
        try {
            cachePrefs.edit().clear().apply();
            Log.d(TAG, "All cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache: " + e.getMessage());
        }
    }

    /**
     * Clear specific cache
     */
    public void clearProductsCache() {
        try {
            cachePrefs.edit()
                .remove(KEY_PRODUCTS_CACHE)
                .remove(KEY_PRODUCTS_CACHE_TIME)
                .apply();
            Log.d(TAG, "Products cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing products cache: " + e.getMessage());
        }
    }

    public void clearConversationsCache() {
        try {
            cachePrefs.edit()
                .remove(KEY_CONVERSATIONS_CACHE)
                .remove(KEY_CONVERSATIONS_CACHE_TIME)
                .apply();
            Log.d(TAG, "Conversations cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing conversations cache: " + e.getMessage());
        }
    }

    public void clearUserCache(String userId) {
        try {
            cachePrefs.edit()
                .remove(KEY_USER_CACHE + userId)
                .remove(KEY_USER_CACHE_TIME + userId)
                .apply();
            Log.d(TAG, "User cache cleared for: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing user cache: " + e.getMessage());
        }
    }

    /**
     * Check if cache exists and is valid
     */
    public boolean isProductsCacheValid() {
        long cacheTime = cachePrefs.getLong(KEY_PRODUCTS_CACHE_TIME, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - cacheTime) <= Constants.PRODUCT_CACHE_DURATION;
    }

    public boolean isConversationsCacheValid() {
        long cacheTime = cachePrefs.getLong(KEY_CONVERSATIONS_CACHE_TIME, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - cacheTime) <= Constants.PRODUCT_CACHE_DURATION;
    }

    public boolean isUserCacheValid(String userId) {
        long cacheTime = cachePrefs.getLong(KEY_USER_CACHE_TIME + userId, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - cacheTime) <= Constants.USER_CACHE_DURATION;
    }

    /**
     * Get cache size for monitoring
     */
    public int getCacheSize() {
        return cachePrefs.getAll().size();
    }

    /**
     * Update specific product in cache
     */
    public void updateProductInCache(Product updatedProduct) {
        try {
            List<Product> cachedProducts = getCachedProducts();
            if (cachedProducts != null) {
                // Find and update the product
                for (int i = 0; i < cachedProducts.size(); i++) {
                    if (cachedProducts.get(i).getId().equals(updatedProduct.getId())) {
                        cachedProducts.set(i, updatedProduct);
                        break;
                    }
                }
                // Re-cache the updated list
                cacheProducts(cachedProducts);
                Log.d(TAG, "Updated product in cache: " + updatedProduct.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating product in cache: " + e.getMessage());
        }
    }

    /**
     * Remove specific product from cache
     */
    public void removeProductFromCache(String productId) {
        try {
            List<Product> cachedProducts = getCachedProducts();
            if (cachedProducts != null) {
                cachedProducts.removeIf(product -> product.getId().equals(productId));
                cacheProducts(cachedProducts);
                Log.d(TAG, "Removed product from cache: " + productId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing product from cache: " + e.getMessage());
        }
    }
}
