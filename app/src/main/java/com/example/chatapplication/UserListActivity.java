package com.example.chatapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class UserListActivity extends AppCompatActivity {

    private RecyclerView usersRecyclerView;
    private UserAdapter userAdapter;
    private ProgressBar progressBar;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this);
        usersRecyclerView.setAdapter(userAdapter);
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        fetchUsers();
    }

    private void fetchUsers() {
        progressBar.setVisibility(View.VISIBLE);

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Userr> userList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Userr user = new Userr();
                    user.setId(snapshot.getKey());
                    if (snapshot.child("createdAt").exists()) {
                        user.setCreatedAt(snapshot.child("createdAt").getValue(String.class));
                    }
                    if (snapshot.child("email").exists()) {
                        user.setEmail(snapshot.child("email").getValue(String.class));
                    }
                    if (snapshot.child("name").exists()) {
                        user.setName(snapshot.child("name").getValue(String.class));
                    }
                    if (snapshot.child("profileImageUrl").exists()) {
                        user.setProfileImageUrl(snapshot.child("profileImageUrl").getValue(String.class));
                    }
                    if (snapshot.child("status").exists()) {
                        user.setStatus(snapshot.child("status").getValue(String.class));
                    }
                    if (snapshot.child("username").exists()) {
                        user.setUsername(snapshot.child("username").getValue(String.class));
                    }

                    userList.add(user);
                }

                userAdapter.setUsers(userList);
                progressBar.setVisibility(View.GONE);

                if (userList.isEmpty()) {
                    //empty
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}