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
    private static final int LOCATION_PERMISSION_CODE = 102;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 101;
    private static final int SPEECH_REQUEST_CODE = 101;

    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler;
    private static final int INTERVAL = 300000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btnStart);
        Button profileIcon = findViewById(R.id.profileIcon);
        profileIcon.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler();

        requestPermissions();

        if (checkPermissions()) {
            startVoiceService();
        } else {
            requestPermissionsForAudio();
        }

        btnStart.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdatesEveryThreeMinutes();
            } else {
                Toast.makeText(MainActivity.this, "Permission denied. Cannot send SMS.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsForAudio() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }

    private void startVoiceService() {
        Intent serviceIntent = new Intent(this, VoiceService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startVoiceService();
                } else {
                    Toast.makeText(this, "Permission denied, cannot start voice service", Toast.LENGTH_SHORT).show();
                }
                break;
            case SMS_PERMISSION_CODE:
                Toast.makeText(this, grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ? "SMS permission granted" : "SMS permission denied", Toast.LENGTH_SHORT).show();
                break;
            case LOCATION_PERMISSION_CODE:
                Toast.makeText(this, grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ? "Location permission granted" : "Location permission denied", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
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
        Runnable locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                checkLocationSettings();
                handler.postDelayed(this, INTERVAL);
            }
        };
        handler.post(locationUpdateRunnable);
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

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
                            sendSmsWithLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            requestNewLocation();
                        }
                    });
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestNewLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(300000);
        locationRequest.setFastestInterval(300000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location updatedLocation = locationResult.getLastLocation();
                if (updatedLocation != null) {
                    sendSmsWithLocation(updatedLocation.getLatitude(), updatedLocation.getLongitude());
                }
            }
        }, getMainLooper());
    }
    private void sendSmsWithLocation(double latitude, double longitude) {
        SharedPreferences sharedPreferences = getSharedPreferences("contacts_prefs", Context.MODE_PRIVATE);
        String json = sharedPreferences.getString("contactList", null);
        Log.d("SharedPreferences", "Stored JSON: " + json);

        if (json != null) {
            try {
                JSONArray contactArray = new JSONArray(json);
                SmsManager smsManager = SmsManager.getDefault();
                String message = "Location: lat: " + latitude + ", log: " + longitude;
                smsManager.sendTextMessage("7905280916", null, message, null, null);
                Toast.makeText(MainActivity.this, "SMS sent to " + "7905280916", Toast.LENGTH_SHORT).show();
                for (int i = 1; i < contactArray.length(); i++) {
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
            Toast.makeText(this, "No contacts available to send SMS", Toast.LENGTH_SHORT).show();
        }
    }

}
