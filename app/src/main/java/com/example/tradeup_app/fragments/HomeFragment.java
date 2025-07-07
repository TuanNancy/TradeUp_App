package com.example.tradeup_app.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.tradeup_app.activities.LocationSettingsActivity;
import com.example.tradeup_app.services.LocationService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

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

    // Location services
    private LocationService locationService;
    private SharedPreferences locationPrefs;
    private double userLatitude = 0;
    private double userLongitude = 0;
    private String userAddress = "";
    private Chip locationChip;
    private MaterialCardView locationSettingsCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupRecyclerViews();
        setupClickListeners();
        loadUserProfile();
        loadCategories();
        // Initialize location services
        initLocationServices();
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

        // Location services
        locationChip = view.findViewById(R.id.chip_location);
        locationSettingsCard = view.findViewById(R.id.card_location_settings);

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

        // Location chip click listener for quick location search
        locationChip.setOnClickListener(v -> {
            showLocationSearchDialog();
        });

        // Location settings card
        locationSettingsCard.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), LocationSettingsActivity.class));
            }
        });
    }

    /**
     * Initialize location services and load saved location preferences
     */
    private void initLocationServices() {
        locationService = new LocationService(getContext());
        locationPrefs = getActivity().getSharedPreferences("location_prefs", MODE_PRIVATE);

        // Load saved location data
        loadSavedLocationData();

        // Update location chip display
        updateLocationChipDisplay();
    }

    /**
     * Load saved location data from SharedPreferences
     */
    private void loadSavedLocationData() {
        userLatitude = Double.longBitsToDouble(locationPrefs.getLong("latitude", 0));
        userLongitude = Double.longBitsToDouble(locationPrefs.getLong("longitude", 0));
        userAddress = locationPrefs.getString("address", "");

        // If no saved location, try to get current location
        if (userLatitude == 0 && userLongitude == 0) {
            getCurrentLocationIfPermitted();
        }
    }

    /**
     * Update the location chip display with current location
     */
    private void updateLocationChipDisplay() {
        if (!userAddress.isEmpty()) {
            // Shorten address for display
            String displayAddress = userAddress.length() > 25 ?
                userAddress.substring(0, 25) + "..." : userAddress;
            locationChip.setText("📍 " + displayAddress);
        } else {
            locationChip.setText("📍 Set location");
        }
    }

    /**
     * Get current location if permissions are granted
     */
    private void getCurrentLocationIfPermitted() {
        if (getContext() == null) return;

        locationService.getCurrentLocation(new LocationService.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude, String address) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        userLatitude = latitude;
                        userLongitude = longitude;
                        userAddress = address;
                        updateLocationChipDisplay();

                        // Apply location-based filtering to current products
                        applyLocationBasedFiltering();
                    });
                }
            }

            @Override
            public void onLocationError(String error) {
                // Silently handle error - user can manually set location
            }
        });
    }

    /**
     * Show location search dialog for quick location-based search
     */
    private void showLocationSearchDialog() {
        if (getContext() == null) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_location_search, null);

        com.google.android.material.textfield.TextInputEditText editRadius = dialogView.findViewById(R.id.edit_search_radius);
        com.google.android.material.chip.ChipGroup chipGroupRadius = dialogView.findViewById(R.id.chip_group_radius);
        TextView textCurrentLocation = dialogView.findViewById(R.id.text_current_location);

        // Set current location display
        textCurrentLocation.setText(userAddress.isEmpty() ? "No location set" : userAddress);

        // Set default radius
        int savedRadius = locationPrefs.getInt("search_radius", 25);
        editRadius.setText(String.valueOf(savedRadius));

        // Setup radius chips
        setupRadiusChips(chipGroupRadius, editRadius);

        builder.setView(dialogView)
               .setTitle("Search by Location")
               .setPositiveButton("Search", (dialog, which) -> {
                   String radiusStr = editRadius.getText().toString().trim();
                   if (!radiusStr.isEmpty()) {
                       try {
                           int radius = Integer.parseInt(radiusStr);
                           performLocationBasedSearch(radius);
                       } catch (NumberFormatException e) {
                           Toast.makeText(getContext(), "Invalid radius value", Toast.LENGTH_SHORT).show();
                       }
                   }
               })
               .setNegativeButton("Cancel", null)
               .setNeutralButton("Settings", (dialog, which) -> {
                   startActivity(new Intent(getActivity(), LocationSettingsActivity.class));
               })
               .show();
    }

    /**
     * Setup radius selection chips
     */
    private void setupRadiusChips(com.google.android.material.chip.ChipGroup chipGroup,
                                 com.google.android.material.textfield.TextInputEditText editRadius) {
        int[] radiusOptions = {5, 10, 25, 50, 100};

        for (int radius : radiusOptions) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(getContext());
            chip.setText(radius + " km");
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    editRadius.setText(String.valueOf(radius));
                }
            });
            chipGroup.addView(chip);
        }
    }

    /**
     * Perform location-based search with specified radius
     */
    private void performLocationBasedSearch(int radiusKm) {
        if (userLatitude == 0 && userLongitude == 0) {
            Toast.makeText(getContext(), "Please set your location first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get all products and filter by location
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                if (getActivity() != null) {
                    locationService.filterProductsByLocation(products, userLatitude, userLongitude,
                        radiusKm, new LocationService.ProductLocationCallback() {
                            @Override
                            public void onProductsFiltered(List<Product> filteredProducts) {
                                getActivity().runOnUiThread(() -> {
                                    // Sort by distance
                                    locationService.sortProductsByDistance(filteredProducts,
                                        userLatitude, userLongitude,
                                        new LocationService.ProductLocationCallback() {
                                            @Override
                                            public void onProductsFiltered(List<Product> sortedProducts) {
                                                getActivity().runOnUiThread(() -> {
                                                    // Update both featured and recent with location-filtered results
                                                    featuredAdapter.updateProducts(sortedProducts.size() > 10 ?
                                                        sortedProducts.subList(0, 10) : sortedProducts);
                                                    recentAdapter.updateProducts(sortedProducts);

                                                    Toast.makeText(getContext(),
                                                        "Found " + sortedProducts.size() + " products within " + radiusKm + " km",
                                                        Toast.LENGTH_SHORT).show();
                                                });
                                            }

                                            @Override
                                            public void onError(String error) {
                                                if (getActivity() != null) {
                                                    getActivity().runOnUiThread(() ->
                                                        Toast.makeText(getContext(), "Error sorting products: " + error,
                                                            Toast.LENGTH_SHORT).show());
                                                }
                                            }
                                        });
                                });
                            }

                            @Override
                            public void onError(String error) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Error filtering products: " + error,
                                            Toast.LENGTH_SHORT).show());
                                }
                            }
                        });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "Error loading products: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Apply location-based filtering to current feed based on saved preferences
     */
    private void applyLocationBasedFiltering() {
        if (userLatitude == 0 && userLongitude == 0) return;

        int savedRadius = locationPrefs.getInt("search_radius", 25);
        boolean locationFilterEnabled = locationPrefs.getBoolean("location_filter_enabled", false);

        if (locationFilterEnabled) {
            performLocationBasedSearch(savedRadius);
        }
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        userRef.get().addOnSuccessListener(snapshot -> {
            // Kiểm tra Fragment còn attach và context còn tồn tại
            if (!isAdded() || getActivity() == null || getContext() == null) {
                return;
            }

            if (snapshot.exists()) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user == null) return;

                // Set name
                String name = user.getUsername() != null && !user.getUsername().isEmpty()
                        ? user.getUsername()
                        : user.getUsername() != null ? user.getUsername() : "Người dùng";
                userNameText.setText(name);

                // Set avatar - Kiểm tra lại trạng thái trước khi dùng Glide
                if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
                    if (isAdded() && getContext() != null) {
                        Glide.with(getContext())
                                .load(user.getProfilePic())
                                .placeholder(R.drawable.ic_user_placeholder)
                                .error(R.drawable.ic_user_placeholder)
                                .into(userProfileImage);
                    }
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
        // Initialize location services
        initializeLocationServices();
    }

    // Add public method to refresh data
    public void refreshData() {
        if (isAdded() && getContext() != null) {
            loadFeaturedItems();
            loadRecentItems();
        }
    }

    // ============ LOCATION SERVICES IMPLEMENTATION ============

    private void initializeLocationServices() {
        if (getContext() == null) return;

        locationService = new LocationService(getContext());
        locationPrefs = getContext().getSharedPreferences("location_prefs", MODE_PRIVATE);

        loadLocationSettings();
        setupLocationChip();
    }

    private void loadLocationSettings() {
        // Load saved location settings
        boolean useGPS = locationPrefs.getBoolean("use_gps", true);
        double savedLat = Double.longBitsToDouble(locationPrefs.getLong("latitude", 0));
        double savedLon = Double.longBitsToDouble(locationPrefs.getLong("longitude", 0));
        String savedAddress = locationPrefs.getString("address", "");

        if (savedLat != 0 && savedLon != 0) {
            userLatitude = savedLat;
            userLongitude = savedLon;
            userAddress = savedAddress;
            updateLocationChip(savedAddress);
        } else if (useGPS) {
            // Get current location automatically
            getCurrentLocationIfNeeded();
        }
    }

    private void getCurrentLocationIfNeeded() {
        if (getContext() == null || locationService == null) return;

        // Check if location was updated recently (within 1 hour)
        long lastUpdated = locationPrefs.getLong("last_updated", 0);
        long currentTime = System.currentTimeMillis();
        long oneHour = 60 * 60 * 1000; // 1 hour in milliseconds

        if (currentTime - lastUpdated > oneHour) {
            locationService.getCurrentLocation(new LocationService.LocationCallback() {
                @Override
                public void onLocationReceived(double latitude, double longitude, String address) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            userLatitude = latitude;
                            userLongitude = longitude;
                            userAddress = address;

                            // Save to preferences
                            SharedPreferences.Editor editor = locationPrefs.edit();
                            editor.putLong("latitude", Double.doubleToLongBits(latitude));
                            editor.putLong("longitude", Double.doubleToLongBits(longitude));
                            editor.putString("address", address);
                            editor.putLong("last_updated", System.currentTimeMillis());
                            editor.apply();

                            updateLocationChip(address);

                            // Reload products with location-based sorting
                            loadLocationBasedProducts();
                        });
                    }
                }

                @Override
                public void onLocationError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateLocationChip("Location unavailable");
                        });
                    }
                }
            });
        }
    }

    private void setupLocationChip() {
        if (locationChip != null) {
            locationChip.setOnClickListener(v -> {
                if (getActivity() != null) {
                    startActivity(new Intent(getActivity(), LocationSettingsActivity.class));
                }
            });
        }
    }

    private void updateLocationChip(String address) {
        if (locationChip != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (address != null && !address.isEmpty()) {
                    // Shorten address for display
                    String displayAddress = address.length() > 30 ?
                        address.substring(0, 30) + "..." : address;
                    locationChip.setText("📍 " + displayAddress);
                    locationChip.setVisibility(View.VISIBLE);
                } else {
                    locationChip.setText("📍 Set location");
                    locationChip.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void loadLocationBasedProducts() {
        if (userLatitude == 0 && userLongitude == 0) {
            // No location available, load normally
            loadFeaturedItems();
            loadRecentItems();
            return;
        }

        // Load products with location-based prioritization
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                if (getActivity() != null && locationService != null) {
                    int searchRadius = locationPrefs.getInt("search_radius", 25); // Default 25km

                    // Sort products by distance from user location
                    locationService.sortProductsByDistance(products, userLatitude, userLongitude,
                        new LocationService.ProductLocationCallback() {
                            @Override
                            public void onProductsFiltered(List<Product> sortedProducts) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        // Update featured products (top 10 nearest)
                                        List<Product> featuredProducts = sortedProducts.size() > 10 ?
                                            sortedProducts.subList(0, 10) : sortedProducts;
                                        featuredAdapter.updateProducts(featuredProducts);

                                        // Update recent products with location info
                                        recentAdapter.updateProducts(sortedProducts);
                                    });
                                }
                            }

                            @Override
                            public void onError(String error) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Error sorting by location: " + error,
                                            Toast.LENGTH_SHORT).show();
                                        // Fallback to normal loading
                                        loadFeaturedItems();
                                        loadRecentItems();
                                    });
                                }
                            }
                        });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error loading products: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
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
            // Show different options based on product status for buyers
            if ("Sold".equals(product.getStatus())) {
                // Product is sold - only allow reporting
                options = new String[]{"Report Product"};
            } else {
                // Product is available - allow both make offer and reporting
                options = new String[]{"Make Offer", "Report Product", "View Distance"};
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
        // Kiểm tra trạng thái sản phẩm trước khi xử lý action
        boolean isProductSold = "Sold".equals(product.getStatus());

        if (isProductSold) {
            // Nếu sản phẩm đã sold, chỉ có thể report
            switch (actionIndex) {
                case 0: // Report Product (duy nhất option cho sản phẩm đã sold)
                    showReportDialog(product);
                    break;
                default:
                    showToastOnce("This product has been sold");
                    break;
            }
        } else {
            // Nếu sản phẩm chưa sold, có thể make offer, report, hoặc view distance
            switch (actionIndex) {
                case 0: // Make Offer
                    showMakeOfferDialog(product);
                    break;
                case 1: // Report Product
                    showReportDialog(product);
                    break;
                case 2: // View Distance
                    showProductDistance(product);
                    break;
                default:
                    showToastOnce("Invalid action");
                    break;
            }
        }
    }

    // ============ LOCATION-SPECIFIC METHODS ============

    private void showProductDistance(Product product) {
        if (userLatitude == 0 && userLongitude == 0) {
            Toast.makeText(getContext(), "Your location is not available. Please set your location in settings.",
                Toast.LENGTH_LONG).show();
            return;
        }

        if (product.getLatitude() == 0 && product.getLongitude() == 0) {
            Toast.makeText(getContext(), "Product location is not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        String distanceStr = LocationService.getDistanceString(
            userLatitude, userLongitude,
            product.getLatitude(), product.getLongitude()
        );

        String message = String.format(
            "Distance from your location:\n\n" +
            "�� Your location: %s\n" +
            "📍 Product location: %s\n\n" +
            "📏 Distance: %s",
            userAddress.isEmpty() ? "Unknown" : userAddress,
            product.getLocation().isEmpty() ? "Unknown" : product.getLocation(),
            distanceStr
        );

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📍 Location & Distance")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Open Maps", (dialog, which) -> openInMaps(product))
            .show();
    }

    private void openInMaps(Product product) {
        if (product.getLatitude() == 0 && product.getLongitude() == 0) {
            Toast.makeText(getContext(), "Product location coordinates not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create intent to open location in Google Maps
            String uri = String.format("geo:%f,%f?q=%f,%f(%s)",
                product.getLatitude(), product.getLongitude(),
                product.getLatitude(), product.getLongitude(),
                product.getTitle());
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback to web browser if Maps app not installed
                String webUri = String.format("https://www.google.com/maps/search/?api=1&query=%f,%f",
                    product.getLatitude(), product.getLongitude());
                Intent webIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(webUri));
                startActivity(webIntent);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening maps: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Method to get nearby products within radius
    public void searchNearbyProducts(double radiusKm, LocationService.ProductLocationCallback callback) {
        if (userLatitude == 0 && userLongitude == 0) {
            callback.onError("User location not available");
            return;
        }

        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                if (locationService != null) {
                    locationService.filterProductsByLocation(products, userLatitude, userLongitude,
                        radiusKm, callback);
                } else {
                    callback.onError("Location service not initialized");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError("Error loading products: " + error);
            }
        });
    }

    // Public method to get user's current location for other fragments
    public double[] getUserLocation() {
        return new double[]{userLatitude, userLongitude};
    }

    public String getUserAddress() {
        return userAddress;
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

        // Ensure report has product title for better admin UI
        report.setReportedItemTitle(product.getTitle());

        android.util.Log.d("HomeFragment", "🚨 Submitting report for product: " + product.getTitle() +
            " at timestamp: " + report.getCreatedAt());
        android.util.Log.d("HomeFragment", "📋 Report details - Reason: " + reason + ", Status: " + report.getStatus());

        // Submit report to Firebase using FirebaseManager
        firebaseManager.submitReport(report, task -> {
            if (task.isSuccessful()) {
                android.util.Log.d("HomeFragment", "✅ Report submitted successfully to Firebase!");

                // Force trigger a notification for testing
                android.util.Log.d("HomeFragment", "🔔 Report should now appear in admin dashboard real-time");

                if (getContext() != null) {
                    Toast.makeText(getContext(), "Report submitted successfully. Admin will be notified immediately.", Toast.LENGTH_LONG).show();
                }
            } else {
                android.util.Log.e("HomeFragment", "❌ Failed to submit report: " +
                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to submit report. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

