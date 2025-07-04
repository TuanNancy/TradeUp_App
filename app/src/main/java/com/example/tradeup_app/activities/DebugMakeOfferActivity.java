package com.example.tradeup_app.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup_app.dialogs.MakeOfferDialog;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;

/**
 * Simple activity to test Make Offer functionality directly
 */
public class DebugMakeOfferActivity extends AppCompatActivity {
    private static final String TAG = "DebugMakeOffer";
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseManager = FirebaseManager.getInstance();

        // Create simple UI
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        // Test MakeOfferDialog directly
        Button btnTestDialog = new Button(this);
        btnTestDialog.setText("Test MakeOfferDialog Directly");
        btnTestDialog.setOnClickListener(v -> testMakeOfferDialog());
        layout.addView(btnTestDialog);

        // Test with dummy product
        Button btnTestWithProduct = new Button(this);
        btnTestWithProduct.setText("Test with Dummy Product");
        btnTestWithProduct.setOnClickListener(v -> testWithDummyProduct());
        layout.addView(btnTestWithProduct);

        // Check current user
        Button btnCheckUser = new Button(this);
        btnCheckUser.setText("Check Current User");
        btnCheckUser.setOnClickListener(v -> checkCurrentUser());
        layout.addView(btnCheckUser);

        setContentView(layout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Debug Make Offer");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void testMakeOfferDialog() {
        Log.d(TAG, "Testing MakeOfferDialog directly");

        try {
            // Create a simple test product
            Product testProduct = new Product();
            testProduct.setId("test_product_id");
            testProduct.setTitle("Test Product");
            testProduct.setPrice(100000);
            testProduct.setSellerId("different_seller_id");
            testProduct.setSellerName("Test Seller");
            testProduct.setNegotiable(true);

            Log.d(TAG, "Created test product: " + testProduct.getTitle());

            MakeOfferDialog dialog = new MakeOfferDialog(this, testProduct, (offerPrice, message) -> {
                Log.d(TAG, "Offer callback called: price=" + offerPrice + ", message=" + message);
                Toast.makeText(this, "Offer would be: " + offerPrice + " - " + message, Toast.LENGTH_LONG).show();
            });

            dialog.show();
            Log.d(TAG, "Dialog shown successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in testMakeOfferDialog", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void testWithDummyProduct() {
        Log.d(TAG, "Testing with dummy product and full validation");

        String currentUserId = firebaseManager.getCurrentUserId();
        Log.d(TAG, "Current user ID: " + currentUserId);

        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            return;
        }

        // Create dummy product
        Product dummyProduct = new Product();
        dummyProduct.setId("dummy_123");
        dummyProduct.setTitle("Dummy Test Product");
        dummyProduct.setPrice(50000);
        dummyProduct.setSellerId("dummy_seller_id"); // Different from current user
        dummyProduct.setSellerName("Dummy Seller");
        dummyProduct.setNegotiable(true);
        dummyProduct.setStatus("Available");

        // Apply same logic as ProductDetailActivity
        if (currentUserId.equals(dummyProduct.getSellerId())) {
            Toast.makeText(this, "Cannot make offer on own product", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "All validations passed, showing dialog");

        try {
            MakeOfferDialog dialog = new MakeOfferDialog(this, dummyProduct, (offerPrice, message) -> {
                Log.d(TAG, "Dummy offer submitted: " + offerPrice + " for " + dummyProduct.getTitle());
                Toast.makeText(this, "SUCCESS! Offer: " + offerPrice + " VNĐ", Toast.LENGTH_LONG).show();
            });
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog", e);
            Toast.makeText(this, "Dialog error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkCurrentUser() {
        String currentUserId = firebaseManager.getCurrentUserId();
        Log.d(TAG, "Current User ID: " + currentUserId);

        if (currentUserId != null) {
            Toast.makeText(this, "Logged in as: " + currentUserId, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "NOT LOGGED IN!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
