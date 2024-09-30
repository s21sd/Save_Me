package com.mobile.saveme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapActivity extends AppCompatActivity {

    private MapView mMap;
    private IMapController controller;
    private MyLocationNewOverlay mMyLocationOverlay;
    Button backButton;


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
        backButton=findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, ChoiceActivity.class);
            startActivity(intent);
            finish();
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        } else {
            initializeMap();
        }


        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("latitude") && intent.hasExtra("longitude")) {
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
                if(isNetworkAvailable()){

                    drawRoute(currentLocation, manualLocation);
                }
                else{
                    drawRouteWithoutNet(currentLocation,manualLocation);
                }
                controller.setCenter(currentLocation);
            }
        }));
    }

    private void drawRouteWithoutNet(GeoPoint start, GeoPoint end) {
        List<GeoPoint> points = new ArrayList<>();
        points.add(start);
        points.add(end);

        Polyline route = new Polyline();
        route.setPoints(points);
        route.setColor(Color.BLUE);
        route.setWidth(5.0f);

        mMap.getOverlays().add(route);
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {

        OkHttpClient client = new OkHttpClient();


        String apiKey = "4dce91b6-74dc-4494-89f2-017f99842e08";
        String url = "https://graphhopper.com/api/1/route?key=" + apiKey;

        // Request body for the API call
        String jsonBody = "{\n" +
                "  \"profile\": \"bike\",\n" +
                "  \"points\": [\n" +
                "    [" + start.getLongitude() + ", " + start.getLatitude() + "],\n" +
                "    [" + end.getLongitude() + ", " + end.getLatitude() + "]\n" +
                "  ],\n" +
                "  \"point_hints\": [\n" +
                "    \"Lindenschmitstraße\",\n" +
                "    \"Thalkirchener Str.\"\n" +
                "  ],\n" +
                "  \"snap_preventions\": [\n" +
                "    \"motorway\",\n" +
                "    \"ferry\",\n" +
                "    \"tunnel\"\n" +
                "  ],\n" +
                "  \"details\": [\"road_class\", \"surface\"]\n" +
                "}";

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        // Request setup
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // Make the network call asynchronously
        client.newCall(request).enqueue(new Callback() {

            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.e("MapActivity", "API call failed");
            }

            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONArray paths = jsonResponse.getJSONArray("paths");
                        if (paths.length() > 0) {
                            JSONObject path = paths.getJSONObject(0);
                            String encodedPolyline = path.getString("points");

                            List<GeoPoint> routePoints = decodePolyline(encodedPolyline);

                            runOnUiThread(() -> {
                                Polyline routeLine = new Polyline();
                                routeLine.setPoints(routePoints);
                                routeLine.setColor(Color.BLUE);
                                routeLine.setWidth(5.0f);
                                mMap.getOverlays().add(routeLine);
                                mMap.invalidate();
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("MapActivity", "Failed to parse JSON response");
                    }
                } else {
                    Log.e("MapActivity", "API call was not successful");
                }
            }

            private List<GeoPoint> decodePolyline(String encodedPolyline) {
                List<GeoPoint> polylinePoints = new ArrayList<>();
                int index = 0, len = encodedPolyline.length();
                int lat = 0, lon = 0;

                while (index < len) {
                    int b, shift = 0, result = 0;
                    do {
                        b = encodedPolyline.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);
                    int deltaLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lat += deltaLat;

                    shift = 0;
                    result = 0;
                    do {
                        b = encodedPolyline.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);
                    int deltaLon = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lon += deltaLon;

                    GeoPoint point = new GeoPoint(lat / 1E5, lon / 1E5);
                    polylinePoints.add(point);
                }

                return polylinePoints;
            }


        });
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                return connectivityManager.getActiveNetworkInfo() != null &&
                        connectivityManager.getActiveNetworkInfo().isConnected();
            }
        }
        return false;
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
