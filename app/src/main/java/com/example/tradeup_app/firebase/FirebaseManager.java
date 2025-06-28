package com.example.tradeup_app.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.models.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {

    private static FirebaseManager instance;
    private FirebaseDatabase database;
    private FirebaseAuth auth;

    // Collection names for Realtime Database
    public static final String PRODUCTS_NODE = "products";
    public static final String USERS_NODE = "users";
    public static final String CONVERSATIONS_NODE = "conversations";
    public static final String MESSAGES_NODE = "messages";

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

    // Product Operations
    public interface ProductCallback {
        void onSuccess(List<Product> products);
        void onFailure(String error);
    }

    public interface SingleProductCallback {
        void onSuccess(Product product);
        void onFailure(String error);
    }

    public void addProduct(Product product, OnCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) listener.onComplete(null);
            return;
        }

        product.setSellerId(userId);

        // Generate unique key for product
        DatabaseReference productsRef = database.getReference(PRODUCTS_NODE);
        String productKey = productsRef.push().getKey();

        if (productKey != null) {
            product.setId(productKey);

            productsRef.child(productKey).setValue(product)
                    .addOnCompleteListener(task -> {
                        if (listener != null) listener.onComplete(task);
                    });
        } else {
            if (listener != null) listener.onComplete(null);
        }
    }

    public void getProducts(ProductCallback callback) {
        DatabaseReference productsRef = database.getReference(PRODUCTS_NODE);

        productsRef.orderByChild("status").equalTo("Available")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Product> products = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Product product = snapshot.getValue(Product.class);
                            if (product != null) {
                                product.setId(snapshot.getKey());
                                products.add(product);
                            }
                        }
                        callback.onSuccess(products);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onFailure(databaseError.getMessage());
                    }
                });
    }

    public void getProductsByCategory(String category, ProductCallback callback) {
        DatabaseReference productsRef = database.getReference(PRODUCTS_NODE);

        Query query = productsRef.orderByChild("category").equalTo(category);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Product> products = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);
                    if (product != null && "Available".equals(product.getStatus())) {
                        product.setId(snapshot.getKey());
                        products.add(product);
                    }
                }
                callback.onSuccess(products);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onFailure(databaseError.getMessage());
            }
        });
    }

    public void searchProducts(String query, String category, String condition,
                              double minPrice, double maxPrice, String sortBy,
                              ProductCallback callback) {
        DatabaseReference productsRef = database.getReference(PRODUCTS_NODE);

        productsRef.orderByChild("status").equalTo("Available")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Product> products = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Product product = snapshot.getValue(Product.class);
                            if (product != null) {
                                product.setId(snapshot.getKey());

                                // Apply filters
                                boolean matchesFilters = true;

                                // Category filter
                                if (!category.equals("Tất cả") && !category.equals(product.getCategory())) {
                                    matchesFilters = false;
                                }

                                // Condition filter
                                if (!condition.equals("Tất cả") && !condition.equals(product.getCondition())) {
                                    matchesFilters = false;
                                }

                                // Price filter
                                if (minPrice > 0 && product.getPrice() < minPrice) {
                                    matchesFilters = false;
                                }
                                if (maxPrice > 0 && product.getPrice() > maxPrice) {
                                    matchesFilters = false;
                                }

                                // Text search filter
                                if (!query.trim().isEmpty()) {
                                    String searchText = query.toLowerCase();
                                    if (!product.getTitle().toLowerCase().contains(searchText) &&
                                        !product.getDescription().toLowerCase().contains(searchText)) {
                                        matchesFilters = false;
                                    }
                                }

                                if (matchesFilters) {
                                    products.add(product);
                                }
                            }
                        }

                        callback.onSuccess(products);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onFailure(databaseError.getMessage());
                    }
                });
    }

    public void getUserProducts(String userId, ProductCallback callback) {
        DatabaseReference productsRef = database.getReference(PRODUCTS_NODE);

        productsRef.orderByChild("sellerId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Product> products = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Product product = snapshot.getValue(Product.class);
                            if (product != null) {
                                product.setId(snapshot.getKey());
                                products.add(product);
                            }
                        }
                        callback.onSuccess(products);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onFailure(databaseError.getMessage());
                    }
                });
    }

    public void updateProductStatus(String productId, String status, OnCompleteListener<Void> listener) {
        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);
        productRef.child("status").setValue(status)
                .addOnCompleteListener(listener);
    }

    public void incrementProductViews(String productId) {
        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);
        productRef.child("viewCount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long currentViews = dataSnapshot.getValue(Long.class);
                int newViews = (currentViews != null ? currentViews.intValue() : 0) + 1;
                productRef.child("viewCount").setValue(newViews);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Ignore error for view count increment
            }
        });
    }

    // Message Operations
    public interface MessageCallback {
        void onSuccess(List<Message> messages);
        void onFailure(String error);
    }

    public interface ConversationCallback {
        void onSuccess(List<Conversation> conversations);
        void onFailure(String error);
    }

    public void sendMessage(Message message, OnCompleteListener<Void> listener) {
        DatabaseReference messagesRef = database.getReference(MESSAGES_NODE);
        String messageKey = messagesRef.push().getKey();

        if (messageKey != null) {
            message.setId(messageKey);
            messagesRef.child(messageKey).setValue(message)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            updateConversationLastMessage(message);
                        }
                        if (listener != null) listener.onComplete(task);
                    });
        } else {
            if (listener != null) listener.onComplete(null);
        }
    }

    private void updateConversationLastMessage(Message message) {
        DatabaseReference conversationRef = database.getReference(CONVERSATIONS_NODE).child(message.getConversationId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message.getContent());
        updates.put("lastMessageTime", message.getTimestamp());

        conversationRef.updateChildren(updates);
    }

    public void getConversations(ConversationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        DatabaseReference conversationsRef = database.getReference(CONVERSATIONS_NODE);
        conversationsRef.orderByChild("buyerId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Conversation> conversations = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Conversation conversation = snapshot.getValue(Conversation.class);
                            if (conversation != null) {
                                conversation.setId(snapshot.getKey());
                                conversations.add(conversation);
                            }
                        }
                        callback.onSuccess(conversations);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onFailure(databaseError.getMessage());
                    }
                });
    }

    // Utility methods
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public FirebaseDatabase getDatabase() {
        return database;
    }
}
