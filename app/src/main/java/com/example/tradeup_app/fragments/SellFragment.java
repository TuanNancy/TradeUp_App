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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

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
import com.example.tradeup_app.adapters.ImagePreviewAdapter;

public class SellFragment extends Fragment {

    private static final int PICK_IMAGES_REQUEST = 1001;
    private static final int LOCATION_PERMISSION_REQUEST = 1002;

    private EditText titleEditText, descriptionEditText, priceEditText, locationEditText, behaviorEditText, addTagEditText;
    private Spinner categorySpinner, conditionSpinner;
    private MaterialSwitch negotiableSwitch;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sell, container, false);

        initViews(view);
        setupSpinners();
        setupLocationFeatures();
        setupImageUpload();
        setupTagSystem();
        setupPreviewAndPublish();

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
        addImagesButton = view.findViewById(R.id.add_images_button);
        previewButton = view.findViewById(R.id.preview_button);
        publishButton = view.findViewById(R.id.publish_button);
        locationGpsButton = view.findViewById(R.id.location_gps_button);
        behaviorEditText = view.findViewById(R.id.behavior_edit_text);
        addTagEditText = view.findViewById(R.id.add_tag_edit_text);

        firebaseManager = FirebaseManager.getInstance();

        // Setup image preview recycler view
        setupImagePreviewRecyclerView();
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
            Toast.makeText(requireContext(), "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();

            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // Successfully got location
                        handleLocationSuccess(location);
                    } else {
                        // Last location is null, request fresh location
                        requestFreshLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    locationGpsButton.setEnabled(true);
                    Toast.makeText(requireContext(), "Lỗi lấy vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void handleLocationSuccess(Location location) {
        LocationUtils.getAddressFromLocation(requireContext(), location.getLatitude(), location.getLongitude(),
            address -> {
                locationEditText.setText(address);
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                locationGpsButton.setEnabled(true);
                Toast.makeText(requireContext(), "Đã lấy vị trí thành công", Toast.LENGTH_SHORT).show();
            });
    }

    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000)
                .setNumUpdates(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    handleLocationSuccess(location);
                    // Stop location updates
                    fusedLocationClient.removeLocationUpdates(this);
                } else {
                    locationGpsButton.setEnabled(true);
                    Toast.makeText(requireContext(), "Không thể lấy vị trí. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show();
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnFailureListener(e -> {
                    locationGpsButton.setEnabled(true);
                    Toast.makeText(requireContext(), "Lỗi yêu cầu vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private void setupImageUpload() {
        imagePreviewAdapter = new ImagePreviewAdapter(getContext(), selectedImages, uri -> {
            selectedImages.remove(uri);
            imagePreviewAdapter.notifyDataSetChanged();
            updateImageButtonState();
        });

        imagesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        imagesRecyclerView.setAdapter(imagePreviewAdapter);

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
        if (locationEditText.getText().toString().trim().isEmpty()) {
            locationEditText.setError("Vui lòng nhập địa chỉ");
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
        product.setPrice(Double.parseDouble(priceEditText.getText().toString().trim()));
        product.setCategory(categorySpinner.getSelectedItem().toString());
        product.setCondition(conditionSpinner.getSelectedItem().toString());
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
        categorySpinner.setSelection(0);
        conditionSpinner.setSelection(0);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up location callback to prevent memory leaks
        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
