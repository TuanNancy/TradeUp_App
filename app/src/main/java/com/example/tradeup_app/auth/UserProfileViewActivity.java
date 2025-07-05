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
import com.example.tradeup_app.utils.VNDPriceFormatter;
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

        // Setup product click listeners
        setupProductClickListeners();
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        messageButton.setOnClickListener(v -> {
            // Navigate to chat activity with the target user
            startChatWithUser();
        });

        reportButton.setOnClickListener(v -> showReportDialog());
    }

    // NEW: Method to start chat with the target user using MessagingService
    private void startChatWithUser() {
        if (targetUser == null || targetUserId == null) {
            Toast.makeText(this, "User information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.equals(targetUserId)) {
            Toast.makeText(this, "Cannot message yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        Toast.makeText(this, "Starting conversation...", Toast.LENGTH_SHORT).show();

        // Use MessagingService with the new logic
        com.example.tradeup_app.services.MessagingService messagingService =
            new com.example.tradeup_app.services.MessagingService(this);

        // Create or get conversation for general chat (no specific product)
        messagingService.createOrGetUserConversation(
            currentUserId,
            targetUserId,
            new com.example.tradeup_app.services.MessagingService.ConversationCallback() {
                @Override
                public void onConversationsLoaded(java.util.List<com.example.tradeup_app.models.Conversation> conversations) {}

                @Override
                public void onConversationCreated(String conversationId) {
                    runOnUiThread(() -> {
                        // Open ChatActivity with conversation details
                        openChatActivity(conversationId, targetUserId, targetUser.getUsername());
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(UserProfileViewActivity.this,
                            "Failed to start conversation: " + error,
                            Toast.LENGTH_LONG).show();
                    });
                }
            });
    }

    private void openChatActivity(String conversationId, String receiverId, String receiverName) {
        Intent intent = new Intent(this, com.example.tradeup_app.activities.ChatActivity.class);
        intent.putExtra("conversationId", conversationId);
        intent.putExtra("receiverId", receiverId);
        intent.putExtra("receiverName", receiverName);
        // No product data for general messaging
        intent.putExtra("productTitle", "");
        intent.putExtra("productId", "");
        startActivity(intent);
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

    private void setupProductClickListeners() {
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                // Navigate to product detail
                com.example.tradeup_app.activities.ProductDetailActivity.startActivity(
                    UserProfileViewActivity.this, product.getId());
            }

            @Override
            public void onProductLongClick(Product product) {
                // Show product options
                showProductOptionsMenu(product);
            }

            @Override
            public void onMakeOffer(Product product) {
                showMakeOfferDialog(product);
            }

            @Override
            public void onBuyProduct(Product product) {
                showBuyProductDialog(product);
            }

            @Override
            public void onReportProduct(Product product) {
                showReportProductDialog(product);
            }

            @Override
            public void onViewSellerProfile(String sellerId) {
                // Already viewing this seller's profile
                Toast.makeText(UserProfileViewActivity.this, "You are already viewing this seller's profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProductOptionsMenu(Product product) {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isOwner = currentUserId != null && currentUserId.equals(product.getSellerId());

        String[] options;
        if (isOwner) {
            options = new String[]{"View Offers", "Edit Product", "Mark as Sold", "Delete Product"};
        } else {
            options = new String[]{"Make Offer", "Buy Product", "Report Product"};
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(product.getTitle())
            .setItems(options, (dialog, which) -> {
                if (isOwner) {
                    handleOwnerAction(product, which);
                } else {
                    handleBuyerAction(product, which);
                }
            })
            .show();
    }

    private void handleOwnerAction(Product product, int actionIndex) {
        switch (actionIndex) {
            case 0: // View Offers
                Toast.makeText(this, "View Offers feature coming soon", Toast.LENGTH_SHORT).show();
                break;
            case 1: // Edit Product
                Toast.makeText(this, "Edit Product feature coming soon", Toast.LENGTH_SHORT).show();
                break;
            case 2: // Mark as Sold
                markProductAsSold(product);
                break;
            case 3: // Delete Product
                deleteProduct(product);
                break;
        }
    }

    private void handleBuyerAction(Product product, int actionIndex) {
        switch (actionIndex) {
            case 0: // Make Offer
                showMakeOfferDialog(product);
                break;
            case 1: // Buy Product
                showBuyProductDialog(product);
                break;
            case 2: // Report Product
                showReportProductDialog(product);
                break;
        }
    }

    private void showMakeOfferDialog(Product product) {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đưa ra lời đề xuất", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.equals(product.getSellerId())) {
            Toast.makeText(this, "Bạn không thể đưa ra lời đề xuất cho sản phẩm của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            com.example.tradeup_app.dialogs.MakeOfferDialog dialog = new com.example.tradeup_app.dialogs.MakeOfferDialog(
                this,
                product,
                (offerPrice, message) -> submitOffer(product, offerPrice, message)
            );
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi mở hộp thoại đề xuất", Toast.LENGTH_SHORT).show();
        }
    }

    private void showBuyProductDialog(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Mua sản phẩm")
            .setMessage("Bạn có muốn mua sản phẩm này không?\n\nTên: " + product.getTitle() + "\nGiá: " + formatPrice(product.getPrice()))
            .setPositiveButton("Mua ngay", (dialog, which) -> {
                handleBuyProduct(product);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void handleBuyProduct(Product product) {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để mua sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is trying to buy their own product
        if (currentUserId.equals(product.getSellerId())) {
            Toast.makeText(this, "Bạn không thể mua sản phẩm của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the product is already sold
        if ("Sold".equals(product.getStatus())) {
            Toast.makeText(this, "Sản phẩm này đã được bán", Toast.LENGTH_SHORT).show();
            return;
        }

        // Proceed with the buying process
        Toast.makeText(this, "Đã mua sản phẩm: " + product.getTitle(), Toast.LENGTH_SHORT).show();

        // TODO: Implement actual buying logic (e.g., payment, order confirmation, etc.)
        // For now, just mark the product as sold
        markProductAsSold(product);
    }

    private void showReportProductDialog(Product product) {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            com.example.tradeup_app.dialogs.ReportDialog dialog = new com.example.tradeup_app.dialogs.ReportDialog(
                this,
                (reason, description) -> submitReport(product, reason, description)
            );
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi mở hộp thoại báo cáo", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitOffer(Product product, double offerPrice, String message) {
        // TODO: Implement offer submission logic
        Toast.makeText(this, "Lời đề xuất đã được gửi!", Toast.LENGTH_SHORT).show();
    }

    private void submitReport(Product product, String reason, String description) {
        // TODO: Implement report submission logic
        Toast.makeText(this, "Báo cáo đã được gửi!", Toast.LENGTH_SHORT).show();
    }

    private void markProductAsSold(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Đánh dấu đã bán")
            .setMessage("Bạn có chắc chắn muốn đánh dấu sản phẩm này đã được bán?")
            .setPositiveButton("Có", (dialog, which) ->
                FirebaseDatabase.getInstance().getReference("Products")
                    .child(product.getId())
                    .child("status")
                    .setValue("Sold")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Sản phẩm đã được đánh dấu là đã bán", Toast.LENGTH_SHORT).show();
                        loadUserListings(); // Refresh listings
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi cập nhật sản phẩm", Toast.LENGTH_SHORT).show();
                    })
            )
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void deleteProduct(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xóa sản phẩm")
            .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa", (dialog, which) ->
                FirebaseDatabase.getInstance().getReference("Products")
                    .child(product.getId())
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Sản phẩm đã được xóa thành công", Toast.LENGTH_SHORT).show();
                        loadUserListings(); // Refresh listings
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi xóa sản phẩm", Toast.LENGTH_SHORT).show();
                    })
            )
            .setNegativeButton("Hủy", null)
            .show();
    }

    private String formatPrice(double price) {
        return VNDPriceFormatter.formatVND(price);
    }

    // Static method to start this activity
    public static void startActivity(Context context, String userId) {
        Intent intent = new Intent(context, UserProfileViewActivity.class);
        intent.putExtra("USER_ID", userId);
        context.startActivity(intent);
    }
}
