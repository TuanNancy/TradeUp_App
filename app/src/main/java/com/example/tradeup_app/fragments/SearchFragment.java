package com.example.tradeup_app.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
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
import com.example.tradeup_app.activities.PaymentActivity;
import com.example.tradeup_app.activities.LocationSettingsActivity;
import com.example.tradeup_app.services.LocationService;
import com.example.tradeup_app.utils.VNDPriceFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class SearchFragment extends Fragment {

    private EditText searchEditText, minPriceEditText, maxPriceEditText;
    private Spinner categorySpinner, conditionSpinner, sortSpinner;
    private RecyclerView searchResultsRecyclerView;
    private View progressBar;
    // ✅ THÊM: RangeSlider cho kho���ng giá
    private com.google.android.material.slider.RangeSlider priceRangeSlider;
    private TextView priceRangeText;

    // Location-based search components
    private ChipGroup locationChipGroup;
    private Chip chipLocationFilter, chipSortByDistance;
    private LocationService locationService;
    private SharedPreferences locationPrefs;
    private double userLatitude = 0;
    private double userLongitude = 0;
    private String userAddress = "";
    private boolean locationFilterEnabled = false;

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

        // Check if category is passed from HomeFragment
        handleCategoryFromBundle();

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
        // ✅ THÊM: Khởi tạo RangeSlider và TextView hiển thị giá trị
        priceRangeSlider = view.findViewById(R.id.price_range_slider);
        priceRangeText = view.findViewById(R.id.price_range_text);

        // Location-based search components
        locationChipGroup = view.findViewById(R.id.location_chip_group);
        chipLocationFilter = view.findViewById(R.id.chip_location_filter);
        chipSortByDistance = view.findViewById(R.id.chip_sort_by_distance);

        firebaseManager = FirebaseManager.getInstance();

        // Initialize location services
        initLocationServices();
    }

    /**
     * Initialize location services for location-based search
     */
    private void initLocationServices() {
        if (getContext() == null) return;

        locationService = new LocationService(getContext());
        locationPrefs = getContext().getSharedPreferences("location_prefs", MODE_PRIVATE);

        // Load saved location data
        loadLocationData();

        // Setup location chips
        setupLocationChips();
    }

    /**
     * Load saved location data from SharedPreferences
     */
    private void loadLocationData() {
        userLatitude = Double.longBitsToDouble(locationPrefs.getLong("latitude", 0));
        userLongitude = Double.longBitsToDouble(locationPrefs.getLong("longitude", 0));
        userAddress = locationPrefs.getString("address", "");
        locationFilterEnabled = locationPrefs.getBoolean("location_filter_enabled", false);

        updateLocationChipsVisibility();
    }

    /**
     * Setup location filter chips
     */
    private void setupLocationChips() {
        if (chipLocationFilter != null) {
            chipLocationFilter.setOnClickListener(v -> {
                if (userLatitude == 0 && userLongitude == 0) {
                    // No location set, open location settings
                    Toast.makeText(getContext(), "Please set your location first", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getActivity(), LocationSettingsActivity.class));
                } else {
                    // Toggle location filter
                    chipLocationFilter.setChecked(!chipLocationFilter.isChecked());
                    applyLocationFilter();
                }
            });
        }

        if (chipSortByDistance != null) {
            chipSortByDistance.setOnClickListener(v -> {
                if (userLatitude == 0 && userLongitude == 0) {
                    Toast.makeText(getContext(), "Please set your location first", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getActivity(), LocationSettingsActivity.class));
                } else {
                    chipSortByDistance.setChecked(!chipSortByDistance.isChecked());
                    performSearch();
                }
            });
        }
    }

    /**
     * Update visibility of location chips based on location availability
     */
    private void updateLocationChipsVisibility() {
        if (locationChipGroup != null) {
            if (userLatitude != 0 && userLongitude != 0) {
                locationChipGroup.setVisibility(View.VISIBLE);

                // Update chip texts
                if (chipLocationFilter != null) {
                    int radius = locationPrefs.getInt("search_radius", 25);
                    chipLocationFilter.setText("📍 Within " + radius + " km");
                    chipLocationFilter.setChecked(locationFilterEnabled);
                }

                if (chipSortByDistance != null) {
                    chipSortByDistance.setText("📏 Sort by distance");
                }
            } else {
                locationChipGroup.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Apply location-based filtering to search results
     */
    private void applyLocationFilter() {
        if (chipLocationFilter == null || !chipLocationFilter.isChecked()) {
            return;
        }

        if (userLatitude == 0 && userLongitude == 0) {
            Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
            chipLocationFilter.setChecked(false);
            return;
        }

        int radius = locationPrefs.getInt("search_radius", 25);

        // Filter current product list by location
        locationService.filterProductsByLocation(new ArrayList<>(productList),
            userLatitude, userLongitude, radius,
            new LocationService.ProductLocationCallback() {
                @Override
                public void onProductsFiltered(List<Product> filteredProducts) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            productList.clear();
                            productList.addAll(filteredProducts);
                            productAdapter.notifyDataSetChanged();

                            Toast.makeText(getContext(),
                                "Found " + filteredProducts.size() + " products within " + radius + " km",
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Error filtering by location: " + error,
                                Toast.LENGTH_SHORT).show();
                            chipLocationFilter.setChecked(false);
                        });
                    }
                }
            });
    }

    /**
     * Enhanced search method with location-based features
     */
    private void performLocationAwareSearch() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                if (getActivity() == null) return;

                // Apply text and filter-based search first
                List<Product> filteredProducts = applyBasicFilters(products);

                // Apply location-based filtering/sorting if enabled
                if (userLatitude != 0 && userLongitude != 0) {
                    applyLocationBasedProcessing(filteredProducts);
                } else {
                    // No location data, just display filtered results
                    displaySearchResults(filteredProducts);
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Search error: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Apply location-based processing (filtering and/or sorting)
     */
    private void applyLocationBasedProcessing(List<Product> products) {
        boolean filterByLocation = chipLocationFilter != null && chipLocationFilter.isChecked();
        boolean sortByDistance = chipSortByDistance != null && chipSortByDistance.isChecked();

        if (filterByLocation) {
            // Filter by location radius first
            int radius = locationPrefs.getInt("search_radius", 25);
            locationService.filterProductsByLocation(products, userLatitude, userLongitude, radius,
                new LocationService.ProductLocationCallback() {
                    @Override
                    public void onProductsFiltered(List<Product> filteredProducts) {
                        if (sortByDistance) {
                            // Then sort by distance
                            sortProductsByDistance(filteredProducts);
                        } else {
                            displaySearchResults(filteredProducts);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Location filter error: " + error,
                                    Toast.LENGTH_SHORT).show();
                                displaySearchResults(products);
                            });
                        }
                    }
                });
        } else if (sortByDistance) {
            // Just sort by distance without filtering
            sortProductsByDistance(products);
        } else {
            // No location processing needed
            displaySearchResults(products);
        }
    }

    /**
     * Sort products by distance from user location
     */
    private void sortProductsByDistance(List<Product> products) {
        locationService.sortProductsByDistance(products, userLatitude, userLongitude,
            new LocationService.ProductLocationCallback() {
                @Override
                public void onProductsFiltered(List<Product> sortedProducts) {
                    displaySearchResults(sortedProducts);
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Distance sort error: " + error,
                                Toast.LENGTH_SHORT).show();
                            displaySearchResults(products);
                        });
                    }
                }
            });
    }

    /**
     * Apply basic text and category filters (existing functionality)
     */
    private List<Product> applyBasicFilters(List<Product> products) {
        List<Product> filteredProducts = new ArrayList<>();

        String searchQuery = searchEditText.getText().toString().toLowerCase().trim();
        String selectedCategory = categorySpinner.getSelectedItem().toString();
        String selectedCondition = conditionSpinner.getSelectedItem().toString();

        // Get price range from RangeSlider if available
        double minPrice = 0;
        double maxPrice = Double.MAX_VALUE;

        if (priceRangeSlider != null) {
            List<Float> values = priceRangeSlider.getValues();
            if (values.size() >= 2) {
                minPrice = values.get(0);
                maxPrice = values.get(1);
            }
        }

        for (Product product : products) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                product.getTitle().toLowerCase().contains(searchQuery) ||
                product.getDescription().toLowerCase().contains(searchQuery) ||
                (product.getTags() != null && product.getTags().toString().toLowerCase().contains(searchQuery));

            boolean matchesCategory = selectedCategory.equals("Tất cả") ||
                product.getCategory().equals(selectedCategory);

            boolean matchesCondition = selectedCondition.equals("Tất cả") ||
                product.getCondition().equals(selectedCondition);

            boolean matchesPrice = product.getPrice() >= minPrice && product.getPrice() <= maxPrice;

            if (matchesSearch && matchesCategory && matchesCondition && matchesPrice) {
                filteredProducts.add(product);
            }
        }

        return filteredProducts;
    }

    /**
     * Display final search results
     */
    private void displaySearchResults(List<Product> products) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                productList.clear();
                productList.addAll(products);
                productAdapter.notifyDataSetChanged();

                // Update product adapter with user location for distance display
                if (productAdapter instanceof ProductAdapter && userLatitude != 0 && userLongitude != 0) {
                    productAdapter.setUserLocation(userLatitude, userLongitude);
                }
            });
        }
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

        // ✅ SỬA: Setup price range input listeners với validation
        TextWatcher priceWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // ✅ THÊM: Validation khoảng giá trước khi tìm kiếm
                if (validatePriceRange()) {
                    // Delay search to avoid too many calls while typing
                    searchEditText.removeCallbacks(searchRunnable);
                    searchEditText.postDelayed(searchRunnable, 300); // Giảm từ 500ms xuống 300ms
                }
            }
        };

        minPriceEditText.addTextChangedListener(priceWatcher);
        maxPriceEditText.addTextChangedListener(priceWatcher);

        // ✅ THÊM: Thiết lập listener cho RangeSlider
        priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                // Cập nhật giá trị hiển thị khi người dùng kéo slider
                updatePriceRangeText();
                // ✅ THÊM: Tự động tìm kiếm khi thay đổi slider
                searchEditText.removeCallbacks(searchRunnable);
                searchEditText.postDelayed(searchRunnable, 200);
            }
        });

        // Đặt giá trị mặc định cho RangeSlider và TextView
        priceRangeSlider.setValueFrom(0);
        priceRangeSlider.setValueTo(100000000); // ✅ Tăng lên 100 triệu VNĐ
        priceRangeSlider.setValues(0f, 100000000f);
        updatePriceRangeText();

        // ✅ THÊM: Tự động điều chỉnh khoảng giá dựa trên dữ liệu thực tế
        adjustPriceRangeBasedOnData();

        // ✅ THÊM: Đồng bộ EditText với RangeSlider khi người dùng nhập tay
        TextWatcher editTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateSliderFromEditText();
            }
        };

        minPriceEditText.addTextChangedListener(editTextWatcher);
        maxPriceEditText.addTextChangedListener(editTextWatcher);
    }

    // ✅ THÊM: Đồng bộ RangeSlider từ EditText
    private void updateSliderFromEditText() {
        try {
            String minStr = minPriceEditText.getText().toString().trim();
            String maxStr = maxPriceEditText.getText().toString().trim();

            float minValue = minStr.isEmpty() ? 0 : Float.parseFloat(minStr);
            float maxValue = maxStr.isEmpty() ? 100000000 : Float.parseFloat(maxStr); // ✅ SỬA: 100 triệu

            // Đảm bảo giá trị trong phạm vi hợp lệ
            minValue = Math.max(0, Math.min(minValue, 100000000)); // ✅ SỬA: 100 triệu
            maxValue = Math.max(minValue, Math.min(maxValue, 100000000)); // ✅ SỬA: 100 triệu

            // Cập nhật slider mà không trigger listener
            priceRangeSlider.clearOnChangeListeners();
            priceRangeSlider.setValues(minValue, maxValue);

            // Khôi phục listener
            priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser) {
                    updatePriceRangeText();
                    searchEditText.removeCallbacks(searchRunnable);
                    searchEditText.postDelayed(searchRunnable, 200);
                }
            });

            // Cập nhật text hiển thị
            String text = "Khoảng giá: " + VNDPriceFormatter.formatVND(minValue) + " - " + VNDPriceFormatter.formatVND(maxValue);
            if (priceRangeText != null) {
                priceRangeText.setText(text);
            }

        } catch (NumberFormatException e) {
            // Ignore invalid input
        }
    }

    // ✅ THÊM: Tự động điều chỉnh khoảng giá dựa trên dữ liệu thực tế
    private void adjustPriceRangeBasedOnData() {
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                if (products.isEmpty() || !isAdded()) return;

                // Tìm giá cao nhất và thấp nhất trong database
                double minPrice = Double.MAX_VALUE;
                double maxPrice = 0;

                for (Product product : products) {
                    if (product.getPrice() > 0) { // Chỉ tính sản ph��m có giá hợp lệ
                        minPrice = Math.min(minPrice, product.getPrice());
                        maxPrice = Math.max(maxPrice, product.getPrice());
                    }
                }

                // Nếu tìm thấy dữ liệu giá hợp lệ
                if (minPrice != Double.MAX_VALUE && maxPrice > 0) {
                    // Thêm buffer 20% để người dùng có thể tìm kiếm rộng hơn
                    double buffer = (maxPrice - minPrice) * 0.2;
                    double adjustedMin = Math.max(0, minPrice - buffer);
                    double adjustedMax = maxPrice + buffer;

                    // Đảm bảo không vượt quá 100 triệu
                    adjustedMax = Math.min(adjustedMax, 100000000);

                    // ✅ SỬA: Làm tròn giá trị theo stepSize để tránh crash
                    final double stepSize = 100000; // 100k VNĐ
                    final double finalAdjustedMax = Math.ceil(adjustedMax / stepSize) * stepSize;
                    final double finalAdjustedMin = Math.floor(adjustedMin / stepSize) * stepSize;

                    // Cập nhật RangeSlider với khoảng giá thực tế
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                // Đảm bảo các giá trị hợp lệ trước khi set
                                float maxValue = (float) Math.max(finalAdjustedMax, stepSize);
                                float minValue = (float) Math.max(finalAdjustedMin, 0);

                                // Đảm bảo maxValue là bội số của stepSize
                                maxValue = (float) (Math.ceil(maxValue / stepSize) * stepSize);
                                minValue = (float) (Math.floor(minValue / stepSize) * stepSize);

                                priceRangeSlider.setValueTo(maxValue);
                                priceRangeSlider.setValues(minValue, maxValue);

                                // Cập nhật TextView hiển thị
                                String text = "Khoảng giá: " + VNDPriceFormatter.formatVND(minValue) + " - " + VNDPriceFormatter.formatVND(maxValue);
                                if (priceRangeText != null) {
                                    priceRangeText.setText(text);
                                }

                                // Cập nhật EditText
                                minPriceEditText.setText(String.valueOf((int) minValue));
                                maxPriceEditText.setText(String.valueOf((int) maxValue));

                                android.util.Log.d("SearchFragment", "📊 Khoảng giá đã được điều chỉnh: " + VNDPriceFormatter.formatVND(minValue) + " - " + VNDPriceFormatter.formatVND(maxValue));
                            } catch (Exception e) {
                                android.util.Log.e("SearchFragment", "❌ Lỗi khi cập nhật RangeSlider: " + e.getMessage());
                                // Fallback to safe default values
                                priceRangeSlider.setValueTo(100000000f);
                                priceRangeSlider.setValues(0f, 100000000f);

                                if (priceRangeText != null) {
                                    priceRangeText.setText("Khoảng giá: " + VNDPriceFormatter.formatVND(0) + " - " + VNDPriceFormatter.formatVND(100000000));
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("SearchFragment", "Lỗi khi điều chỉnh khoảng giá: " + error);
                // Giữ nguyên giá trị mặc định 100 triệu nếu có lỗi
            }
        });
    }

    // ✅ THÊM: Phương thức cập nhật văn bản khoảng giá
    private void updatePriceRangeText() {
        List<Float> values = priceRangeSlider.getValues();
        if (values.size() == 2) {
            float minValue = values.get(0);
            float maxValue = values.get(1);

            // Cập nhật giá trị vào EditText
            minPriceEditText.setText(String.valueOf((int) minValue));
            maxPriceEditText.setText(String.valueOf((int) maxValue));

            // Cập nhật giá trị hiển thị
            String text = "Khoảng giá: " + VNDPriceFormatter.formatVND(minValue) + " - " + VNDPriceFormatter.formatVND(maxValue);
            priceRangeText.setText(text);
        }
    }

    // ✅ THÊM: Phương thức validation khoảng giá
    private boolean validatePriceRange() {
        String minPriceStr = minPriceEditText.getText().toString().trim();
        String maxPriceStr = maxPriceEditText.getText().toString().trim();

        // Nếu cả hai đều trống, không có lỗi
        if (minPriceStr.isEmpty() && maxPriceStr.isEmpty()) {
            clearPriceErrors();
            return true;
        }

        try {
            double minPrice = 0, maxPrice = 0;

            if (!minPriceStr.isEmpty()) {
                minPrice = Double.parseDouble(minPriceStr);
                if (minPrice < 0) {
                    minPriceEditText.setError("Giá không được âm");
                    return false;
                }
            }

            if (!maxPriceStr.isEmpty()) {
                maxPrice = Double.parseDouble(maxPriceStr);
                if (maxPrice < 0) {
                    maxPriceEditText.setError("Giá không được âm");
                    return false;
                }
            }

            // Kiểm tra minPrice <= maxPrice khi cả hai đều có giá trị
            if (!minPriceStr.isEmpty() && !maxPriceStr.isEmpty() && minPrice > maxPrice) {
                maxPriceEditText.setError("Giá tối đa phải lớn hơn giá tối thiểu");
                return false;
            }

            clearPriceErrors();
            return true;

        } catch (NumberFormatException e) {
            if (!minPriceStr.isEmpty() && !isValidNumber(minPriceStr)) {
                minPriceEditText.setError("Giá không hợp lệ");
            }
            if (!maxPriceStr.isEmpty() && !isValidNumber(maxPriceStr)) {
                maxPriceEditText.setError("Giá không hợp lệ");
            }
            return false;
        }
    }

    // ✅ THÊM: Phương thức kiểm tra số hợp lệ
    private boolean isValidNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ✅ THÊM: Xóa lỗi giá
    private void clearPriceErrors() {
        minPriceEditText.setError(null);
        maxPriceEditText.setError(null);
    }

    private void performInitialSearch() {
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                if (getActivity() != null && isAdded()) {
                    productAdapter.updateProducts(products);
                    updateEmptyState();
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    Toast.makeText(getContext(), "Lỗi tải sản phẩm: " + error, Toast.LENGTH_SHORT).show();
                    productAdapter.clearProducts();
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
                    if (getActivity() != null && isAdded()) {
                        hideProgressBar();
                        // Sử dụng phương thức mới để cập nhật adapter an toàn
                        productAdapter.updateProducts(products);
                        updateEmptyState();
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null && isAdded()) {
                        hideProgressBar();
                        Toast.makeText(getContext(), "Lỗi tìm ki���m: " + error, Toast.LENGTH_SHORT).show();
                        productAdapter.clearProducts();
                        updateEmptyState();
                    }
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

    private void openProductDetail(Product product) {
        // Navigate to ProductDetailActivity instead of ChatActivity
        com.example.tradeup_app.activities.ProductDetailActivity.startActivity(
            getContext(), product.getId());

        // Increment view count
        firebaseManager.incrementProductViewCount(product.getId());
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
        android.util.Log.d("SearchFragment", "showMakeOfferDialog called for product: " + product.getTitle());

        String currentUserId = firebaseManager.getCurrentUserId();
        android.util.Log.d("SearchFragment", "Current user ID: " + currentUserId);
        android.util.Log.d("SearchFragment", "Product seller ID: " + product.getSellerId());

        if (currentUserId == null) {
            android.util.Log.d("SearchFragment", "User not logged in - showing login message");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please login to make an offer", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (currentUserId.equals(product.getSellerId())) {
            android.util.Log.d("SearchFragment", "User trying to make offer on own product");
            if (getContext() != null) {
                Toast.makeText(getContext(), "You cannot make an offer on your own product", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (getContext() != null) {
            try {
                android.util.Log.d("SearchFragment", "Creating and showing MakeOfferDialog");
                com.example.tradeup_app.dialogs.MakeOfferDialog dialog = new com.example.tradeup_app.dialogs.MakeOfferDialog(
                    getContext(),
                    product,
                    (offerPrice, message) -> submitOffer(product, offerPrice, message)
                );
                dialog.show();
                android.util.Log.d("SearchFragment", "MakeOfferDialog shown successfully");
            } catch (Exception e) {
                android.util.Log.e("SearchFragment", "Error showing MakeOfferDialog", e);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error opening offer dialog", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            android.util.Log.e("SearchFragment", "Context is null, cannot show dialog");
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

    private void handleCategoryFromBundle() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("category")) {
            String categoryFromHome = args.getString("category");
            if (categoryFromHome != null) {
                setCategoryFilter(categoryFromHome);
            }
        }
    }

    private void setCategoryFilter(String category) {
        if (getContext() == null) return;

        // Map category names to spinner positions
        String[] categories = {"Tất cả", "Điện tử", "Thời trang", "Xe cộ", "Nhà cửa", "Sách", "Thể thao", "Khác"};

        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                categorySpinner.setSelection(i);
                // Trigger search with this category
                performSearch();
                break;
            }
        }
    }

    private void showBuyProductDialog(Product product) {
        // Implement the dialog to handle product buying
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Mua sản phẩm")
            .setMessage("Bạn có muốn mua sản phẩm này không?\n\nTên: " + product.getTitle() + "\nGiá: " + formatPrice(product.getPrice()))
            .setPositiveButton("Mua ngay", (dialog, which) -> {
                // Handle the buy action
                handleBuyProduct(product);
            })
            .setNegativeButton("Hủy", null)
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
                       "Giá: " + formatPrice(product.getPrice()) + "\n\n" +
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

    private String formatPrice(double price) {
        return VNDPriceFormatter.formatVND(price);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload location data when returning from settings
        if (getContext() != null) {
            loadLocationData();
            updateLocationChipsVisibility();
        }
    }
}
