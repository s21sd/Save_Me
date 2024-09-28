package com.mobile.saveme;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ChoiceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        double latitude = getIntent().getDoubleExtra("latitude", 0);
        double longitude = getIntent().getDoubleExtra("longitude", 0);

        Button btnInApp = findViewById(R.id.btn_in_app);
        Button btnGoogleMaps = findViewById(R.id.btn_google_maps);

        btnInApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(ChoiceActivity.this, MapActivity.class);
                mapIntent.putExtra("latitude", latitude);
                mapIntent.putExtra("longitude", longitude);
                startActivity(mapIntent);
                finish(); // Close this activity
            }
        });

        btnGoogleMaps.setOnClickListener(v -> {
            String geoUri = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude;
            Intent googleMapsIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(geoUri));
            googleMapsIntent.setPackage("com.google.android.apps.maps");
            startActivity(googleMapsIntent);
            finish(); // Close this activity
        });
    }
}
