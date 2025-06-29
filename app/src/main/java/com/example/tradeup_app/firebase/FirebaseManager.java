package com.example.tradeup_app.firebase;

import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Message;
import com.example.tradeup_app.models.Conversation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.List;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;

    // Node names
    public static final String MESSAGES_NODE = "messages";
    public static final String PRODUCTS_NODE = "products";
    public static final String CONVERSATIONS_NODE = "conversations";

    // Callback interfaces
    public interface ProductCallback {
        void onProductsLoaded(List<Product> products);
        void onError(String error);
    }

    public interface ConversationCallback {
        void onConversationsLoaded(List<Conversation> conversations);
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
        product.setCreatedAt(new java.util.Date());
        product.setUpdatedAt(new java.util.Date());
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
                        results.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                        break;
                }
            }

            callback.onProductsLoaded(results);
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private boolean matchesSearchCriteria(Product product, String query, String condition,
                                        double minPrice, double maxPrice) {
        // Match query text
        if (query != null && !query.isEmpty()) {
            String lowerQuery = query.toLowerCase();
            boolean matchesTitle = product.getTitle().toLowerCase().contains(lowerQuery);
            boolean matchesDesc = product.getDescription().toLowerCase().contains(lowerQuery);
            if (!matchesTitle && !matchesDesc) {
                return false;
            }
        }

        // Match condition
        if (condition != null && !condition.isEmpty() && !condition.equals("Tất cả")) {
            if (!product.getCondition().equals(condition)) {
                return false;
            }
        }

        // Match price range
        if (minPrice > 0 && product.getPrice() < minPrice) {
            return false;
        }
        if (maxPrice > 0 && product.getPrice() > maxPrice) {
            return false;
        }

        return true;
    }

    // Message methods
    public void sendMessage(Message message, OnCompleteListener<Void> listener) {
        DatabaseReference messagesRef = database.getReference(MESSAGES_NODE);
        String key = messagesRef.push().getKey();
        if (key != null) {
            message.setId(key);
            messagesRef.child(key).setValue(message).addOnCompleteListener(listener);
        }
    }

    // Conversation methods
    public void getConversations(ConversationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        database.getReference(CONVERSATIONS_NODE)
            .orderByChild("participantsMap/" + userId)
            .equalTo(true)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Conversation> conversations = new java.util.ArrayList<>();
                for (com.google.firebase.database.DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Conversation conversation = dataSnapshot.getValue(Conversation.class);
                    if (conversation != null) {
                        conversation.setId(dataSnapshot.getKey());
                        conversations.add(conversation);
                    }
                }
                callback.onConversationsLoaded(conversations);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void incrementProductViews(String productId) {
        DatabaseReference productRef = database.getReference(PRODUCTS_NODE).child(productId);
        productRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData mutableData) {
                Product product = mutableData.getValue(Product.class);
                if (product != null) {
                    product.setViewCount(product.getViewCount() + 1);
                    mutableData.setValue(product);
                }
                return com.google.firebase.database.Transaction.success(mutableData);
            }

            @Override
            public void onComplete(com.google.firebase.database.DatabaseError databaseError, boolean committed, com.google.firebase.database.DataSnapshot dataSnapshot) {
                // Optional: handle completion
            }
        });
    }
}
