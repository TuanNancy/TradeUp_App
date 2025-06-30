package com.example.tradeup_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ProductAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.activities.ChatActivity;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewFeatured, recyclerViewRecent;
    private TextInputEditText searchBar;
    private ProductAdapter featuredAdapter, recentAdapter;
    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupRecyclerViews();
        loadFeaturedItems();
        loadRecentItems();
        setupSearchBar();

        return view;
    }

    private void initViews(View view) {
        recyclerViewFeatured = view.findViewById(R.id.featured_products_recycler);
        recyclerViewRecent = view.findViewById(R.id.recent_products_recycler);
        searchBar = view.findViewById(R.id.search_bar);
        firebaseManager = FirebaseManager.getInstance();
    }

    private void setupRecyclerViews() {
        // Featured products (horizontal)
        featuredAdapter = new ProductAdapter(getContext(), new ArrayList<>());
        recyclerViewFeatured.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewFeatured.setAdapter(featuredAdapter);

        // Recent products (vertical)
        recentAdapter = new ProductAdapter(getContext(), new ArrayList<>());
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRecent.setAdapter(recentAdapter);

        // Set click listeners
        featuredAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                openProductDetail(product);
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
            public void onReportProduct(Product product) {
                showReportDialog(product);
            }

            @Override
            public void onViewSellerProfile(String sellerId) {
                openSellerProfile(sellerId);
            }
        });

        recentAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                openProductDetail(product);
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
            public void onReportProduct(Product product) {
                showReportDialog(product);
            }

            @Override
            public void onViewSellerProfile(String sellerId) {
                openSellerProfile(sellerId);
            }
        });
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

    private void openProductDetail(Product product) {
        // Navigate to ProductDetailActivity instead of ChatActivity
        com.example.tradeup_app.activities.ProductDetailActivity.startActivity(
            getContext(), product.getId());

        // Increment view count
        firebaseManager.incrementProductViewCount(product.getId());
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
            options = new String[]{"View Offers", "Edit Product", "Mark as Sold", "Delete Product"};
        } else {
            options = new String[]{"Make Offer", "Report Product", "Save Item"};
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
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please login to make an offer", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (currentUserId.equals(product.getSellerId())) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "You cannot make an offer on your own product", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (!product.isNegotiable()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "This product is not open for offers", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (getContext() != null) {
            com.example.tradeup_app.dialogs.MakeOfferDialog dialog = new com.example.tradeup_app.dialogs.MakeOfferDialog(
                getContext(),
                product,
                (offerPrice, message) -> submitOffer(product, offerPrice, message)
            );
            dialog.show();
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

        // TODO: Implement submitOffer method in FirebaseManager
        // For now, show a placeholder message
        if (getContext() != null) {
            Toast.makeText(getContext(), "Offer feature will be implemented soon", Toast.LENGTH_SHORT).show();
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
}
