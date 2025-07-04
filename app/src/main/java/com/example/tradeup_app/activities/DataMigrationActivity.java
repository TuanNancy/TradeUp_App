package com.example.tradeup_app.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup_app.R;
import com.example.tradeup_app.utils.ProductDataMigration;

/**
 * Activity to run data migration for fixing Make Offer functionality
 */
public class DataMigrationActivity extends AppCompatActivity {
    private static final String TAG = "DataMigrationActivity";
    private ProductDataMigration migration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        migration = new ProductDataMigration();

        // Create migration buttons
        createMigrationButtons(layout);
        setContentView(layout);

        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Data Migration");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void createMigrationButtons(LinearLayout layout) {
        // Enable offers for all products
        Button btnEnableOffers = new Button(this);
        btnEnableOffers.setText("Enable Offers for All Products");
        btnEnableOffers.setOnClickListener(v -> {
            Log.d(TAG, "Starting migration to enable offers");
            Toast.makeText(this, "Starting migration...", Toast.LENGTH_SHORT).show();
            migration.enableOffersForAllProducts();
        });
        layout.addView(btnEnableOffers);

        // Check negotiable status
        Button btnCheckStatus = new Button(this);
        btnCheckStatus.setText("Check Products Negotiable Status");
        btnCheckStatus.setOnClickListener(v -> {
            Log.d(TAG, "Checking products negotiable status");
            migration.checkProductNegotiableStatus();
        });
        layout.addView(btnCheckStatus);
    }
}
