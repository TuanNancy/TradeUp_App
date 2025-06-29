package com.example.tradeup_app.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;

import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {
    private final Context context;
    private List<Uri> images;
    private final OnImageRemoveListener onImageRemoveListener;
    private OnImageRemoveClickListener imageRemoveClickListener;

    public interface OnImageRemoveListener {
        void onImageRemove(Uri uri);
    }

    public interface OnImageRemoveClickListener {
        void onImageRemove(int position);
    }

    public ImagePreviewAdapter(Context context, List<Uri> images, OnImageRemoveListener listener) {
        this.context = context;
        this.images = images;
        this.onImageRemoveListener = listener;
    }

    public void setOnImageRemoveListener(OnImageRemoveClickListener listener) {
        this.imageRemoveClickListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = images.get(position);

        Glide.with(context)
            .load(imageUri)
            .centerCrop()
            .into(holder.imageView);

        holder.removeButton.setOnClickListener(v -> {
            if (imageRemoveClickListener != null) {
                imageRemoveClickListener.onImageRemove(position);
            }
            if (onImageRemoveListener != null) {
                onImageRemoveListener.onImageRemove(imageUri);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void updateImages(List<Uri> newImages) {
        this.images = newImages;
        notifyDataSetChanged();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton removeButton;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_preview);
            removeButton = itemView.findViewById(R.id.remove_image_button);
        }
    }
}
