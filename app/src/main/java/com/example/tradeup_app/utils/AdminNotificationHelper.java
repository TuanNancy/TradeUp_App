package com.example.tradeup_app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.tradeup_app.R;

public class AdminNotificationHelper {
    private static final String CHANNEL_ID = "admin_reports_channel";
    private static final String CHANNEL_NAME = "Admin Reports";
    private static final String CHANNEL_DESCRIPTION = "Notifications for new reports";
    private static final int NOTIFICATION_ID = 1001;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void showNewReportNotification(Context context, String productTitle, String reportReason) {
        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flag)
            .setContentTitle("🚨 New Report Received")
            .setContentText("Product: " + productTitle + " - Reason: " + reportReason)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            android.util.Log.d("AdminNotification", "✅ Notification sent for new report");
        } catch (SecurityException e) {
            android.util.Log.e("AdminNotification", "❌ Permission denied for notification", e);
        }
    }
}
