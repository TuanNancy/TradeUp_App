package com.example.tradeup_app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageUploadManager {
    private static final String TAG = "ImageUploadManager";
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1MB
    private static final int COMPRESSION_QUALITY = 85;
    private static final String[] ALLOWED_FORMATS = {"image/jpeg", "image/png"};
    private static final String CLOUDINARY_UPLOAD_PRESET = "my_profile_upload"; // Upload preset đúng

    // Đổi tên interface để tránh xung đột với Cloudinary UploadCallback
    public interface ImageUploadCallback {
        void onSuccess(List<String> imageUrls);
        void onFailure(Exception e);
    }

    public static void uploadImages(List<Uri> imageUris, Context context, ImageUploadCallback callback) {
        if (imageUris == null || imageUris.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // Upload images to Cloudinary
        List<String> uploadedUrls = new ArrayList<>();
        AtomicInteger uploadCount = new AtomicInteger(0);
        AtomicInteger totalImages = new AtomicInteger(imageUris.size());

        Log.d(TAG, "Starting Cloudinary upload for " + imageUris.size() + " images");

        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);
            final int index = i;

            if (!isValidImageFormat(context, imageUri)) {
                callback.onFailure(new IllegalArgumentException("Chỉ hỗ trợ định dạng JPEG và PNG"));
                return;
            }

            try {
                // Prepare upload options
                Map<String, Object> options = new HashMap<>();
                options.put("upload_preset", CLOUDINARY_UPLOAD_PRESET);
                options.put("folder", "tradeup/products");
                options.put("public_id", "product_" + System.currentTimeMillis() + "_" + index);
                options.put("resource_type", "image");

                Log.d(TAG, "Uploading image " + (index + 1) + "/" + imageUris.size() + " to Cloudinary");

                MediaManager.get().upload(imageUri)
                    .options(options)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Upload started for image " + (index + 1) + " with requestId: " + requestId);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            int progress = (int) ((bytes * 100) / totalBytes);
                            Log.d(TAG, "Upload progress for image " + (index + 1) + ": " + progress + "%");
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            if (imageUrl != null) {
                                uploadedUrls.add(imageUrl);
                                Log.d(TAG, "Successfully uploaded image " + (index + 1) + ": " + imageUrl);

                                // Check if all uploads are complete
                                if (uploadCount.incrementAndGet() == totalImages.get()) {
                                    Log.d(TAG, "All images uploaded successfully to Cloudinary");
                                    callback.onSuccess(uploadedUrls);
                                }
                            } else {
                                Log.e(TAG, "No secure_url in response for image " + (index + 1));
                                callback.onFailure(new Exception("Không nhận được URL từ Cloudinary cho ảnh " + (index + 1)));
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e(TAG, "Upload failed for image " + (index + 1) + ": " + error.getDescription());
                            callback.onFailure(new Exception("Lỗi upload ảnh " + (index + 1) + ": " + error.getDescription()));
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Upload rescheduled for image " + (index + 1) + ": " + error.getDescription());
                        }
                    })
                    .dispatch();

            } catch (Exception e) {
                Log.e(TAG, "Error setting up upload for image " + (index + 1), e);
                callback.onFailure(new Exception("Lỗi chuẩn bị upload ảnh " + (index + 1) + ": " + e.getMessage()));
                return;
            }
        }
    }

    /**
     * Upload single image for chat messages
     */
    public static void uploadChatImage(Uri imageUri, Context context, ChatImageUploadCallback callback) {
        if (imageUri == null) {
            callback.onFailure(new Exception("No image selected"));
            return;
        }

        Log.d(TAG, "Starting chat image upload to Cloudinary");

        try {
            // Prepare upload options for chat images
            Map<String, Object> options = new HashMap<>();
            options.put("upload_preset", CLOUDINARY_UPLOAD_PRESET);
            options.put("folder", "tradeup/chat_images");
            options.put("public_id", "chat_" + System.currentTimeMillis());
            options.put("resource_type", "image");
            options.put("transformation", "c_limit,w_800,h_800,q_auto:good"); // Optimize for chat

            Log.d(TAG, "Uploading chat image to Cloudinary with options: " + options.toString());

            MediaManager.get().upload(imageUri)
                .options(options)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Chat image upload started with requestId: " + requestId);
                        callback.onStart();
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        Log.d(TAG, "Chat image upload progress: " + progress + "% (" + bytes + "/" + totalBytes + " bytes)");
                        callback.onProgress(progress);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        if (imageUrl != null) {
                            Log.d(TAG, "Chat image uploaded successfully: " + imageUrl);
                            callback.onSuccess(imageUrl);
                        } else {
                            Log.e(TAG, "No secure_url in response for chat image. Response: " + resultData.toString());
                            callback.onFailure(new Exception("Failed to get image URL from Cloudinary"));
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Chat image upload failed - RequestID: " + requestId +
                                   ", Error: " + error.getDescription() +
                                   ", Code: " + error.getCode());
                        callback.onFailure(new Exception("Upload failed: " + error.getDescription()));
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Chat image upload rescheduled - RequestID: " + requestId +
                                   ", Error: " + error.getDescription());
                    }
                })
                .dispatch();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up chat image upload", e);
            callback.onFailure(new Exception("Upload setup failed: " + e.getMessage()));
        }
    }

    // Interface for chat image upload
    public interface ChatImageUploadCallback {
        void onStart();
        void onProgress(int progress);
        void onSuccess(String imageUrl);
        void onFailure(Exception e);
    }

    private static boolean isValidImageFormat(Context context, Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType == null) {
                Log.w(TAG, "Could not determine MIME type for URI: " + uri);
                return false;
            }

            for (String format : ALLOWED_FORMATS) {
                if (mimeType.equals(format)) return true;
            }
            Log.w(TAG, "Unsupported MIME type: " + mimeType);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking image format", e);
            return false;
        }
    }

    private static byte[] compressImage(Context context, Uri imageUri) throws IOException {
        InputStream input = null;
        try {
            input = context.getContentResolver().openInputStream(imageUri);
            if (input == null) throw new IOException("Không thể mở ảnh");

            // Get image dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();

            // Calculate sample size
            int sampleSize = 1;
            int maxDimension = Math.max(options.outWidth, options.outHeight);
            while (maxDimension / sampleSize > 2048) {
                sampleSize *= 2;
            }

            // Decode with sample size
            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            input = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);

            if (bitmap == null) throw new IOException("Không thể giải mã ảnh");

            // Compress to JPEG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream);
            byte[] data = outputStream.toByteArray();

            // If still too large, reduce quality
            int quality = COMPRESSION_QUALITY;
            while (data.length > MAX_IMAGE_SIZE && quality > 10) {
                outputStream.reset();
                quality -= 10;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                data = outputStream.toByteArray();
            }

            bitmap.recycle();
            Log.d(TAG, "Image compressed successfully. Final size: " + data.length + " bytes, Quality: " + quality);
            return data;

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing input stream", e);
                }
            }
        }
    }

    private static String getFileExtension(Context context, Uri uri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (extension == null) {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            }
        }
        return extension != null ? "." + extension : ".jpg";
    }
}
