package com.example.tradeup_app.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DecimalFormat;

/**
 * Dialog for sending offers in chat
 */
public class ChatOfferDialog extends Dialog {

    public interface OnOfferSubmitListener {
        void onOfferSubmit(double offerPrice, String message);
    }

    private Product product;
    private OnOfferSubmitListener listener;

    private ImageView productImageView;
    private TextView productTitleText;
    private TextView originalPriceText;
    private TextView savingsText;
    private TextInputEditText offerPriceEditText;
    private TextInputEditText offerMessageEditText;
    private TextInputLayout offerPriceInputLayout;
    private MaterialButton cancelButton;
    private MaterialButton sendOfferButton;

    public ChatOfferDialog(@NonNull Context context, Product product, OnOfferSubmitListener listener) {
        super(context);
        this.product = product;
        this.listener = listener;

        initDialog();
    }

    private void initDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_chat_offer, null);
        setContentView(view);

        initViews(view);
        setupProduct();
        setupListeners();
        setupValidation();

        // Configure dialog
        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    private void initViews(View view) {
        productImageView = view.findViewById(R.id.productImageView);
        productTitleText = view.findViewById(R.id.productTitleText);
        originalPriceText = view.findViewById(R.id.originalPriceText);
        savingsText = view.findViewById(R.id.savingsText);
        offerPriceEditText = view.findViewById(R.id.offerPriceEditText);
        offerMessageEditText = view.findViewById(R.id.offerMessageEditText);
        offerPriceInputLayout = view.findViewById(R.id.offerPriceInputLayout);
        cancelButton = view.findViewById(R.id.cancelButton);
        sendOfferButton = view.findViewById(R.id.sendOfferButton);
    }

    private void setupProduct() {
        if (product == null) return;

        // Set product title
        productTitleText.setText(product.getTitle());

        // Set original price
        originalPriceText.setText(formatPrice(product.getPrice()));

        // Load product image
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            Glide.with(getContext())
                    .load(product.getImageUrls().get(0))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(productImageView);
        }

        // Set default offer price (90% of original price)
        double suggestedPrice = product.getPrice() * 0.9;
        offerPriceEditText.setText(String.valueOf((int) suggestedPrice));
        updateSavings();
    }

    private void setupListeners() {
        cancelButton.setOnClickListener(v -> dismiss());

        sendOfferButton.setOnClickListener(v -> {
            if (validateAndSubmitOffer()) {
                dismiss();
            }
        });

        // Real-time price validation and savings calculation
        offerPriceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateOfferPrice();
                updateSavings();
            }
        });
    }

    private void setupValidation() {
        sendOfferButton.setEnabled(false);
        validateOfferPrice();
    }

    private boolean validateOfferPrice() {
        String priceText = offerPriceEditText.getText().toString().trim();

        if (priceText.isEmpty()) {
            offerPriceInputLayout.setError("Please enter offer price");
            sendOfferButton.setEnabled(false);
            return false;
        }

        try {
            double offerPrice = Double.parseDouble(priceText);

            if (offerPrice <= 0) {
                offerPriceInputLayout.setError("Price must be greater than 0");
                sendOfferButton.setEnabled(false);
                return false;
            }

            if (offerPrice >= product.getPrice()) {
                offerPriceInputLayout.setError("Offer must be less than original price");
                sendOfferButton.setEnabled(false);
                return false;
            }

            // Clear error and enable button
            offerPriceInputLayout.setError(null);
            sendOfferButton.setEnabled(true);
            return true;

        } catch (NumberFormatException e) {
            offerPriceInputLayout.setError("Invalid price format");
            sendOfferButton.setEnabled(false);
            return false;
        }
    }

    private void updateSavings() {
        String priceText = offerPriceEditText.getText().toString().trim();

        if (!priceText.isEmpty()) {
            try {
                double offerPrice = Double.parseDouble(priceText);
                double savings = product.getPrice() - offerPrice;
                double savingsPercent = (savings / product.getPrice()) * 100;

                if (savings > 0) {
                    savingsText.setText(String.format("Save %s (%.1f%%)",
                            formatPrice(savings), savingsPercent));
                    savingsText.setVisibility(View.VISIBLE);
                } else {
                    savingsText.setVisibility(View.GONE);
                }
            } catch (NumberFormatException e) {
                savingsText.setVisibility(View.GONE);
            }
        } else {
            savingsText.setVisibility(View.GONE);
        }
    }

    private boolean validateAndSubmitOffer() {
        if (!validateOfferPrice()) {
            return false;
        }

        try {
            double offerPrice = Double.parseDouble(offerPriceEditText.getText().toString().trim());
            String message = offerMessageEditText.getText().toString().trim();

            // Minimum offer validation (at least 50% of original price)
            if (offerPrice < product.getPrice() * 0.5) {
                Toast.makeText(getContext(), "Offer too low. Minimum 50% of original price.", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Submit offer
            if (listener != null) {
                listener.onOfferSubmit(offerPrice, message);
            }

            return true;

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid price format", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private String formatPrice(double price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(price) + " VNĐ";
    }
}
