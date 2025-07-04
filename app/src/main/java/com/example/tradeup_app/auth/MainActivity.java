package com.example.tradeup_app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.tradeup_app.R;
import com.example.tradeup_app.fragments.HomeFragment;
import com.example.tradeup_app.fragments.MessagesFragment;
import com.example.tradeup_app.fragments.ProfileFragment;
import com.example.tradeup_app.fragments.SearchFragment;
import com.example.tradeup_app.fragments.SellFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupBottomNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Add this for testing messaging system (remove after testing)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            android.util.Log.d("MainActivity", "User authenticated, ready for messaging");
            // Uncomment below line to create test conversation
            // createTestConversationIfNeeded();

            // TEMPORARY: Add notification test button for easy access
            addTemporaryNotificationTestButton();
        }
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.nav_sell) {
                selectedFragment = new SellFragment();
            } else if (itemId == R.id.nav_messages) {
                selectedFragment = new MessagesFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_blocked_users) {
            Intent intent = new Intent(this, com.example.tradeup_app.activities.BlockedUsersActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_notification_test) {
            Intent intent = new Intent(this, com.example.tradeup_app.activities.NotificationTestActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, AccountSettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Test method to create sample conversation data
    private void createTestConversationIfNeeded() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Create test conversation with current user as buyer
        com.example.tradeup_app.utils.MessagingTestHelper testHelper =
                new com.example.tradeup_app.utils.MessagingTestHelper();

        testHelper.createTestConversation(
                "test_product_001",
                "iPhone 15 Pro Max - Test Product",
                currentUserId,  // current user as buyer
                "test_seller_id"  // dummy seller ID
        );

        android.util.Log.d("MainActivity", "Test conversation creation initiated");
    }

    // TEMPORARY METHOD: For testing notification button (remove after testing)
    private void addTemporaryNotificationTestButton() {
        // Logic to add a temporary button for testing notifications
        // This could be a simple button that, when clicked, triggers a test notification
        android.util.Log.d("MainActivity", "Temporary notification test button added");
    }
}
