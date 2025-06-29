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

public class MakeOfferDialog extends Dialog {

    public interface OnOfferSubmitListener {
        void onOfferSubmit(double offerPrice, String message);
    }

    private Product product;
    private OnOfferSubmitListener listener;

    private ImageView productImageView;
    private TextView productTitleText;
    private TextView originalPriceText;
    private TextInputEditText offerPriceEditText;
    private TextInputEditText offerMessageEditText;
    private TextInputLayout offerPriceInputLayout;
    private MaterialButton cancelButton;
    private MaterialButton sendOfferButton;

    public MakeOfferDialog(@NonNull Context context, Product product, OnOfferSubmitListener listener) {
        super(context);
        this.product = product;
        this.listener = listener;

        initDialog();
    }

    private void initDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_make_offer, null);
        setContentView(view);

        initViews(view);
        setupViews();
        setupListeners();
    }

    private void initViews(View view) {
        productImageView = view.findViewById(R.id.productImageView);
        productTitleText = view.findViewById(R.id.productTitleText);
        originalPriceText = view.findViewById(R.id.originalPriceText);
        offerPriceEditText = view.findViewById(R.id.offerPriceEditText);
        offerMessageEditText = view.findViewById(R.id.offerMessageEditText);
        offerPriceInputLayout = view.findViewById(R.id.offerPriceInputLayout);
        cancelButton = view.findViewById(R.id.cancelButton);
        sendOfferButton = view.findViewById(R.id.sendOfferButton);
    }

    private void setupViews() {
        // Set product info
        productTitleText.setText(product.getTitle());
        originalPriceText.setText(String.format("$%.2f", product.getPrice()));

        // Load product image
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            Glide.with(getContext())
                .load(product.getImageUrls().get(0))
                .placeholder(R.drawable.placeholder_image)
                .into(productImageView);
        }

        // Disable send button initially
        sendOfferButton.setEnabled(false);
    }

    private void setupListeners() {
        cancelButton.setOnClickListener(v -> dismiss());

        sendOfferButton.setOnClickListener(v -> {
            if (validateInput()) {
                double offerPrice = Double.parseDouble(offerPriceEditText.getText().toString());
                String message = offerMessageEditText.getText() != null ?
                    offerMessageEditText.getText().toString().trim() : "";

                if (listener != null) {
                    listener.onOfferSubmit(offerPrice, message);
                }
                dismiss();
            }
        });

        // Validate input as user types
        offerPriceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInput();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean validateInput() {
        String priceText = offerPriceEditText.getText() != null ?
            offerPriceEditText.getText().toString().trim() : "";

        if (priceText.isEmpty()) {
            offerPriceInputLayout.setError(null);
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
                offerPriceInputLayout.setError("Offer should be less than original price");
                sendOfferButton.setEnabled(false);
                return false;
            }

            offerPriceInputLayout.setError(null);
            sendOfferButton.setEnabled(true);
            return true;

        } catch (NumberFormatException e) {
            offerPriceInputLayout.setError("Invalid price format");
            sendOfferButton.setEnabled(false);
            return false;
        }
    }
}
