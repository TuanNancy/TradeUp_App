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
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText searchEditText, minPriceEditText, maxPriceEditText;
    private Spinner categorySpinner, conditionSpinner, sortSpinner;
    private RecyclerView searchResultsRecyclerView;
    private View progressBar;

    private ProductAdapter productAdapter;
    private FirebaseManager firebaseManager;
    private List<Product> productList = new ArrayList<>();

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

    private final Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            performSearch();
        }
    };

    private void setupRecyclerView() {
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
                // TODO: Show product options
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
                    productAdapter.notifyDataSetChanged();
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
            Toast.makeText(getContext(), "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        String sortBy = getSortByValue();
        showProgressBar();

        firebaseManager.searchProducts(query, category, condition, minPrice, maxPrice, sortBy,
            new FirebaseManager.ProductCallback() {
                @Override
                public void onProductsLoaded(List<Product> products) {
                    hideProgressBar();
                    productList.clear();
                    productList.addAll(products);
                    productAdapter.notifyDataSetChanged();
                    updateEmptyState();
                }

                @Override
                public void onError(String error) {
                    hideProgressBar();
                    Toast.makeText(getContext(), "Lỗi tìm kiếm: " + error, Toast.LENGTH_SHORT).show();
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
        View emptyState = getView().findViewById(R.id.empty_state);
        if (emptyState != null) {
            emptyState.setVisibility(productList.isEmpty() ? View.VISIBLE : View.GONE);
            searchResultsRecyclerView.setVisibility(productList.isEmpty() ? View.GONE : View.VISIBLE);
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
}
