package com.example.tradeup_app.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Product;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductPreviewDialog {
    public static void show(Context context, Product product) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_product_preview, null);
        dialog.setContentView(view);

        setupViews(view, product, dialog, context, null);
        dialog.show();
    }

    // ✅ NEW: Add method to show preview with local images (URI)
    public static void showWithImages(Context context, Product product, List<Uri> localImages) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_product_preview, null);
        dialog.setContentView(view);

        setupViews(view, product, dialog, context, localImages);
        dialog.show();
    }

    private static void setupViews(View view, Product product, Dialog dialog, Context context, List<Uri> localImages) {
        // Set up close button - only use toolbar navigation button
        com.google.android.material.appbar.MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> dialog.dismiss());
        }

        // Set up basic information
        ((TextView) view.findViewById(R.id.title_text)).setText(product.getTitle());
        ((TextView) view.findViewById(R.id.description_text)).setText(product.getDescription());
        ((TextView) view.findViewById(R.id.location_text)).setText(product.getLocation());
        ((TextView) view.findViewById(R.id.condition_text)).setText(product.getCondition());
        ((TextView) view.findViewById(R.id.category_text)).setText(product.getCategory());

        // Format and set price with VND formatting
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        ((TextView) view.findViewById(R.id.price_text)).setText(formatter.format(product.getPrice()));

        // Set negotiable status using chip instead of text
        com.google.android.material.chip.Chip negotiableChip = view.findViewById(R.id.negotiable_chip);
        if (product.isNegotiable()) {
            negotiableChip.setVisibility(View.VISIBLE);
        } else {
            negotiableChip.setVisibility(View.GONE);
        }

        // ✅ IMPROVED: Set item behavior if available
        TextView behaviorText = view.findViewById(R.id.behavior_text);
        LinearLayout behaviorLayout = view.findViewById(R.id.behavior_layout);
        if (product.getItemBehavior() != null && !product.getItemBehavior().trim().isEmpty()) {
            behaviorText.setText(product.getItemBehavior());
            behaviorLayout.setVisibility(View.VISIBLE);
        } else {
            behaviorLayout.setVisibility(View.GONE);
        }

        // ✅ IMPROVED: Set up tags with proper card visibility
        com.google.android.material.card.MaterialCardView tagsCard = view.findViewById(R.id.tags_card);
        ChipGroup tagsChipGroup = view.findViewById(R.id.tags_chip_group);
        if (product.getTags() != null && !product.getTags().isEmpty()) {
            tagsChipGroup.removeAllViews();
            for (String tag : product.getTags()) {
                Chip chip = new Chip(context);
                chip.setText(tag);
                chip.setClickable(false);
                chip.setCheckable(false);
                tagsChipGroup.addView(chip);
            }
            tagsCard.setVisibility(View.VISIBLE);
        } else {
            tagsCard.setVisibility(View.GONE);
        }

        // ✅ IMPROVED: Set up image preview with local images support and counter
        ViewPager2 imageViewPager = view.findViewById(R.id.image_view_pager);
        TextView imageCounter = view.findViewById(R.id.image_counter);

        if (localImages != null && !localImages.isEmpty()) {
            // Show local images for preview (before publishing)
            LocalImagePreviewAdapter adapter = new LocalImagePreviewAdapter(localImages);
            imageViewPager.setAdapter(adapter);

            // Update counter
            imageCounter.setText("1 / " + localImages.size());

            // Add page change listener for counter
            imageViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    imageCounter.setText((position + 1) + " / " + localImages.size());
                }
            });

        } else if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            // Show uploaded images (after publishing)
            ImagePreviewAdapter adapter = new ImagePreviewAdapter(product.getImageUrls());
            imageViewPager.setAdapter(adapter);

            // Update counter
            imageCounter.setText("1 / " + product.getImageUrls().size());

            // Add page change listener for counter
            imageViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    imageCounter.setText((position + 1) + " / " + product.getImageUrls().size());
                }
            });

        } else {
            // No images available - hide image section
            imageViewPager.setVisibility(View.GONE);
            imageCounter.setVisibility(View.GONE);
        }
    }

    // ✅ NEW: Adapter for local images (URI)
    private static class LocalImagePreviewAdapter extends RecyclerView.Adapter<LocalImagePreviewAdapter.LocalImageViewHolder> {
        private final List<Uri> imageUris;

        LocalImagePreviewAdapter(List<Uri> imageUris) {
            this.imageUris = imageUris;
        }

        @NonNull
        @Override
        public LocalImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new LocalImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull LocalImageViewHolder holder, int position) {
            Glide.with(holder.imageView)
                .load(imageUris.get(position))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUris.size();
        }

        static class LocalImageViewHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;

            LocalImageViewHolder(@NonNull ImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }

    private static class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {
        private final List<String> imageUrls;

        ImagePreviewAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Glide.with(holder.imageView)
                .load(imageUrls.get(position))
                .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;

            ImageViewHolder(@NonNull ImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }
}
