package com.example.tradeup_app.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        setupViews(view, product, dialog, context);
        dialog.show();
    }

    private static void setupViews(View view, Product product, Dialog dialog, Context context) {
        // Set up close button
        view.findViewById(R.id.close_button).setOnClickListener(v -> dialog.dismiss());

        // Set up basic information
        ((TextView) view.findViewById(R.id.title_text)).setText(product.getTitle());
        ((TextView) view.findViewById(R.id.description_text)).setText(product.getDescription());
        ((TextView) view.findViewById(R.id.location_text)).setText(product.getLocation());
        ((TextView) view.findViewById(R.id.condition_text)).setText(product.getCondition());
        ((TextView) view.findViewById(R.id.category_text)).setText(product.getCategory());

        // Format and set price
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        ((TextView) view.findViewById(R.id.price_text)).setText(formatter.format(product.getPrice()));

        // Set negotiable status
        TextView negotiableText = view.findViewById(R.id.negotiable_text);
        negotiableText.setVisibility(product.isNegotiable() ? View.VISIBLE : View.GONE);

        // Set item behavior if available
        TextView behaviorText = view.findViewById(R.id.behavior_text);
        if (product.getItemBehavior() != null && !product.getItemBehavior().isEmpty()) {
            behaviorText.setText(product.getItemBehavior());
            behaviorText.setVisibility(View.VISIBLE);
        } else {
            behaviorText.setVisibility(View.GONE);
        }

        // Set up tags
        ChipGroup tagsChipGroup = view.findViewById(R.id.tags_chip_group);
        if (product.getTags() != null && !product.getTags().isEmpty()) {
            for (String tag : product.getTags()) {
                Chip chip = new Chip(context);
                chip.setText(tag);
                chip.setClickable(false);
                tagsChipGroup.addView(chip);
            }
            tagsChipGroup.setVisibility(View.VISIBLE);
        } else {
            tagsChipGroup.setVisibility(View.GONE);
        }

        // Set up image preview
        ViewPager2 imageViewPager = view.findViewById(R.id.image_view_pager);
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            imageViewPager.setAdapter(new ImagePreviewAdapter(product.getImageUrls()));
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
