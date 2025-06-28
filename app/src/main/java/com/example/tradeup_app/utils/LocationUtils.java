package com.example.tradeup_app.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationUtils {

    public interface LocationCallback {
        void onLocationResult(String address);
        void onLocationError(String error);
    }

    public static void getAddressFromLocation(Context context, double latitude, double longitude, LocationCallback callback) {
        try {
            // Check if Geocoder is available
            if (!Geocoder.isPresent()) {
                callback.onLocationError("Geocoder không khả dụng trên thiết bị này");
                return;
            }

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());

            // Run in background thread to avoid blocking UI
            new Thread(() -> {
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String addressText = formatAddress(address);

                        // Return to main thread using Handler
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onLocationResult(addressText));
                    } else {
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onLocationError("Không tìm thấy địa chỉ"));
                    }
                } catch (IOException e) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onLocationError("Lỗi geocoding: " + e.getMessage()));
                } catch (Exception e) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onLocationError("Lỗi không xác định: " + e.getMessage()));
                }
            }).start();

        } catch (Exception e) {
            callback.onLocationError("Lỗi: " + e.getMessage());
        }
    }

    private static String formatAddress(Address address) {
        StringBuilder addressText = new StringBuilder();

        if (address.getSubThoroughfare() != null) {
            addressText.append(address.getSubThoroughfare()).append(" ");
        }

        if (address.getThoroughfare() != null) {
            addressText.append(address.getThoroughfare()).append(", ");
        }

        if (address.getSubLocality() != null) {
            addressText.append(address.getSubLocality()).append(", ");
        }

        if (address.getLocality() != null) {
            addressText.append(address.getLocality()).append(", ");
        }

        if (address.getSubAdminArea() != null) {
            addressText.append(address.getSubAdminArea()).append(", ");
        }

        if (address.getAdminArea() != null) {
            addressText.append(address.getAdminArea());
        }

        return addressText.toString().trim();
    }

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // convert to km

        return distance;
    }

    public static String formatDistance(double distance) {
        if (distance < 1) {
            return String.format("%.0f m", distance * 1000);
        } else {
            return String.format("%.1f km", distance);
        }
    }
}
