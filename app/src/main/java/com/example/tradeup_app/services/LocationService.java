package com.example.tradeup_app.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import com.example.tradeup_app.models.Product;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LocationService {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    private Context context;
    private LocationManager locationManager;
    private Location currentLocation;
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private boolean canGetLocation = false;

    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude, String address);
        void onLocationError(String error);
    }

    public interface ProductLocationCallback {
        void onProductsFiltered(List<Product> filteredProducts);
        void onError(String error);
    }

    public LocationService(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Get current location using GPS or Network
     */
    public void getCurrentLocation(LocationCallback callback) {
        try {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            // Getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // Getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                callback.onLocationError("No network provider is enabled");
                return;
            }

            this.canGetLocation = true;

            // Check permissions
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                callback.onLocationError("Location permissions not granted");
                return;
            }

            // First, get location from Network Provider
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            currentLocation = location;
                            getAddressFromLocation(location.getLatitude(), location.getLongitude(), callback);
                            locationManager.removeUpdates(this);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {}

                        @Override
                        public void onProviderEnabled(String provider) {}

                        @Override
                        public void onProviderDisabled(String provider) {}
                    }
                );

                if (locationManager != null) {
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        currentLocation = location;
                        getAddressFromLocation(location.getLatitude(), location.getLongitude(), callback);
                        return;
                    }
                }
            }

            // If GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            currentLocation = location;
                            getAddressFromLocation(location.getLatitude(), location.getLongitude(), callback);
                            locationManager.removeUpdates(this);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {}

                        @Override
                        public void onProviderEnabled(String provider) {}

                        @Override
                        public void onProviderDisabled(String provider) {}
                    }
                );

                if (locationManager != null) {
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        currentLocation = location;
                        getAddressFromLocation(location.getLatitude(), location.getLongitude(), callback);
                    }
                }
            }

        } catch (Exception e) {
            callback.onLocationError("Error getting location: " + e.getMessage());
        }
    }

    /**
     * Get address from coordinates using Geocoder
     */
    private void getAddressFromLocation(double latitude, double longitude, LocationCallback callback) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String addressString = address.getAddressLine(0);
                callback.onLocationReceived(latitude, longitude, addressString);
            } else {
                callback.onLocationReceived(latitude, longitude, "Unknown Location");
            }
        } catch (Exception e) {
            callback.onLocationReceived(latitude, longitude, "Unknown Location");
        }
    }

    /**
     * Get coordinates from address string
     */
    public void getLocationFromAddress(String address, LocationCallback callback) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(address, 1);

            if (addresses != null && addresses.size() > 0) {
                Address location = addresses.get(0);
                callback.onLocationReceived(location.getLatitude(), location.getLongitude(), address);
            } else {
                callback.onLocationError("Address not found");
            }
        } catch (Exception e) {
            callback.onLocationError("Error geocoding address: " + e.getMessage());
        }
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // Distance in km

        return distance;
    }

    /**
     * Filter products by location radius
     */
    public void filterProductsByLocation(List<Product> products, double userLat, double userLon,
                                       double radiusKm, ProductLocationCallback callback) {
        try {
            List<Product> filteredProducts = new ArrayList<>();

            for (Product product : products) {
                if (product.getLatitude() != 0 && product.getLongitude() != 0) {
                    double distance = calculateDistance(userLat, userLon,
                                                      product.getLatitude(), product.getLongitude());
                    if (distance <= radiusKm) {
                        filteredProducts.add(product);
                    }
                }
            }

            callback.onProductsFiltered(filteredProducts);
        } catch (Exception e) {
            callback.onError("Error filtering products by location: " + e.getMessage());
        }
    }

    /**
     * Sort products by distance from user location
     */
    public void sortProductsByDistance(List<Product> products, double userLat, double userLon,
                                     ProductLocationCallback callback) {
        try {
            // Calculate distance for each product and sort
            Collections.sort(products, new Comparator<Product>() {
                @Override
                public int compare(Product p1, Product p2) {
                    double distance1 = Double.MAX_VALUE;
                    double distance2 = Double.MAX_VALUE;

                    if (p1.getLatitude() != 0 && p1.getLongitude() != 0) {
                        distance1 = calculateDistance(userLat, userLon, p1.getLatitude(), p1.getLongitude());
                    }

                    if (p2.getLatitude() != 0 && p2.getLongitude() != 0) {
                        distance2 = calculateDistance(userLat, userLon, p2.getLatitude(), p2.getLongitude());
                    }

                    return Double.compare(distance1, distance2);
                }
            });

            callback.onProductsFiltered(products);
        } catch (Exception e) {
            callback.onError("Error sorting products by distance: " + e.getMessage());
        }
    }

    /**
     * Get distance string for display
     */
    public static String getDistanceString(double lat1, double lon1, double lat2, double lon2) {
        if (lat2 == 0 && lon2 == 0) {
            return "Location unknown";
        }

        double distance = calculateDistance(lat1, lon1, lat2, lon2);

        if (distance < 1) {
            return String.format("%.0f m", distance * 1000);
        } else {
            return String.format("%.1f km", distance);
        }
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public void stopUsingGPS() {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates((LocationListener) this);
            }
        }
    }
}
