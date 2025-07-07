package com.example.tradeup_app.firebase;

import android.util.Log;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Message;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.models.Offer;
import com.example.tradeup_app.models.Transaction;
import com.example.tradeup_app.models.Rating;
import com.example.tradeup_app.models.Report;
import com.example.tradeup_app.utils.NotificationManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;

    // Node names
    public static final String MESSAGES_NODE = "messages";
    public static final String PRODUCTS_NODE = "products";
    public static final String CONVERSATIONS_NODE = "conversations";
    public static final String OFFERS_NODE = "offers";
    public static final String TRANSACTIONS_NODE = "transactions";
    public static final String RATINGS_NODE = "ratings";
    public static final String REPORTS_NODE = "reports";
    public static final String USERS_NODE = "Users";

    // Callback interfaces
    public interface ProductCallback {
        void onProductsLoaded(List<Product> products);
        void onError(String error);
    }

    public interface ConversationCallback {
        void onConversationsLoaded(List<Conversation> conversations);
        void onError(String error);
    }

    public interface OfferCallback {
        void onOffersLoaded(List<Offer> offers);
        void onError(String error);
    }

    public interface TransactionCallback {
        void onTransactionsLoaded(List<Transaction> transactions);
        void onError(String error);
    }

    // Payment-related callback interfaces
    public interface OnTransactionSavedListener {
        void onSuccess(String transactionId);
        void onError(String error);
    }

    public interface OnTransactionsLoadedListener {
        void onSuccess(List<Transaction> transactions);
        void onError(String error);
    }

    public interface OnStatusUpdateListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnPaymentSuccessListener {
        void onSuccess(String transactionId);
        void onError(String error);
    }

    public interface RatingCallback {
        void onRatingsLoaded(List<Rating> ratings);
        void onError(String error);
    }

    public interface ReportCallback {
        void onReportsLoaded(List<Report> reports);
        void onError(String error);
    }

    public interface FlaggedUsersCallback {
        void onFlaggedUsersLoaded(List<com.example.tradeup_app.auth.Domain.UserModel> users);
        void onError(String error);
    }

    private FirebaseManager() {
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseDatabase getDatabase() {
        return database;
    }

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public void addProduct(Product product, OnCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            database.getReference(PRODUCTS_NODE).child("error")
                .setValue(null)
                .addOnCompleteListener(task -> listener.onComplete(task));
            return;
        }

        product.setSellerId(userId);
        product.setCreatedAt(System.currentTimeMillis());
        product.setUpdatedAt(System.currentTimeMillis());
        product.setStatus("Available");
        product.setViewCount(0);

        String key = database.getReference(PRODUCTS_NODE).push().getKey();
        if (key != null) {
            product.setId(key);
            database.getReference(PRODUCTS_NODE)
                .child(key)
                .setValue(product)
                .addOnCompleteListener(listener);
        }
    }

    // ==================== PRODUCT UPDATE METHODS - DATABASE SPECIFIC ====================

    /**
     * Update specific fields of a product in database
     */
    public void updateProductFields(String productId, java.util.Map<String, Object> updates, OnCompleteListener<Void> listener) {
        Log.d("FirebaseManager", "=== UPDATING PRODUCT FIELDS ===");
        Log.d("FirebaseManager", "Product ID: " + productId);
        Log.d("FirebaseManager", "Fields to update: " + updates.keySet());

        if (productId == null || productId.isEmpty()) {
            Log.e("FirebaseManager", "Product ID is null or empty");
            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Product ID is required")));
            return;
        }

        if (updates == null || updates.isEmpty()) {
            Log.e("FirebaseManager", "No updates provided");
            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Updates map is empty")));
            return;
        }

        // Always add updatedAt timestamp
        updates.put("updatedAt", System.currentTimeMillis());

        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);

        productRef.updateChildren(updates)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("FirebaseManager", "✅ Product fields updated successfully");
                } else {
                    Log.e("FirebaseManager", "❌ Failed to update product fields: " +
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                }
                listener.onComplete(task);
            });
    }

    /**
     * Update product title and description
     */
    public void updateProductBasicInfo(String productId, String title, String description, OnCompleteListener<Void> listener) {
        Log.d("FirebaseManager", "Updating product basic info - ID: " + productId);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("title", title);
        updates.put("description", description);

        updateProductFields(productId, updates, listener);
    }

    /**
     * Update product price
     */
    public void updateProductPrice(String productId, double price, OnCompleteListener<Void> listener) {
        Log.d("FirebaseManager", "Updating product price - ID: " + productId + ", New price: " + price);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("price", price);

        updateProductFields(productId, updates, listener);
    }

    /**
     * Update product category and condition
     */
    public void updateProductCategoryAndCondition(String productId, String category, String condition, OnCompleteListener<Void> listener) {
        Log.d("FirebaseManager", "Updating product category and condition - ID: " + productId);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("category", category);
        updates.put("condition", condition);

        updateProductFields(productId, updates, listener);
    }

    /**
     * Update product images
     */
    public void updateProductImages(String productId, java.util.List<String> imageUrls, OnCompleteListener<Void> listener) {
        Log.d("FirebaseManager", "Updating product images - ID: " + productId + ", Images count: " +
            (imageUrls != null ? imageUrls.size() : 0));

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("imageUrls", imageUrls);

        updateProductFields(productId, updates, listener);
    }

    /**
     * Update product location and tags
     */
    public void updateProductLocationAndTags(String productId, String location, java.util.List<String> tags, OnCompleteListener<Void> listener) {
        Log.d("FirebaseManager", "Updating product location and tags - ID: " + productId);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("location", location);
        if (tags != null) {
            updates.put("tags", tags);
        }

        updateProductFields(productId, updates, listener);
    }

    /**
     * Update product negotiable status
     */
    public void updateProductNegotiable(String productId, boolean negotiable, OnCompleteListener<Void> listener) {
        Log.d("FirebaseManager", "Updating product negotiable status - ID: " + productId + ", Negotiable: " + negotiable);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("negotiable", negotiable);

        updateProductFields(productId, updates, listener);
    }

    public void updateProduct(Product product, OnCompleteListener<Void> listener) {
        Log.d("FirebaseManager", "=== STARTING COMPLETE PRODUCT UPDATE ===");

        // Validate input
        if (product == null) {
            Log.e("FirebaseManager", "Product is null - cannot update");
            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Product is null")));
            return;
        }

        if (product.getId() == null || product.getId().isEmpty()) {
            Log.e("FirebaseManager", "Product ID is null or empty - cannot update");
            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Product ID is null or empty")));
            return;
        }

        String productId = product.getId();
        Log.d("FirebaseManager", "Updating complete product with ID: " + productId);
        Log.d("FirebaseManager", "Product details:");
        Log.d("FirebaseManager", "  - Title: " + product.getTitle());
        Log.d("FirebaseManager", "  - Price: " + product.getPrice());
        Log.d("FirebaseManager", "  - Description: " + product.getDescription());
        Log.d("FirebaseManager", "  - Category: " + product.getCategory());
        Log.d("FirebaseManager", "  - Condition: " + product.getCondition());
        Log.d("FirebaseManager", "  - Location: " + product.getLocation());
        Log.d("FirebaseManager", "  - Negotiable: " + product.isNegotiable());
        Log.d("FirebaseManager", "  - Images count: " + (product.getImageUrls() != null ? product.getImageUrls().size() : 0));

        // Set updated timestamp
        long currentTime = System.currentTimeMillis();
        product.setUpdatedAt(currentTime);
        Log.d("FirebaseManager", "Set updatedAt timestamp: " + currentTime);

        // Get reference to the product in Firebase
        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);

        Log.d("FirebaseManager", "Saving complete product to Firebase path: " + PRODUCTS_NODE + "/" + productId);

        // Update the entire product object in Firebase
        productRef.setValue(product)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("FirebaseManager", "✅ Complete product update SUCCESSFUL for ID: " + productId);
                    Log.d("FirebaseManager", "=== COMPLETE PRODUCT UPDATE COMPLETED ===");
                } else {
                    Log.e("FirebaseManager", "❌ Complete product update FAILED for ID: " + productId);
                    if (task.getException() != null) {
                        Log.e("FirebaseManager", "Error details: " + task.getException().getMessage());
                        task.getException().printStackTrace();
                    }
                    Log.d("FirebaseManager", "=== COMPLETE PRODUCT UPDATE FAILED ===");
                }

                // Always call the listener
                listener.onComplete(task);
            })
            .addOnFailureListener(e -> {
                Log.e("FirebaseManager", "❌ Firebase setValue failed: " + e.getMessage());
                e.printStackTrace();
            });
    }

    // Product methods
    public void getProducts(ProductCallback callback) {
        database.getReference(PRODUCTS_NODE)
                .orderByChild("createdAt")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> products = new java.util.ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Product product = dataSnapshot.getValue(Product.class);
                        if (product != null) {
                            product.setId(dataSnapshot.getKey());
                            products.add(product);
                        }
                    }
                    callback.onProductsLoaded(products);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void searchProducts(String query, String category, String condition,
                             double minPrice, double maxPrice, String sortBy,
                             ProductCallback callback) {
        DatabaseReference ref = database.getReference(PRODUCTS_NODE);
        Query baseQuery = ref;

        // Filter by category if specified
        if (category != null && !category.isEmpty() && !category.equals("Tất cả")) {
            baseQuery = ref.orderByChild("category").equalTo(category);
        }

        baseQuery.get().addOnSuccessListener(snapshot -> {
            List<Product> results = new java.util.ArrayList<>();
            for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                Product product = dataSnapshot.getValue(Product.class);
                if (product != null) {
                    product.setId(dataSnapshot.getKey());

                    // Apply filters
                    if (matchesSearchCriteria(product, query, condition, minPrice, maxPrice)) {
                        results.add(product);
                    }
                }
            }

            // Apply sorting
            if (sortBy != null) {
                switch (sortBy) {
                    case "price_asc":
                        results.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                        break;
                    case "price_desc":
                        results.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                        break;
                    case "date":
                        results.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        break;
                }
            }

            callback.onProductsLoaded(results);
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private boolean matchesSearchCriteria(Product product, String query, String condition,
                                        double minPrice, double maxPrice) {
        // Match query text - TÌM KIẾM TRONG TITLE, DESCRIPTION VÀ TAGS
        if (query != null && !query.isEmpty()) {
            String lowerQuery = query.toLowerCase();
            boolean matchesTitle = product.getTitle().toLowerCase().contains(lowerQuery);
            boolean matchesDesc = product.getDescription().toLowerCase().contains(lowerQuery);

            // ✅ THÊM: Tìm kiếm trong tags
            boolean matchesTags = false;
            if (product.getTags() != null && !product.getTags().isEmpty()) {
                for (String tag : product.getTags()) {
                    if (tag.toLowerCase().contains(lowerQuery)) {
                        matchesTags = true;
                        break;
                    }
                }
            }

            if (!matchesTitle && !matchesDesc && !matchesTags) {
                return false;
            }
        }

        // Match condition
        if (condition != null && !condition.isEmpty() && !condition.equals("Tất cả")) {
            if (!product.getCondition().equals(condition)) {
                return false;
            }
        }

        // ✅ SỬA: Validation khoảng giá tốt hơn
        if (minPrice > 0 && product.getPrice() < minPrice) {
            return false;
        }
        if (maxPrice > 0 && product.getPrice() > maxPrice) {
            return false;
        }

        // ✅ THÊM: Validation minPrice <= maxPrice
        if (minPrice > 0 && maxPrice > 0 && minPrice > maxPrice) {
            return false;
        }

        return true;
    }

    // ==================== MESSAGES AND CONVERSATIONS METHODS ====================

    public void sendMessage(Message message, OnCompleteListener<Void> listener) {
        String key = database.getReference(MESSAGES_NODE).push().getKey();
        if (key != null) {
            message.setId(key);
            message.setTimestamp(System.currentTimeMillis());

            database.getReference(MESSAGES_NODE)
                .child(key)
                .setValue(message)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update conversation with last message
                        updateConversationLastMessage(message);
                    }
                    listener.onComplete(task);
                });
        }
    }

    public void getMessagesForConversation(String conversationId, ValueEventListener listener) {
        database.getReference(MESSAGES_NODE)
            .orderByChild("conversationId")
            .equalTo(conversationId)
            .addValueEventListener(listener);
    }

    public void createOrGetConversation(String productId, String buyerId, String sellerId,
                                      String productTitle, String productImageUrl,
                                      OnCompleteListener<String> listener) {
        // Check if conversation already exists
        String conversationId = generateConversationId(productId, buyerId, sellerId);

        database.getReference(CONVERSATIONS_NODE)
            .child(conversationId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        // Conversation exists
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(conversationId));
                    } else {
                        // Create new conversation
                        Conversation conversation = new Conversation();
                        conversation.setId(conversationId);
                        conversation.setProductId(productId);
                        conversation.setProductTitle(productTitle);
                        conversation.setProductImageUrl(productImageUrl);
                        conversation.setBuyerId(buyerId);
                        conversation.setSellerId(sellerId);

                        // Get user names
                        getUserName(buyerId, buyerName -> {
                            conversation.setBuyerName(buyerName);
                            getUserName(sellerId, sellerName -> {
                                conversation.setSellerName(sellerName);

                                database.getReference(CONVERSATIONS_NODE)
                                    .child(conversationId)
                                    .setValue(conversation)
                                    .addOnCompleteListener(createTask -> {
                                        if (createTask.isSuccessful()) {
                                            listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(conversationId));
                                        } else {
                                            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(createTask.getException()));
                                        }
                                    });
                            });
                        });
                    }
                } else {
                    listener.onComplete(com.google.android.gms.tasks.Tasks.forException(task.getException()));
                }
            });
    }

    private String generateConversationId(String productId, String buyerId, String sellerId) {
        return productId + "_" + buyerId + "_" + sellerId;
    }

    private void updateConversationLastMessage(Message message) {
        String conversationId = message.getConversationId();
        if (conversationId != null) {
            DatabaseReference conversationRef = database.getReference(CONVERSATIONS_NODE).child(conversationId);
            conversationRef.child("lastMessage").setValue(message.getContent());
            conversationRef.child("lastMessageTime").setValue(message.getTimestamp());
            conversationRef.child("updatedAt").setValue(System.currentTimeMillis());

            // Increment unread count for receiver
            conversationRef.child("unreadCount").get().addOnSuccessListener(snapshot -> {
                int currentCount = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
                conversationRef.child("unreadCount").setValue(currentCount + 1);
            });
        }
    }

    public void markConversationAsRead(String conversationId, OnCompleteListener<Void> listener) {
        database.getReference(CONVERSATIONS_NODE)
            .child(conversationId)
            .child("unreadCount")
            .setValue(0)
            .addOnCompleteListener(listener);
    }

    public void getConversationsForUser(String userId, ConversationCallback callback) {
        database.getReference(CONVERSATIONS_NODE)
            .orderByChild("updatedAt")
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Conversation> conversations = new java.util.ArrayList<>();
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Conversation conversation = dataSnapshot.getValue(Conversation.class);
                    if (conversation != null &&
                        (userId.equals(conversation.getBuyerId()) || userId.equals(conversation.getSellerId()))) {
                        conversation.setId(dataSnapshot.getKey());
                        conversations.add(conversation);
                    }
                }
                // Sort by most recent
                conversations.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
                callback.onConversationsLoaded(conversations);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void getUserName(String userId, UserNameCallback callback) {
        database.getReference(USERS_NODE)
            .child(userId)
            .child("name")
            .get()
            .addOnSuccessListener(snapshot -> {
                String name = snapshot.exists() ? snapshot.getValue(String.class) : "Unknown User";
                callback.onUserNameRetrieved(name);
            })
            .addOnFailureListener(e -> callback.onUserNameRetrieved("Unknown User"));
    }

    public interface UserNameCallback {
        void onUserNameRetrieved(String name);
    }

    // ==================== UTILITY METHODS ====================

    public void updateProductStatus(String productId, String status, OnCompleteListener<Void> listener) {
        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);
        productRef.child("status").setValue(status);
        productRef.child("updatedAt").setValue(System.currentTimeMillis())
            .addOnCompleteListener(listener);
    }

    public void incrementProductViewCount(String productId) {
        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);
        productRef.child("viewCount").get().addOnSuccessListener(snapshot -> {
            int currentCount = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
            productRef.child("viewCount").setValue(currentCount + 1);
            productRef.child("lastViewedAt").setValue(System.currentTimeMillis());
        });
    }

    public void toggleProductLike(String productId, String userId, OnCompleteListener<Boolean> listener) {
        DatabaseReference likesRef = database.getReference("product_likes").child(productId).child(userId);
        likesRef.get().addOnSuccessListener(snapshot -> {
            boolean isLiked = snapshot.exists();
            if (isLiked) {
                // Unlike
                likesRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        decrementLikeCount(productId);
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(false));
                    }
                });
            } else {
                // Like
                likesRef.setValue(System.currentTimeMillis()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        incrementLikeCount(productId);
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(true));
                    }
                });
            }
        });
    }

    private void incrementLikeCount(String productId) {
        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);
        productRef.child("likeCount").get().addOnSuccessListener(snapshot -> {
            int currentCount = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
            productRef.child("likeCount").setValue(currentCount + 1);
        });
    }

    private void decrementLikeCount(String productId) {
        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);
        productRef.child("likeCount").get().addOnSuccessListener(snapshot -> {
            int currentCount = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
            productRef.child("likeCount").setValue(Math.max(0, currentCount - 1));
        });
    }

    // ==================== OFFERS METHODS ====================

    public void submitOffer(Offer offer, OnCompleteListener<Void> listener) {
        String key = database.getReference(OFFERS_NODE).push().getKey();
        if (key != null) {
            offer.setId(key);
            database.getReference(OFFERS_NODE)
                .child(key)
                .setValue(offer)
                .addOnCompleteListener(listener);
        }
    }

    public void getOffersForProduct(String productId, OfferCallback callback) {
        database.getReference(OFFERS_NODE)
            .orderByChild("productId")
            .equalTo(productId)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Offer> offers = new java.util.ArrayList<>();
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Offer offer = dataSnapshot.getValue(Offer.class);
                    if (offer != null) {
                        offer.setId(dataSnapshot.getKey());
                        offers.add(offer);
                    }
                }
                callback.onOffersLoaded(offers);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getOffersForSeller(String sellerId, OfferCallback callback) {
        database.getReference(OFFERS_NODE)
            .orderByChild("sellerId")
            .equalTo(sellerId)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Offer> offers = new java.util.ArrayList<>();
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Offer offer = dataSnapshot.getValue(Offer.class);
                    if (offer != null) {
                        offer.setId(dataSnapshot.getKey());
                        offers.add(offer);
                    }
                }
                callback.onOffersLoaded(offers);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getOffersForBuyer(String buyerId, OfferCallback callback) {
        database.getReference(OFFERS_NODE)
            .orderByChild("buyerId")
            .equalTo(buyerId)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Offer> offers = new java.util.ArrayList<>();
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Offer offer = dataSnapshot.getValue(Offer.class);
                    if (offer != null) {
                        offer.setId(dataSnapshot.getKey());
                        offers.add(offer);
                    }
                }
                callback.onOffersLoaded(offers);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateOfferStatus(String offerId, String status, OnCompleteListener<Void> listener) {
        database.getReference(OFFERS_NODE)
            .child(offerId)
            .child("status")
            .setValue(status)
            .addOnCompleteListener(listener);

        // Update timestamp
        database.getReference(OFFERS_NODE)
            .child(offerId)
            .child("updatedAt")
            .setValue(System.currentTimeMillis());
    }

    public void counterOffer(String offerId, double counterPrice, String counterMessage, OnCompleteListener<Void> listener) {
        DatabaseReference offerRef = database.getReference(OFFERS_NODE).child(offerId);
        offerRef.child("status").setValue("COUNTERED");
        offerRef.child("counterPrice").setValue(counterPrice);
        offerRef.child("counterMessage").setValue(counterMessage);
        offerRef.child("updatedAt").setValue(System.currentTimeMillis())
            .addOnCompleteListener(listener);
    }

    // ==================== TRANSACTIONS METHODS ====================

    public void createTransaction(Transaction transaction, OnCompleteListener<Void> listener) {
        String key = database.getReference(TRANSACTIONS_NODE).push().getKey();
        if (key != null) {
            transaction.setId(key);

            // Save to main transactions node
            database.getReference(TRANSACTIONS_NODE)
                .child(key)
                .setValue(transaction)
                .addOnSuccessListener(aVoid -> {
                    // Also save to user_transactions for both buyer and seller
                    database.getReference("user_transactions")
                        .child(transaction.getBuyerId())
                        .child(key)
                        .setValue(transaction);

                    database.getReference("user_transactions")
                        .child(transaction.getSellerId())
                        .child(key)
                        .setValue(transaction);

                    if (listener != null) {
                        listener.onComplete(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onComplete(null);
                    }
                });
        }
    }

    public void getTransactionsForUser(String userId, TransactionCallback callback) {
        // Get transactions where user is either buyer or seller
        database.getReference(TRANSACTIONS_NODE)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Transaction> transactions = new java.util.ArrayList<>();
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Transaction transaction = dataSnapshot.getValue(Transaction.class);
                    if (transaction != null &&
                        (userId.equals(transaction.getBuyerId()) || userId.equals(transaction.getSellerId()))) {
                        transaction.setId(dataSnapshot.getKey());
                        transactions.add(transaction);
                    }
                }
                callback.onTransactionsLoaded(transactions);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateTransactionStatus(String transactionId, String status, OnCompleteListener<Void> listener) {
        database.getReference(TRANSACTIONS_NODE)
            .child(transactionId)
            .child("status")
            .setValue(status)
            .addOnCompleteListener(listener);
    }

    public void markTransactionCompleted(String transactionId, OnCompleteListener<Void> listener) {
        DatabaseReference transactionRef = database.getReference(TRANSACTIONS_NODE).child(transactionId);
        transactionRef.child("status").setValue("COMPLETED");
        transactionRef.child("completedAt").setValue(System.currentTimeMillis())
            .addOnCompleteListener(listener);
    }

    // ==================== RATINGS METHODS ====================

    public void submitRating(Rating rating, OnCompleteListener<Void> listener) {
        String key = database.getReference(RATINGS_NODE).push().getKey();
        if (key != null) {
            rating.setId(key);
            database.getReference(RATINGS_NODE)
                .child(key)
                .setValue(rating)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update user's average rating
                        updateUserRating(rating.getRatedUserId());

                        // Mark transaction as rated
                        if (rating.getTransactionId() != null) {
                            String ratingField = "BUYER".equals(rating.getUserType()) ? "buyerRated" : "sellerRated";
                            database.getReference(TRANSACTIONS_NODE)
                                .child(rating.getTransactionId())
                                .child(ratingField)
                                .setValue(true);
                        }
                    }
                    listener.onComplete(task);
                });
        }
    }

    public void getRatingsForUser(String userId, RatingCallback callback) {
        database.getReference(RATINGS_NODE)
            .orderByChild("ratedUserId")
            .equalTo(userId)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Rating> ratings = new java.util.ArrayList<>();
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Rating rating = dataSnapshot.getValue(Rating.class);
                    if (rating != null) {
                        rating.setId(dataSnapshot.getKey());
                        ratings.add(rating);
                    }
                }
                callback.onRatingsLoaded(ratings);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void updateUserRating(String userId) {
        getRatingsForUser(userId, new RatingCallback() {
            @Override
            public void onRatingsLoaded(List<Rating> ratings) {
                if (!ratings.isEmpty()) {
                    double totalStars = 0;
                    for (Rating rating : ratings) {
                        totalStars += rating.getStars();
                    }
                    double averageRating = totalStars / ratings.size();

                    database.getReference(USERS_NODE)
                        .child(userId)
                        .child("rating")
                        .setValue(String.format("%.1f", averageRating));
                }
            }

            @Override
            public void onError(String error) {
                // Log error but don't fail the rating submission
            }
        });
    }

    // ==================== REPORTS METHODS ====================

    public void submitReport(Report report, OnCompleteListener<Void> listener) {
        String key = database.getReference(REPORTS_NODE).push().getKey();
        if (key != null) {
            report.setId(key);
            database.getReference(REPORTS_NODE)
                .child(key)
                .setValue(report)
                .addOnCompleteListener(listener);
        }
    }

    public void getReportsForAdmin(ReportCallback callback) {
        database.getReference(REPORTS_NODE)
            .orderByChild("status")
            .equalTo("PENDING")
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Report> reports = new java.util.ArrayList<>();
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Report report = dataSnapshot.getValue(Report.class);
                    if (report != null) {
                        report.setId(dataSnapshot.getKey());
                        reports.add(report);
                    }
                }
                callback.onReportsLoaded(reports);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateReportStatus(String reportId, String status, String adminId, String adminNotes,
                                 String actionTaken, OnCompleteListener<Void> listener) {
        DatabaseReference reportRef = database.getReference(REPORTS_NODE).child(reportId);
        reportRef.child("status").setValue(status);
        reportRef.child("adminId").setValue(adminId);
        reportRef.child("adminNotes").setValue(adminNotes);
        reportRef.child("actionTaken").setValue(actionTaken);
        reportRef.child("reviewedAt").setValue(System.currentTimeMillis())
            .addOnCompleteListener(listener);
    }

    // ==================== PAYMENT-SPECIFIC METHODS ====================

    public void saveTransaction(Transaction transaction, OnTransactionSavedListener listener) {
        String key = database.getReference(TRANSACTIONS_NODE).push().getKey();
        if (key != null) {
            transaction.setId(key);
            database.getReference(TRANSACTIONS_NODE)
                .child(key)
                .setValue(transaction)
                .addOnSuccessListener(aVoid -> listener.onSuccess(key))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            listener.onError("Failed to generate transaction ID");
        }
    }

    /**
     * Handle successful payment - save transaction and send notifications
     */
    public void handlePaymentSuccess(Transaction transaction, OnPaymentSuccessListener listener) {
        // Save transaction first
        saveTransaction(transaction, new OnTransactionSavedListener() {
            @Override
            public void onSuccess(String transactionId) {
                // Update product status to sold
                updateProductStatus(transaction.getProductId(), "Sold", new OnStatusUpdateListener() {
                    @Override
                    public void onSuccess() {
                        // Save transaction to both buyer and seller history
                        saveTransactionToUserHistory(transaction, transactionId);

                        // Send notification to seller
                        sendPaymentSuccessNotification(transaction);

                        listener.onSuccess(transactionId);
                    }

                    @Override
                    public void onError(String error) {
                        listener.onError("Failed to update product status: " + error);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                listener.onError("Failed to save transaction: " + errorMessage);
            }
        });
    }

    /**
     * Save transaction to both buyer's and seller's transaction history
     */
    private void saveTransactionToUserHistory(Transaction transaction, String transactionId) {
        // Save to buyer's history
        database.getReference("user_transactions")
                .child(transaction.getBuyerId())
                .child(transactionId)
                .setValue(transaction);

        // Save to seller's history
        database.getReference("user_transactions")
                .child(transaction.getSellerId())
                .child(transactionId)
                .setValue(transaction);
    }

    /**
     * Send payment success notification to seller
     */
    private void sendPaymentSuccessNotification(Transaction transaction) {
        try {
            NotificationManager notificationManager = NotificationManager.getInstance();
            notificationManager.sendPaymentSuccessNotification(
                transaction.getProductId(),
                transaction.getProductTitle(),
                transaction.getBuyerName(),
                transaction.getFinalPrice(),
                transaction.getSellerId()
            );
        } catch (Exception e) {
            Log.e("FirebaseManager", "Failed to send payment notification: " + e.getMessage());
        }
    }

    public void getUserTransactions(String userId, OnTransactionsLoadedListener listener) {
        // Read from user_transactions node where we actually save the data
        database.getReference("user_transactions")
            .child(userId)
            .orderByChild("createdAt")
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Transaction> transactions = new java.util.ArrayList<>();
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Transaction transaction = dataSnapshot.getValue(Transaction.class);
                    if (transaction != null) {
                        transaction.setId(dataSnapshot.getKey());
                        transactions.add(transaction);
                    }
                }
                // Sort by most recent first
                transactions.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                listener.onSuccess(transactions);
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void updateProductStatus(String productId, String status, OnStatusUpdateListener listener) {
        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);
        productRef.child("status").setValue(status);
        productRef.child("updatedAt").setValue(System.currentTimeMillis())
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void checkPendingOffers(String productId, String userId, OnCompleteListener<Boolean> listener) {
        database.getReference(OFFERS_NODE)
            .orderByChild("productId")
            .equalTo(productId)
            .get()
            .addOnSuccessListener(snapshot -> {
                boolean hasPendingOffers = false;
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Offer offer = dataSnapshot.getValue(Offer.class);
                    if (offer != null && userId.equals(offer.getBuyerId()) &&
                        "PENDING".equals(offer.getStatus())) {
                        hasPendingOffers = true;
                        break;
                    }
                }
                listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(hasPendingOffers));
            })
            .addOnFailureListener(e ->
                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e)));
    }

    // ==================== ENHANCED TRANSACTION METHODS ====================

    /**
     * Get all transactions for a user from both main transactions node and user_transactions node
     * This ensures we display both old transactions (before the fix) and new transactions
     */
    public void getAllTransactionsForUser(String userId, TransactionCallback callback) {
        Log.d("FirebaseManager", "getAllTransactionsForUser called for userId: " + userId);

        // Use a map to avoid duplicates and track transactions by ID
        java.util.Map<String, Transaction> transactionMap = new java.util.HashMap<>();
        final boolean[] firstCallCompleted = {false};
        final boolean[] secondCallCompleted = {false};

        // First, get transactions from main TRANSACTIONS_NODE (for old data)
        Log.d("FirebaseManager", "Starting to fetch from TRANSACTIONS_NODE");
        database.getReference(TRANSACTIONS_NODE)
            .get()
            .addOnSuccessListener(snapshot -> {
                Log.d("FirebaseManager", "TRANSACTIONS_NODE fetch success, children count: " + snapshot.getChildrenCount());
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Transaction transaction = dataSnapshot.getValue(Transaction.class);
                    if (transaction != null &&
                        (userId.equals(transaction.getBuyerId()) || userId.equals(transaction.getSellerId()))) {
                        transaction.setId(dataSnapshot.getKey());
                        transactionMap.put(transaction.getId(), transaction);
                        Log.d("FirebaseManager", "Found transaction in TRANSACTIONS_NODE: " + transaction.getId() + " for product: " + transaction.getProductTitle());
                    }
                }
                Log.d("FirebaseManager", "Total transactions found in TRANSACTIONS_NODE: " + transactionMap.size());
                firstCallCompleted[0] = true;
                checkAndReturnResults(transactionMap, firstCallCompleted, secondCallCompleted, callback);
            })
            .addOnFailureListener(e -> {
                Log.e("FirebaseManager", "Error fetching from TRANSACTIONS_NODE: " + e.getMessage());
                firstCallCompleted[0] = true;
                checkAndReturnResults(transactionMap, firstCallCompleted, secondCallCompleted, callback);
            });

        // Second, get transactions from user_transactions node (for new data)
        Log.d("FirebaseManager", "Starting to fetch from user_transactions");
        database.getReference("user_transactions")
            .child(userId)
            .get()
            .addOnSuccessListener(snapshot -> {
                Log.d("FirebaseManager", "user_transactions fetch success, children count: " + snapshot.getChildrenCount());
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Transaction transaction = dataSnapshot.getValue(Transaction.class);
                    if (transaction != null) {
                        transaction.setId(dataSnapshot.getKey());
                        // This will overwrite if exists, ensuring we have the most recent data
                        transactionMap.put(transaction.getId(), transaction);
                        Log.d("FirebaseManager", "Found transaction in user_transactions: " + transaction.getId() + " for product: " + transaction.getProductTitle());
                    }
                }
                Log.d("FirebaseManager", "Total transactions after user_transactions: " + transactionMap.size());
                secondCallCompleted[0] = true;
                checkAndReturnResults(transactionMap, firstCallCompleted, secondCallCompleted, callback);
            })
            .addOnFailureListener(e -> {
                Log.e("FirebaseManager", "Error fetching from user_transactions: " + e.getMessage());
                secondCallCompleted[0] = true;
                checkAndReturnResults(transactionMap, firstCallCompleted, secondCallCompleted, callback);
            });
    }

    private void checkAndReturnResults(java.util.Map<String, Transaction> transactionMap,
                                     boolean[] firstCallCompleted, boolean[] secondCallCompleted,
                                     TransactionCallback callback) {
        if (firstCallCompleted[0] && secondCallCompleted[0]) {
            // Both calls completed, return merged results
            List<Transaction> allTransactions = new java.util.ArrayList<>(transactionMap.values());
            Log.d("FirebaseManager", "Both calls completed. Final transaction count: " + allTransactions.size());

            // Sort by most recent first
            allTransactions.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

            for (Transaction t : allTransactions) {
                Log.d("FirebaseManager", "Final transaction: " + t.getId() + " - " + t.getProductTitle() + " - Created: " + t.getCreatedAt());
            }

            callback.onTransactionsLoaded(allTransactions);
        }
    }

    // ==================== TEST METHODS FOR DEBUG ====================

    /**
     * Create a test transaction for debugging purposes
     */
    public void createTestTransaction(OnTransactionSavedListener listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            listener.onError("User not logged in");
            return;
        }

        // Create a test transaction
        Transaction testTransaction = new Transaction();
        testTransaction.setProductId("test_product_123");
        testTransaction.setProductTitle("Test Product for Transaction History");
        testTransaction.setBuyerId(currentUserId);
        testTransaction.setBuyerName("Test Buyer");
        testTransaction.setSellerId("test_seller_456");
        testTransaction.setSellerName("Test Seller");
        testTransaction.setFinalPrice(100000.0);
        testTransaction.setStatus("COMPLETED");
        testTransaction.setCreatedAt(System.currentTimeMillis());
        testTransaction.setPaymentMethod("TEST");

        // Save the test transaction
        String key = database.getReference(TRANSACTIONS_NODE).push().getKey();
        if (key != null) {
            testTransaction.setId(key);

            // Save to main transactions node
            database.getReference(TRANSACTIONS_NODE)
                .child(key)
                .setValue(testTransaction)
                .addOnSuccessListener(aVoid -> {
                    // Also save to user_transactions
                    database.getReference("user_transactions")
                        .child(currentUserId)
                        .child(key)
                        .setValue(testTransaction)
                        .addOnSuccessListener(aVoid2 -> {
                            Log.d("FirebaseManager", "Test transaction created successfully: " + key);
                            listener.onSuccess(key);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseManager", "Failed to save test transaction to user_transactions: " + e.getMessage());
                            listener.onError(e.getMessage());
                        });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseManager", "Failed to save test transaction to main node: " + e.getMessage());
                    listener.onError(e.getMessage());
                });
        } else {
            listener.onError("Failed to generate transaction ID");
        }
    }

    /**
     * Check if there are any transactions in the database
     */
    public void checkTransactionData(OnTransactionCheckListener listener) {
        Log.d("FirebaseManager", "Checking transaction data in database...");

        // Check main transactions node
        database.getReference(TRANSACTIONS_NODE)
            .get()
            .addOnSuccessListener(snapshot -> {
                long mainCount = snapshot.getChildrenCount();
                Log.d("FirebaseManager", "Main transactions node has " + mainCount + " transactions");

                // Check user_transactions node
                database.getReference("user_transactions")
                    .get()
                    .addOnSuccessListener(userSnapshot -> {
                        long userCount = userSnapshot.getChildrenCount();
                        Log.d("FirebaseManager", "User transactions node has " + userCount + " users");

                        String result = "Main transactions: " + mainCount + ", User nodes: " + userCount;
                        listener.onCheckComplete(result);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirebaseManager", "Error checking user_transactions: " + e.getMessage());
                        listener.onCheckComplete("Main: " + mainCount + ", User check failed: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                Log.e("FirebaseManager", "Error checking main transactions: " + e.getMessage());
                listener.onCheckComplete("Main check failed: " + e.getMessage());
            });
    }

    public interface OnTransactionCheckListener {
        void onCheckComplete(String result);
    }

    public void getFlaggedUsers(final FlaggedUsersCallback callback) {
        database.getReference(USERS_NODE)
            .orderByChild("isFlagged").equalTo(true)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    List<com.example.tradeup_app.auth.Domain.UserModel> flaggedUsers = new java.util.ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot userSnap : snapshot.getChildren()) {
                        com.example.tradeup_app.auth.Domain.UserModel user = userSnap.getValue(com.example.tradeup_app.auth.Domain.UserModel.class);
                        if (user != null) flaggedUsers.add(user);
                    }
                    callback.onFlaggedUsersLoaded(flaggedUsers);
                }
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    callback.onError(error.getMessage());
                }
            });
    }
}
