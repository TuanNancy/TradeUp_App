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

    private EditText searchEditText;
    private Spinner categorySpinner, conditionSpinner, sortSpinner;
    private RangeSlider priceRangeSlider;
    private RecyclerView searchResultsRecyclerView;

    private ProductAdapter searchAdapter;
    private FirebaseManager firebaseManager;

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
        categorySpinner = view.findViewById(R.id.category_spinner);
        conditionSpinner = view.findViewById(R.id.condition_spinner);
        sortSpinner = view.findViewById(R.id.sort_spinner);
        priceRangeSlider = view.findViewById(R.id.price_range_slider);
        searchResultsRecyclerView = view.findViewById(R.id.search_results_recycler);

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
        searchAdapter = new ProductAdapter(getContext(), new ArrayList<>());
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        searchResultsRecyclerView.setAdapter(searchAdapter);

        searchAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
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
        // Setup price range slider
        priceRangeSlider.setValues(0f, 10000000f);
        priceRangeSlider.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                if (fromUser) {
                    // Delay search to avoid too many calls
                    searchEditText.removeCallbacks(searchRunnable);
                    searchEditText.postDelayed(searchRunnable, 500);
                }
            }
        });

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
    }

    private void performInitialSearch() {
        // Load all products initially
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                if (getActivity() != null) {
                    searchAdapter.updateProducts(products);
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "Lỗi tải sản phẩm: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void performSearch() {
        String query = searchEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String condition = conditionSpinner.getSelectedItem().toString();
        String sortBy = sortSpinner.getSelectedItem().toString();
        List<Float> priceRange = priceRangeSlider.getValues();

        double minPrice = priceRange.get(0);
        double maxPrice = priceRange.get(1);

        firebaseManager.searchProducts(query, category, condition, minPrice, maxPrice, sortBy,
            new FirebaseManager.ProductCallback() {
                @Override
                public void onSuccess(List<Product> products) {
                    if (getActivity() != null) {
                        searchAdapter.updateProducts(products);
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Lỗi tìm kiếm: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
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
