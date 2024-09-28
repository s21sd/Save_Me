package com.mobile.saveme;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private MapView mMap;
    private IMapController controller;
    private MyLocationNewOverlay mMyLocationOverlay;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                if (fineLocationGranted != null && fineLocationGranted) {
                    initializeMap();
                } else {
                    Log.e("MapActivity", "Location permission not granted");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        } else {
            initializeMap();
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("latitude") && intent.hasExtra("longitude")) {
            // Use getDoubleExtra() with a default value
            double latitude = intent.getDoubleExtra("latitude", 0.0);
            double longitude = intent.getDoubleExtra("longitude", 0.0);
            Log.d("MapActivity", "Received Latitude: " + latitude + ", Longitude: " + longitude);

            GeoPoint startPoint = new GeoPoint(latitude, longitude);
            controller.setCenter(startPoint);
        } else {
            Log.e("MapActivity", "No location data received.");
        }
    }

    private void initializeMap() {
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        );

        mMap = findViewById(R.id.osmmap);
        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setMultiTouchControls(true);

        mMyLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMap);
        mMyLocationOverlay.enableMyLocation();
        mMyLocationOverlay.enableFollowLocation();
        mMyLocationOverlay.setDrawAccuracyEnabled(true);
        mMap.getOverlays().add(mMyLocationOverlay);

        controller = mMap.getController();
        controller.setZoom(16.0);

        Intent intent = getIntent();
        double manualLatitude = intent.getDoubleExtra("latitude", 0.0);
        double manualLongitude = intent.getDoubleExtra("longitude", 0.0);
        GeoPoint manualLocation = new GeoPoint(manualLatitude, manualLongitude);

        mMyLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            GeoPoint currentLocation = mMyLocationOverlay.getMyLocation();
            if (currentLocation != null) {
                drawRoute(currentLocation, manualLocation);
                controller.setCenter(currentLocation);
            }
        }));
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        List<GeoPoint> points = new ArrayList<>();
        points.add(start);
        points.add(end);

        Polyline route = new Polyline();
        route.setPoints(points);
        route.setColor(Color.RED);
        route.setWidth(5.0f);

        mMap.getOverlays().add(route);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMap.onPause();
    }
}
