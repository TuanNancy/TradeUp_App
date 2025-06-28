package com.example.tradeup_app;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class TradeUpApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Cloudinary
        initCloudinary();
    }

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        // Chỉ cần cloud_name cho unsigned upload
        config.put("cloud_name", "dskljyaxx"); // Thay bằng cloud name thật của bạn từ Cloudinary dashboard

        try {
            MediaManager.init(this, config);
            android.util.Log.d("TradeUpApp", "Cloudinary initialized successfully");
        } catch (Exception e) {
            android.util.Log.e("TradeUpApp", "Failed to initialize Cloudinary: " + e.getMessage());
            // Fallback init nếu có lỗi
            try {
                MediaManager.init(this);
                android.util.Log.d("TradeUpApp", "Cloudinary initialized with default config");
            } catch (Exception fallbackError) {
                android.util.Log.e("TradeUpApp", "Fallback init also failed: " + fallbackError.getMessage());
            }
        }
    }
}
