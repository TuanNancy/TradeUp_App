package com.example.tradeup_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Transaction;
import com.example.tradeup_app.services.StripePaymentService;
import com.example.tradeup_app.utils.NotificationManager;
import com.example.tradeup_app.utils.VNDPriceFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentMethodCreateParams;

import java.text.DecimalFormat;

public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";

    // UI Components
    private Toolbar toolbar;
    private ImageView productImage;
    private TextView productTitle, productPrice, totalAmount;
    private MaterialCardView paymentMethodCard;
    private TextInputLayout cardNumberLayout, expiryLayout, cvcLayout;
    private TextInputEditText cardNumberEdit, expiryEdit, cvcEdit;
    private MaterialButton payNowButton;
    private ProgressBar progressBar;

    // Data
    private Product product;
    private StripePaymentService stripeService;
    private FirebaseManager firebaseManager;
    private String currentUserId;
    private DecimalFormat priceFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initializeComponents();
        setupData();
        setupEventListeners();
    }

    private void initializeComponents() {
        toolbar = findViewById(R.id.toolbar);
        productImage = findViewById(R.id.productImage);
        productTitle = findViewById(R.id.productTitle);
        productPrice = findViewById(R.id.productPrice);
        totalAmount = findViewById(R.id.totalAmount);
        paymentMethodCard = findViewById(R.id.paymentMethodCard);
        cardNumberLayout = findViewById(R.id.cardNumberLayout);
        expiryLayout = findViewById(R.id.expiryLayout);
        cvcLayout = findViewById(R.id.cvcLayout);
        cardNumberEdit = findViewById(R.id.cardNumberEdit);
        expiryEdit = findViewById(R.id.expiryEdit);
        cvcEdit = findViewById(R.id.cvcEdit);
        payNowButton = findViewById(R.id.payNowButton);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thanh toán");
        }

        stripeService = new StripePaymentService(this);
        firebaseManager = FirebaseManager.getInstance();

        currentUserId = getCurrentUserId();
        Log.d(TAG, "Current User ID: " + currentUserId);

        priceFormat = new DecimalFormat("#,###.##");
    }

    private String getCurrentUserId() {
        // Kiểm tra CurrentUser trước
        UserModel currentUser = CurrentUser.getUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            if (uid != null && !uid.trim().isEmpty()) {
                Log.d(TAG, "Got user ID from CurrentUser: " + uid);
                return uid;
            }
        }

        // Fallback đến Firebase Auth nếu CurrentUser không có thông tin
        try {
            com.google.firebase.auth.FirebaseAuth firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
            com.google.firebase.auth.FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                String uid = firebaseUser.getUid();
                Log.d(TAG, "Got user ID from Firebase Auth: " + uid);
                return uid;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting Firebase user", e);
        }

        Log.w(TAG, "No valid user found in CurrentUser or Firebase Auth");
        return null;
    }

    private void setupData() {
        // Get product from intent
        product = (Product) getIntent().getSerializableExtra("product");
        if (product == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Validate purchase conditions
        if (!validatePurchaseConditions()) {
            return;
        }

        // Display product information
        displayProductInfo();
    }

    private boolean validatePurchaseConditions() {
        // Check if user is logged in
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để mua hàng", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        // Check if product is available
        if (!"Available".equals(product.getStatus())) {
            Toast.makeText(this, "Sản phẩm này không còn khả dụng", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        // Check if user is not the seller
        if (currentUserId.equals(product.getSellerId())) {
            Toast.makeText(this, "Bạn không thể mua sản phẩm của chính mình", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        return true;
    }

    private void displayProductInfo() {
        productTitle.setText(product.getTitle());
        productPrice.setText(VNDPriceFormatter.formatVND(product.getPrice()));
        totalAmount.setText(VNDPriceFormatter.formatVND(product.getPrice()));

        // Load product image
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            Glide.with(this)
                    .load(product.getImageUrls().get(0))
                    .centerCrop()
                    .into(productImage);
        }
    }

    private void setupEventListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());

        // Card number formatting
        cardNumberEdit.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                String text = s.toString().replaceAll("\\s+", "");
                StringBuilder formatted = new StringBuilder();

                for (int i = 0; i < text.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(text.charAt(i));
                }

                s.replace(0, s.length(), formatted.toString());
                isFormatting = false;
                validateForm();
            }
        });

        // Expiry date formatting (MM/YY)
        expiryEdit.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                String input = s.toString();
                String digitsOnly = input.replaceAll("[^0-9]", "");

                String formatted = "";
                if (digitsOnly.length() > 0) {
                    formatted = digitsOnly.substring(0, Math.min(2, digitsOnly.length()));
                    if (digitsOnly.length() >= 3) {
                        formatted += "/" + digitsOnly.substring(2, Math.min(4, digitsOnly.length()));
                    }
                }

                if (!formatted.equals(input)) {
                    s.replace(0, s.length(), formatted);
                }

                isFormatting = false;
                expiryEdit.post(() -> validateForm());
            }
        });

        cvcEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateForm();
            }
        });

        payNowButton.setOnClickListener(v -> processPayment());
    }

    private void validateForm() {
        String cardNumber = cardNumberEdit.getText() != null ? cardNumberEdit.getText().toString().replaceAll("\\s+", "") : "";
        String expiry = expiryEdit.getText() != null ? expiryEdit.getText().toString() : "";
        String cvc = cvcEdit.getText() != null ? cvcEdit.getText().toString() : "";

        boolean isValidCard = StripePaymentService.PaymentValidation.isValidCardNumber(cardNumber);
        boolean isValidExpiry = isValidExpiryDate(expiry);
        boolean isValidCvc = StripePaymentService.PaymentValidation.isValidCvc(cvc);

        boolean isValid = isValidCard && isValidExpiry && isValidCvc;
        payNowButton.setEnabled(isValid);

        // Update UI to show validation errors
        updateValidationUI(cardNumber, expiry, cvc);
    }

    private boolean isValidExpiryDate(String expiry) {
        if (expiry == null || expiry.length() != 5 || !expiry.contains("/")) {
            return false;
        }

        String[] parts = expiry.split("/");
        if (parts.length != 2) {
            return false;
        }

        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]) + 2000; // Convert YY to YYYY

            // Kiểm tra tháng hợp lệ (1-12)
            boolean isValidMonth = StripePaymentService.PaymentValidation.isValidExpiryMonth(month);

            // Lấy thời gian hiện tại
            java.util.Calendar now = java.util.Calendar.getInstance();
            int currentYear = now.get(java.util.Calendar.YEAR);
            int currentMonth = now.get(java.util.Calendar.MONTH) + 1;

            // Kiểm tra ngày hết hạn
            boolean isValidYear = year > currentYear || (year == currentYear && month >= currentMonth);

            return isValidMonth && isValidYear;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing expiry date: " + expiry, e);
            return false;
        }
    }

    private void updateValidationUI(String cardNumber, String expiry, String cvc) {
        // Clear previous errors
        cardNumberLayout.setError(null);
        expiryLayout.setError(null);
        cvcLayout.setError(null);

        // Validate card number
        if (!cardNumber.isEmpty() && !StripePaymentService.PaymentValidation.isValidCardNumber(cardNumber)) {
            cardNumberLayout.setError("Số thẻ không hợp lệ");
        }

        // Validate expiry date
        if (!expiry.isEmpty() && !isValidExpiryDate(expiry)) {
            if (expiry.length() < 5) {
                expiryLayout.setError("Nhập đầy đủ MM/YY");
            } else {
                String[] parts = expiry.split("/");
                if (parts.length == 2) {
                    try {
                        int month = Integer.parseInt(parts[0]);
                        int year = Integer.parseInt(parts[1]) + 2000;

                        if (!StripePaymentService.PaymentValidation.isValidExpiryMonth(month)) {
                            expiryLayout.setError("Tháng phải từ 01-12");
                        } else if (!StripePaymentService.PaymentValidation.isValidExpiryYear(year)) {
                            expiryLayout.setError("Thẻ đã hết hạn hoặc năm không hợp lệ");
                        }
                    } catch (NumberFormatException e) {
                        expiryLayout.setError("Định dạng không hợp lệ");
                    }
                } else {
                    expiryLayout.setError("Định dạng phải là MM/YY");
                }
            }
        }

        // Validate CVC
        if (!cvc.isEmpty() && !StripePaymentService.PaymentValidation.isValidCvc(cvc)) {
            cvcLayout.setError("CVC phải có 3-4 số");
        }
    }

    private void processPayment() {
        setLoadingState(true);

        String cardNumber = cardNumberEdit.getText().toString().replaceAll("\\s+", "");
        String expiry = expiryEdit.getText().toString();
        String cvc = cvcEdit.getText().toString();

        // Parse expiry date
        String[] expiryParts = expiry.split("/");
        int expMonth = Integer.parseInt(expiryParts[0]);
        int expYear = 2000 + Integer.parseInt(expiryParts[1]);

        // Validate expiry date
        if (!StripePaymentService.PaymentValidation.isValidExpiryMonth(expMonth) ||
            !StripePaymentService.PaymentValidation.isValidExpiryYear(expYear)) {
            Toast.makeText(this, "Ngày hết hạn không hợp lệ", Toast.LENGTH_SHORT).show();
            setLoadingState(false);
            return;
        }

        Log.d(TAG, "Creating PaymentIntent for product: " + product.getTitle() + ", amount: " + product.getPrice());

        // Create payment intent
        stripeService.createPaymentIntent(product, currentUserId, new StripePaymentService.PaymentIntentCallback() {
            @Override
            public void onSuccess(String clientSecret, String paymentIntentId) {
                Log.d(TAG, "PaymentIntent created successfully: " + paymentIntentId);
                runOnUiThread(() -> confirmPayment(clientSecret, paymentIntentId, cardNumber, expMonth, expYear, cvc));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to create PaymentIntent: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(PaymentActivity.this, "Lỗi tạo thanh toán: " + error, Toast.LENGTH_SHORT).show();
                    setLoadingState(false);
                });
            }
        });
    }

    private void confirmPayment(String clientSecret, String paymentIntentId, String cardNumber,
                               int expMonth, int expYear, String cvc) {

        Log.d(TAG, "Confirming payment with clientSecret: " + clientSecret);

        // Create PaymentMethodCreateParams using Stripe SDK
        PaymentMethodCreateParams paymentMethodParams = stripeService.createCardPaymentMethod(
                cardNumber, expMonth, expYear, cvc);

        stripeService.confirmPayment(clientSecret, paymentMethodParams,
                new StripePaymentService.PaymentConfirmationCallback() {
            @Override
            public void onSuccess(PaymentIntent paymentIntent) {
                Log.d(TAG, "Payment confirmed successfully");
                runOnUiThread(() -> handlePaymentSuccess(paymentIntent, paymentIntentId));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Payment confirmation failed: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(PaymentActivity.this, "Thanh toán thất bại: " + error, Toast.LENGTH_SHORT).show();
                    setLoadingState(false);
                });
            }
        });
    }

    private void handlePaymentSuccess(PaymentIntent paymentIntent, String paymentIntentId) {
        // Extract payment intent ID
        String finalPaymentIntentId = paymentIntentId;
        if (paymentIntent != null && paymentIntent.getId() != null) {
            finalPaymentIntentId = paymentIntent.getId();
            Log.d(TAG, "Using real PaymentIntent ID: " + finalPaymentIntentId);
        } else {
            Log.d(TAG, "Using fallback PaymentIntent ID: " + finalPaymentIntentId);
        }

        // Create transaction record with detailed payment information
        Transaction transaction = new Transaction(
                product.getId(),
                product.getTitle(),
                currentUserId,
                CurrentUser.getUser() != null ? CurrentUser.getUser().getUsername() : "Unknown",
                product.getSellerId(),
                product.getSellerName(),
                product.getPrice()
        );

        transaction.setStatus("PAID");
        transaction.setPaymentStatus("SUCCEEDED");
        transaction.setStripePaymentIntentId(finalPaymentIntentId);
        transaction.setPaymentMethod("card");
        transaction.setPaidAt(System.currentTimeMillis());

        // Add payment details for better tracking
        if (paymentIntent != null) {
            Log.d(TAG, "PaymentIntent status: " + paymentIntent.getStatus());
            Log.d(TAG, "PaymentIntent amount: " + paymentIntent.getAmount());
        }

        // Save transaction and update product status
        saveTransactionAndUpdateProduct(transaction);
    }

    private void saveTransactionAndUpdateProduct(Transaction transaction) {
        Log.d(TAG, "Processing payment success for product: " + transaction.getProductTitle());

        // Use the new comprehensive payment success handler
        firebaseManager.handlePaymentSuccess(transaction, new FirebaseManager.OnPaymentSuccessListener() {
            @Override
            public void onSuccess(String transactionId) {
                Log.d(TAG, "Payment success handling completed: " + transactionId);

                runOnUiThread(() -> {
                    setLoadingState(false);
                    showPaymentSuccessMessage();

                    // Return to previous activity with success result
                    Intent intent = new Intent();
                    intent.putExtra("payment_success", true);
                    intent.putExtra("transaction_id", transactionId);
                    intent.putExtra("product_id", product.getId());
                    setResult(RESULT_OK, intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Payment success handling failed: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(PaymentActivity.this, "Lỗi xử lý thanh toán: " + error, Toast.LENGTH_SHORT).show();
                    setLoadingState(false);
                });
            }
        });
    }

    private void showPaymentSuccessMessage() {
        Toast.makeText(this, "Thanh toán thành công! Bạn đã mua " + product.getTitle(), Toast.LENGTH_LONG).show();
    }

    private void sendPurchaseNotification() {
        try {
            NotificationManager notificationManager = NotificationManager.getInstance(this);
            notificationManager.sendListingUpdateNotification(
                    product.getId(),
                    product.getTitle(),
                    "purchased",
                    product.getSellerId()
            );
            Log.d(TAG, "Purchase notification sent to seller");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send purchase notification", e);
        }
    }

    private void setLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        payNowButton.setEnabled(!loading);
        cardNumberEdit.setEnabled(!loading);
        expiryEdit.setEnabled(!loading);
        cvcEdit.setEnabled(!loading);

        if (loading) {
            payNowButton.setText("Đang xử lý...");
        } else {
            payNowButton.setText("Thanh toán ngay");
        }
    }
}
