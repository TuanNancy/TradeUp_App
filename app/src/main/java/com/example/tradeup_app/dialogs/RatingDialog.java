package com.example.tradeup_app.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import de.hdodenhof.circleimageview.CircleImageView;

public class RatingDialog extends Dialog {

    public interface OnRatingSubmitListener {
        void onRatingSubmit(int stars, String review);
        void onSkip();
    }

    private OnRatingSubmitListener listener;

    private CircleImageView userProfileImage;
    private TextView userNameText;
    private ImageView[] stars;
    private TextInputEditText reviewEditText;
    private MaterialButton skipButton;
    private MaterialButton submitRatingButton;

    private int selectedStars = 0;

    public RatingDialog(@NonNull Context context, String userName, String userProfileUrl, OnRatingSubmitListener listener) {
        super(context);
        this.listener = listener;

        initDialog(userName, userProfileUrl);
    }

    private void initDialog(String userName, String userProfileUrl) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rating, null);
        setContentView(view);

        initViews(view);
        setupViews(userName, userProfileUrl);
        setupListeners();
    }

    private void initViews(View view) {
        userProfileImage = view.findViewById(R.id.userProfileImage);
        userNameText = view.findViewById(R.id.userNameText);
        reviewEditText = view.findViewById(R.id.reviewEditText);
        skipButton = view.findViewById(R.id.skipButton);
        submitRatingButton = view.findViewById(R.id.submitRatingButton);

        // Initialize star array
        stars = new ImageView[5];
        stars[0] = view.findViewById(R.id.star1);
        stars[1] = view.findViewById(R.id.star2);
        stars[2] = view.findViewById(R.id.star3);
        stars[3] = view.findViewById(R.id.star4);
        stars[4] = view.findViewById(R.id.star5);
    }

    private void setupViews(String userName, String userProfileUrl) {
        userNameText.setText(userName);

        // Load user profile image
        if (userProfileUrl != null && !userProfileUrl.isEmpty()) {
            Glide.with(getContext())
                .load(userProfileUrl)
                .placeholder(R.drawable.ic_user_placeholder)
                .into(userProfileImage);
        }

        // Disable submit button initially
        submitRatingButton.setEnabled(false);

        // Setup stars
        updateStarDisplay();
    }

    private void setupListeners() {
        skipButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSkip();
            }
            dismiss();
        });

        submitRatingButton.setOnClickListener(v -> {
            if (selectedStars > 0) {
                String review = reviewEditText.getText() != null ?
                    reviewEditText.getText().toString().trim() : "";

                if (listener != null) {
                    listener.onRatingSubmit(selectedStars, review);
                }
                dismiss();
            }
        });

        // Setup star click listeners
        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedStars = starIndex;
                updateStarDisplay();
                submitRatingButton.setEnabled(true);
            });
        }
    }

    private void updateStarDisplay() {
        for (int i = 0; i < stars.length; i++) {
            if (i < selectedStars) {
                // Filled star
                stars[i].setColorFilter(ContextCompat.getColor(getContext(), R.color.star_filled));
            } else {
                // Empty star
                stars[i].setColorFilter(ContextCompat.getColor(getContext(), R.color.star_empty));
            }
        }
    }
}
