package com.example.tradeup_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.auth.AccountSettingsActivity;
import com.example.tradeup_app.auth.LoginActivity;
import com.example.tradeup_app.auth.UserProfileActivity;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileFragment extends Fragment {

    private ImageView profileImageView;
    private TextView usernameTextView, emailTextView, ratingTextView, totalSalesTextView, bioTextView;
    private MaterialButton editProfileButton;
    private LinearLayout myListingsButton, purchaseHistoryButton, savedItemsButton,
            accountSettingsButton, logoutButton;

    private UserModel currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        setupListeners();
        loadUserData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data when returning to fragment
        loadUserData();
    }

    private void initViews(View view) {
        profileImageView = view.findViewById(R.id.profile_image_view);
        usernameTextView = view.findViewById(R.id.username_text_view);
        emailTextView = view.findViewById(R.id.email_text_view);
        ratingTextView = view.findViewById(R.id.rating_text_view);
        totalSalesTextView = view.findViewById(R.id.total_sales_text_view);
        bioTextView = view.findViewById(R.id.bio_text_view);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        myListingsButton = view.findViewById(R.id.my_listings_button);
        purchaseHistoryButton = view.findViewById(R.id.purchase_history_button);
        savedItemsButton = view.findViewById(R.id.saved_items_button);
        accountSettingsButton = view.findViewById(R.id.account_settings_button);
        logoutButton = view.findViewById(R.id.logout_button);
    }

    private void setupListeners() {
        // FR-1.2.2: Users can update profile and profile photo
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UserProfileActivity.class);
            startActivity(intent);
        });

        // My Listings - Show user's active listings
        myListingsButton.setOnClickListener(v -> {
            // TODO: Navigate to user's listings activity
            Toast.makeText(getContext(), "My Listings - Coming soon", Toast.LENGTH_SHORT).show();
        });

        // Purchase History - Show user's purchase history
        purchaseHistoryButton.setOnClickListener(v -> {
            // TODO: Navigate to purchase history activity
            Toast.makeText(getContext(), "Purchase History - Coming soon", Toast.LENGTH_SHORT).show();
        });

        // Saved Items - Show user's saved/favorite items
        savedItemsButton.setOnClickListener(v -> {
            // TODO: Navigate to saved items activity
            Toast.makeText(getContext(), "Saved Items - Coming soon", Toast.LENGTH_SHORT).show();
        });

        // FR-1.2.3: Option to deactivate or permanently delete account
        accountSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AccountSettingsActivity.class);
            startActivity(intent);
        });

        // FR-1.1.5: Logout option must be accessible via profile/settings
        logoutButton.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadUserData() {
        // First check Firebase Auth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            // No Firebase user - redirect to login
            redirectToLogin();
            return;
        }

        // Try to get user from CurrentUser singleton
        currentUser = CurrentUser.getUser();

        if (currentUser != null) {
            updateUI();
        } else {
            // Firebase user exists but CurrentUser is null - load from Firebase
            loadUserFromFirebase(firebaseUser.getUid());
        }
    }

    private void loadUserFromFirebase(String uid) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        if (userModel != null) {
                            CurrentUser.setUser(userModel);
                            currentUser = userModel;
                            updateUI();
                        } else {
                            // User data exists but couldn't parse - redirect to profile setup
                            redirectToProfileSetup();
                        }
                    } else {
                        // User authenticated but no profile data - redirect to profile setup
                        redirectToProfileSetup();
                    }
                })
                .addOnFailureListener(e -> {
                    // Failed to load user data - show error and redirect to login
                    Toast.makeText(getContext(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                });
    }

    private void redirectToProfileSetup() {
        // Redirect to UserProfileActivity to complete profile setup
        Intent intent = new Intent(getContext(), UserProfileActivity.class);
        startActivity(intent);
    }

    private void updateUI() {
        if (currentUser == null) return;

        // Display name (username)
        usernameTextView.setText(currentUser.getUsername());

        // Email
        emailTextView.setText(currentUser.getEmail());

        // Profile picture
        String profilePicUrl = currentUser.getProfilePic();
        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
            Glide.with(this)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_user_placeholder);
        }

        // Rating
        double rating = currentUser.getRating();
        if (rating > 0) {
            ratingTextView.setText(getString(R.string.rating_format, rating));
        } else {
            ratingTextView.setText(R.string.new_user);
        }

        // Bio
        String bio = currentUser.getBio();
        if (bio != null && !bio.trim().isEmpty()) {
            bioTextView.setText(bio);
        } else {
            bioTextView.setText(R.string.no_bio_available);
        }

        // TODO: Load actual transaction count from Firebase
        // For now showing placeholder
        totalSalesTextView.setText("0");
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performLogout() {
        // Clear user session
        FirebaseAuth.getInstance().signOut();
        CurrentUser.setUser(null);

        // Show logout message
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Redirect to login
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
