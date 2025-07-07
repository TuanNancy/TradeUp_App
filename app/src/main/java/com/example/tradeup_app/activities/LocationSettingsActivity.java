package com.example.tradeup_app.activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.tradeup_app.R;
import com.example.tradeup_app.services.LocationService;

public class LocationSettingsActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private Switch switchUseGPS;
    private Switch switchLocationFilter; // New switch for enabling location-based filtering
    private EditText editCustomLocation;
    private Button btnGetCurrentLocation;
    private Button btnSetCustomLocation;
    private Button btnSaveSettings;
    private Button btnClearLocation; // New button to clear saved location
    private SeekBar seekBarRadius;
    private TextView textRadiusValue;
    private TextView textCurrentLocationDisplay;
    private TextView textLocationInfo; // New text view for additional info

    private LocationService locationService;
    private SharedPreferences prefs;

    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private String currentAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_settings);

        initializeViews();
        setupToolbar();
        loadSettings();
        setupListeners();

        locationService = new LocationService(this);
        prefs = getSharedPreferences("location_prefs", MODE_PRIVATE);
    }

    private void initializeViews() {
        switchUseGPS = findViewById(R.id.switch_use_gps);
        switchLocationFilter = findViewById(R.id.switch_location_filter);
        editCustomLocation = findViewById(R.id.edit_custom_location);
        btnGetCurrentLocation = findViewById(R.id.btn_get_current_location);
        btnSetCustomLocation = findViewById(R.id.btn_set_custom_location);
        btnSaveSettings = findViewById(R.id.btn_save_settings);
        btnClearLocation = findViewById(R.id.btn_clear_location);
        seekBarRadius = findViewById(R.id.seek_bar_radius);
        textRadiusValue = findViewById(R.id.text_radius_value);
        textCurrentLocationDisplay = findViewById(R.id.text_current_location_display);
        textLocationInfo = findViewById(R.id.text_location_info);
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Location Settings");
        }
    }

    private void loadSettings() {
        // Load saved settings
        boolean useGPS = prefs.getBoolean("use_gps", true);
        boolean locationFilterEnabled = prefs.getBoolean("location_filter_enabled", false);
        String customLocation = prefs.getString("custom_location", "");
        int radius = prefs.getInt("search_radius", 25); // Default 25km
        double savedLat = Double.longBitsToDouble(prefs.getLong("latitude", 0));
        double savedLon = Double.longBitsToDouble(prefs.getLong("longitude", 0));
        String savedAddress = prefs.getString("address", "");

        switchUseGPS.setChecked(useGPS);
        switchLocationFilter.setChecked(locationFilterEnabled);
        editCustomLocation.setText(customLocation);
        seekBarRadius.setProgress(radius);
        textRadiusValue.setText(radius + " km");

        if (savedLat != 0 && savedLon != 0) {
            currentLatitude = savedLat;
            currentLongitude = savedLon;
            currentAddress = savedAddress;
            textCurrentLocationDisplay.setText("Current: " + savedAddress);
            updateLocationInfo();
        }

        updateUIBasedOnGPSMode(useGPS);
        updateLocationFilterInfo(locationFilterEnabled);
    }

    private void setupListeners() {
        switchUseGPS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateUIBasedOnGPSMode(isChecked);
        });

        switchLocationFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateLocationFilterInfo(isChecked);
        });

        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Minimum 5km, maximum 100km
                int radius = Math.max(5, progress);
                textRadiusValue.setText(radius + " km");

                // Update location info if filter is enabled
                if (switchLocationFilter.isChecked()) {
                    updateLocationInfo();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnGetCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnSetCustomLocation.setOnClickListener(v -> setCustomLocation());
        btnClearLocation.setOnClickListener(v -> clearLocation());
        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    /**
     * Update location filter information display
     */
    private void updateLocationFilterInfo(boolean enabled) {
        if (enabled) {
            textLocationInfo.setText("📍 Location-based filtering is ENABLED. Products will be prioritized by distance from your location.");
            textLocationInfo.setVisibility(View.VISIBLE);
        } else {
            textLocationInfo.setText("❌ Location-based filtering is DISABLED. All products will be shown without location priority.");
            textLocationInfo.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Update location information display
     */
    private void updateLocationInfo() {
        if (currentLatitude != 0 && currentLongitude != 0) {
            int radius = Math.max(5, seekBarRadius.getProgress());
            String info = String.format("📍 Location: %.6f, %.6f\n🔍 Search radius: %d km",
                currentLatitude, currentLongitude, radius);

            if (switchLocationFilter.isChecked()) {
                info += "\n✅ Products will be filtered and sorted by distance";
            }

            textLocationInfo.setText(info);
        }
    }

    /**
     * Clear saved location data
     */
    private void clearLocation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear Location")
            .setMessage("Are you sure you want to clear your saved location? This will disable location-based features.")
            .setPositiveButton("Clear", (dialog, which) -> {
                currentLatitude = 0;
                currentLongitude = 0;
                currentAddress = "";

                // Clear from preferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("latitude");
                editor.remove("longitude");
                editor.remove("address");
                editor.remove("last_updated");
                editor.putBoolean("location_filter_enabled", false);
                editor.apply();

                // Update UI
                textCurrentLocationDisplay.setText("No location set");
                switchLocationFilter.setChecked(false);
                updateLocationFilterInfo(false);

                Toast.makeText(this, "Location cleared successfully", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateUIBasedOnGPSMode(boolean useGPS) {
        if (useGPS) {
            editCustomLocation.setEnabled(false);
            btnSetCustomLocation.setEnabled(false);
            btnGetCurrentLocation.setEnabled(true);
            editCustomLocation.setHint("GPS location will be used");
        } else {
            editCustomLocation.setEnabled(true);
            btnSetCustomLocation.setEnabled(true);
            btnGetCurrentLocation.setEnabled(false);
            editCustomLocation.setHint("Enter custom location");
        }
    }

    private void getCurrentLocation() {
        if (!checkLocationPermissions()) {
            requestLocationPermissions();
            return;
        }

        btnGetCurrentLocation.setEnabled(false);
        btnGetCurrentLocation.setText("Getting location...");

        locationService.getCurrentLocation(new LocationService.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude, String address) {
                runOnUiThread(() -> {
                    currentLatitude = latitude;
                    currentLongitude = longitude;
                    currentAddress = address;

                    textCurrentLocationDisplay.setText("Current: " + address);
                    btnGetCurrentLocation.setEnabled(true);
                    btnGetCurrentLocation.setText("Get Current Location");

                    Toast.makeText(LocationSettingsActivity.this,
                        "Location updated: " + address, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onLocationError(String error) {
                runOnUiThread(() -> {
                    btnGetCurrentLocation.setEnabled(true);
                    btnGetCurrentLocation.setText("Get Current Location");
                    Toast.makeText(LocationSettingsActivity.this,
                        "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setCustomLocation() {
        String customLocation = editCustomLocation.getText().toString().trim();
        if (customLocation.isEmpty()) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSetCustomLocation.setEnabled(false);
        btnSetCustomLocation.setText("Setting location...");

        locationService.getLocationFromAddress(customLocation, new LocationService.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude, String address) {
                runOnUiThread(() -> {
                    currentLatitude = latitude;
                    currentLongitude = longitude;
                    currentAddress = address;

                    textCurrentLocationDisplay.setText("Custom: " + address);
                    btnSetCustomLocation.setEnabled(true);
                    btnSetCustomLocation.setText("Set Custom Location");

                    Toast.makeText(LocationSettingsActivity.this,
                        "Custom location set: " + address, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onLocationError(String error) {
                runOnUiThread(() -> {
                    btnSetCustomLocation.setEnabled(true);
                    btnSetCustomLocation.setText("Set Custom Location");
                    Toast.makeText(LocationSettingsActivity.this,
                        "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void saveSettings() {
        boolean useGPS = switchUseGPS.isChecked();
        boolean locationFilterEnabled = switchLocationFilter.isChecked();
        String customLocation = editCustomLocation.getText().toString().trim();
        int radius = Math.max(5, seekBarRadius.getProgress());

        // Validate that location is set if filter is enabled
        if (locationFilterEnabled && currentLatitude == 0 && currentLongitude == 0) {
            Toast.makeText(this, "Please set your location before enabling location filter", Toast.LENGTH_LONG).show();
            switchLocationFilter.setChecked(false);
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("use_gps", useGPS);
        editor.putBoolean("location_filter_enabled", locationFilterEnabled);
        editor.putString("custom_location", customLocation);
        editor.putInt("search_radius", radius);
        editor.putLong("latitude", Double.doubleToLongBits(currentLatitude));
        editor.putLong("longitude", Double.doubleToLongBits(currentLongitude));
        editor.putString("address", currentAddress);
        editor.putLong("last_updated", System.currentTimeMillis());
        editor.apply();

        String message = "Settings saved successfully";
        if (locationFilterEnabled) {
            message += "\nLocation-based filtering is now active";
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
            LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
