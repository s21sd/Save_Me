package com.mobile.saveme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.ResolvableApiException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int SMS_PERMISSION_CODE = 1;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 101;
    private static final int SPEECH_REQUEST_CODE = 101;

    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler;
    private static final int INTERVAL = 180000;
    private TextView tvLatLong;

    private String latitude = "0.0";
    private String longitude = "0.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvLatLong = findViewById(R.id.tvLatLong);
        Button btnStart = findViewById(R.id.btnStart);
        Button profileIcon = findViewById(R.id.profileIcon);
        Button helperLocation = findViewById(R.id.helperlocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler();

        // Request all necessary permissions
        requestAllPermissions();

        handleIncomingIntent(getIntent());

        profileIcon.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnStart.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                // First, get and send the current location
                getLastKnownLocation();

                // Then, start the recurring location update every 3 minutes
                startLocationUpdatesEveryThreeMinutes();
            } else {
                Toast.makeText(MainActivity.this, "Permission denied. Cannot send SMS.", Toast.LENGTH_SHORT).show();
            }
        });

        helperLocation.setOnClickListener(view -> {
            Intent intent3 = new Intent(MainActivity.this, MapActivity.class);
            intent3.putExtra("latitude", latitude);
            intent3.putExtra("longitude", longitude);
            startActivity(intent3);
        });
    }

    private void requestAllPermissions() {
        // Check if any permissions are missing
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // Request all necessary permissions in one go
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.RECORD_AUDIO
            }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean smsGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean locationGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean audioGranted = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                if (smsGranted && locationGranted && audioGranted) {
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permissions denied. Functionality may be limited.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && intent.hasExtra("message")) {
            String message = intent.getStringExtra("message");
            if (message != null && !message.isEmpty()) {
                Log.e("MainActivity", "Received message: " + message);
                extractLatLongFromMessage(message);
            } else {
                Log.e("MainActivity", "Message was empty or null.");
            }
        } else {
            Log.e("MainActivity", "No message extra found in Intent.");
        }
    }

    private void extractLatLongFromMessage(String message) {
        Log.d("MainActivity", "Extracting lat/long from message: " + message);
        try {
            String[] parts = message.split(",");
            if (parts.length == 2) {
                String[] latitudeParts = parts[0].split(":");
                String[] longitudeParts = parts[1].split(":");

                if (latitudeParts.length == 2 && longitudeParts.length == 2) {
                    latitude = latitudeParts[1].trim();
                    longitude = longitudeParts[1].trim();
                    Log.d("MainActivity", "Extracted Latitude: " + latitude + ", Longitude: " + longitude);
                    tvLatLong.setText("Latitude: " + latitude + ", Longitude: " + longitude);
                } else {
                    Log.e("MainActivity", "Latitude or Longitude format is incorrect.");
                }
            } else {
                Log.e("MainActivity", "Message format is incorrect.");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error parsing message: " + e.getMessage());
        }
    }

    public void speak(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Start Speaking");
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0).trim();
                if ("help".equalsIgnoreCase(spokenText) || "help me".equalsIgnoreCase(spokenText)) {
                    getLastKnownLocation();
                }
            }
        }
    }

    private void startLocationUpdatesEveryThreeMinutes() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkLocationSettings();
                handler.postDelayed(this, INTERVAL);
            }
        }, INTERVAL);
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> getLastKnownLocation());
        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MainActivity.this, LOCATION_SETTINGS_REQUEST_CODE);
                } catch (IntentSender.SendIntentException sendEx) {
                    sendEx.printStackTrace();
                }
            }
        });
    }

    private void getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Send SMS immediately with the current location
                            sendSmsWithLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            // Request a new location if last known is null
                            requestNewLocation();
                        }
                    });
        }
    }

    private void requestNewLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(INTERVAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    sendSmsWithLocation(location.getLatitude(), location.getLongitude());
                }
            }
        }, null);
    }

    private void sendSmsWithLocation(double latitude, double longitude) {
        SmsManager smsManager2 = SmsManager.getDefault();
        String message2 = "Help! I am at Location: lat: " + latitude + ", log: " + longitude;

        // Send to predefined number and contact list
        smsManager2.sendTextMessage("7905280916", null, message2, null, null);
        SharedPreferences sharedPreferences = getSharedPreferences("contacts_prefs", Context.MODE_PRIVATE);
        String json = sharedPreferences.getString("contactList", null);
        Log.d("SharedPreferences", "Stored JSON: " + json);

        if (json != null) {
            try {
                JSONArray contactArray = new JSONArray(json);
                SmsManager smsManager = SmsManager.getDefault();
                String message = "Help! I am at Location: lat: " + latitude + ", log: " + longitude;

                // Send to predefined number and contact list
                smsManager.sendTextMessage("7905280916", null, message, null, null);
                Toast.makeText(MainActivity.this, "SMS sent to 7905280916", Toast.LENGTH_SHORT).show();

                for (int i = 0; i < contactArray.length(); i++) {
                    JSONObject contact = contactArray.getJSONObject(i);
                    String phoneNumber = contact.getString("phoneNumber");
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                    Toast.makeText(MainActivity.this, "SMS sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error parsing contact list", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No contacts found.", Toast.LENGTH_SHORT).show();
        }
    }


}
