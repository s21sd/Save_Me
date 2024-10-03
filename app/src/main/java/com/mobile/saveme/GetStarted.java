package com.mobile.saveme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class GetStarted extends AppCompatActivity {

    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        auth = FirebaseAuth.getInstance();

        Button getStarted = findViewById(R.id.getstartbtn);
        getStarted.setOnClickListener(v -> checkUserLogin());
    }

    private void checkUserLogin() {
        Intent intent;

        if (auth.getCurrentUser() != null) {
            intent = new Intent(GetStarted.this, MainActivity.class);
        } else {
            intent = new Intent(GetStarted.this, LoginActivity.class);
            Toast.makeText(GetStarted.this, "Please log in to continue", Toast.LENGTH_SHORT).show();
        }

        startActivity(intent);
        finish();
    }
}
