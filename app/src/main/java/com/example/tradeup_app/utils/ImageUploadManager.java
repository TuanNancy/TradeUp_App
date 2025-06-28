package com.example.tradeup_app.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageUploadManager {

    private static ImageUploadManager instance;
    private static final String TAG = "ImageUploadManager";

    // Cloudinary upload preset for product images - sử dụng cùng preset như UserProfileActivity
    private static final String CLOUDINARY_UPLOAD_PRESET = "my_profile_upload";

    public interface ImageUploadCallback {
        void onSuccess(List<String> imageUrls);
        void onFailure(String error);
        void onProgress(int uploadedCount, int totalCount);
    }

    private ImageUploadManager() {
        // Initialize Cloudinary if needed
    }

    public static synchronized ImageUploadManager getInstance() {
        if (instance == null) {
            instance = new ImageUploadManager();
        }
        return instance;
    }

    public void uploadImages(Context context, List<Uri> imageUris, String folder, ImageUploadCallback callback) {
        if (imageUris == null || imageUris.isEmpty()) {
            callback.onFailure("Không có hình ảnh để tải lên");
            return;
        }

        List<String> uploadedUrls = new ArrayList<>();
        int totalImages = imageUris.size();
        final int[] uploadedCount = {0};
        final int[] failedCount = {0};

        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);
            final int currentIndex = i;

            // Upload to Cloudinary
            MediaManager.get().upload(imageUri)
                    .unsigned(CLOUDINARY_UPLOAD_PRESET)
                    .option("folder", folder) // Organize by folder (e.g., "products")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Starting upload for image " + (currentIndex + 1));
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Progress for individual image
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            if (imageUrl != null) {
                                synchronized (uploadedUrls) {
                                    uploadedUrls.add(imageUrl);
                                    uploadedCount[0]++;

                                    Log.d(TAG, "Upload success: " + imageUrl);
                                    callback.onProgress(uploadedCount[0], totalImages);

                                    // Check if all uploads completed
                                    if (uploadedCount[0] + failedCount[0] == totalImages) {
                                        if (uploadedUrls.size() > 0) {
                                            callback.onSuccess(uploadedUrls);
                                        } else {
                                            callback.onFailure("Tất cả uploads đều thất bại");
                                        }
                                    }
                                }
                            } else {
                                handleUploadFailure("Không nhận được URL từ Cloudinary", callback, uploadedUrls, uploadedCount, failedCount, totalImages);
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            String errorMsg = "Upload failed: " + error.getDescription();
                            Log.e(TAG, errorMsg);
                            handleUploadFailure(errorMsg, callback, uploadedUrls, uploadedCount, failedCount, totalImages);
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                        }
                    })
                    .dispatch();
        }
    }

    private void handleUploadFailure(String errorMsg, ImageUploadCallback callback,
                                   List<String> uploadedUrls, int[] uploadedCount,
                                   int[] failedCount, int totalImages) {
        synchronized (uploadedUrls) {
            failedCount[0]++;

            // Check if all uploads completed (success + failed)
            if (uploadedCount[0] + failedCount[0] == totalImages) {
                if (uploadedUrls.size() > 0) {
                    // Some uploads succeeded
                    callback.onSuccess(uploadedUrls);
                } else {
                    // All uploads failed
                    callback.onFailure(errorMsg);
                }
            }
        }
    }

    public void uploadSingleImage(Context context, Uri imageUri, String folder, SingleImageUploadCallback callback) {
        if (imageUri == null) {
            callback.onFailure("URI hình ảnh không hợp lệ");
            return;
        }

        MediaManager.get().upload(imageUri)
                .unsigned(CLOUDINARY_UPLOAD_PRESET)
                .option("folder", folder)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        if (imageUrl != null) {
                            callback.onSuccess(imageUrl);
                        } else {
                            callback.onFailure("Không nhận được URL từ Cloudinary");
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        callback.onFailure("Upload failed: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    public interface SingleImageUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }
}
