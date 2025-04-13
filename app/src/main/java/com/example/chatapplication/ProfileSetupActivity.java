package com.example.chatapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
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
import androidx.core.app.ActivityCompat;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private CardView profileImageCard;
    private ImageView profileImageView;
    private View loadingLayout;
    private TextView loadingText;
    private TextInputEditText statusEditText;

    private Uri currentPhotoUri;
    private String profilePhotoPath;
    private boolean isPhotoChanged = false;

    // Firebase components
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

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleGalleryImage(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Check if user is logged in
        if (currentUser == null) {
            // Redirect to login screen
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
        String userId = currentUser.getUid();

        loadingLayout.setVisibility(View.VISIBLE);
        loadingText.setText(R.string.loading_profile);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);

                        if (profile != null) {
                            // Set status
                            if (profile.getStatus() != null && !profile.getStatus().isEmpty()) {
                                statusEditText.setText(profile.getStatus());
                            }

                            // Load profile image if available
                            if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
                                previousImageUrl = profile.getProfileImageUrl();
                                Glide.with(this)
                                        .load(profile.getProfileImageUrl())
                                        .apply(new RequestOptions()
                                                .placeholder(R.drawable.default_profile)
                                                .error(R.drawable.default_profile)
                                                .diskCacheStrategy(DiskCacheStrategy.ALL))
                                        .into(profileImageView);
                            }
                        }
                    }
                    // Hide loading
                    loadingLayout.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    // Error handling
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    loadingLayout.setVisibility(View.GONE);
                });
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
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
            }
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);

        try {
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            profilePhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void handleCameraImage() {
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
        if (imageUri == null) return;

        isPhotoChanged = true;
        showImageLoadingProgress(true);

        try {
            // Create a file to save the selected image
            File photoFile = createImageFile();
            if (photoFile != null) {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                processAndDisplayImage(bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showImageLoadingProgress(false);
            Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show();
        }
    }

    private void processAndDisplayImage(Bitmap bitmap) {
        // Save compressed bitmap to file
        try {
            File imageFile = new File(profilePhotoPath);
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            out.close();

            // Load the image with Glide
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
        findViewById(R.id.image_loading_progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void saveProfileAndNavigate() {
        // Show loading spinner
        loadingLayout.setVisibility(View.VISIBLE);
        loadingText.setText(R.string.updating_profile);

        String userId = currentUser.getUid();
        String status = statusEditText.getText().toString().trim();

        // Create user profile object
        UserProfile profile = new UserProfile();
        profile.setStatus(status);

        // If previous image URL exists and image not changed, keep using it
        if (!isPhotoChanged && previousImageUrl != null) {
            profile.setProfileImageUrl(previousImageUrl);
            saveUserProfile(userId, profile);
        }
        // If profile photo was changed, upload it first
        else if (isPhotoChanged && profilePhotoPath != null) {
            uploadProfileImageAndSaveProfile(userId, profile);
        } else {
            // No image change or previous image
            saveUserProfile(userId, profile);
        }
    }

    private void uploadProfileImageAndSaveProfile(String userId, UserProfile profile) {
        // Create file reference
        StorageReference imageRef = storageRef.child("profile_images/" + userId + ".jpg");

        Uri fileUri = Uri.fromFile(new File(profilePhotoPath));

        // Upload file
        imageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        profile.setProfileImageUrl(uri.toString());
                        saveUserProfile(userId, profile);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileSetupActivity.this,
                            "Failed to upload image: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    loadingLayout.setVisibility(View.GONE);
                });
    }

    private void saveUserProfile(String userId, UserProfile profile) {
        // Save to Firestore
        db.collection("users").document(userId)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Hide loading spinner
                    loadingLayout.setVisibility(View.GONE);

                    // Navigate to MainActivity
                    Intent intent = new Intent(ProfileSetupActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileSetupActivity.this,
                            "Failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    loadingLayout.setVisibility(View.GONE);
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show();
            }
        }
    }
}