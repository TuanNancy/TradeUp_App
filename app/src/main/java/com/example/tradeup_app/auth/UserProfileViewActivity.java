package com.example.tradeup_app.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ProductAdapter;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.utils.ReportUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UserProfileViewActivity extends AppCompatActivity {

    private ImageView backButton, profileImageView;
    private TextView usernameText, ratingText, transactionCountText, bioText, emailText, contactText;
    private LinearLayout contactLayout;
    private MaterialButton messageButton, reportButton;
    private RecyclerView userListingsRecyclerView;

    private String targetUserId;
    private UserModel targetUser;
    private ProductAdapter productAdapter;
    private List<Product> userProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_view);

        // Get user ID from intent
        targetUserId = getIntent().getStringExtra("USER_ID");
        if (targetUserId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadUserProfile();
        loadUserListings();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        profileImageView = findViewById(R.id.profileImageView);
        usernameText = findViewById(R.id.usernameText);
        ratingText = findViewById(R.id.ratingText);
        transactionCountText = findViewById(R.id.transactionCountText);
        bioText = findViewById(R.id.bioText);
        emailText = findViewById(R.id.emailText);
        contactText = findViewById(R.id.contactText);
        contactLayout = findViewById(R.id.contactLayout);
        messageButton = findViewById(R.id.messageButton);
        reportButton = findViewById(R.id.reportButton);
        userListingsRecyclerView = findViewById(R.id.userListingsRecyclerView);

        // Setup RecyclerView
        userProducts = new ArrayList<>();
        productAdapter = new ProductAdapter(this, userProducts);
        userListingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userListingsRecyclerView.setAdapter(productAdapter);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        messageButton.setOnClickListener(v -> {
            // TODO: Navigate to chat activity
            Toast.makeText(this, R.string.chat_feature_coming_soon, Toast.LENGTH_SHORT).show();
        });

        reportButton.setOnClickListener(v -> showReportDialog());
    }

    private void loadUserProfile() {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(targetUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            targetUser = snapshot.getValue(UserModel.class);
                            if (targetUser != null) {
                                updateUI();
                            }
                        } else {
                            Toast.makeText(UserProfileViewActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserProfileViewActivity.this,
                                     getString(R.string.failed_load_user_data) + error.getMessage(),
                                     Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void updateUI() {
        if (targetUser == null) return;

        // Basic info
        usernameText.setText(targetUser.getUsername());

        // Profile image
        String profileImageUrl = targetUser.getProfilePic();
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(profileImageView);
        }

        // Rating and transactions
        double rating = targetUser.getRating();
        if (rating > 0) {
            ratingText.setText(String.valueOf(rating));
        } else {
            ratingText.setText(R.string.new_user);
        }

        // For now, show placeholder transaction count
        transactionCountText.setText(R.string.view_listings_activity);

        // Bio
        String bio = targetUser.getBio();
        if (bio != null && !bio.trim().isEmpty()) {
            bioText.setText(bio);
        } else {
            bioText.setText(R.string.no_bio_available);
        }

        // Contact info
        emailText.setText(targetUser.getEmail());

        String contact = targetUser.getContact();
        if (contact != null && !contact.trim().isEmpty()) {
            contactText.setText(contact);
            contactLayout.setVisibility(View.VISIBLE);
        } else {
            contactLayout.setVisibility(View.GONE);
        }
    }

    private void loadUserListings() {
        FirebaseDatabase.getInstance().getReference("Products")
                .orderByChild("sellerId")
                .equalTo(targetUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userProducts.clear();
                        for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                            Product product = productSnapshot.getValue(Product.class);
                            if (product != null && "Available".equals(product.getStatus())) {
                                userProducts.add(product);
                            }
                        }
                        productAdapter.notifyItemRangeInserted(0, userProducts.size());

                        // Update transaction count based on listings
                        int totalListings = userProducts.size();
                        transactionCountText.setText(getString(R.string.active_listings_count, totalListings));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserProfileViewActivity.this,
                                     R.string.failed_load_user_listings,
                                     Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showReportDialog() {
        if (targetUser == null) {
            Toast.makeText(this, "User information not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the new comprehensive reporting system
        ReportUtils.reportUser(
            this,
            targetUserId,
            targetUser.getUsername() != null ? targetUser.getUsername() : "Unknown User"
        );
    }

    // Static method to start this activity
    public static void startActivity(Context context, String userId) {
        Intent intent = new Intent(context, UserProfileViewActivity.class);
        intent.putExtra("USER_ID", userId);
        context.startActivity(intent);
    }
}
