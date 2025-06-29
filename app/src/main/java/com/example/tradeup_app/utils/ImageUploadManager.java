package com.example.tradeup_app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageUploadManager {
    private static final String TAG = "ImageUploadManager";
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1MB
    private static final int COMPRESSION_QUALITY = 85;
    private static final String[] ALLOWED_FORMATS = {"image/jpeg", "image/png"};

    public interface UploadCallback {
        void onSuccess(List<String> imageUrls);
        void onFailure(Exception e);
    }

    public static void uploadImages(List<Uri> imageUris, Context context, UploadCallback callback) {
        if (imageUris == null || imageUris.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<String> uploadedUrls = new ArrayList<>();
        AtomicInteger uploadCount = new AtomicInteger(0);
        FirebaseStorage storage = FirebaseStorage.getInstance();

        for (Uri imageUri : imageUris) {
            if (!isValidImageFormat(context, imageUri)) {
                callback.onFailure(new IllegalArgumentException("Chỉ hỗ trợ định dạng JPEG và PNG"));
                return;
            }

            try {
                byte[] compressedImage = compressImage(context, imageUri);
                String fileName = "products/" + UUID.randomUUID().toString() + getFileExtension(context, imageUri);
                StorageReference ref = storage.getReference().child(fileName);

                ref.putBytes(compressedImage)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful() && task.getException() != null) {
                            throw task.getException();
                        }
                        return ref.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            uploadedUrls.add(task.getResult().toString());
                            if (uploadCount.incrementAndGet() == imageUris.size()) {
                                callback.onSuccess(uploadedUrls);
                            }
                        } else {
                            callback.onFailure(task.getException());
                        }
                    });
            } catch (IOException e) {
                Log.e(TAG, "Error compressing image: " + e.getMessage());
                callback.onFailure(e);
                return;
            }
        }
    }

    private static boolean isValidImageFormat(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null) return false;

        for (String format : ALLOWED_FORMATS) {
            if (mimeType.equals(format)) return true;
        }
        return false;
    }

    private static byte[] compressImage(Context context, Uri imageUri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(imageUri);
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
        input.close();

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
        return data;
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
