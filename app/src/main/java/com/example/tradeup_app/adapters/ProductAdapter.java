package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.services.LocationService;
import com.example.tradeup_app.utils.VNDPriceFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> products;
    private final Context context;
    private OnProductClickListener listener;

    // Location-related fields for distance display
    private double userLatitude = 0;
    private double userLongitude = 0;
    private boolean showDistance = false;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onProductLongClick(Product product);
        void onMakeOffer(Product product);
        void onBuyProduct(Product product);
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

    /**
     * Set user location for distance calculation and display
     */
    public void setUserLocation(double latitude, double longitude) {
        this.userLatitude = latitude;
        this.userLongitude = longitude;
        this.showDistance = (latitude != 0 && longitude != 0);

        // Refresh all items to show distance
        notifyDataSetChanged();
    }

    /**
     * Clear user location and hide distance display
     */
    public void clearUserLocation() {
        this.userLatitude = 0;
        this.userLongitude = 0;
        this.showDistance = false;

        // Refresh all items to hide distance
        notifyDataSetChanged();
    }

    /**
     * Check if distance display is enabled
     */
    public boolean isDistanceDisplayEnabled() {
        return showDistance;
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
        // Đảm bảo cập nhật được thực hiện trên UI thread
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> {
                updateProductsInternal(newProducts);
            });
        } else {
            updateProductsInternal(newProducts);
        }
    }

    private void updateProductsInternal(List<Product> newProducts) {
        int oldSize = products.size();
        products.clear();

        // Thông báo về việc xóa dữ liệu cũ
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }

        // Thêm dữ liệu mới
        if (newProducts != null && !newProducts.isEmpty()) {
            products.addAll(newProducts);
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
            notifyItemRangeChanged(position, products.size() - position);
        }
    }

    // Thêm phương thức clearProducts để xóa an toàn
    public void clearProducts() {
        int oldSize = products.size();
        products.clear();
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productTitle, productPrice, productDescription;
        private final TextView sellerName, sellerRating, viewCount, timePosted;
        private final TextView soldCount; // Add sold count TextView
        private final Chip statusChip, categoryChip, conditionChip;
        private final CircleImageView sellerAvatar;
        private final MaterialButton btnChat, btnMakeOffer, btnBuy;
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
            soldCount = itemView.findViewById(R.id.sold_count); // Initialize sold count
            statusChip = itemView.findViewById(R.id.status_chip);
            categoryChip = itemView.findViewById(R.id.category_chip);
            conditionChip = itemView.findViewById(R.id.condition_chip);
            sellerAvatar = itemView.findViewById(R.id.seller_avatar);
            btnChat = itemView.findViewById(R.id.btn_chat);
            btnMakeOffer = itemView.findViewById(R.id.btn_make_offer);
            btnBuy = itemView.findViewById(R.id.btn_buy);
        }

        public void bind(Product product) {
            // Set product data
            productTitle.setText(product.getTitle());
            productPrice.setText(formatPrice(product.getPrice()));
            productDescription.setText(product.getDescription());
            sellerName.setText(product.getSellerName());
            viewCount.setText("👁 " + product.getViewCount() + " lượt xem");

            // Use interactionCount for sold items display
            soldCount.setText("🛒 " + product.getInteractionCount() + " đã bán");

            // Set chips
            setupStatusChip(product.getStatus());
            categoryChip.setText(product.getCategory());
            conditionChip.setText(product.getCondition());

            // Set time posted
            timePosted.setText(formatTimeAgo(product.getCreatedAt()));

            // Setup buttons based on product status
            setupButtonStates(product.getStatus());

            // Load product image
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                com.bumptech.glide.Glide.with(itemView.getContext())
                    .load(product.getImageUrls().get(0))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(productImage);
            } else {
                productImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Set item behavior
            TextView itemBehaviorView = itemView.findViewById(R.id.item_behavior);
            itemBehaviorView.setText(product.getItemBehavior() != null ? product.getItemBehavior() : "N/A");

            // Set distance if available
            TextView distanceView = itemView.findViewById(R.id.distance_view);
            if (adapter.isDistanceDisplayEnabled()) {
                double distance = calculateDistance(product.getLatitude(), product.getLongitude());
                distanceView.setText(formatDistance(distance));
                distanceView.setVisibility(View.VISIBLE);
            } else {
                distanceView.setVisibility(View.GONE);
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

            // Chat button click
            btnChat.setOnClickListener(v -> {
                if (adapter.listener != null) {
                    adapter.listener.onProductClick(product);
                }
            });

            // Make offer button click with debounce protection
            btnMakeOffer.setOnClickListener(v -> {
                // Prevent multiple rapid clicks
                if (!v.isEnabled()) return;

                v.setEnabled(false);
                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.postDelayed(() -> v.setEnabled(true), 1000); // Re-enable after 1 second

                android.util.Log.d("ProductAdapter", "Make Offer button clicked for product: " + product.getTitle());
                if (adapter.listener != null) {
                    adapter.listener.onMakeOffer(product);
                } else {
                    android.util.Log.w("ProductAdapter", "OnProductClickListener is null");
                }
            });

            // Buy button click
            btnBuy.setOnClickListener(v -> {
                if (adapter.listener != null) {
                    adapter.listener.onBuyProduct(product);
                }
            });
        }

        private void setupStatusChip(String status) {
            statusChip.setText(status);

            switch (status) {
                case "Available":
                    statusChip.setChipBackgroundColorResource(android.R.color.holo_green_light);
                    statusChip.setTextColor(itemView.getContext().getColor(android.R.color.white));
                    break;
                case "Sold":
                    statusChip.setChipBackgroundColorResource(android.R.color.holo_red_light);
                    statusChip.setTextColor(itemView.getContext().getColor(android.R.color.white));
                    break;
                case "Paused":
                    statusChip.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                    statusChip.setTextColor(itemView.getContext().getColor(android.R.color.white));
                    break;
                default:
                    statusChip.setChipBackgroundColorResource(android.R.color.darker_gray);
                    statusChip.setTextColor(itemView.getContext().getColor(android.R.color.white));
                    break;
            }
        }

        private void setupButtonStates(String status) {
            switch (status) {
                case "Available":
                    // Enable all buttons for available products
                    btnBuy.setEnabled(true);
                    btnMakeOffer.setEnabled(true);
                    btnBuy.setBackgroundTintList(itemView.getContext().getColorStateList(android.R.color.holo_green_light));
                    btnMakeOffer.setAlpha(1.0f);
                    break;
                case "Sold":
                    // Disable buy and make offer buttons for sold products
                    btnBuy.setEnabled(false);
                    btnMakeOffer.setEnabled(false);
                    btnBuy.setBackgroundTintList(itemView.getContext().getColorStateList(android.R.color.darker_gray));
                    btnMakeOffer.setAlpha(0.5f);
                    break;
                case "Paused":
                    // Disable buy and make offer buttons for paused products
                    btnBuy.setEnabled(false);
                    btnMakeOffer.setEnabled(false);
                    btnBuy.setBackgroundTintList(itemView.getContext().getColorStateList(android.R.color.darker_gray));
                    btnMakeOffer.setAlpha(0.5f);
                    break;
                default:
                    // Default to enabled for unknown status
                    btnBuy.setEnabled(true);
                    btnMakeOffer.setEnabled(true);
                    btnBuy.setBackgroundTintList(itemView.getContext().getColorStateList(android.R.color.holo_green_light));
                    btnMakeOffer.setAlpha(1.0f);
                    break;
            }
        }

        private String formatPrice(double price) {
            return VNDPriceFormatter.formatVND(price);
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

        private double calculateDistance(double productLatitude, double productLongitude) {
            // Haversine formula to calculate the distance between two points on the Earth
            final int R = 6371; // Radius of the Earth in kilometers

            double latDistance = Math.toRadians(productLatitude - adapter.userLatitude);
            double lonDistance = Math.toRadians(productLongitude - adapter.userLongitude);

            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                    Math.cos(Math.toRadians(adapter.userLatitude)) * Math.cos(Math.toRadians(productLatitude)) *
                    Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            return R * c; // Distance in kilometers
        }

        private String formatDistance(double distance) {
            // Format distance to show in kilometers or meters
            if (distance < 1) {
                return String.format(Locale.getDefault(), "%.0f meters", distance * 1000);
            } else {
                return String.format(Locale.getDefault(), "%.1f km", distance);
            }
        }
    }
}
