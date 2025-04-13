package com.example.chatapplication;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private CircleImageView profileImage;
    private TextView textUsername;
    private TextView textBio, textEmail;
    private ViewPager2 viewPager;
    private MaterialButton btnEditProfile;
    private MaterialButton btnStartChat;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();
        profileImage = findViewById(R.id.profile_image);
        textUsername = findViewById(R.id.text_username);
        textBio = findViewById(R.id.text_bio);
        viewPager = findViewById(R.id.view_pager);
        btnEditProfile = findViewById(R.id.button_edit_profile);
        btnStartChat = findViewById(R.id.button_start_chat);
        progressBar = findViewById(R.id.progress_bar);
        textEmail = findViewById(R.id.text_email);
        setupButtonListeners();
        loadProfileData();
    }

    private void setupButtonListeners() {
        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Edit Profile clicked", Toast.LENGTH_SHORT).show();
            }
        });

        btnStartChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Start Chat clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileData() {
        progressBar.setVisibility(View.VISIBLE);
        if (currentUser == null) {
            Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }
        String userId = currentUser.getUid();
        loadProfileDetailsFromDatabase(userId);
        loadProfileImageFromStorage(userId);
    }

    private void loadProfileDetailsFromDatabase(String userId) {
        DatabaseReference userRef = databaseReference.child("users").child(userId);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);

                    if (user != null) {
                        updateUserProfileUI(user);
                    } else {
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String status = dataSnapshot.child("status").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);

                        if (username != null) {
                            User manualUser = new User(username, status, email);
                            updateUserProfileUI(manualUser);
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "User not found in database", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: ", databaseError.toException());
                Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadProfileImageFromStorage(String userId) {
        StorageReference imageRef = storageReference.child("profile_images").child(userId + ".jpg");
        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(MainActivity.this)
                        .load(uri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(profileImage);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Failed to load profile image: ", e);
                profileImage.setImageResource(R.drawable.default_profile);
            }
        });
    }

    private void updateUserProfileUI(User user) {
        textUsername.setText("Username: " + user.getUsername());
        textBio.setText(user.getStatus());
        textEmail.setText("Email: " + user.getEmail());
        progressBar.setVisibility(View.GONE);
    }

    public static class User {
        private String username;
        private String status;
        private String email;
        private String profileImageUrl;
        private String createdAt;

        public User() {
        }

        public User(String username, String status, String email) {
            this.username = username;
            this.status = status;
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        // For backward compatibility
        public String getBio() {
            return status;
        }

        // For backward compatibility
        public void setBio(String bio) {
            this.status = bio;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getProfileImageUrl() {
            return profileImageUrl;
        }

        public void setProfileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}