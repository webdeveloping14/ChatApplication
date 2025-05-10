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
                            Toast.makeText(LoginActivity.this,
                                    "Failed to send reset email: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
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
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            checkUserProfileAndNavigate(user);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserProfileAndNavigate(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    } else {
                        createBasicUserProfile(user);
                    }
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(LoginActivity.this,
                                "Error checking profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void createBasicUserProfile(FirebaseUser user) {
        UserProfile profile = new UserProfile();
        profile.setEmail(user.getEmail());
        if (user.getDisplayName() != null) {
            profile.setDisplayName(user.getDisplayName());
        }

        db.collection("users").document(user.getUid())
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    startActivity(new Intent(LoginActivity.this, ProfileSetupActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(LoginActivity.this,
                                "Error creating profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
}
