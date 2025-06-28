package com.example.tradeup_app.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.tradeup_app.utils.LocationUtils;
import com.example.tradeup_app.utils.ImageUploadManager;
import com.example.tradeup_app.adapters.ImagePreviewAdapter;

public class SellFragment extends Fragment {

    private static final int PICK_IMAGES_REQUEST = 1001;
    private static final int LOCATION_PERMISSION_REQUEST = 1002;

    private EditText titleEditText, descriptionEditText, priceEditText, locationEditText;
    private Spinner categorySpinner, conditionSpinner;
    private Switch negotiableSwitch;
    private ChipGroup tagsChipGroup;
    private RecyclerView imagesRecyclerView;
    private Button addLocationButton, addImagesButton, previewButton, publishButton;

    private List<Uri> selectedImages = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseManager firebaseManager;
    private ImagePreviewAdapter imagePreviewAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sell, container, false);

        initViews(view);
        setupSpinners();
        setupButtons();
        setupLocation();

        return view;
    }

    private void initViews(View view) {
        titleEditText = view.findViewById(R.id.title_edit_text);
        descriptionEditText = view.findViewById(R.id.description_edit_text);
        priceEditText = view.findViewById(R.id.price_edit_text);
        locationEditText = view.findViewById(R.id.location_edit_text);
        categorySpinner = view.findViewById(R.id.category_spinner);
        conditionSpinner = view.findViewById(R.id.condition_spinner);
        negotiableSwitch = view.findViewById(R.id.negotiable_switch);
        tagsChipGroup = view.findViewById(R.id.tags_chip_group);
        imagesRecyclerView = view.findViewById(R.id.images_recycler_view);
        addLocationButton = view.findViewById(R.id.add_location_button);
        addImagesButton = view.findViewById(R.id.add_images_button);
        previewButton = view.findViewById(R.id.preview_button);
        publishButton = view.findViewById(R.id.publish_button);

        firebaseManager = FirebaseManager.getInstance();

        // Setup image preview recycler view
        setupImagePreviewRecyclerView();
    }

    private void setupImagePreviewRecyclerView() {
        imagePreviewAdapter = new ImagePreviewAdapter(getContext(), selectedImages);
        imagesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        imagesRecyclerView.setAdapter(imagePreviewAdapter);

        imagePreviewAdapter.setOnImageRemoveListener(position -> {
            selectedImages.remove(position);
            imagePreviewAdapter.updateImages(selectedImages);
            Toast.makeText(getContext(), "Đã xóa hình ảnh", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSpinners() {
        // Category spinner
        String[] categories = {"Điện tử", "Thời trang", "Xe cộ", "Nhà cửa", "Sách", "Thể thao", "Khác"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Condition spinner
        String[] conditions = {"Mới", "Như mới", "Tốt", "Khá tốt", "Cũ"};
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, conditions);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        conditionSpinner.setAdapter(conditionAdapter);
    }

    private void setupButtons() {
        addImagesButton.setOnClickListener(v -> openImagePicker());
        addLocationButton.setOnClickListener(v -> getCurrentLocation());
        previewButton.setOnClickListener(v -> previewListing());
        publishButton.setOnClickListener(v -> publishListing());
    }

    private void setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Chọn hình ảnh"), PICK_IMAGES_REQUEST);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        // Show loading indicator
        addLocationButton.setEnabled(false);
        addLocationButton.setText("Đang lấy vị trí...");

        // Create location request
        com.google.android.gms.location.LocationRequest locationRequest =
            com.google.android.gms.location.LocationRequest.create()
                .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000)
                .setNumUpdates(1);

        // Try to get current location
        fusedLocationClient.requestLocationUpdates(locationRequest, new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                super.onLocationResult(locationResult);

                addLocationButton.setEnabled(true);
                addLocationButton.setText("GPS");

                android.location.Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Stop location updates
                    fusedLocationClient.removeLocationUpdates(this);

                    // Convert coordinates to address using LocationUtils
                    LocationUtils.getAddressFromLocation(requireContext(),
                        location.getLatitude(), location.getLongitude(),
                        new LocationUtils.LocationCallback() {
                            @Override
                            public void onLocationResult(String address) {
                                locationEditText.setText(address);
                                Toast.makeText(getContext(), "Đã lấy vị trí thành công", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onLocationError(String error) {
                                // Fallback to coordinates
                                String coordinates = String.format("%.6f, %.6f", location.getLatitude(), location.getLongitude());
                                locationEditText.setText(coordinates);
                                Toast.makeText(getContext(), "Lấy địa chỉ lỗi, hiển thị tọa độ: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                } else {
                    // Try getLastLocation as fallback
                    getLastKnownLocation();
                }
            }
        }, android.os.Looper.getMainLooper())
        .addOnFailureListener(e -> {
            addLocationButton.setEnabled(true);
            addLocationButton.setText("GPS");

            // Try getLastLocation as fallback
            getLastKnownLocation();
        });
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    LocationUtils.getAddressFromLocation(requireContext(),
                        location.getLatitude(), location.getLongitude(),
                        new LocationUtils.LocationCallback() {
                            @Override
                            public void onLocationResult(String address) {
                                locationEditText.setText(address);
                                Toast.makeText(getContext(), "Sử dụng vị trí gần đây nhất", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onLocationError(String error) {
                                String coordinates = String.format("%.6f, %.6f", location.getLatitude(), location.getLongitude());
                                locationEditText.setText(coordinates);
                                Toast.makeText(getContext(), "Hiển thị tọa độ: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                } else {
                    Toast.makeText(getContext(), "Không thể lấy vị trí. Vui lòng bật GPS và thử lại hoặc nhập thủ công.", Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Lỗi lấy vị trí: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void previewListing() {
        if (validateForm()) {
            Product product = createProductFromForm();
            showPreviewDialog(product);
        }
    }

    private void publishListing() {
        if (!validateForm()) return;

        // Show loading
        publishButton.setEnabled(false);
        publishButton.setText("Đang tải ảnh...");

        // Upload images to Cloudinary first, then save product
        if (!selectedImages.isEmpty()) {
            ImageUploadManager.getInstance().uploadImages(
                getContext(),
                selectedImages,
                "products",
                new ImageUploadManager.ImageUploadCallback() {
                    @Override
                    public void onSuccess(List<String> imageUrls) {
                        publishButton.setText("Đang đăng...");
                        Product product = createProductFromForm();
                        product.setImageUrls(imageUrls); // Set real Cloudinary URLs

                        saveProductToFirebase(product);
                    }

                    @Override
                    public void onFailure(String error) {
                        publishButton.setEnabled(true);
                        publishButton.setText("Đăng bán");
                        Toast.makeText(getContext(), "Lỗi tải ảnh: " + error, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(int uploadedCount, int totalCount) {
                        publishButton.setText("Đang tải ảnh (" + uploadedCount + "/" + totalCount + ")");
                    }
                });
        } else {
            // No images selected, save product directly
            Product product = createProductFromForm();
            saveProductToFirebase(product);
        }
    }

    private void saveProductToFirebase(Product product) {
        firebaseManager.addProduct(product, task -> {
            publishButton.setEnabled(true);
            publishButton.setText("Đăng bán");

            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Đăng sản phẩm thành công!", Toast.LENGTH_SHORT).show();
                clearForm();

                // Switch to home fragment to see the posted item
                if (getActivity() != null) {
                    ((androidx.fragment.app.FragmentActivity) getActivity()).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new com.example.tradeup_app.fragments.HomeFragment())
                        .commit();
                }
            } else {
                Toast.makeText(getContext(), "Lỗi đăng sản phẩm. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Product createProductFromForm() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        double price = Double.parseDouble(priceEditText.getText().toString().trim());
        String category = categorySpinner.getSelectedItem().toString();
        String condition = conditionSpinner.getSelectedItem().toString();
        String location = locationEditText.getText().toString().trim();
        boolean isNegotiable = negotiableSwitch.isChecked();

        Product product = new Product(title, description, price, category, condition, location, firebaseManager.getCurrentUserId());
        product.setNegotiable(isNegotiable);
        product.setCreatedAt(new Date());
        product.setUpdatedAt(new Date());

        // Add current user's name
        UserModel currentUser = CurrentUser.getUser();
        if (currentUser != null) {
            product.setSellerName(currentUser.getUsername());
        }

        // Add tags from chip group
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < tagsChipGroup.getChildCount(); i++) {
            View child = tagsChipGroup.getChildAt(i);
            if (child instanceof Chip) {
                tags.add(((Chip) child).getText().toString());
            }
        }
        product.setTags(tags);

        // Initialize empty image URLs list - will be set later after upload
        product.setImageUrls(new ArrayList<>());

        return product;
    }

    private void showPreviewDialog(Product product) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Xem trước sản phẩm");

        String previewText = String.format(
            "Tiêu đề: %s\n\nMô tả: %s\n\nGiá: %.0f VNĐ\n\nDanh mục: %s\n\nTình trạng: %s\n\nVị trí: %s\n\nCó thể thương lượng: %s",
            product.getTitle(),
            product.getDescription(),
            product.getPrice(),
            product.getCategory(),
            product.getCondition(),
            product.getLocation(),
            product.isNegotiable() ? "Có" : "Không"
        );

        builder.setMessage(previewText);
        builder.setPositiveButton("Đăng ngay", (dialog, which) -> publishListing());
        builder.setNegativeButton("Chỉnh sửa", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (titleEditText.getText().toString().trim().isEmpty()) {
            titleEditText.setError("Vui lòng nhập tiêu đề");
            isValid = false;
        }

        if (descriptionEditText.getText().toString().trim().isEmpty()) {
            descriptionEditText.setError("Vui lòng nhập mô tả");
            isValid = false;
        }

        String priceText = priceEditText.getText().toString().trim();
        if (priceText.isEmpty()) {
            priceEditText.setError("Vui lòng nhập giá");
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(priceText);
                if (price <= 0) {
                    priceEditText.setError("Giá phải lớn hơn 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                priceEditText.setError("Giá không hợp lệ");
                isValid = false;
            }
        }

        if (locationEditText.getText().toString().trim().isEmpty()) {
            locationEditText.setError("Vui lòng nhập vị trí");
            isValid = false;
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng chọn ít nhất 1 hình ảnh", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void clearForm() {
        titleEditText.setText("");
        descriptionEditText.setText("");
        priceEditText.setText("");
        locationEditText.setText("");
        negotiableSwitch.setChecked(false);
        selectedImages.clear();
        tagsChipGroup.removeAllViews();
        categorySpinner.setSelection(0);
        conditionSpinner.setSelection(0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && data != null) {
            selectedImages.clear();

            if (data.getClipData() != null) {
                // Multiple images
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < Math.min(count, 10); i++) {
                    selectedImages.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                // Single image
                selectedImages.add(data.getData());
            }

            // Update images recycler view adapter
            imagePreviewAdapter.updateImages(selectedImages);
            Toast.makeText(getContext(), "Đã chọn " + selectedImages.size() + " hình ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(getContext(), "Cần quyền truy cập vị trí để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
