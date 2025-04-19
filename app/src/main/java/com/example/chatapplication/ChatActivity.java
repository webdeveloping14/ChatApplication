package com.example.chatapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView username, userStatus;
    private ImageView btnBack;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private String receiverId, receiverName, receiverImage;
    private String senderId;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        senderId = currentUser.getUid();
        receiverId = getIntent().getStringExtra("userId");
        receiverName = getIntent().getStringExtra("userName");
        receiverImage = getIntent().getStringExtra("userImage");
        profileImage = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        userStatus = findViewById(R.id.user_status);
        btnBack = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.messages_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        username.setText(receiverName);
        if (receiverImage != null && !receiverImage.isEmpty()) {
            Glide.with(this).load(receiverImage).placeholder(R.drawable.default_profile).into(profileImage);
        }
        checkUserStatus();
        btnBack.setOnClickListener(v -> finish());
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, senderId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);
        loadMessages();

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void checkUserStatus() {
        db.collection("users").document(receiverId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Boolean online = documentSnapshot.getBoolean("online");
                        userStatus.setText(online != null && online ? "Online" : "Offline");
                    }
                });
    }

    private void loadMessages() {
        String chatId = getChatId(senderId, receiverId);

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(ChatActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message message = dc.getDocument().toObject(Message.class);
                                messageList.add(message);
                            }
                        }
                        messageAdapter.notifyDataSetChanged();

                        if (messageList.size() > 0) {
                            recyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        String msg = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        messageInput.setText("");

        long timestamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String time = sdf.format(new Date(timestamp));

        Message message = new Message(senderId, receiverId, msg, timestamp, time);
        String chatId = getChatId(senderId, receiverId);

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    updateLastMessage(chatId, msg, timestamp);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLastMessage(String chatId, String message, long timestamp) {
        Map<String, Object> senderChat = new HashMap<>();
        senderChat.put("userId", receiverId);
        senderChat.put("username", receiverName);
        senderChat.put("profileImage", receiverImage);
        senderChat.put("lastMessage", message);
        senderChat.put("timestamp", timestamp);
        senderChat.put("unreadCount", 0); // Reset for sender

        db.collection("users")
                .document(senderId)
                .collection("chats")
                .document(receiverId)
                .set(senderChat);

        db.collection("users")
                .document(senderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String senderName = documentSnapshot.getString("name");
                        String senderImage = documentSnapshot.getString("profileImageUrl");

                        Map<String, Object> receiverChat = new HashMap<>();
                        receiverChat.put("userId", senderId);
                        receiverChat.put("username", senderName);
                        receiverChat.put("profileImage", senderImage);
                        receiverChat.put("lastMessage", message);
                        receiverChat.put("timestamp", timestamp);

                        db.collection("users")
                                .document(receiverId)
                                .collection("chats")
                                .document(senderId)
                                .get()
                                .addOnSuccessListener(chatDoc -> {
                                    int unreadCount = 0;
                                    if (chatDoc.exists() && chatDoc.contains("unreadCount")) {
                                        Long count = chatDoc.getLong("unreadCount");
                                        if (count != null) {
                                            unreadCount = count.intValue();
                                        }
                                    }
                                    receiverChat.put("unreadCount", unreadCount + 1);

                                    db.collection("users")
                                            .document(receiverId)
                                            .collection("chats")
                                            .document(senderId)
                                            .set(receiverChat);
                                });
                    }
                });
    }

    private String getChatId(String user1, String user2) {
        if (user1.compareTo(user2) < 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        db.collection("users").document(senderId).update("online", true);
    }

    @Override
    protected void onPause() {
        db.collection("users").document(senderId).update("online", false);
        super.onPause();
    }
}