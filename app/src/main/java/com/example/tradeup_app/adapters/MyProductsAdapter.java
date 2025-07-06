package com.example.tradeup_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.activities.ProductDetailActivity;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.utils.VNDPriceFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyProductsAdapter extends RecyclerView.Adapter<MyProductsAdapter.MyProductViewHolder> {

    private final List<Product> products;
    private final Context context;
    private OnMyProductClickListener listener;

    public interface OnMyProductClickListener {
        void onProductClick(Product product);
        void onEditProduct(Product product);
        void onDeleteProduct(Product product);
        void onViewAnalytics(Product product);
    }

    public MyProductsAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products != null ? products : new ArrayList<>();
    }

    public void setOnMyProductClickListener(OnMyProductClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_product, parent, false);
        return new MyProductViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull MyProductViewHolder holder, int position) {
        Product product = products.get(position);
        Log.d("MyProductsAdapter", "Binding product at position " + position + ": " + product.getTitle());
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        int count = products.size();
        Log.d("MyProductsAdapter", "getItemCount: " + count);
        return count;
    }

    public void updateProducts(List<Product> newProducts) {
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

        Log.d("MyProductsAdapter", "updateProductsInternal: oldSize=" + oldSize +
              ", newSize=" + (newProducts != null ? newProducts.size() : 0));

        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }

        if (newProducts != null && !newProducts.isEmpty()) {
            products.addAll(newProducts);
            notifyItemRangeInserted(0, newProducts.size());
            Log.d("MyProductsAdapter", "Products updated, new count: " + products.size());
        } else {
            Log.d("MyProductsAdapter", "No new products to add");
        }
    }

    static class MyProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productTitle, productPrice, productDescription;
        private final TextView viewCount, timePosted;
        private final Chip statusChip, categoryChip, conditionChip;
        private final MaterialButton btnEdit, btnDelete, btnViewDetails;
        private final MyProductsAdapter adapter;

        public MyProductViewHolder(@NonNull View itemView, MyProductsAdapter adapter) {
            super(itemView);
            this.adapter = adapter;

            // Initialize views
            productImage = itemView.findViewById(R.id.product_image);
            productTitle = itemView.findViewById(R.id.product_title);
            productPrice = itemView.findViewById(R.id.product_price);
            productDescription = itemView.findViewById(R.id.product_description);
            viewCount = itemView.findViewById(R.id.view_count);
            timePosted = itemView.findViewById(R.id.time_posted);
            statusChip = itemView.findViewById(R.id.status_chip);
            categoryChip = itemView.findViewById(R.id.category_chip);
            conditionChip = itemView.findViewById(R.id.condition_chip);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
        }

        public void bind(Product product) {
            // Set product data
            productTitle.setText(product.getTitle());
            productPrice.setText(VNDPriceFormatter.formatVND(product.getPrice())); // Fix: use formatVND instead of format
            productDescription.setText(product.getDescription());
            viewCount.setText(product.getViewCount() + " lượt xem");

            // Set chips
            statusChip.setText(getStatusText(product.getStatus()));
            categoryChip.setText(product.getCategory());
            conditionChip.setText(product.getCondition());

            // Set status chip color
            setStatusChipColor(statusChip, product.getStatus());

            // Set time posted
            timePosted.setText(formatTimeAgo(product.getCreatedAt()));

            // Load product image
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(product.getImageUrls().get(0))
                    .placeholder(android.R.drawable.ic_menu_gallery) // Fix: use existing drawable
                    .error(android.R.drawable.ic_menu_gallery) // Fix: use existing drawable
                    .into(productImage);
            } else {
                productImage.setImageResource(android.R.drawable.ic_menu_gallery); // Fix: use existing drawable
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (adapter.listener != null) {
                    adapter.listener.onProductClick(product);
                }
            });

            btnViewDetails.setOnClickListener(v -> {
                // Mở ProductDetailActivity để xem chi tiết
                Intent intent = new Intent(itemView.getContext(), ProductDetailActivity.class);
                intent.putExtra("product_id", product.getId());
                itemView.getContext().startActivity(intent);
            });

            btnEdit.setOnClickListener(v -> {
                if (adapter.listener != null) {
                    adapter.listener.onEditProduct(product);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (adapter.listener != null) {
                    adapter.listener.onDeleteProduct(product);
                }
            });
        }

        private String getStatusText(String status) {
            switch (status) {
                case "Available":
                    return "Đang bán";
                case "Sold":
                    return "Đã bán";
                case "Paused":
                    return "Tạm dừng";
                default:
                    return status;
            }
        }

        private void setStatusChipColor(Chip chip, String status) {
            int colorRes;
            switch (status) {
                case "Available":
                    colorRes = R.color.success_color;
                    break;
                case "Sold":
                    colorRes = R.color.gray;
                    break;
                case "Paused":
                    colorRes = R.color.warning;
                    break;
                default:
                    colorRes = R.color.gray;
                    break;
            }
            chip.setChipBackgroundColorResource(colorRes);
        }

        private String formatTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            if (diff < 60000) { // Less than 1 minute
                return "Vừa xong";
            } else if (diff < 3600000) { // Less than 1 hour
                return (diff / 60000) + " phút trước";
            } else if (diff < 86400000) { // Less than 1 day
                return (diff / 3600000) + " giờ trước";
            } else if (diff < 604800000) { // Less than 1 week
                return (diff / 86400000) + " ngày trước";
            } else {
                // More than 1 week, show actual date
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}
