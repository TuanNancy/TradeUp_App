package com.example.tradeup_app;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import com.example.tradeup_app.utils.NotificationManager;
import java.util.HashMap;
import java.util.Map;

public class TradeUpApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Cloudinary
        initCloudinary();
        
        // Initialize Notification System
        initNotificationSystem();
    }

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        // Cấu hình đầy đủ Cloudinary credentials
        config.put("cloud_name", "dskljyaxx");
        config.put("api_key", "111495714631442");
        config.put("api_secret", "at88YOt6ZbYT4GMrOJ1BK2XHzfI");

        try {
            MediaManager.init(this, config);
            android.util.Log.d("TradeUpApp", "Cloudinary initialized successfully with full credentials");
        } catch (Exception e) {
            android.util.Log.e("TradeUpApp", "Failed to initialize Cloudinary: " + e.getMessage());
            // Fallback với chỉ cloud_name để test
            try {
                Map<String, String> fallbackConfig = new HashMap<>();
                fallbackConfig.put("cloud_name", "dskljyaxx");
                MediaManager.init(this, fallbackConfig);
                android.util.Log.d("TradeUpApp", "Cloudinary initialized with fallback config (cloud_name only)");
            } catch (Exception fallbackError) {
                android.util.Log.e("TradeUpApp", "Fallback init also failed: " + fallbackError.getMessage());
            }
        }
    }
    
    private void initNotificationSystem() {
        try {
            NotificationManager.getInstance(this).initialize();
            android.util.Log.d("TradeUpApp", "Notification system initialized successfully");
        } catch (Exception e) {
            android.util.Log.e("TradeUpApp", "Failed to initialize notification system: " + e.getMessage());
        }
    }
}
