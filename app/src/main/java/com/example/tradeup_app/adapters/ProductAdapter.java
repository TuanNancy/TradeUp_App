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
        holder.bind(product, context);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateProducts(List<Product> newProducts) {
        int oldSize = this.products.size();
        this.products.clear();
        notifyItemRangeRemoved(0, oldSize);

        if (newProducts != null) {
            this.products.addAll(newProducts);
            notifyItemRangeInserted(0, newProducts.size());
        }
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

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views according to the new layout
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

            // Initialize buttons locally since they're only used in constructor
            MaterialButton chatButton = itemView.findViewById(R.id.chat_button);
            MaterialButton offerButton = itemView.findViewById(R.id.offer_button);
            ImageButton favoriteButton = itemView.findViewById(R.id.favorite_button);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                ProductAdapter adapter = getAdapterFromContext(itemView.getContext());
                if (adapter != null && adapter.listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    adapter.listener.onProductClick(adapter.products.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                ProductAdapter adapter = getAdapterFromContext(itemView.getContext());
                if (adapter != null && adapter.listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    adapter.listener.onProductLongClick(adapter.products.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });

            if (chatButton != null) {
                chatButton.setOnClickListener(v -> {
                    ProductAdapter adapter = getAdapterFromContext(itemView.getContext());
                    if (adapter != null && adapter.listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        adapter.listener.onProductClick(adapter.products.get(getAdapterPosition()));
                    }
                });
            }

            if (offerButton != null) {
                offerButton.setOnClickListener(v -> {
                    ProductAdapter adapter = getAdapterFromContext(itemView.getContext());
                    if (adapter != null && adapter.listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        adapter.listener.onMakeOffer(adapter.products.get(getAdapterPosition()));
                    }
                });
            }

            if (favoriteButton != null) {
                favoriteButton.setOnClickListener(v ->
                    android.widget.Toast.makeText(itemView.getContext(), R.string.added_to_favorites, android.widget.Toast.LENGTH_SHORT).show()
                );
            }
        }

        private ProductAdapter getAdapterFromContext(Context context) {
            // This is a workaround since we can't access the adapter directly from ViewHolder
            // In a real implementation, you might want to pass the adapter reference or use a different approach
            return null; // TODO: Implement proper adapter reference
        }

        public void bind(Product product, Context context) {
            // Set product title
            productTitle.setText(product.getTitle());

            // Format and set price
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            productPrice.setText(formatter.format(product.getPrice()));

            // Set description
            productDescription.setText(product.getDescription());

            // Set seller info
            sellerName.setText(product.getSellerName());

            // Handle seller rating - using a default value since getSellerRating() doesn't exist
            sellerRating.setText("4.5"); // TODO: Implement proper seller rating system

            // Set view count with string resource
            viewCount.setText(context.getString(R.string.view_count_format, product.getViewCount()));

            // Set time posted
            timePosted.setText(getTimeAgo(product.getCreatedAt()));

            // Set status chip
            statusChip.setText(product.getStatus());
            setStatusChipColor(statusChip, product.getStatus());

            // Set category chip
            categoryChip.setText(product.getCategory());

            // Set condition chip
            conditionChip.setText(product.getCondition());

            // Load product image
            loadProductImage(product, context);

            // Load seller avatar (placeholder for now)
            sellerAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }

        private void loadProductImage(Product product, Context context) {
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                String firstImageUrl = product.getImageUrls().get(0);
                // Use Glide to load image
                com.bumptech.glide.Glide.with(context)
                    .load(firstImageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.ic_image_placeholder);
            }
        }

        private void setStatusChipColor(Chip chip, String status) {
            int colorRes;
            switch (status.toLowerCase()) {
                case "available":
                case "có sẵn":
                    colorRes = R.color.success;
                    break;
                case "sold":
                case "đã bán":
                    colorRes = R.color.sold;
                    break;
                default:
                    colorRes = R.color.text_secondary;
                    break;
            }
            chip.setChipBackgroundColorResource(colorRes);
        }

        private String getTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            if (diff < 60000) { // Less than 1 minute
                return "Vừa xong";
            } else if (diff < 3600000) { // Less than 1 hour
                return (diff / 60000) + " phút trước";
            } else if (diff < 86400000) { // Less than 1 day
                return (diff / 3600000) + " giờ trước";
            } else { // More than 1 day
                return (diff / 86400000) + " ngày trước";
            }
        }
    }
}
