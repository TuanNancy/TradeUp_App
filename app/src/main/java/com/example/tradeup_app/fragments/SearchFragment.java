package com.example.tradeup_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ProductAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.activities.ChatActivity;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText searchEditText, minPriceEditText, maxPriceEditText;
    private Spinner categorySpinner, conditionSpinner, sortSpinner;
    private RecyclerView searchResultsRecyclerView;
    private View progressBar;

    private ProductAdapter productAdapter;
    private FirebaseManager firebaseManager;
    private final List<Product> productList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initViews(view);
        setupSpinners();
        setupSearchListener();
        setupRecyclerView();
        setupFilters();
        performInitialSearch();

        return view;
    }

    private void initViews(View view) {
        searchEditText = view.findViewById(R.id.search_edit_text);
        minPriceEditText = view.findViewById(R.id.min_price_edit_text);
        maxPriceEditText = view.findViewById(R.id.max_price_edit_text);
        categorySpinner = view.findViewById(R.id.category_spinner);
        conditionSpinner = view.findViewById(R.id.condition_spinner);
        sortSpinner = view.findViewById(R.id.sort_spinner);
        searchResultsRecyclerView = view.findViewById(R.id.search_results_recycler);
        progressBar = view.findViewById(R.id.progress_bar);

        firebaseManager = FirebaseManager.getInstance();
    }

    private void setupSpinners() {
        if (getContext() == null) return;

        // Category spinner
        String[] categories = {"Tất cả", "Điện tử", "Thời trang", "Xe cộ", "Nhà cửa", "Sách", "Thể thao", "Khác"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Condition spinner
        String[] conditions = {"Tất cả", "Mới", "Như mới", "Tốt", "Khá tốt", "Cũ"};
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, conditions);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        conditionSpinner.setAdapter(conditionAdapter);

        // Sort spinner
        String[] sortOptions = {"Liên quan nhất", "Mới nhất", "Giá thấp đến cao", "Giá cao đến thấp"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Trigger search after 200ms delay
                searchEditText.removeCallbacks(searchRunnable);
                searchEditText.postDelayed(searchRunnable, 200);
            }
        });
    }

    private final Runnable searchRunnable = this::performSearch;

    private void setupRecyclerView() {
        if (getContext() == null) return;

        productAdapter = new ProductAdapter(getContext(), productList);
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        searchResultsRecyclerView.setAdapter(productAdapter);

        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
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
            public void onReportProduct(Product product) {
                showReportDialog(product);
            }

            @Override
            public void onViewSellerProfile(String sellerId) {
                openSellerProfile(sellerId);
            }
        });
    }

    private void setupFilters() {
        // Setup spinner listeners
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        categorySpinner.setOnItemSelectedListener(spinnerListener);
        conditionSpinner.setOnItemSelectedListener(spinnerListener);
        sortSpinner.setOnItemSelectedListener(spinnerListener);

        // Setup price range input listeners
        TextWatcher priceWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Delay search to avoid too many calls while typing
                searchEditText.removeCallbacks(searchRunnable);
                searchEditText.postDelayed(searchRunnable, 500);
            }
        };

        minPriceEditText.addTextChangedListener(priceWatcher);
        maxPriceEditText.addTextChangedListener(priceWatcher);
    }

    private void performInitialSearch() {
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                if (getActivity() != null) {
                    productList.clear();
                    productList.addAll(products);
                    productAdapter.notifyItemRangeInserted(0, products.size());
                    updateEmptyState();
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "Lỗi tải sản phẩm: " + error, Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                }
            }
        });
    }

    private void performSearch() {
        String query = searchEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItemPosition() == 0 ? "" :
                        categorySpinner.getSelectedItem().toString();
        String condition = conditionSpinner.getSelectedItemPosition() == 0 ? "" :
                         conditionSpinner.getSelectedItem().toString();
        double minPrice = 0, maxPrice = 0;

        try {
            if (!minPriceEditText.getText().toString().isEmpty()) {
                minPrice = Double.parseDouble(minPriceEditText.getText().toString());
            }
            if (!maxPriceEditText.getText().toString().isEmpty()) {
                maxPrice = Double.parseDouble(maxPriceEditText.getText().toString());
            }
        } catch (NumberFormatException e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String sortBy = getSortByValue();
        showProgressBar();

        firebaseManager.searchProducts(query, category, condition, minPrice, maxPrice, sortBy,
            new FirebaseManager.ProductCallback() {
                @Override
                public void onProductsLoaded(List<Product> products) {
                    hideProgressBar();
                    int oldSize = productList.size();
                    productList.clear();
                    productAdapter.notifyItemRangeRemoved(0, oldSize);
                    productList.addAll(products);
                    productAdapter.notifyItemRangeInserted(0, products.size());
                    updateEmptyState();
                }

                @Override
                public void onError(String error) {
                    hideProgressBar();
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi tìm kiếm: " + error, Toast.LENGTH_SHORT).show();
                    }
                    updateEmptyState();
                }
            });
    }

    private String getSortByValue() {
        if (sortSpinner.getSelectedItemPosition() == 0) return null;

        String selected = sortSpinner.getSelectedItem().toString();
        switch (selected) {
            case "Giá thấp đến cao":
                return "price_asc";
            case "Giá cao đến thấp":
                return "price_desc";
            case "Mới nhất":
                return "date";
            default:
                return null;
        }
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            searchResultsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        View rootView = getView();
        if (rootView != null) {
            View emptyState = rootView.findViewById(R.id.empty_state);
            if (emptyState != null) {
                emptyState.setVisibility(productList.isEmpty() ? View.VISIBLE : View.GONE);
                searchResultsRecyclerView.setVisibility(productList.isEmpty() ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void openProductChat(Product product) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("productId", product.getId());
        intent.putExtra("sellerId", product.getSellerId());
        startActivity(intent);

        // Increment view count
        firebaseManager.incrementProductViews(product.getId());
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
                offersIntent.putExtra("product", product);
                offersIntent.putExtra("isSellerView", true);
                startActivity(offersIntent);
                break;
            case 1: // Edit Product
                Toast.makeText(getContext(), "Edit Product feature coming soon", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "Save Item feature coming soon", Toast.LENGTH_SHORT).show();
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

        firebaseManager.submitOffer(offer, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Offer submitted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to submit offer", Toast.LENGTH_SHORT).show();
            }
        });
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

        firebaseManager.submitReport(report, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Report submitted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to submit report", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markProductAsSold(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Mark as Sold")
            .setMessage("Are you sure you want to mark this product as sold?")
            .setPositiveButton("Yes", (dialog, which) ->
                firebaseManager.getDatabase().getReference(com.example.tradeup_app.firebase.FirebaseManager.PRODUCTS_NODE)
                    .child(product.getId())
                    .child("status")
                    .setValue("Sold")
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Product marked as sold", Toast.LENGTH_SHORT).show();
                        }
                        performSearch(); // Refresh search results
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
                firebaseManager.getDatabase().getReference(com.example.tradeup_app.firebase.FirebaseManager.PRODUCTS_NODE)
                    .child(product.getId())
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Product deleted successfully", Toast.LENGTH_SHORT).show();
                        }
                        performSearch(); // Refresh search results
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
