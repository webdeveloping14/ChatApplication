package com.example.chatapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private CardView profileImageCard;
    private ImageView profileImageView;
    private View loadingLayout;
    private TextView loadingText;
    private TextInputEditText statusEditText, nameEditText;
    private View imageLoadingProgress;

    private Uri currentPhotoUri;
    private String profilePhotoPath;
    private boolean isPhotoChanged = false;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String previousImageUrl;

    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    handleCameraImage();
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    handleGalleryImage(uri);
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            openCamera();
                        } else {
                            Toast.makeText(this, getString(R.string.camera_permission_required),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadUserProfileFromFirebase();
    }

    private void initViews() {
        profileImageCard = findViewById(R.id.profile_image_card);
        profileImageView = findViewById(R.id.profile_image_view);
        loadingLayout = findViewById(R.id.loading_layout);
        loadingText = findViewById(R.id.loading_text);
        statusEditText = findViewById(R.id.status_profile);
        nameEditText = findViewById(R.id.display_name);
        imageLoadingProgress = findViewById(R.id.image_loading_progress);

        FloatingActionButton cameraBtn = findViewById(R.id.camera_btn);
        MaterialButton saveButton = findViewById(R.id.save_button);

        cameraBtn.setOnClickListener(v -> showImageSourceDialog());
        saveButton.setOnClickListener(v -> saveProfileAndNavigate());
    }

    private void setupListeners() {
        profileImageCard.setOnClickListener(v -> showImageSourceDialog());
        findViewById(R.id.change_photo).setOnClickListener(v -> showImageSourceDialog());
    }

    private void loadUserProfileFromFirebase() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        loadingLayout.setVisibility(View.VISIBLE);
        loadingText.setText(R.string.loading_profile);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        String display = documentSnapshot.getString("Name");
                        previousImageUrl = documentSnapshot.getString("profileImageUrl");

                        if (status != null && !status.isEmpty()) {
                            statusEditText.setText(status);
                        }
                        if (display != null && !display.isEmpty()) {
                            nameEditText.setText(display);
                        }
                        if (previousImageUrl != null && !previousImageUrl.isEmpty()) {
                            loadProfileImage(previousImageUrl);
                        }
                    }
                    loadingLayout.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    loadingLayout.setVisibility(View.GONE);
                });
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(profileImageView);
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_from_gallery)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.change_profile_picture));
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpenCamera();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                takePictureLauncher.launch(takePictureIntent);
            } else {
                Toast.makeText(this, getString(R.string.error_creating_image_file),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.no_camera_app_found),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);

        try {
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            profilePhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_creating_image_file),
                    Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private void handleCameraImage() {
        if (currentPhotoUri == null) {
            Toast.makeText(this, getString(R.string.error_loading_image),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        isPhotoChanged = true;
        showImageLoadingProgress(true);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), currentPhotoUri);
            processAndDisplayImage(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            showImageLoadingProgress(false);
            Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGalleryImage(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, getString(R.string.error_loading_image),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        isPhotoChanged = true;
        showImageLoadingProgress(true);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            File photoFile = createImageFile();
            if (photoFile != null) {
                currentPhotoUri = Uri.fromFile(photoFile);
                processAndDisplayImage(bitmap);
            } else {
                showImageLoadingProgress(false);
                Toast.makeText(this, getString(R.string.error_creating_image_file),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showImageLoadingProgress(false);
            Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show();
        }
    }

    private void processAndDisplayImage(Bitmap bitmap) {
        if (bitmap == null || profilePhotoPath == null) {
            showImageLoadingProgress(false);
            Toast.makeText(this, getString(R.string.error_processing_image),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File imageFile = new File(profilePhotoPath);
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            out.close();

            Glide.with(this)
                    .load(imageFile)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true))
                    .into(profileImageView);

            showImageLoadingProgress(false);
        } catch (IOException e) {
            e.printStackTrace();
            showImageLoadingProgress(false);
            Toast.makeText(this, getString(R.string.error_saving_image), Toast.LENGTH_SHORT).show();
        }
    }

    private void showImageLoadingProgress(boolean show) {
        imageLoadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void saveProfileAndNavigate() {
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.user_not_logged_in),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        loadingLayout.setVisibility(View.VISIBLE);
        loadingText.setText(R.string.updating_profile);

        String userId = currentUser.getUid();
        String status = statusEditText.getText().toString().trim();

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("status", status);

        if (isPhotoChanged && profilePhotoPath != null) {
            uploadProfileImageAndSaveProfile(userId, profileUpdates);
        } else if (previousImageUrl != null && !previousImageUrl.isEmpty()) {
            profileUpdates.put("profileImageUrl", previousImageUrl);
            saveUserProfile(userId, profileUpdates);
        } else {
            saveUserProfile(userId, profileUpdates);
        }
    }

    private void uploadProfileImageAndSaveProfile(String userId, Map<String, Object> profileUpdates) {
        if (profilePhotoPath == null) {
            saveUserProfile(userId, profileUpdates);
            return;
        }

        File imageFile = new File(profilePhotoPath);
        if (!imageFile.exists()) {
            Toast.makeText(this, getString(R.string.error_image_not_found),
                    Toast.LENGTH_SHORT).show();
            loadingLayout.setVisibility(View.GONE);
            return;
        }

        StorageReference imageRef = storageRef.child("profile_images/" + userId + ".jpg");
        Uri fileUri = Uri.fromFile(imageFile);

        if (previousImageUrl != null && !previousImageUrl.isEmpty()) {
            StorageReference oldImageRef = storage.getReferenceFromUrl(previousImageUrl);
            oldImageRef.delete().addOnCompleteListener(task -> {
                performImageUpload(imageRef, fileUri, userId, profileUpdates);
            });
        } else {
            performImageUpload(imageRef, fileUri, userId, profileUpdates);
        }
    }

    private void performImageUpload(StorageReference imageRef, Uri fileUri, String userId, Map<String, Object> profileUpdates) {
        loadingLayout.setVisibility(View.VISIBLE); // Show loading

        imageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        profileUpdates.put("profileImageUrl", uri.toString());
                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                                .updateChildren(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    loadingLayout.setVisibility(View.GONE); // Hide loading
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getApplicationContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    loadingLayout.setVisibility(View.GONE); // Hide loading
                    Toast.makeText(getApplicationContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfile(String userId, Map<String, Object> profileUpdates) {
        db.collection("users").document(userId)
                .update(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    loadingLayout.setVisibility(View.GONE);
                    Toast.makeText(this, "✅ Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Or navigate somewhere
                })
                .addOnFailureListener(e -> {
                    loadingLayout.setVisibility(View.GONE);
                    Toast.makeText(this, "❌ Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}