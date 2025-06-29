package com.example.tradeup_app.firebase;

import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Message;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.models.Offer;
import com.example.tradeup_app.models.Transaction;
import com.example.tradeup_app.models.Rating;
import com.example.tradeup_app.models.Report;
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

    public interface RatingCallback {
        void onRatingsLoaded(List<Rating> ratings);
        void onError(String error);
    }

    public interface ReportCallback {
        void onReportsLoaded(List<Report> reports);
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
            database.getReference(TRANSACTIONS_NODE)
                .child(key)
                .setValue(transaction)
                .addOnCompleteListener(listener);
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
}
