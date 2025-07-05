package com.example.tradeup_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.tradeup_app.R;
import com.example.tradeup_app.auth.LoginActivity;
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
        setupBackPressedCallback();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Add this for testing messaging system (remove after testing)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            android.util.Log.d("MainActivity", "User authenticated, ready for messaging");
        }
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();
    }

    private void setupBottomNavigation() {
        // Sửa: Sử dụng OnNavigationItemSelectedListener mới thay vì deprecated
        bottomNavigationView.setOnItemSelectedListener(item -> {
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
                return true;
            }
            return false;
        });
    }

    private void setupBackPressedCallback() {
        // Sửa: Thay thế onBackPressed() deprecated bằng OnBackPressedCallback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getCurrentFragment();

                // If we're not on the home fragment, go back to home
                if (!(currentFragment instanceof HomeFragment)) {
                    loadFragment(new HomeFragment());
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                } else {
                    // If we're on home fragment, exit app
                    finish();
                }
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Sửa: Loại bỏ menu items không tồn tại
        // getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Sửa: Loại bỏ các action không tồn tại
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if user is still logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    // Helper method to programmatically select a tab
    public void selectTab(int tabId) {
        bottomNavigationView.setSelectedItemId(tabId);
    }

    // Helper method to get the currently selected fragment
    public Fragment getCurrentFragment() {
        return fragmentManager.findFragmentById(R.id.fragment_container);
    }
}
