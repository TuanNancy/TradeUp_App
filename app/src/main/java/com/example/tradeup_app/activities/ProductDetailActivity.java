package com.example.tradeup_app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ImagePagerAdapter;
import com.example.tradeup_app.adapters.ProductAdapter;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.auth.UserProfileViewActivity;
import com.example.tradeup_app.dialogs.MakeOfferDialog;
import com.example.tradeup_app.dialogs.ReportDialog;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Offer;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Report;
import com.example.tradeup_app.utils.Constants;
import com.example.tradeup_app.utils.DataValidator;
import com.example.tradeup_app.utils.NotificationManager;
import com.example.tradeup_app.utils.ReportUtils;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProductDetailActivity extends AppCompatActivity {

    // UI Components
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ViewPager2 imageViewPager;
    private TabLayout imageIndicator;
    private FloatingActionButton fabFavorite;

    // Product Info
    private TextView productTitle, productPrice, productLocation, viewCount, productDescription;
    private Chip statusChip, categoryChip, conditionChip;

    // Seller Info
    private MaterialCardView sellerCard;
    private CircleImageView sellerAvatar;
    private TextView sellerName, sellerRatingText;
    private RatingBar sellerRating;
    private MaterialButton viewProfileButton;

    // Action Buttons
    private MaterialButton contactSellerButton, chatButton, makeOfferButton, shareButton, reportButton;

    // Owner Actions
    private MaterialCardView ownerActionsCard;
    private MaterialButton viewOffersButton, editProductButton, markSoldButton, deleteProductButton;

    // Product Details
    private TextView detailCondition, detailPostedDate, detailNegotiable;

    // Similar Products
    private RecyclerView similarProductsRecycler;
    private ProductAdapter similarProductsAdapter;

    // Data
    private Product currentProduct;
    private FirebaseManager firebaseManager;
    private ImagePagerAdapter imagePagerAdapter;
    private String productId;
    private String currentUserId;
    private boolean isFavorited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        initViews();
        getIntentData();
        setupToolbar();
        setupImagePager();
        setupSimilarProducts();
        setupListeners();
        loadProductData();
    }

    private void initViews() {
        // Initialize FirebaseManager
        firebaseManager = FirebaseManager.getInstance();
        currentUserId = firebaseManager.getCurrentUserId();

        // Toolbar and AppBar
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);

        // Image components
        imageViewPager = findViewById(R.id.image_view_pager);
        imageIndicator = findViewById(R.id.image_indicator);
        fabFavorite = findViewById(R.id.fab_favorite);

        // Product info
        productTitle = findViewById(R.id.product_title);
        productPrice = findViewById(R.id.product_price);
        productLocation = findViewById(R.id.product_location);
        viewCount = findViewById(R.id.view_count);
        productDescription = findViewById(R.id.product_description);
        statusChip = findViewById(R.id.status_chip);
        categoryChip = findViewById(R.id.category_chip);
        conditionChip = findViewById(R.id.condition_chip);

        // Seller info
        sellerCard = findViewById(R.id.seller_card);
        sellerAvatar = findViewById(R.id.seller_avatar);
        sellerName = findViewById(R.id.seller_name);
        sellerRating = findViewById(R.id.seller_rating);
        sellerRatingText = findViewById(R.id.seller_rating_text);
        viewProfileButton = findViewById(R.id.view_profile_button);

        // Action buttons
        contactSellerButton = findViewById(R.id.contact_seller_button);
        chatButton = findViewById(R.id.chat_button);
        makeOfferButton = findViewById(R.id.make_offer_button);
        shareButton = findViewById(R.id.share_button);
        reportButton = findViewById(R.id.report_button);

        // Owner actions
        ownerActionsCard = findViewById(R.id.owner_actions_card);
        viewOffersButton = findViewById(R.id.view_offers_button);
        editProductButton = findViewById(R.id.edit_product_button);
        markSoldButton = findViewById(R.id.mark_sold_button);
        deleteProductButton = findViewById(R.id.delete_product_button);

        // Product details
        detailCondition = findViewById(R.id.detail_condition);
        detailPostedDate = findViewById(R.id.detail_posted_date);
        detailNegotiable = findViewById(R.id.detail_negotiable);

        // Similar products
        similarProductsRecycler = findViewById(R.id.similar_products_recycler);
    }

    private void getIntentData() {
        productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        if (productId == null) {
            Toast.makeText(this, Constants.ERROR_DATA_NOT_FOUND, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupImagePager() {
        imagePagerAdapter = new ImagePagerAdapter(this, new ArrayList<>());
        imageViewPager.setAdapter(imagePagerAdapter);
    }

    private void setupSimilarProducts() {
        similarProductsAdapter = new ProductAdapter(this, new ArrayList<>());
        similarProductsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        similarProductsRecycler.setAdapter(similarProductsAdapter);

        similarProductsAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                // Navigate to another product detail
                Intent intent = new Intent(ProductDetailActivity.this, ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onProductLongClick(Product product) {
                // Not implemented for similar products
            }

            @Override
            public void onMakeOffer(Product product) {
                showMakeOfferDialog(product);
            }

            @Override
            public void onReportProduct(Product product) {
                showReportDialog(product);
            }

            @Override
            public void onViewSellerProfile(String sellerId) {
                openSellerProfile(sellerId);
            }
        });
    }

    private void setupListeners() {
        // Favorite button
        fabFavorite.setOnClickListener(v -> toggleFavorite());

        // Contact seller
        contactSellerButton.setOnClickListener(v -> contactSeller());

        // Chat button - new functionality
        chatButton.setOnClickListener(v -> openChatDirectly());

        // Make offer
        makeOfferButton.setOnClickListener(v -> showMakeOfferDialog(currentProduct));

        // Share product
        shareButton.setOnClickListener(v -> shareProduct());

        // Report product
        reportButton.setOnClickListener(v -> showReportDialog(currentProduct));

        // View seller profile
        viewProfileButton.setOnClickListener(v -> openSellerProfile(currentProduct.getSellerId()));
        sellerCard.setOnClickListener(v -> openSellerProfile(currentProduct.getSellerId()));

        // Owner actions
        viewOffersButton.setOnClickListener(v -> viewOffers());
        editProductButton.setOnClickListener(v -> editProduct());
        markSoldButton.setOnClickListener(v -> markAsSold());
        deleteProductButton.setOnClickListener(v -> deleteProduct());
    }

    private void loadProductData() {
        if (productId == null) return;

        firebaseManager.getDatabase().getReference(Constants.PRODUCTS_NODE)
            .child(productId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        currentProduct = snapshot.getValue(Product.class);
                        if (currentProduct != null) {
                            currentProduct.setId(snapshot.getKey());
                            updateUI();
                            incrementViewCount();
                            loadSimilarProducts();
                            checkFavoriteStatus();
                        }
                    } else {
                        Toast.makeText(ProductDetailActivity.this, Constants.ERROR_DATA_NOT_FOUND, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(ProductDetailActivity.this, Constants.ERROR_NETWORK, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
    }

    private void updateUI() {
        if (currentProduct == null) return;

        // Set toolbar title
        collapsingToolbar.setTitle(currentProduct.getTitle());

        // Product info
        productTitle.setText(currentProduct.getTitle());
        productPrice.setText(formatPrice(currentProduct.getPrice()));
        productLocation.setText(currentProduct.getLocation());
        viewCount.setText(currentProduct.getViewCount() + " views");
        productDescription.setText(currentProduct.getDescription());

        // Status chip
        updateStatusChip();

        // Category and condition
        categoryChip.setText(currentProduct.getCategory());
        conditionChip.setText(currentProduct.getCondition());

        // Setup images
        setupImages();

        // Seller info
        updateSellerInfo();

        // Product details
        detailCondition.setText(currentProduct.getCondition());
        detailPostedDate.setText(formatDate(currentProduct.getCreatedAt()));
        detailNegotiable.setText(currentProduct.isNegotiable() ? "Yes" : "No");

        // Show/hide owner actions
        updateOwnerActions();

        // Update action buttons based on product status and ownership
        updateActionButtons();
    }

    private void updateStatusChip() {
        statusChip.setText(currentProduct.getStatus());

        switch (currentProduct.getStatus()) {
            case Constants.PRODUCT_STATUS_AVAILABLE:
                statusChip.setChipBackgroundColorResource(R.color.success_color);
                break;
            case Constants.PRODUCT_STATUS_SOLD:
                statusChip.setChipBackgroundColorResource(R.color.error_color);
                break;
            case Constants.PRODUCT_STATUS_PAUSED:
                statusChip.setChipBackgroundColorResource(R.color.warning_color);
                break;
            default:
                statusChip.setChipBackgroundColorResource(R.color.secondary_text);
                break;
        }
    }

    private void setupImages() {
        if (currentProduct.getImageUrls() != null && !currentProduct.getImageUrls().isEmpty()) {
            imagePagerAdapter.updateImages(currentProduct.getImageUrls());

            // Setup indicator if more than one image
            if (currentProduct.getImageUrls().size() > 1) {
                new TabLayoutMediator(imageIndicator, imageViewPager, (tab, position) -> {
                    // Tab configuration is handled by the indicator
                }).attach();
            } else {
                imageIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void updateSellerInfo() {
        sellerName.setText(currentProduct.getSellerName());

        // Load seller avatar and rating from Users node
        firebaseManager.getDatabase().getReference(Constants.USERS_NODE)
            .child(currentProduct.getSellerId())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String profilePic = snapshot.child("profilePic").getValue(String.class);
                        if (profilePic != null && !profilePic.isEmpty()) {
                            Glide.with(ProductDetailActivity.this)
                                .load(profilePic)
                                .placeholder(R.drawable.ic_user)
                                .into(sellerAvatar);
                        }

                        // Fixed: Handle both String and Number types for rating
                        Object ratingObj = snapshot.child("rating").getValue();
                        if (ratingObj != null) {
                            try {
                                float rating = 0f;
                                if (ratingObj instanceof String) {
                                    rating = Float.parseFloat((String) ratingObj);
                                } else if (ratingObj instanceof Number) {
                                    rating = ((Number) ratingObj).floatValue();
                                }

                                if (rating > 0) {
                                    sellerRating.setRating(rating);
                                    sellerRatingText.setText(String.format(Locale.getDefault(), "%.1f", rating));
                                } else {
                                    sellerRating.setRating(0);
                                    sellerRatingText.setText("New User");
                                }
                            } catch (NumberFormatException e) {
                                sellerRating.setRating(0);
                                sellerRatingText.setText("New User");
                            }
                        } else {
                            sellerRating.setRating(0);
                            sellerRatingText.setText("New User");
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Handle error silently
                }
            });
    }

    private void updateOwnerActions() {
        boolean isOwner = currentUserId != null && currentUserId.equals(currentProduct.getSellerId());
        ownerActionsCard.setVisibility(isOwner ? View.VISIBLE : View.GONE);
    }

    private void updateActionButtons() {
        boolean isOwner = currentUserId != null && currentUserId.equals(currentProduct.getSellerId());
        boolean isAvailable = Constants.PRODUCT_STATUS_AVAILABLE.equals(currentProduct.getStatus());
        boolean isLoggedIn = currentUserId != null;

        // Contact seller - hidden for owner, disabled if sold
        contactSellerButton.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        contactSellerButton.setEnabled(isAvailable && isLoggedIn);

        // Chat button - new separate functionality
        chatButton.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        chatButton.setEnabled(isLoggedIn);

        // Make offer - hidden for owner, disabled if sold or not negotiable
        makeOfferButton.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        // TEMPORARY FIX: Ignore negotiable check for testing
        makeOfferButton.setEnabled(isAvailable && isLoggedIn);

        // Log for debugging
        android.util.Log.d("ProductDetailActivity",
            "Make Offer Button - Visible: " + (makeOfferButton.getVisibility() == View.VISIBLE) +
            ", Enabled: " + makeOfferButton.isEnabled() +
            ", isOwner: " + isOwner +
            ", isAvailable: " + isAvailable +
            ", isLoggedIn: " + isLoggedIn +
            ", isNegotiable: " + currentProduct.isNegotiable());

        // Report - hidden for owner
        reportButton.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        reportButton.setEnabled(isLoggedIn);
    }

    private void incrementViewCount() {
        if (currentUserId != null && !currentUserId.equals(currentProduct.getSellerId())) {
            firebaseManager.incrementProductViewCount(productId);
        }
    }

    private void loadSimilarProducts() {
        firebaseManager.searchProducts(null, currentProduct.getCategory(), null, 0, 0, null,
            new FirebaseManager.ProductCallback() {
                @Override
                public void onProductsLoaded(List<Product> products) {
                    // Filter out current product and limit to 5
                    List<Product> similarProducts = new ArrayList<>();
                    for (Product product : products) {
                        if (!product.getId().equals(productId) && similarProducts.size() < 5) {
                            similarProducts.add(product);
                        }
                    }
                    similarProductsAdapter.updateProducts(similarProducts);
                }

                @Override
                public void onError(String error) {
                    // Handle error silently
                }
            });
    }

    private void checkFavoriteStatus() {
        if (currentUserId == null) return;

        firebaseManager.getDatabase().getReference("product_likes")
            .child(productId)
            .child(currentUserId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    isFavorited = snapshot.exists();
                    updateFavoriteButton();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Handle error silently
                }
            });
    }

    private void toggleFavorite() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.toggleProductLike(productId, currentUserId, task -> {
            if (task.isSuccessful()) {
                isFavorited = task.getResult();
                updateFavoriteButton();
                String message = isFavorited ? "Added to favorites" : "Removed from favorites";
                Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ProductDetailActivity.this, "Failed to update favorite", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFavoriteButton() {
        int iconRes = isFavorited ? R.drawable.ic_favorite : R.drawable.ic_favorite_border;
        fabFavorite.setImageResource(iconRes);
    }

    private void contactSeller() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to contact seller", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.equals(currentProduct.getSellerId())) {
            Toast.makeText(this, "You cannot contact yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create or get conversation and navigate to chat
        firebaseManager.createOrGetConversation(
            productId,
            currentUserId,
            currentProduct.getSellerId(),
            currentProduct.getTitle(),
            currentProduct.getImageUrls() != null && !currentProduct.getImageUrls().isEmpty() ?
                currentProduct.getImageUrls().get(0) : null,
            task -> {
                if (task.isSuccessful()) {
                    String conversationId = task.getResult();
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("conversationId", conversationId);
                    intent.putExtra("receiverId", currentProduct.getSellerId());
                    intent.putExtra("receiverName", currentProduct.getSellerName());
                    intent.putExtra("productTitle", currentProduct.getTitle());
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Failed to start conversation", Toast.LENGTH_SHORT).show();
                }
            });
    }

    // New method for direct chat access
    private void openChatDirectly() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to start chat", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.equals(currentProduct.getSellerId())) {
            Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get seller name first, then create conversation and open chat
        firebaseManager.getDatabase().getReference(Constants.USERS_NODE)
            .child(currentProduct.getSellerId())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    String sellerName = "User";

                    if (snapshot.exists()) {
                        // Try different possible field names for user name
                        sellerName = snapshot.child("name").getValue(String.class);
                        if (sellerName == null) {
                            sellerName = snapshot.child("fullName").getValue(String.class);
                        }
                        if (sellerName == null) {
                            sellerName = snapshot.child("username").getValue(String.class);
                        }
                        if (sellerName == null) {
                            sellerName = currentProduct.getSellerName();
                        }
                        if (sellerName == null) {
                            sellerName = "User";
                        }
                    }

                    // Create conversation with correct seller name
                    createConversationAndOpenChat(sellerName);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Use fallback name and continue
                    String fallbackName = currentProduct.getSellerName() != null ?
                                        currentProduct.getSellerName() : "User";
                    createConversationAndOpenChat(fallbackName);
                }
            });
    }

    private void createConversationAndOpenChat(String sellerName) {
        firebaseManager.createOrGetConversation(
            productId,
            currentUserId,
            currentProduct.getSellerId(),
            currentProduct.getTitle(),
            currentProduct.getImageUrls() != null && !currentProduct.getImageUrls().isEmpty() ?
                currentProduct.getImageUrls().get(0) : null,
            task -> {
                if (task.isSuccessful()) {
                    String conversationId = task.getResult();
                    Intent intent = new Intent(ProductDetailActivity.this, ChatActivity.class);
                    intent.putExtra("conversationId", conversationId);
                    intent.putExtra("receiverId", currentProduct.getSellerId());
                    intent.putExtra("receiverName", sellerName);
                    intent.putExtra("productTitle", currentProduct.getTitle());
                    startActivity(intent);
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Failed to start conversation", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showMakeOfferDialog(Product product) {
        // Log để debug
        android.util.Log.d("ProductDetailActivity", "showMakeOfferDialog called for product: " +
            (product != null ? product.getTitle() : "null"));

        if (currentUserId == null) {
            Toast.makeText(this, "Please login to make an offer", Toast.LENGTH_SHORT).show();
            android.util.Log.d("ProductDetailActivity", "User not logged in");
            return;
        }

        if (currentUserId.equals(product.getSellerId())) {
            Toast.makeText(this, "You cannot make an offer on your own product", Toast.LENGTH_SHORT).show();
            android.util.Log.d("ProductDetailActivity", "User is the seller");
            return;
        }

        // TEMPORARY: Remove negotiable check for testing
        // if (!product.isNegotiable()) {
        //     Toast.makeText(this, "This product is not open for offers", Toast.LENGTH_SHORT).show();
        //     android.util.Log.d("ProductDetailActivity", "Product not negotiable");
        //     return;
        // }

        android.util.Log.d("ProductDetailActivity", "All checks passed, showing MakeOfferDialog");

        try {
            MakeOfferDialog dialog = new MakeOfferDialog(this, product, (offerPrice, message) -> {
                android.util.Log.d("ProductDetailActivity", "Offer submitted: " + offerPrice + ", " + message);
                submitOffer(product, offerPrice, message);
            });
            dialog.show();
            android.util.Log.d("ProductDetailActivity", "MakeOfferDialog shown successfully");
        } catch (Exception e) {
            android.util.Log.e("ProductDetailActivity", "Error showing MakeOfferDialog", e);
            Toast.makeText(this, "Error opening offer dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void submitOffer(Product product, double offerPrice, String message) {
        String currentUserName = CurrentUser.getUser() != null ?
            CurrentUser.getUser().getUsername() : "Anonymous";

        Offer offer = new Offer(
            product.getId(),
            currentUserId,
            currentUserName,
            product.getSellerId(),
            product.getPrice(),
            offerPrice,
            message
        );

        DataValidator.ValidationResult validation = DataValidator.validateOffer(offer);
        if (!validation.isValid()) {
            Toast.makeText(this, validation.getErrorMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.submitOffer(offer, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, Constants.SUCCESS_OFFER_SENT, Toast.LENGTH_SHORT).show();

                // Send notification to seller about new price offer
                sendPriceOfferNotification(product, offerPrice, currentUserName);
            } else {
                Toast.makeText(this, Constants.ERROR_NETWORK, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Send notification for new price offer
    private void sendPriceOfferNotification(Product product, double offerPrice, String buyerName) {
        try {
            NotificationManager notificationManager = NotificationManager.getInstance(this);
            notificationManager.sendPriceOfferNotification(
                product.getId(),
                product.getTitle(),
                String.valueOf(offerPrice),
                buyerName,
                product.getSellerId()
            );
        } catch (Exception e) {
            android.util.Log.e("ProductDetailActivity", "Failed to send price offer notification", e);
        }
    }

    private void shareProduct() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = String.format("Check out this product: %s\nPrice: %s\n\nShared via TradeUp App",
            currentProduct.getTitle(), formatPrice(currentProduct.getPrice()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentProduct.getTitle());
        startActivity(Intent.createChooser(shareIntent, "Share Product"));
    }

    private void showReportDialog(Product product) {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to report", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the new comprehensive reporting system
        ReportUtils.reportProduct(
            this,
            product.getId(),
            product.getTitle(),
            product.getSellerId(),
            product.getSellerName()
        );
    }

    private void openSellerProfile(String sellerId) {
        if (sellerId != null) {
            UserProfileViewActivity.startActivity(this, sellerId);
        }
    }

    private void viewOffers() {
        Intent intent = new Intent(this, OffersActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, productId);
        intent.putExtra("isSellerView", true);
        startActivity(intent);
    }

    private void editProduct() {
        // TODO: Implement edit product functionality
        Toast.makeText(this, "Edit product feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void markAsSold() {
        new AlertDialog.Builder(this)
            .setTitle("Mark as Sold")
            .setMessage("Are you sure you want to mark this product as sold?")
            .setPositiveButton("Yes", (dialog, which) -> {
                firebaseManager.updateProductStatus(productId, Constants.PRODUCT_STATUS_SOLD, task -> {
                    if (task.isSuccessful()) {
                        currentProduct.setStatus(Constants.PRODUCT_STATUS_SOLD);
                        updateUI();
                        Toast.makeText(this, "Product marked as sold", Toast.LENGTH_SHORT).show();

                        // Send notification about listing update
                        sendListingUpdateNotification("sold");
                    } else {
                        Toast.makeText(this, "Failed to update product status", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // Send notification for listing updates
    private void sendListingUpdateNotification(String updateType) {
        try {
            NotificationManager notificationManager = NotificationManager.getInstance(this);
            notificationManager.sendListingUpdateNotification(
                currentProduct.getId(),
                currentProduct.getTitle(),
                updateType,
                currentUserId
            );
        } catch (Exception e) {
            android.util.Log.e("ProductDetailActivity", "Failed to send listing update notification", e);
        }
    }

    private void deleteProduct() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                firebaseManager.getDatabase().getReference(Constants.PRODUCTS_NODE)
                    .child(productId)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete product", Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // Utility methods
    private String formatPrice(double price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(price) + " VNĐ";
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Static method to start this activity
    public static void startActivity(android.content.Context context, String productId) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, productId);
        context.startActivity(intent);
    }
}
