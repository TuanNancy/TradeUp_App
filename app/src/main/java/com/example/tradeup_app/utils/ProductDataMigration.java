package com.example.tradeup_app.utils;

import android.util.Log;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to update existing products in Firebase to enable offers
 */
public class ProductDataMigration {
    private static final String TAG = "ProductDataMigration";
    private FirebaseManager firebaseManager;

    public ProductDataMigration() {
        this.firebaseManager = FirebaseManager.getInstance();
    }

    /**
     * Update all existing products to enable offers (set isNegotiable = true)
     */
    public void enableOffersForAllProducts() {
        DatabaseReference productsRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.PRODUCTS_NODE);

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Starting migration for " + dataSnapshot.getChildrenCount() + " products");

                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    String productId = productSnapshot.getKey();

                    // Update isNegotiable to true for each product
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("negotiable", true);

                    productsRef.child(productId).updateChildren(updates)
                        .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Successfully updated product: " + productId))
                        .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to update product: " + productId, e));
                }

                Log.d(TAG, "Migration completed!");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Migration failed", databaseError.toException());
            }
        });
    }

    /**
     * Check negotiable status of all products
     */
    public void checkProductNegotiableStatus() {
        DatabaseReference productsRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.PRODUCTS_NODE);

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int totalProducts = (int) dataSnapshot.getChildrenCount();
                int negotiableProducts = 0;

                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    Boolean isNegotiable = productSnapshot.child("negotiable").getValue(Boolean.class);
                    if (Boolean.TRUE.equals(isNegotiable)) {
                        negotiableProducts++;
                    }

                    Log.d(TAG, "Product " + productSnapshot.getKey() +
                        " - negotiable: " + isNegotiable);
                }

                Log.d(TAG, "Summary: " + negotiableProducts + "/" + totalProducts +
                    " products are negotiable");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Check failed", databaseError.toException());
            }
        });
    }
}
