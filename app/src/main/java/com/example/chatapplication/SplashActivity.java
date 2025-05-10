package com.example.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.splash_progress);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        checkCurrentUser();
    }

    private void checkCurrentUser() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        progressBar.setVisibility(View.GONE);
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        } else {
                            startActivity(new Intent(SplashActivity.this, ProfileSetupActivity.class));
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        finish();
                    });
        } else {
            progressBar.setVisibility(View.GONE);
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }
    }
}