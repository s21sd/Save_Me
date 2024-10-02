package com.mobile.saveme;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class ChoiceActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private double actualLatitude;
    private double actualLongitude;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        LottieAnimationView lottieAnimationView = findViewById(R.id.animation_view);
        lottieAnimationView.playAnimation();

        double intentLatitude = getIntent().getDoubleExtra("latitude", 0);
        double intentLongitude = getIntent().getDoubleExtra("longitude", 0);
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        Button btnInApp = findViewById(R.id.btn_in_app);
        Button btnGoogleMaps = findViewById(R.id.btn_google_maps);
        Button sendbtn = findViewById(R.id.sendmsgbtn);
        Button btnCall = findViewById(R.id.btn_call);

        btnInApp.setOnClickListener(v -> {
            Intent mapIntent = new Intent(ChoiceActivity.this, MapActivity.class);
            mapIntent.putExtra("latitude", intentLatitude);
            mapIntent.putExtra("longitude", intentLongitude);
            startActivity(mapIntent);
            finish();
        });

        btnGoogleMaps.setOnClickListener(v -> {
            String geoUri = "geo:" + intentLatitude + "," + intentLongitude + "?q=" + intentLatitude + "," + intentLongitude;
            Intent googleMapsIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(geoUri));
            googleMapsIntent.setPackage("com.google.android.apps.maps");
            startActivity(googleMapsIntent);
            finish();
        });

        sendbtn.setOnClickListener(view -> {
            getCurrentLocationAndSendSms();
        });

        sendbtn.setOnClickListener(view -> getCurrentLocationAndSendSms());

        // Call button functionality
        btnCall.setOnClickListener(view -> {
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                makePhoneCall(phoneNumber);
            } else {
                Toast.makeText(ChoiceActivity.this, "No phone number provided.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makePhoneCall(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 102);
            return;
        }
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }

    private void getCurrentLocationAndSendSms() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                actualLatitude = location.getLatitude();
                actualLongitude = location.getLongitude();

                String messageToSend = "We will save you, be safe! Latitude: " + actualLatitude + ", Longitude: " + actualLongitude;
                sendSmsWithLocation(phoneNumber, messageToSend);
            } else {
                requestNewLocation();
            }
        });
    }

    private void requestNewLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationProviderClient.removeLocationUpdates(this);
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    actualLatitude = location.getLatitude();
                    actualLongitude = location.getLongitude();

                    // Send SMS with the new location
                    String messageToSend = "We will save you, be safe! Latitude: " + actualLatitude + ", Longitude: " + actualLongitude;
                    sendSmsWithLocation(phoneNumber, messageToSend);
                } else {
                    Toast.makeText(ChoiceActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                }
            }
        }, getMainLooper());
    }

    private void sendSmsWithLocation(String phoneNumber, String message) {
        if (phoneNumber != null && message != null) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Message sent to " + phoneNumber + " with location.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndSendSms();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
