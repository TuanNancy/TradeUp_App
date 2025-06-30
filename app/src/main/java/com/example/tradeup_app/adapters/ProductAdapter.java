package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import de.hdodenhof.circleimageview.CircleImageView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> products;
    private final Context context;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onProductLongClick(Product product);
        void onMakeOffer(Product product);
        void onReportProduct(Product product);
        void onViewSellerProfile(String sellerId);
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
        return new ProductViewHolder(view, this);
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

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productTitle, productPrice, productDescription;
        private final TextView sellerName, sellerRating, viewCount, timePosted;
        private final Chip statusChip, categoryChip, conditionChip;
        private final CircleImageView sellerAvatar;
        private final ProductAdapter adapter;

        public ProductViewHolder(@NonNull View itemView, ProductAdapter adapter) {
            super(itemView);
            this.adapter = adapter;

            // Initialize views
            productImage = itemView.findViewById(R.id.product_image);
            productTitle = itemView.findViewById(R.id.product_title);
            productPrice = itemView.findViewById(R.id.product_price);
            productDescription = itemView.findViewById(R.id.product_description);
            sellerName = itemView.findViewById(R.id.seller_name);
            sellerRating = itemView.findViewById(R.id.seller_rating);
            viewCount = itemView.findViewById(R.id.view_count);
            timePosted = itemView.findViewById(R.id.time_posted);
            statusChip = itemView.findViewById(R.id.status_chip);
            categoryChip = itemView.findViewById(R.id.category_chip);
            conditionChip = itemView.findViewById(R.id.condition_chip);
            sellerAvatar = itemView.findViewById(R.id.seller_avatar);
        }

        public void bind(Product product) {
            // Set product data
            productTitle.setText(product.getTitle());
            productPrice.setText(formatPrice(product.getPrice()));
            productDescription.setText(product.getDescription());
            sellerName.setText(product.getSellerName());
            viewCount.setText(product.getViewCount() + " views");

            // Set chips
            statusChip.setText(product.getStatus());
            categoryChip.setText(product.getCategory());
            conditionChip.setText(product.getCondition());

            // Set time posted
            timePosted.setText(formatTimeAgo(product.getCreatedAt()));

            // Load product image
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                com.bumptech.glide.Glide.with(itemView.getContext())
                    .load(product.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.ic_image_placeholder);
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (adapter.listener != null) {
                    // Navigate to ProductDetailActivity instead of ChatActivity
                    com.example.tradeup_app.activities.ProductDetailActivity.startActivity(
                        itemView.getContext(), product.getId());
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (adapter.listener != null) {
                    adapter.listener.onProductLongClick(product);
                }
                return true;
            });

            // Seller avatar click
            sellerAvatar.setOnClickListener(v -> {
                if (adapter.listener != null) {
                    adapter.listener.onViewSellerProfile(product.getSellerId());
                }
            });
        }

        private String formatPrice(double price) {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            return formatter.format(price);
        }

        private String formatTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else if (hours > 0) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (minutes > 0) {
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else {
                return "Just now";
            }
        }
    }
}
