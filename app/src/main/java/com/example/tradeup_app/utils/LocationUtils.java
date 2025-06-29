package com.example.tradeup_app.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationUtils {
    private static final String TAG = "LocationUtils";

    public interface LocationCallback {
        void onAddressReceived(String address);
    }

    public static void getAddressFromLocation(Context context, double latitude, double longitude, LocationCallback callback) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(context, new Locale("vi"));
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder addressString = new StringBuilder();

                    // Get detailed address components
                    String streetNumber = address.getSubThoroughfare();
                    String street = address.getThoroughfare();
                    String ward = address.getSubLocality();
                    String district = address.getLocality();
                    String city = address.getAdminArea();

                    // Build address string
                    if (streetNumber != null) addressString.append(streetNumber).append(" ");
                    if (street != null) addressString.append(street).append(", ");
                    if (ward != null) addressString.append(ward).append(", ");
                    if (district != null) addressString.append(district).append(", ");
                    if (city != null) addressString.append(city);

                    String finalAddress = addressString.toString().trim();
                    if (finalAddress.endsWith(",")) {
                        finalAddress = finalAddress.substring(0, finalAddress.length() - 1);
                    }

                    String addressToReturn = finalAddress;
                    new Handler(Looper.getMainLooper()).post(() ->
                        callback.onAddressReceived(addressToReturn)
                    );
                } else {
                    Log.w(TAG, "No address found for location: " + latitude + ", " + longitude);
                    new Handler(Looper.getMainLooper()).post(() ->
                        callback.onAddressReceived("")
                    );
                }
            } catch (IOException e) {
                Log.e(TAG, "Error getting address: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() ->
                    callback.onAddressReceived("")
                );
            }
        }).start();
    }

    public static void getLocationFromAddress(Context context, String addressString, LocationCallback callback) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(context, new Locale("vi"));
            try {
                List<Address> addresses = geocoder.getFromLocationName(addressString, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String formattedAddress = String.format("%s, %s, %s",
                        address.getAddressLine(0),
                        address.getLocality(),
                        address.getAdminArea());

                    new Handler(Looper.getMainLooper()).post(() ->
                        callback.onAddressReceived(formattedAddress)
                    );
                } else {
                    Log.w(TAG, "No location found for address: " + addressString);
                    new Handler(Looper.getMainLooper()).post(() ->
                        callback.onAddressReceived("")
                    );
                }
            } catch (IOException e) {
                Log.e(TAG, "Error getting location: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() ->
                    callback.onAddressReceived("")
                );
            }
        }).start();
    }
}
