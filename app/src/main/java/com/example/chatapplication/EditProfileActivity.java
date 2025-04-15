package com.example.chatapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView emailTextView, accountCreatedTextView;
    private EditText nameEditText, usernameEditText, statusEditText;
    private View loadingLayout;

    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private StorageReference storageRef;
    private String userId;
    private Uri selectedImageUri = null;
    private String currentProfileImageUrl = "";

    // Activity result launcher for image picking
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        profileImageView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase components
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User not logged in, redirect to login
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        emailTextView = findViewById(R.id.emailTextView);
        nameEditText = findViewById(R.id.nameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        statusEditText = findViewById(R.id.statusEditText);
        accountCreatedTextView = findViewById(R.id.accountCreatedTextView);
        loadingLayout = findViewById(R.id.loadingLayout);

        // Set click listeners
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.saveButton).setOnClickListener(v -> saveProfileChanges());
        findViewById(R.id.changePhotoButton).setOnClickListener(v -> openImagePicker());

        // Load user data
        loadUserData();
    }

    private void loadUserData() {
        showLoading(true);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("email").exists()) {
                        String email = snapshot.child("email").getValue(String.class);
                        emailTextView.setText(email);
                    }

                    if (snapshot.child("name").exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        nameEditText.setText(name);
                    }

                    if (snapshot.child("username").exists()) {
                        String username = snapshot.child("username").getValue(String.class);
                        usernameEditText.setText(username);
                    }

                    if (snapshot.child("status").exists()) {
                        String status = snapshot.child("status").getValue(String.class);
                        statusEditText.setText(status);
                    }

                    if (snapshot.child("profileImageUrl").exists()) {
                        currentProfileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                        if (!TextUtils.isEmpty(currentProfileImageUrl)) {
                            Glide.with(EditProfileActivity.this)
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(profileImageView);
                        }
                    }

                    if (snapshot.child("createdAt").exists()) {
                        String createdAt = snapshot.child("createdAt").getValue(String.class);
                        displayFormattedCreationDate(createdAt);
                    }
                }
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(EditProfileActivity.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayFormattedCreationDate(String createdAtStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            Date createdAt = inputFormat.parse(createdAtStr);
            String formattedDate = outputFormat.format(createdAt);
            accountCreatedTextView.setText("Account created on " + formattedDate);
        } catch (ParseException e) {
            accountCreatedTextView.setText("Account created on " + createdAtStr);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Profile Image"));
    }

    private void saveProfileChanges() {
        String name = nameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String status = statusEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Username is required");
            usernameEditText.requestFocus();
            return;
        }

        showLoading(true);

        // If user selected a new image, upload it first
        if (selectedImageUri != null) {
            uploadImage(name, username, status);
        } else {
            // No new image, just update the profile data
            updateProfile(name, username, status, currentProfileImageUrl);
        }
    }

    private void uploadImage(String name, String username, String status) {
        // Create a unique file name based on user ID
        final StorageReference imageRef = storageRef.child(userId + ".jpg");

        try {
            // Compress the image before uploading
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            // Upload the image
            UploadTask uploadTask = imageRef.putBytes(data);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get the download URL
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    updateProfile(name, username, status, imageUrl);
                }).addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(EditProfileActivity.this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                showLoading(false);
                Toast.makeText(EditProfileActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            showLoading(false);
            Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void updateProfile(String name, String username, String status, String profileImageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("username", username);
        updates.put("status", status);

        // Only update image URL if it's not empty
        if (!TextUtils.isEmpty(profileImageUrl)) {
            updates.put("profileImageUrl", profileImageUrl);
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}