package com.example.tradeup_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ProductAdapter;
import com.example.tradeup_app.adapters.CategoryAdapter;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.activities.ChatActivity;
import com.example.tradeup_app.activities.PaymentActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewFeatured, recyclerViewRecent, recyclerViewCategories;
    private TextInputEditText searchBar;
    private ProductAdapter featuredAdapter, recentAdapter;
    private CategoryAdapter categoryAdapter;
    private FirebaseManager firebaseManager;

    // User profile views
    private CircleImageView userProfileImage;
    private TextView greetingText, userNameText;
    private TextView totalProductsCount;

    // Quick action cards
    private MaterialCardView quickSellCard, quickChatCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupRecyclerViews();
        setupClickListeners();
        loadUserProfile();
        loadCategories();
        loadFeaturedItems();
        loadRecentItems();
        setupSearchBar();

        return view;
    }

    private void initViews(View view) {
        // RecyclerViews
        recyclerViewFeatured = view.findViewById(R.id.featured_products_recycler);
        recyclerViewRecent = view.findViewById(R.id.recent_products_recycler);
        recyclerViewCategories = view.findViewById(R.id.categories_recycler);

        // Search bar
        searchBar = view.findViewById(R.id.search_bar);

        // User profile views
        userProfileImage = view.findViewById(R.id.user_profile_image);
        greetingText = view.findViewById(R.id.greeting_text);
        userNameText = view.findViewById(R.id.user_name_text);
        totalProductsCount = view.findViewById(R.id.total_products_count);

        // Quick action cards
        quickSellCard = view.findViewById(R.id.quick_sell_card);
        quickChatCard = view.findViewById(R.id.quick_chat_card);

        firebaseManager = FirebaseManager.getInstance();
    }

    private void setupRecyclerViews() {
        // Categories (horizontal)
        categoryAdapter = new CategoryAdapter(getContext(), new ArrayList<>());
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewCategories.setAdapter(categoryAdapter);

        // Featured products (horizontal)
        featuredAdapter = new ProductAdapter(getContext(), new ArrayList<>());
        recyclerViewFeatured.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewFeatured.setAdapter(featuredAdapter);

        // Recent products (vertical)
        recentAdapter = new ProductAdapter(getContext(), new ArrayList<>());
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRecent.setAdapter(recentAdapter);

        // Set click listeners
        setupProductClickListeners();
        setupCategoryClickListener();
    }

    private void setupClickListeners() {
        // Quick sell card
        quickSellCard.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((androidx.fragment.app.FragmentActivity) getActivity()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new com.example.tradeup_app.fragments.SellFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });

        // Quick chat card
        quickChatCard.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((androidx.fragment.app.FragmentActivity) getActivity()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new com.example.tradeup_app.fragments.MessagesFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user == null) return;

                // Set name
                String name = user.getUsername() != null && !user.getUsername().isEmpty()
                        ? user.getUsername()
                        : user.getUsername() != null ? user.getUsername() : "Người dùng";
                userNameText.setText(name);

                // Set avatar
                if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
                    Glide.with(this)
                            .load(user.getProfilePic())
                            .placeholder(R.drawable.ic_user_placeholder)
                            .error(R.drawable.ic_user_placeholder)
                            .into(userProfileImage);
                } else {
                    userProfileImage.setImageResource(R.drawable.ic_user_placeholder);
                }

                // Set greeting
                int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
                String greeting;
                if (hour < 12) {
                    greeting = "Chào buổi sáng!";
                } else if (hour < 18) {
                    greeting = "Chào buổi chiều!";
                } else {
                    greeting = "Chào buổi tối!";
                }
                greetingText.setText(greeting);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Lỗi khi tải hồ sơ người dùng", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCategories() {
        // Cập nhật tên danh mục để khớp với SearchFragment
        List<CategoryAdapter.CategoryItem> categories = Arrays.asList(
            new CategoryAdapter.CategoryItem("Điện tử", R.drawable.ic_category),
            new CategoryAdapter.CategoryItem("Thời trang", R.drawable.ic_category),
            new CategoryAdapter.CategoryItem("Xe cộ", R.drawable.ic_category),
            new CategoryAdapter.CategoryItem("Nhà cửa", R.drawable.ic_category),
            new CategoryAdapter.CategoryItem("Sách", R.drawable.ic_category),
            new CategoryAdapter.CategoryItem("Thể thao", R.drawable.ic_category),
            new CategoryAdapter.CategoryItem("Khác", R.drawable.ic_category)
        );
        categoryAdapter.updateCategories(categories);
    }

    private void setupCategoryClickListener() {
        categoryAdapter.setOnCategoryClickListener(categoryName -> {
            if (getActivity() != null) {
                // Tạo SearchFragment với Bundle chứa category
                SearchFragment searchFragment = new SearchFragment();
                Bundle bundle = new Bundle();
                bundle.putString("category", categoryName);
                searchFragment.setArguments(bundle);

                // Chuyển sang SearchFragment
                getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, searchFragment)
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    private void setupProductClickListeners() {
        ProductAdapter.OnProductClickListener clickListener = new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                openProductChat(product);
            }

            @Override
            public void onProductLongClick(Product product) {
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
                showReportDialog(product);
            }

            @Override
            public void onViewSellerProfile(String sellerId) {
                openSellerProfile(sellerId);
            }
        };

        featuredAdapter.setOnProductClickListener(clickListener);
        recentAdapter.setOnProductClickListener(clickListener);
    }

    private void loadFeaturedItems() {
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                if (getActivity() != null) {
                    // Get featured products (e.g., top 10 most viewed)
                    List<Product> featuredProducts = products.size() > 10 ?
                        products.subList(0, 10) : products;
                    featuredAdapter.updateProducts(featuredProducts);
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "L��i tải sản phẩm nổi bật: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadRecentItems() {
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                // ✅ FIX: Sort products by creation time - newest first
                products.sort((p1, p2) -> {
                    // Compare by createdAt timestamp in descending order (newest first)
                    long time1 = p1.getCreatedAt(); // Remove null check since getCreatedAt() returns primitive long
                    long time2 = p2.getCreatedAt(); // Remove null check since getCreatedAt() returns primitive long
                    return Long.compare(time2, time1); // Descending order
                });

                recentAdapter.updateProducts(products);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchBar() {
        searchBar.setOnClickListener(v -> {
            // Switch to search fragment
            if (getActivity() != null) {
                ((androidx.fragment.app.FragmentActivity) getActivity()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SearchFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    private void openProductChat(Product product) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để chat", Toast.LENGTH_SHORT).show();
            return;
        }

        // Không thể chat với chính mình
        if (currentUserId.equals(product.getSellerId())) {
            Toast.makeText(getContext(), "Bạn không thể chat với chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo hoặc lấy conversation trước khi mở ChatActivity
        com.example.tradeup_app.services.MessagingService messagingService =
            new com.example.tradeup_app.services.MessagingService();

        String productImageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
            ? product.getImageUrls().get(0) : "";

        messagingService.createOrGetConversation(
            product.getId(),
            currentUserId, // buyerId
            product.getSellerId(), // sellerId
            product.getTitle(),
            productImageUrl,
            new com.example.tradeup_app.services.MessagingService.ConversationCallback() {
                @Override
                public void onConversationCreated(String conversationId) {
                    if (getActivity() != null) {
                        // Mở ChatActivity với đầy đủ thông tin
                        Intent intent = new Intent(getContext(), ChatActivity.class);
                        intent.putExtra("conversationId", conversationId);
                        intent.putExtra("receiverId", product.getSellerId());
                        intent.putExtra("receiverName", product.getSellerName());
                        intent.putExtra("productTitle", product.getTitle());
                        intent.putExtra("productId", product.getId()); // Add missing productId
                        startActivity(intent);

                        // Increment view count
                        firebaseManager.incrementProductViewCount(product.getId());
                    }
                }

                @Override
                public void onConversationsLoaded(List<com.example.tradeup_app.models.Conversation> conversations) {
                    // Not used in this context
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Lỗi tạo cuộc trò chuyện: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment resumes
        refreshData();
    }

    // Add public method to refresh data
    public void refreshData() {
        if (isAdded() && getContext() != null) {
            loadFeaturedItems();
            loadRecentItems();
        }
    }

    // NEW METHODS for handling offers and reports
    private void showProductOptionsMenu(Product product) {
        String currentUserId = firebaseManager.getCurrentUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(product.getSellerId());

        String[] options;
        if (isOwner) {
            // ✅ FIX: Show different options based on product status
            if ("Sold".equals(product.getStatus())) {
                options = new String[]{"View Offers", "Edit Product", "Mark as Available", "Delete Product"};
            } else {
                options = new String[]{"View Offers", "Edit Product", "Mark as Sold", "Delete Product"};
            }
        } else {
            // Only show buyer options if product is available
            if ("Sold".equals(product.getStatus())) {
                options = new String[]{"Product Sold", "Report Product"};
            } else {
                options = new String[]{"Make Offer", "Report Product", "Save Item"};
            }
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(product.getTitle())
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
                Intent offersIntent = new Intent(getContext(), com.example.tradeup_app.activities.OffersActivity.class);
                offersIntent.putExtra("productId", product.getId());
                offersIntent.putExtra("isSellerView", true);
                startActivity(offersIntent);
                break;
            case 1: // Edit Product
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Edit Product feature coming soon", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2: // ✅ FIX: Handle both Mark as Sold and Mark as Available based on current status
                if ("Sold".equals(product.getStatus())) {
                    markProductAsAvailable(product);
                } else {
                    markProductAsSold(product);
                }
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
            case 1: // Report Product
                showReportDialog(product);
                break;
            case 2: // Save Item
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Save Item feature coming soon", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void showMakeOfferDialog(Product product) {
        android.util.Log.d("HomeFragment", "showMakeOfferDialog called for product: " + product.getTitle());

        String currentUserId = firebaseManager.getCurrentUserId();
        android.util.Log.d("HomeFragment", "Current user ID: " + currentUserId);
        android.util.Log.d("HomeFragment", "Product seller ID: " + product.getSellerId());

        if (currentUserId == null) {
            android.util.Log.d("HomeFragment", "User not logged in - showing login message");
            showToastOnce("Please login to make an offer");
            return;
        }

        if (currentUserId.equals(product.getSellerId())) {
            android.util.Log.d("HomeFragment", "User trying to make offer on own product");
            showToastOnce("You cannot make an offer on your own product");
            return;
        }

        if (getContext() != null) {
            try {
                android.util.Log.d("HomeFragment", "Creating and showing MakeOfferDialog");
                com.example.tradeup_app.dialogs.MakeOfferDialog dialog = new com.example.tradeup_app.dialogs.MakeOfferDialog(
                    getContext(),
                    product,
                    (offerPrice, message) -> submitOffer(product, offerPrice, message)
                );
                dialog.show();
                android.util.Log.d("HomeFragment", "MakeOfferDialog shown successfully");
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error showing MakeOfferDialog", e);
                showToastOnce("Error opening offer dialog");
            }
        } else {
            android.util.Log.e("HomeFragment", "Context is null, cannot show dialog");
        }
    }

    // Utility method to prevent toast spam
    private static long lastToastTime = 0;
    private void showToastOnce(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastToastTime > 2000) { // Show toast only if 2 seconds have passed
            lastToastTime = currentTime;
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showReportDialog(Product product) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please login to report", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (getContext() != null) {
            com.example.tradeup_app.dialogs.ReportDialog dialog = new com.example.tradeup_app.dialogs.ReportDialog(
                getContext(),
                (reason, description) -> submitReport(product, reason, description)
            );
            dialog.show();
        }
    }

    private void submitOffer(Product product, double offerPrice, String message) {
        String currentUserId = firebaseManager.getCurrentUserId();
        String currentUserName = com.example.tradeup_app.auth.Helper.CurrentUser.getUser() != null ?
            com.example.tradeup_app.auth.Helper.CurrentUser.getUser().getUsername() : "Anonymous";

        com.example.tradeup_app.models.Offer offer = new com.example.tradeup_app.models.Offer(
            product.getId(),
            currentUserId,
            currentUserName,
            product.getSellerId(),
            product.getPrice(),
            offerPrice,
            message
        );

        // Submit offer to database
        firebaseManager.submitOffer(offer, task -> {
            if (task.isSuccessful()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Offer submitted successfully!", Toast.LENGTH_SHORT).show();

                    // Also create/send chat message for the offer
                    createChatMessageForOffer(product, offerPrice, message);
                }
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to submit offer", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createChatMessageForOffer(Product product, double offerPrice, String message) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            android.util.Log.e("HomeFragment", "Current user ID is null, cannot create chat message");
            return;
        }

        android.util.Log.d("HomeFragment", "Creating chat message for offer: " + offerPrice + " for product: " + product.getTitle());

        // Create conversation first if it doesn't exist
        com.example.tradeup_app.services.MessagingService messagingService =
            new com.example.tradeup_app.services.MessagingService();

        String productImageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
            ? product.getImageUrls().get(0) : "";

        android.util.Log.d("HomeFragment", "Creating conversation between buyer: " + currentUserId + " and seller: " + product.getSellerId());

        messagingService.createOrGetConversation(
            product.getId(),
            currentUserId, // buyerId
            product.getSellerId(), // sellerId
            product.getTitle(),
            productImageUrl,
            new com.example.tradeup_app.services.MessagingService.ConversationCallback() {
                @Override
                public void onConversationCreated(String conversationId) {
                    android.util.Log.d("HomeFragment", "Conversation created/found: " + conversationId);
                    // Send offer message to chat
                    sendOfferToChatConversation(conversationId, product, offerPrice, message);
                }

                @Override
                public void onConversationsLoaded(List<com.example.tradeup_app.models.Conversation> conversations) {
                    // Not used in this context
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("HomeFragment", "Failed to create conversation for offer: " + error);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to create chat conversation: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            }
        );
    }

    private void sendOfferToChatConversation(String conversationId, Product product, double offerPrice, String message) {
        android.util.Log.d("HomeFragment", "Sending offer to chat conversation: " + conversationId);
        android.util.Log.d("HomeFragment", "Offer details - Price: " + offerPrice + ", Message: " + message);

        try {
            // Use ChatOfferService to send offer in chat
            com.example.tradeup_app.services.ChatOfferService chatOfferService =
                new com.example.tradeup_app.services.ChatOfferService();

            String currentUserId = firebaseManager.getCurrentUserId();
            String currentUserName = com.example.tradeup_app.auth.Helper.CurrentUser.getUser() != null ?
                com.example.tradeup_app.auth.Helper.CurrentUser.getUser().getUsername() : "Anonymous";

            android.util.Log.d("HomeFragment", "Sending offer with ChatOfferService - User: " + currentUserName);

            chatOfferService.sendOfferInChat(
                conversationId,
                product.getId(),
                product.getTitle(),
                currentUserId,
                currentUserName,
                product.getSellerId(),
                product.getPrice(),
                offerPrice,
                message,
                new com.example.tradeup_app.services.ChatOfferService.ChatOfferCallback() {
                    @Override
                    public void onOfferSent(com.example.tradeup_app.models.ChatOffer chatOffer) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                android.util.Log.d("HomeFragment", "✅ Offer sent to chat successfully! OfferId: " + chatOffer.getId());

                                // Show success message
                                Toast.makeText(getContext(), "💰 Offer sent to chat successfully!", Toast.LENGTH_SHORT).show();

                                // Optionally open the chat to show the sent offer
                                openChatWithOffer(conversationId, product);
                            });
                        }
                    }

                    @Override
                    public void onOfferResponded(com.example.tradeup_app.models.ChatOffer chatOffer, String response) {
                        // Not used for sending offers
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                android.util.Log.e("HomeFragment", "❌ Failed to send offer to chat: " + error);
                                Toast.makeText(getContext(), "Failed to send offer to chat: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                }
            );
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Exception in sendOfferToChatConversation", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error sending offer to chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // Helper method to open chat and show the offer that was just sent
    private void openChatWithOffer(String conversationId, Product product) {
        android.util.Log.d("HomeFragment", "Opening chat with offer - ConversationId: " + conversationId);

        try {
            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
            chatIntent.putExtra("conversationId", conversationId);
            chatIntent.putExtra("receiverId", product.getSellerId());
            chatIntent.putExtra("receiverName", product.getSellerName());
            chatIntent.putExtra("productTitle", product.getTitle());
            chatIntent.putExtra("productId", product.getId());

            // Add a flag to indicate this is from an offer
            chatIntent.putExtra("fromOffer", true);

            startActivity(chatIntent);
            android.util.Log.d("HomeFragment", "Chat activity started successfully");

        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error opening chat activity", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error opening chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitReport(Product product, String reason, String description) {
        String currentUserId = firebaseManager.getCurrentUserId();
        String currentUserName = com.example.tradeup_app.auth.Helper.CurrentUser.getUser() != null ?
            com.example.tradeup_app.auth.Helper.CurrentUser.getUser().getUsername() : "Anonymous";

        com.example.tradeup_app.models.Report report = new com.example.tradeup_app.models.Report(
            currentUserId,
            currentUserName,
            product.getSellerId(),
            product.getSellerName(),
            product.getId(),
            "PRODUCT",
            reason,
            description
        );

        // TODO: Implement submitReport method in FirebaseManager
        // For now, show a placeholder message
        if (getContext() != null) {
            Toast.makeText(getContext(), "Report feature will be implemented soon", Toast.LENGTH_SHORT).show();
        }
    }

    private void markProductAsSold(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Mark as Sold")
            .setMessage("Are you sure you want to mark this product as sold?")
            .setPositiveButton("Yes", (dialog, which) ->
                firebaseManager.getDatabase().getReference(FirebaseManager.PRODUCTS_NODE)
                    .child(product.getId())
                    .child("status")
                    .setValue("Sold")
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Product marked as sold", Toast.LENGTH_SHORT).show();
                        }
                        refreshData();
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to update product", Toast.LENGTH_SHORT).show();
                        }
                    })
            )
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void markProductAsAvailable(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Mark as Available")
            .setMessage("Are you sure you want to mark this product as available?")
            .setPositiveButton("Yes", (dialog, which) ->
                firebaseManager.getDatabase().getReference(FirebaseManager.PRODUCTS_NODE)
                    .child(product.getId())
                    .child("status")
                    .setValue("Available")
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Product marked as available", Toast.LENGTH_SHORT).show();
                        }
                        refreshData();
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to update product", Toast.LENGTH_SHORT).show();
                        }
                    })
            )
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteProduct(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) ->
                firebaseManager.getDatabase().getReference(FirebaseManager.PRODUCTS_NODE)
                    .child(product.getId())
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Product deleted successfully", Toast.LENGTH_SHORT).show();
                        }
                        refreshData();
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to delete product", Toast.LENGTH_SHORT).show();
                        }
                    })
            )
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openSellerProfile(String sellerId) {
        if (sellerId != null && getContext() != null) {
            // Use the static method from UserProfileViewActivity to start the activity
            com.example.tradeup_app.auth.UserProfileViewActivity.startActivity(getContext(), sellerId);
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Cannot open seller profile", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showBuyProductDialog(Product product) {
        // Implement the dialog to handle product buying
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Buy Product")
            .setMessage("Do you want to buy this product?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // Handle the buy action
                handleBuyProduct(product);
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void handleBuyProduct(Product product) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để mua sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.equals(product.getSellerId())) {
            Toast.makeText(getContext(), "Bạn không thể mua sản phẩm của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!"Available".equals(product.getStatus())) {
            Toast.makeText(getContext(), "Sản phẩm này không còn khả dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user has pending offers for this product
        firebaseManager.checkPendingOffers(product.getId(), currentUserId, task -> {
            if (task.isSuccessful() && task.getResult()) {
                // User has pending offers, ask if they want to proceed
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Bạn có đề xuất giá đang chờ")
                    .setMessage("Bạn có đề xuất giá đang chờ xử lý cho sản phẩm này. Bạn có muốn tiếp tục mua với giá gốc không?")
                    .setPositiveButton("Tiếp tục mua", (dialog, which) -> proceedToPurchase(product))
                    .setNegativeButton("Hủy", null)
                    .show();
            } else {
                // No pending offers, proceed directly
                proceedToPurchase(product);
            }
        });
    }

    private void proceedToPurchase(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận mua hàng")
            .setMessage("Bạn có muốn mua sản phẩm này không?\n\n" +
                       "Tên: " + product.getTitle() + "\n" +
                       "Giá: " + com.example.tradeup_app.utils.VNDPriceFormatter.formatVND(product.getPrice()) + "\n\n" +
                       "Bạn sẽ được chuyển đến trang thanh toán.")
            .setPositiveButton("Mua ngay", (dialog, which) -> {
                openPaymentActivity(product);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void openPaymentActivity(Product product) {
        Intent intent = new Intent(getContext(), PaymentActivity.class);
        intent.putExtra("product", product);
        startActivity(intent);
    }
}
