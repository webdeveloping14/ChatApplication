package com.example.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class LoginActivity extends AppCompatActivity {

    private TextView login_reg, forgot_password;
    private TextInputEditText email, password;
    private Button loginBtn;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);

        login_reg = findViewById(R.id.register_log);
        email = findViewById(R.id.email_log);
        password = findViewById(R.id.password_log);
        loginBtn = findViewById(R.id.login_button);
        forgot_password = findViewById(R.id.forgot_password);
        progressBar = findViewById(R.id.progress_bar);

        forgot_password.setOnClickListener(view -> {
            String emailInput = email.getText() != null ? email.getText().toString().trim() : "";
            if (TextUtils.isEmpty(emailInput)) {
                Toast.makeText(LoginActivity.this, "Please enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            auth.sendPasswordResetEmail(emailInput)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this,
                                    "Password reset email sent to " + emailInput,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            if (task.getException() != null) {
                                Toast.makeText(LoginActivity.this,
                                        "Failed to send reset email: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Failed to send reset email. Please try again later.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        loginBtn.setOnClickListener(view -> {
            String userEmail = email.getText() != null ? email.getText().toString().trim() : "";
            String userPassword = password.getText() != null ? password.getText().toString().trim() : "";
            signInFirebase(userEmail, userPassword);
        });

        login_reg.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    private void signInFirebase(@NonNull String userEmail, String userPassword) {
        if (TextUtils.isEmpty(userEmail)) {
            email.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(userPassword)) {
            password.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            checkUserProfileAndNavigate(user);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this,
                                    "Authentication error: User is null",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        String errorMsg = "Authentication failed";
                        if (task.getException() != null) {
                            errorMsg += ": " + task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserProfileAndNavigate(FirebaseUser user) {
        try {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        progressBar.setVisibility(View.GONE);
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            createBasicUserProfile(user);
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this,
                                "Error checking profile: " + e.getMessage() + ". Trying to create profile...",
                                Toast.LENGTH_SHORT).show();

                        createBasicUserProfile(user);
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(LoginActivity.this,
                    "Unexpected error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void createBasicUserProfile(FirebaseUser user) {
        UserProfile profile = new UserProfile();
        profile.setEmail(user.getEmail());
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            profile.setDisplayName(user.getDisplayName());
        } else {
            String email = user.getEmail();
            if (email != null && email.contains("@")) {
                String username = email.substring(0, email.indexOf('@'));
                profile.setDisplayName(username);
            } else {
                profile.setDisplayName("User");
            }
        }

        db.collection("users").document(user.getUid())
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    startActivity(new Intent(LoginActivity.this, ProfileSetupActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this,
                            "Error creating profile: " + e.getMessage() + ". Proceeding to main activity.",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                });
    }
}