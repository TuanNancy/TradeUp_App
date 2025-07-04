package com.example.tradeup_app.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup_app.R;
import com.example.tradeup_app.utils.NotificationTestHelper;

/**
 * Test Activity for debugging notification functionality
 */
public class NotificationTestActivity extends AppCompatActivity {
    private static final String TAG = "NotificationTest";
    private NotificationTestHelper testHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        testHelper = new NotificationTestHelper(this);

        // Create test buttons
        createTestButtons(layout);

        setContentView(layout);

        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notification Test");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Log.d(TAG, "NotificationTestActivity created");
    }

    private void createTestButtons(LinearLayout layout) {
        // Test Message Notification
        Button btnTestMessage = new Button(this);
        btnTestMessage.setText("Test Message Notification");
        btnTestMessage.setOnClickListener(v -> {
            Log.d(TAG, "Testing message notification");
            testHelper.testMessageNotification();
        });
        layout.addView(btnTestMessage);

        // Test Price Offer Notification
        Button btnTestOffer = new Button(this);
        btnTestOffer.setText("Test Price Offer Notification");
        btnTestOffer.setOnClickListener(v -> {
            Log.d(TAG, "Testing price offer notification");
            testHelper.testPriceOfferNotification();
        });
        layout.addView(btnTestOffer);

        // Test Listing Update Notification
        Button btnTestListing = new Button(this);
        btnTestListing.setText("Test Listing Update Notification");
        btnTestListing.setOnClickListener(v -> {
            Log.d(TAG, "Testing listing update notification");
            testHelper.testListingUpdateNotification();
        });
        layout.addView(btnTestListing);

        // Test Promotional Notification
        Button btnTestPromo = new Button(this);
        btnTestPromo.setText("Test Promotional Notification");
        btnTestPromo.setOnClickListener(v -> {
            Log.d(TAG, "Testing promotional notification");
            testHelper.testPromotionalNotification();
        });
        layout.addView(btnTestPromo);

        // Test All Notifications
        Button btnTestAll = new Button(this);
        btnTestAll.setText("Test All Notifications");
        btnTestAll.setOnClickListener(v -> {
            Log.d(TAG, "Testing all notifications");
            testHelper.testAllNotifications();
        });
        layout.addView(btnTestAll);

        // Debug Firebase Connection
        Button btnDebugFirebase = new Button(this);
        btnDebugFirebase.setText("Debug Firebase Connection");
        btnDebugFirebase.setOnClickListener(v -> {
            Log.d(TAG, "Debugging Firebase connection");
            testHelper.debugFirebaseConnection();
        });
        layout.addView(btnDebugFirebase);

        // Debug Messaging Service
        Button btnDebugMessaging = new Button(this);
        btnDebugMessaging.setText("Debug Messaging Service");
        btnDebugMessaging.setOnClickListener(v -> {
            Log.d(TAG, "Debugging messaging service");
            testHelper.debugMessagingService();
        });
        layout.addView(btnDebugMessaging);

        // Test FCM Token
        Button btnTestFCMToken = new Button(this);
        btnTestFCMToken.setText("Test FCM Token");
        btnTestFCMToken.setOnClickListener(v -> {
            Log.d(TAG, "Testing FCM token");
            testFCMToken();
        });
        layout.addView(btnTestFCMToken);
    }

    private void testFCMToken() {
        try {
            com.example.tradeup_app.services.TokenService tokenService =
                new com.example.tradeup_app.services.TokenService();

            tokenService.getCurrentToken(new com.example.tradeup_app.services.TokenService.TokenCallback() {
                @Override
                public void onTokenReceived(String token) {
                    Log.d(TAG, "FCM Token received: " + token);
                    runOnUiThread(() -> {
                        Toast.makeText(NotificationTestActivity.this,
                            "FCM Token: " + token.substring(0, Math.min(token.length(), 50)) + "...",
                            Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "FCM Token error: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(NotificationTestActivity.this,
                            "FCM Token Error: " + error,
                            Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "FCM Token test failed", e);
            Toast.makeText(this, "FCM Token test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
