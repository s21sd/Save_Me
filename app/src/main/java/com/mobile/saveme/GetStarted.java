package com.mobile.saveme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class GetStarted extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        Button getStarted= findViewById(R.id.getstartbtn);
        getStarted.setOnClickListener(v -> checkUserLogin());
    }
    private void checkUserLogin() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Intent intent;

        if (auth.getCurrentUser() != null) {
            intent = new Intent(GetStarted.this, MainActivity.class);
        } else {
            intent = new Intent(GetStarted.this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
