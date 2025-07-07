package com.example.tradeup_app.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.dialogs.ProductPreviewDialog;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.tradeup_app.utils.LocationUtils;
import com.example.tradeup_app.utils.ImageUploadManager;
import com.example.tradeup_app.utils.VNDPriceFormatter;
import com.example.tradeup_app.adapters.ImagePreviewAdapter;

public class SellFragment extends Fragment {

    private static final int PICK_IMAGES_REQUEST = 1001;
    private static final int LOCATION_PERMISSION_REQUEST = 1002;
    private static final int BACKGROUND_LOCATION_PERMISSION_REQUEST = 1003;

    private EditText titleEditText, descriptionEditText, priceEditText, locationEditText, behaviorEditText, addTagEditText;
    private AutoCompleteTextView categoryDropdown, conditionDropdown;
    private Switch negotiableSwitch; // Changed from MaterialSwitch to Switch
    private ChipGroup tagsChipGroup;
    private RecyclerView imagesRecyclerView;
    private Button addImagesButton, previewButton, publishButton;
    private ImageButton locationGpsButton;

    private List<Uri> selectedImages = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseManager firebaseManager;
    private ImagePreviewAdapter imagePreviewAdapter;
    private LocationCallback locationCallback;

    private boolean locationPermissionGranted = false;
    private static final int MAX_IMAGES = 10;
    private double latitude;
    private double longitude;
    private Dialog loadingDialog;

    private static final float REQUIRED_ACCURACY = 50.0f; // 50 meters accuracy
    private static final long LOCATION_TIMEOUT = 30000; // 30 seconds timeout

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sell, container, false);

        initViews(view);
        setupSpinners();
        setupLocationFeatures();
        setupImageUpload(view); // Pass view parameter
        setupTagSystem();
        setupPreviewAndPublish();

        return view;
    }

    private void initViews(View view) {
        titleEditText = view.findViewById(R.id.title_edit_text);
        descriptionEditText = view.findViewById(R.id.description_edit_text);
        priceEditText = view.findViewById(R.id.price_edit_text);
        locationEditText = view.findViewById(R.id.location_edit_text);
        categoryDropdown = view.findViewById(R.id.category_dropdown);
        conditionDropdown = view.findViewById(R.id.condition_dropdown);
        negotiableSwitch = view.findViewById(R.id.negotiable_switch);
        tagsChipGroup = view.findViewById(R.id.tags_chip_group);
        imagesRecyclerView = view.findViewById(R.id.images_recycler_view);
        addImagesButton = view.findViewById(R.id.add_images_button);
        previewButton = view.findViewById(R.id.preview_button);
        publishButton = view.findViewById(R.id.publish_button);
        locationGpsButton = view.findViewById(R.id.location_gps_button);
        behaviorEditText = view.findViewById(R.id.behavior_edit_text);
        addTagEditText = view.findViewById(R.id.add_tag_edit_text);

        firebaseManager = FirebaseManager.getInstance();

        // Setup VND price formatting
        setupPriceFormatting();

        // Setup image preview recycler view
        setupImagePreviewRecyclerView();
    }

    private void setupPriceFormatting() {
        // Thêm TextWatcher để format giá VNĐ tự động khi nhập
        priceEditText.addTextChangedListener(new VNDPriceFormatter.VNDTextWatcher(priceEditText));

        // Set hint cho price EditText
        priceEditText.setHint("Nhập giá (VNĐ)");
    }

    private void setupImagePreviewRecyclerView() {
        imagePreviewAdapter = new ImagePreviewAdapter(getContext(), selectedImages, uri -> {
            selectedImages.remove(uri);
            imagePreviewAdapter.notifyDataSetChanged();
            updateImageButtonState();
        });
        imagesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        imagesRecyclerView.setAdapter(imagePreviewAdapter);

        imagePreviewAdapter.setOnImageRemoveListener(position -> {
            selectedImages.remove(position);
            imagePreviewAdapter.updateImages(selectedImages);
            Toast.makeText(getContext(), "Đã xóa hình ảnh", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSpinners() {
        // Category dropdown
        String[] categories = {"Điện tử", "Thời trang", "Xe cộ", "Nhà cửa", "Sách", "Thể thao", "Khác"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, categories);
        categoryDropdown.setAdapter(categoryAdapter);
        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            categoryDropdown.setError(null);
        });

        // Condition dropdown
        String[] conditions = {"Mới", "Như mới", "Tốt", "Khá tốt", "Cũ"};
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, conditions);
        conditionDropdown.setAdapter(conditionAdapter);
        conditionDropdown.setOnItemClickListener((parent, view, position, id) -> {
            conditionDropdown.setError(null);
        });
    }

    private void setupLocationFeatures() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        locationGpsButton.setOnClickListener(v -> requestLocation());
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Check if GPS is enabled
            LocationManager locationManager = (LocationManager) requireContext().getSystemService(requireContext().LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGPSEnableDialog();
                return;
            }

            // Show loading state
            locationGpsButton.setEnabled(false);
            Toast.makeText(requireContext(), "Đang lấy vị trí hiện tại...", Toast.LENGTH_SHORT).show();

            // Use simplified location request - just get current location
            requestCurrentLocation();
        }
    }

    private void requestCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Create simple location request for current location
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500)
                .setNumUpdates(1); // Only get one update

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // ✅ FIX: Check if fragment is still attached before accessing context
                if (!isAdded() || getContext() == null) {
                    return;
                }

                locationGpsButton.setEnabled(true);

                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();

                    if (location != null) {
                        // Log location details for debugging
                        android.util.Log.d("SellFragment", String.format(
                            "Got location: lat=%.6f, lng=%.6f, accuracy=%.2f, provider=%s, time=%d, isMock=%b",
                            location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                            location.getProvider(), location.getTime(), location.isFromMockProvider()));

                        // Check if this is a reasonable location (not at 0,0 or obviously fake)
                        if (isValidLocation(location)) {
                            handleLocationSuccess(location);
                        } else {
                            Toast.makeText(getContext(), "Vị trí không hợp lệ. Vui lòng thử lại hoặc nhập thủ công.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Không thể lấy vị trí. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Không nhận được dữ liệu vị trí. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                }

                // Clean up
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        // Add timeout
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // ✅ FIX: Check if fragment is still attached before accessing context
            if (locationCallback != null && isAdded() && getContext() != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                locationGpsButton.setEnabled(true);
                Toast.makeText(getContext(), "Timeout: Không thể lấy vị trí. Vui lòng kiểm tra GPS.", Toast.LENGTH_LONG).show();
            }
        }, 15000); // 15 seconds timeout

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnFailureListener(e -> {
                    // ✅ FIX: Check if fragment is still attached before accessing context
                    if (isAdded() && getContext() != null) {
                        locationGpsButton.setEnabled(true);
                        Toast.makeText(getContext(), "Lỗi GPS: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        android.util.Log.e("SellFragment", "Location request failed", e);
                    }
                });
    }

    private boolean isValidLocation(Location location) {
        // Check for null or invalid coordinates
        if (location == null) return false;

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        // Check for 0,0 coordinates (invalid)
        if (lat == 0.0 && lng == 0.0) return false;

        // Check for valid latitude/longitude ranges
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return false;

        // Check if location is too old (more than 5 minutes)
        long currentTime = System.currentTimeMillis();
        long locationTime = location.getTime();
        if (currentTime - locationTime > 5 * 60 * 1000) { // 5 minutes
            android.util.Log.w("SellFragment", "Location is too old: " + (currentTime - locationTime) / 1000 + " seconds");
        }

        return true;
    }

    private void handleLocationSuccess(Location location) {
        // ✅ FIX: Check if fragment is still attached before accessing context
        if (!isAdded() || getContext() == null) {
            return;
        }

        // Check if this is a mock location (fake/simulated location)
        if (location.isFromMockProvider()) {
            locationGpsButton.setEnabled(true);
            Toast.makeText(getContext(), "Phát hiện vị trí giả lập. Vui lòng tắt mock location và thử lại.", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if location is Google headquarters (indicates emulator or mock location)
        double googleLat = 37.4220936;
        double googleLng = -122.083922;
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), googleLat, googleLng, results);

        if (results[0] < 1000) // Within 1km of Google HQ
        {
            locationGpsButton.setEnabled(true);
            new AlertDialog.Builder(getContext())
                .setTitle("Vị trí không chính xác")
                .setMessage("Ứng dụng đang lấy vị trí giả lập (Google HQ). Điều này có thể do:\n\n" +
                    "1. Bạn đang sử dụng máy ảo (emulator)\n" +
                    "2. Có ứng dụng mock location đang chạy\n" +
                    "3. Vị trí GPS chưa được cập nhật\n\n" +
                    "Bạn có muốn nhập địa chỉ thủ công không?")
                .setPositiveButton("Nhập thủ công", (dialog, which) -> {
                    locationEditText.requestFocus();
                    Toast.makeText(getContext(), "Vui lòng nhập địa chỉ của bạn", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Thử lại", (dialog, which) -> {
                    // Reset and try again
                    latitude = 0;
                    longitude = 0;
                    requestLocation();
                })
                .show();
            return;
        }

        // Check accuracy
        if (location.getAccuracy() > 100) // More than 100 meters accuracy
        {
            Toast.makeText(getContext(), String.format("Độ chính xác thấp (±%.0fm). Đang thử cải thiện...", location.getAccuracy()), Toast.LENGTH_SHORT).show();
            // Continue trying for better accuracy, but don't return immediately
        }

        LocationUtils.getAddressFromLocation(getContext(), location.getLatitude(), location.getLongitude(),
            address -> {
                // ✅ FIX: Check if fragment is still attached before accessing context in callback
                if (!isAdded() || getContext() == null) {
                    return;
                }

                if (address == null || address.trim().isEmpty()) {
                    locationGpsButton.setEnabled(true);
                    Toast.makeText(getContext(), "Không thể chuyển đổi tọa độ thành địa chỉ. Vui lòng nhập thủ công.", Toast.LENGTH_LONG).show();
                    locationEditText.requestFocus();
                    return;
                }

                locationEditText.setText(address);
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                locationGpsButton.setEnabled(true);

                String successMessage = String.format("Vị trí: %s (±%.0fm)", address, location.getAccuracy());
                Toast.makeText(getContext(), successMessage, Toast.LENGTH_LONG).show();

                // Log for debugging
                android.util.Log.d("SellFragment", String.format("Location obtained: lat=%.6f, lng=%.6f, accuracy=%.2f, provider=%s",
                    location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getProvider()));
            });
    }


    private void setupImageUpload(View view) {
        imagePreviewAdapter = new ImagePreviewAdapter(getContext(), selectedImages, uri -> {
            selectedImages.remove(uri);
            imagePreviewAdapter.notifyDataSetChanged();
            updateImageButtonState();
        });

        imagesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        imagesRecyclerView.setAdapter(imagePreviewAdapter);

        // Setup click listener for the new add images card
        com.google.android.material.card.MaterialCardView addImagesCard = view.findViewById(R.id.add_images_card);
        addImagesCard.setOnClickListener(v -> {
            if (selectedImages.size() >= MAX_IMAGES) {
                Toast.makeText(getContext(), "Tối đa 10 hình ảnh", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Chọn hình ảnh"), PICK_IMAGES_REQUEST);
        });

        // Keep the old button listener as backup (hidden button)
        addImagesButton.setOnClickListener(v -> {
            if (selectedImages.size() >= MAX_IMAGES) {
                Toast.makeText(getContext(), "Tối đa 10 hình ảnh", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Chọn hình ảnh"), PICK_IMAGES_REQUEST);
        });
    }

    private void updateImageButtonState() {
        addImagesButton.setEnabled(selectedImages.size() < MAX_IMAGES);
    }

    private void setupTagSystem() {
        addTagEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTag(addTagEditText.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag.trim());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> tagsChipGroup.removeView(chip));
            tagsChipGroup.addView(chip);
            addTagEditText.setText("");
        }
    }

    private void setupPreviewAndPublish() {
        previewButton.setOnClickListener(v -> showPreview());
        publishButton.setOnClickListener(v -> validateAndPublish());
    }

    private void showPreview() {
        if (!validateInputs(true)) return;

        Product preview = createProductFromInputs();
        // Show preview dialog/activity
        ProductPreviewDialog.show(requireContext(), preview);
    }

    private boolean validateInputs(boolean isPreview) {
        if (titleEditText.getText().toString().trim().isEmpty()) {
            titleEditText.setError("Vui lòng nhập tiêu đề");
            return false;
        }
        if (descriptionEditText.getText().toString().trim().isEmpty()) {
            descriptionEditText.setError("Vui lòng nhập mô tả");
            return false;
        }
        if (priceEditText.getText().toString().trim().isEmpty()) {
            priceEditText.setError("Vui lòng nhập giá");
            return false;
        }
        // Validate giá VNĐ với định dạng
        if (!VNDPriceFormatter.isValidVNDPrice(priceEditText.getText().toString().trim())) {
            priceEditText.setError("Giá không hợp lệ (tối đa 999 tỷ VNĐ)");
            return false;
        }
        if (locationEditText.getText().toString().trim().isEmpty()) {
            locationEditText.setError("Vui lòng nhập địa chỉ");
            return false;
        }
        // Validate category
        if (categoryDropdown.getText().toString().trim().isEmpty()) {
            categoryDropdown.setError("Vui lòng chọn danh mục sản phẩm");
            categoryDropdown.requestFocus();
            return false;
        }
        // Validate condition
        if (conditionDropdown.getText().toString().trim().isEmpty()) {
            conditionDropdown.setError("Vui lòng chọn tình trạng sản phẩm");
            conditionDropdown.requestFocus();
            return false;
        }
        if (selectedImages.isEmpty() && !isPreview) {
            Toast.makeText(getContext(), "Vui lòng thêm ít nhất 1 hình ảnh", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private Product createProductFromInputs() {
        Product product = new Product();
        product.setTitle(titleEditText.getText().toString().trim());
        product.setDescription(descriptionEditText.getText().toString().trim());
        // Sử dụng VNDPriceFormatter để parse giá có định dạng VNĐ
        product.setPrice(VNDPriceFormatter.parseVND(priceEditText.getText().toString().trim()));
        product.setCategory(categoryDropdown.getText().toString());
        product.setCondition(conditionDropdown.getText().toString());
        product.setLocation(locationEditText.getText().toString().trim());
        product.setNegotiable(negotiableSwitch.isChecked());
        product.setItemBehavior(behaviorEditText.getText().toString().trim());
        product.setLatitude(latitude);
        product.setLongitude(longitude);

        // Set seller information
        UserModel currentUser = CurrentUser.getUser();
        if (currentUser != null) {
            product.setSellerId(currentUser.getUid());
            product.setSellerName(currentUser.getUsername());
        }

        // Get tags
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < tagsChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) tagsChipGroup.getChildAt(i);
            tags.add(chip.getText().toString());
        }
        product.setTags(tags);

        return product;
    }

    private void validateAndPublish() {
        if (!validateInputs(false)) return;

        Product product = createProductFromInputs();
        showLoadingDialog();

        // Upload images first
        ImageUploadManager.uploadImages(selectedImages, requireContext(), new ImageUploadManager.ImageUploadCallback() {
            @Override
            public void onSuccess(List<String> imageUrls) {
                product.setImageUrls(imageUrls);
                publishProduct(product);
            }

            @Override
            public void onFailure(Exception e) {
                hideLoadingDialog();
                Toast.makeText(getContext(), "Lỗi tải lên hình ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void publishProduct(Product product) {
        firebaseManager.addProduct(product, task -> {
            hideLoadingDialog();

            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Đăng bán thành công!", Toast.LENGTH_LONG).show();
                clearForm();

                // Navigate safely without causing crash
                try {
                    // Instead of onBackPressed, navigate to home fragment
                    if (getActivity() != null && isAdded()) {
                        androidx.fragment.app.FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

                        // Clear back stack to prevent navigation issues
                        fragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

                        // Navigate to home fragment and refresh data
                        com.example.tradeup_app.fragments.HomeFragment homeFragment = new com.example.tradeup_app.fragments.HomeFragment();
                        fragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, homeFragment)
                            .commit();

                        // Notify that data has changed so home fragment can refresh
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (homeFragment.isAdded()) {
                                // Trigger refresh of home fragment data
                                homeFragment.onResume();
                            }
                        }, 500);
                    }
                } catch (Exception e) {
                    android.util.Log.e("SellFragment", "Error navigating after publish", e);
                    // Fallback: just stay on current screen
                }
            } else {
                String errorMessage = "Lỗi đăng bán";
                if (task.getException() != null && task.getException().getMessage() != null) {
                    errorMessage += ": " + task.getException().getMessage();
                }
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(requireContext());
            loadingDialog.setContentView(R.layout.dialog_loading);
            loadingDialog.setCancelable(false);
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void clearForm() {
        titleEditText.setText("");
        descriptionEditText.setText("");
        priceEditText.setText("");
        locationEditText.setText("");
        behaviorEditText.setText("");
        addTagEditText.setText("");
        negotiableSwitch.setChecked(false);
        categoryDropdown.setText("", false);
        conditionDropdown.setText("", false);
        selectedImages.clear();
        imagePreviewAdapter.notifyDataSetChanged();
        tagsChipGroup.removeAllViews();
        latitude = 0;
        longitude = 0;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = Math.min(data.getClipData().getItemCount(), MAX_IMAGES - selectedImages.size());
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImages.add(imageUri);
                }
            } else if (data.getData() != null) {
                selectedImages.add(data.getData());
            }
            imagePreviewAdapter.notifyDataSetChanged();
            updateImageButtonState();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                getLocation();
            } else {
                locationPermissionGranted = false;
                showLocationPermissionDeniedDialog();
            }
        }
    }

    private void showLocationPermissionDeniedDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Quyền truy cập vị trí")
                .setMessage("Để sử dụng chức năng lấy vị trí tự động, bạn cần cấp quyền truy cập vị trí. Bạn có thể:\n\n1. Cấp quyền trong cài đặt ứng dụng\n2. Hoặc nhập địa chỉ thủ công")
                .setPositiveButton("Mở cài đặt", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Nhập thủ công", (dialog, which) -> {
                    dialog.dismiss();
                    locationEditText.requestFocus();
                    Toast.makeText(requireContext(), "Vui lòng nhập địa chỉ thủ công", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showGPSEnableDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Bật GPS")
                .setMessage("Để sử dụng chức năng lấy vị trí, bạn cần bật GPS. Bạn có muốn mở cài đặt GPS không?")
                .setPositiveButton("Mở cài đặt", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Bạn có thể nhập địa chỉ thủ công", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void tryGetLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Toast.makeText(requireContext(), "Đang thử lấy vị trí cuối cùng...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && isValidLocation(location)) {
                        // Check if location is recent (less than 10 minutes old)
                        long locationAge = System.currentTimeMillis() - location.getTime();
                        if (locationAge < 10 * 60 * 1000) { // 10 minutes
                            android.util.Log.d("SellFragment", "Using last known location (age: " + locationAge/1000 + "s)");
                            handleLocationSuccess(location);
                        } else {
                            android.util.Log.d("SellFragment", "Last known location too old, requesting fresh location");
                            requestCurrentLocation();
                        }
                    } else {
                        android.util.Log.d("SellFragment", "No valid last known location, requesting fresh location");
                        requestCurrentLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SellFragment", "Failed to get last location: " + e.getMessage());
                    requestCurrentLocation();
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up location callback to prevent memory leaks
        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
