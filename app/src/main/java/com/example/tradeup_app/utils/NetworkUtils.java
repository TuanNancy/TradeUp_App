package com.example.tradeup_app.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Utility class để kiểm tra kết nối mạng
 */
public class NetworkUtils {

    /**
     * Kiểm tra xem thiết bị có kết nối internet không
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6.0+
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork);

            return networkCapabilities != null &&
                   (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // For older Android versions
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    /**
     * Kiểm tra loại kết nối mạng
     */
    public static String getNetworkType(Context context) {
        if (context == null) return "Unknown";

        ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return "Unknown";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return "No Connection";

            NetworkCapabilities networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork);

            if (networkCapabilities == null) return "Unknown";

            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WiFi";
            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Mobile Data";
            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "Ethernet";
            }
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                return activeNetworkInfo.getTypeName();
            }
        }

        return "Unknown";
    }

    /**
     * Kiểm tra xem có kết nối WiFi không
     */
    public static boolean isWiFiConnected(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork);

            return networkCapabilities != null &&
                   networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiInfo != null && wifiInfo.isConnected();
        }
    }

    /**
     * Kiểm tra xem có kết nối Mobile Data không
     */
    public static boolean isMobileDataConnected(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork);

            return networkCapabilities != null &&
                   networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            NetworkInfo mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return mobileInfo != null && mobileInfo.isConnected();
        }
    }
}
