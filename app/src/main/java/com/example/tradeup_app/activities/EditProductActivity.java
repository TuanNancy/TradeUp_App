package com.example.tradeup_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.FirebaseDatabase;

public class EditProductActivity extends AppCompatActivity {

    private Product product;

    // UI components
    private TextInputEditText titleEditText, descriptionEditText, priceEditText, locationEditText;
    private ChipGroup conditionChipGroup;
    private Chip conditionNewChip, conditionLikeNewChip, conditionGoodChip, conditionFairChip, conditionPoorChip;
    private MaterialButton saveButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        // Get product from intent
        if (getIntent().hasExtra("product")) {
            product = (Product) getIntent().getSerializableExtra("product");
        }

        if (product == null) {
            Toast.makeText(this, "Không thể tải thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        populateFields();
        setupClickListeners();
    }

    private void initViews() {
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        priceEditText = findViewById(R.id.priceEditText);
        locationEditText = findViewById(R.id.locationEditText);

        conditionChipGroup = findViewById(R.id.conditionChipGroup);
        conditionNewChip = findViewById(R.id.conditionNewChip);
        conditionLikeNewChip = findViewById(R.id.conditionLikeNewChip);
        conditionGoodChip = findViewById(R.id.conditionGoodChip);
        conditionFairChip = findViewById(R.id.conditionFairChip);
        conditionPoorChip = findViewById(R.id.conditionPoorChip);

        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateFields() {
        // Fill in current product data
        titleEditText.setText(product.getTitle());
        descriptionEditText.setText(product.getDescription());
        priceEditText.setText(String.valueOf((long) product.getPrice()));
        locationEditText.setText(product.getLocation());

        // Set condition chip
        selectConditionChip(product.getCondition());
    }

    private void selectConditionChip(String condition) {
        // Clear all selections first
        conditionChipGroup.clearCheck();

        // Select appropriate chip based on condition
        switch (condition) {
            case "Mới":
                conditionNewChip.setChecked(true);
                break;
            case "Như mới":
                conditionLikeNewChip.setChecked(true);
                break;
            case "Tốt":
                conditionGoodChip.setChecked(true);
                break;
            case "Khá":
                conditionFairChip.setChecked(true);
                break;
            case "Cũ":
                conditionPoorChip.setChecked(true);
                break;
            default:
                conditionGoodChip.setChecked(true); // Default selection
                break;
        }
    }

    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> saveChanges());
        cancelButton.setOnClickListener(v -> finish());

        // Location picker (for now just editable text, you can enhance this later)
        locationEditText.setOnClickListener(v -> {
            // Enable editing for location
            locationEditText.setFocusable(true);
            locationEditText.setFocusableInTouchMode(true);
            locationEditText.requestFocus();
        });
    }

    private void saveChanges() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get updated values
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String priceText = priceEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String condition = getSelectedCondition();

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        saveButton.setEnabled(false);
        saveButton.setText("Đang lưu...");

        // Create a copy of product with updated values (don't modify original until Firebase confirms)
        Product updatedProduct = new Product();
        updatedProduct.setId(product.getId());
        updatedProduct.setTitle(title);
        updatedProduct.setDescription(description);
        updatedProduct.setPrice(price);
        updatedProduct.setCondition(condition);
        updatedProduct.setLocation(location);
        updatedProduct.setUpdatedAt(System.currentTimeMillis());

        // Copy other fields from original product
        updatedProduct.setCategory(product.getCategory());
        updatedProduct.setSellerId(product.getSellerId());
        updatedProduct.setSellerName(product.getSellerName());
        updatedProduct.setImageUrls(product.getImageUrls());
        updatedProduct.setTags(product.getTags());
        updatedProduct.setCreatedAt(product.getCreatedAt());
        updatedProduct.setStatus(product.getStatus());
        updatedProduct.setViewCount(product.getViewCount());
        updatedProduct.setLikeCount(product.getLikeCount());
        updatedProduct.setLatitude(product.getLatitude());
        updatedProduct.setLongitude(product.getLongitude());
        updatedProduct.setNegotiable(product.isNegotiable());

        // Update in Firebase FIRST, only update local object if successful
        FirebaseDatabase.getInstance()
                .getReference("products") // Sửa từ "Products" thành "products" để khớp với Constants.PRODUCTS_NODE
                .child(product.getId())
                .setValue(updatedProduct)
                .addOnSuccessListener(aVoid -> {
                    // Only NOW update the original product object after Firebase confirms success
                    // CHỈ CẬP NHẬT CÁC FIELD ĐƯỢC PHÉP EDIT
                    product.setTitle(title);
                    product.setDescription(description);
                    product.setPrice(price);
                    product.setCondition(condition);
                    product.setLocation(location);
                    product.setUpdatedAt(updatedProduct.getUpdatedAt());

                    Toast.makeText(this, "Cập nhật sản phẩm thành công!", Toast.LENGTH_SHORT).show();

                    // Return updated product to calling activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated_product", product);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveButton.setEnabled(true);
                    saveButton.setText("Lưu thay đổi");
                    // Original product object remains unchanged if Firebase update fails
                });
    }

    private boolean validateInputs() {
        // Validate title
        String title = titleEditText.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            titleEditText.setError("Vui lòng nhập tiêu đề");
            titleEditText.requestFocus();
            return false;
        }
        if (title.length() < 3) {
            titleEditText.setError("Tiêu đề phải có ít nhất 3 ký tự");
            titleEditText.requestFocus();
            return false;
        }

        // Validate description
        String description = descriptionEditText.getText().toString().trim();
        if (TextUtils.isEmpty(description)) {
            descriptionEditText.setError("Vui lòng nhập mô tả");
            descriptionEditText.requestFocus();
            return false;
        }
        if (description.length() < 10) {
            descriptionEditText.setError("Mô tả phải có ít nhất 10 ký tự");
            descriptionEditText.requestFocus();
            return false;
        }

        // Validate price
        String priceText = priceEditText.getText().toString().trim();
        if (TextUtils.isEmpty(priceText)) {
            priceEditText.setError("Vui lòng nhập giá");
            priceEditText.requestFocus();
            return false;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                priceEditText.setError("Giá phải lớn hơn 0");
                priceEditText.requestFocus();
                return false;
            }
            if (price > 1000000000) { // 1 billion VND
                priceEditText.setError("Giá quá cao");
                priceEditText.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            priceEditText.setError("Giá không hợp lệ");
            priceEditText.requestFocus();
            return false;
        }

        // Validate location
        String location = locationEditText.getText().toString().trim();
        if (TextUtils.isEmpty(location)) {
            locationEditText.setError("Vui lòng nhập địa điểm");
            locationEditText.requestFocus();
            return false;
        }

        // Validate condition selection
        if (conditionChipGroup.getCheckedChipId() == -1) {
            Toast.makeText(this, "Vui lòng chọn tình trạng sản phẩm", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String getSelectedCondition() {
        int checkedId = conditionChipGroup.getCheckedChipId();
        if (checkedId == R.id.conditionNewChip) {
            return "Mới";
        } else if (checkedId == R.id.conditionLikeNewChip) {
            return "Như mới";
        } else if (checkedId == R.id.conditionGoodChip) {
            return "Tốt";
        } else if (checkedId == R.id.conditionFairChip) {
            return "Khá";
        } else if (checkedId == R.id.conditionPoorChip) {
            return "Cũ";
        }
        return "Tốt"; // Default
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
