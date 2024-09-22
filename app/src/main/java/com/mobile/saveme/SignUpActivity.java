package com.mobile.saveme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {
    private Button btnCreate,btnLogin;
    private EditText editNameSignup, editEmailSignup, editPasswordSignup;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();

        editNameSignup = findViewById(R.id.etName);
        editEmailSignup = findViewById(R.id.etEmail);
        editPasswordSignup = findViewById(R.id.etPassword);
        btnCreate = findViewById(R.id.btnSignUp);
        btnLogin=findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(view -> {
            Intent intent =new Intent(SignUpActivity.this,LoginActivity.class);
            startActivity(intent);
        });

        btnCreate.setOnClickListener(view -> {
            String name = editNameSignup.getText().toString().trim();
            String email = editEmailSignup.getText().toString().trim();
            String password = editPasswordSignup.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "No empty fields are allowed", Toast.LENGTH_SHORT).show();
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(SignUpActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                finish();
                            } else {
                                Toast.makeText(SignUpActivity.this, "Sign Up Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}
