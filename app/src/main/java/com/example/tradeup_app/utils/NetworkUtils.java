package com.example.tradeup_app.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

/**
 * Network utility to manage connectivity and sync data when online
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;

    public interface NetworkStatusListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    public NetworkUtils(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Check if device is currently connected to the internet
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null &&
               (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    /**
     * Check if device has WiFi connection
     */
    public boolean isWiFiConnected() {
        if (connectivityManager == null) return false;

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    /**
     * Check if device has cellular connection
     */
    public boolean isCellularConnected() {
        if (connectivityManager == null) return false;

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    /**
     * Register network callback to listen for connectivity changes
     */
    public void registerNetworkCallback(NetworkStatusListener listener) {
        if (connectivityManager == null) return;

        networkCallback = new NetworkCallback(listener);

        // Fixed: Use proper API level check and method calls
        NetworkRequest.Builder builder = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        // Add transport types if API level supports it
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                   .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        }

        NetworkRequest networkRequest = builder.build();

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        Log.d(TAG, "Network callback registered");
    }

    /**
     * Unregister network callback
     */
    public void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
            Log.d(TAG, "Network callback unregistered");
        }
    }

    /**
     * Get network type as string
     */
    public String getNetworkType() {
        if (!isNetworkAvailable()) return "No Connection";

        if (isWiFiConnected()) return "WiFi";
        if (isCellularConnected()) return "Cellular";
        return "Unknown";
    }

    /**
     * Check if it's good time to sync large data (WiFi connection)
     */
    public boolean isGoodTimeForLargeSync() {
        return isWiFiConnected();
    }

    /**
     * Check if it's good time to upload images (WiFi or strong cellular)
     */
    public boolean isGoodTimeForImageUpload() {
        if (isWiFiConnected()) return true;

        if (isCellularConnected()) {
            // Could add signal strength check here if needed
            return true;
        }

        return false;
    }

    private static class NetworkCallback extends ConnectivityManager.NetworkCallback {
        private final NetworkStatusListener listener;

        public NetworkCallback(NetworkStatusListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            Log.d(TAG, "Network available: " + network);
            if (listener != null) {
                listener.onNetworkAvailable();
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            Log.d(TAG, "Network lost: " + network);
            if (listener != null) {
                listener.onNetworkLost();
            }
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            Log.d(TAG, "Network capabilities changed: " + network);
        }
    }
}
