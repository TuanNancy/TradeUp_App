package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> products;
    private Context context;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onProductLongClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products != null ? products : new ArrayList<>();
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateProducts(List<Product> newProducts) {
        this.products.clear();
        if (newProducts != null) {
            this.products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }

    public void addProduct(Product product) {
        if (product != null) {
            products.add(0, product);
            notifyItemInserted(0);
        }
    }

    public void removeProduct(int position) {
        if (position >= 0 && position < products.size()) {
            products.remove(position);
            notifyItemRemoved(position);
        }
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private androidx.viewpager2.widget.ViewPager2 imagesViewPager;
        private LinearLayout dotsIndicator;
        private TextView titleText, priceText, locationText, conditionText, imageCounter;
        private ImagePagerAdapter imagePagerAdapter;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imagesViewPager = itemView.findViewById(R.id.images_viewpager);
            dotsIndicator = itemView.findViewById(R.id.dots_indicator);
            titleText = itemView.findViewById(R.id.title_text);
            priceText = itemView.findViewById(R.id.price_text);
            locationText = itemView.findViewById(R.id.location_text);
            conditionText = itemView.findViewById(R.id.condition_text);
            imageCounter = itemView.findViewById(R.id.image_counter);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onProductClick(products.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onProductLongClick(products.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }

        public void bind(Product product) {
            titleText.setText(product.getTitle());

            // Format price in Vietnamese currency
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            priceText.setText(formatter.format(product.getPrice()));

            locationText.setText(product.getLocation());
            conditionText.setText(product.getCondition());

            // Setup images with ViewPager2
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                // Clear any existing callbacks to prevent memory leaks
                imagesViewPager.unregisterOnPageChangeCallback(pageChangeCallback);

                // Set up adapter
                imagePagerAdapter = new ImagePagerAdapter(context, product.getImageUrls());
                imagesViewPager.setAdapter(imagePagerAdapter);
                imagesViewPager.setVisibility(View.VISIBLE);

                // Setup dots indicator only if more than 1 image
                if (product.getImageUrls().size() > 1) {
                    setupDotsIndicator(product.getImageUrls().size());
                    dotsIndicator.setVisibility(View.VISIBLE);
                } else {
                    dotsIndicator.setVisibility(View.GONE);
                }

                // Set image counter
                imageCounter.setText(String.format(Locale.getDefault(), "1/%d", product.getImageUrls().size()));
                imageCounter.setVisibility(View.VISIBLE);

                android.util.Log.d("ProductAdapter", "Setup ViewPager with " + product.getImageUrls().size() + " images");
            } else {
                // No images - show placeholder
                android.util.Log.d("ProductAdapter", "No image URLs found for product: " + product.getTitle());
                imagesViewPager.setVisibility(View.GONE);
                dotsIndicator.setVisibility(View.GONE);
                imageCounter.setVisibility(View.GONE);
            }
        }

        private androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback pageChangeCallback =
            new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    // Update dots indicator safely
                    if (dotsIndicator != null && dotsIndicator.getChildCount() > position) {
                        for (int i = 0; i < dotsIndicator.getChildCount(); i++) {
                            View dot = dotsIndicator.getChildAt(i);
                            if (dot != null) {
                                int dotColor = (i == position) ? R.drawable.dot_indicator_active : R.drawable.dot_indicator;
                                dot.setBackgroundResource(dotColor);
                            }
                        }

                        // Update counter badge
                        if (imageCounter != null) {
                            imageCounter.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, dotsIndicator.getChildCount()));
                        }
                    }
                }
            };

        private void setupDotsIndicator(int size) {
            dotsIndicator.removeAllViews();

            // Only show dots if there are multiple images
            if (size <= 1) {
                dotsIndicator.setVisibility(View.GONE);
                return;
            }

            for (int i = 0; i < Math.min(size, 5); i++) { // Limit to 5 dots maximum
                View dot = new View(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(12, 12); // Fixed size in pixels
                params.setMargins(6, 0, 6, 0);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(R.drawable.dot_indicator);
                dotsIndicator.addView(dot);
            }

            // Highlight the first dot
            if (dotsIndicator.getChildCount() > 0) {
                dotsIndicator.getChildAt(0).setBackgroundResource(R.drawable.dot_indicator_active);
            }

            // Register page change callback
            imagesViewPager.registerOnPageChangeCallback(pageChangeCallback);
        }
    }
}
