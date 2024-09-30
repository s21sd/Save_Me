package com.mobile.saveme;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class ChoiceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        LottieAnimationView lottieAnimationView = findViewById(R.id.animation_view);
        lottieAnimationView.playAnimation();

        double latitude = getIntent().getDoubleExtra("latitude", 0);
        double longitude = getIntent().getDoubleExtra("longitude", 0);
        String phoneNumber=getIntent().getStringExtra("phoneNumber");

        Button btnInApp = findViewById(R.id.btn_in_app);
        Button btnGoogleMaps = findViewById(R.id.btn_google_maps);
        Button sendbtn=findViewById(R.id.sendmsgbtn);


        btnInApp.setOnClickListener(v -> {
            Intent mapIntent = new Intent(ChoiceActivity.this, MapActivity.class);
            mapIntent.putExtra("latitude", latitude);
            mapIntent.putExtra("longitude", longitude);
            startActivity(mapIntent);
            finish();
        });

        btnGoogleMaps.setOnClickListener(v -> {
            String geoUri = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude;
            Intent googleMapsIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(geoUri));
            googleMapsIntent.setPackage("com.google.android.apps.maps");
            startActivity(googleMapsIntent);
            finish();
        });

        sendbtn.setOnClickListener(view->{
            String message="We will save you, be safe!";
            if (phoneNumber != null && message != null) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(this, "Message sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
