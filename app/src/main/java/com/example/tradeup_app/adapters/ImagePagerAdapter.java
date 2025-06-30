package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.tradeup_app.R;

import java.util.List;

/**
 * Adapter for displaying product images in ViewPager2
 */
public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {

    private Context context;
    private List<String> imageUrls;
    private OnImageClickListener clickListener;

    public interface OnImageClickListener {
        void onImageClick(String imageUrl, int position);
    }

    public ImagePagerAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.clickListener = listener;
    }

    public void updateImages(List<String> newImageUrls) {
        this.imageUrls.clear();
        this.imageUrls.addAll(newImageUrls);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_pager, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        holder.bind(imageUrl, position);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }

        public void bind(String imageUrl, int position) {
            // Load image with Glide
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(imageView);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onImageClick(imageUrl, position);
                }
            });
        }
    }
}
