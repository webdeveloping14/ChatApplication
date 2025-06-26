package com.example.chatapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

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
    private DatabaseReference databaseRef;
    private String receiverId, receiverName, receiverImage;
    private String senderId;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private static final String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeFields();
        setUserDetails();
        checkUserStatus();
        setupRecyclerView();
        loadMessages();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void initializeFields() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseRef = FirebaseDatabase.getInstance().getReference();
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

        messageList = new ArrayList<>();
    }

    private void setUserDetails() {
        username.setText(receiverName);
        if (receiverImage != null && !receiverImage.isEmpty()) {
            Glide.with(this).load(receiverImage).placeholder(R.drawable.default_profile).into(profileImage);
        }
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, messageList, senderId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);
    }

    private void checkUserStatus() {
        if (receiverId == null) {
            Toast.makeText(this, "Receiver ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseRef.child("users").child(receiverId).child("online")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Boolean online = dataSnapshot.getValue(Boolean.class);
                            userStatus.setText(online != null && online ? "Online" : "Offline");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Failed to check user status: " + databaseError.getMessage());
                    }
                });
    }

    private void loadMessages() {
        if (senderId == null || receiverId == null) {
            Toast.makeText(this, "Sender or Receiver ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId = getChatId(senderId, receiverId);

        messageList.clear();
        messageAdapter.notifyDataSetChanged();

        databaseRef.child("chats").child(chatId).child("messages")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        messageList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Message message = snapshot.getValue(Message.class);
                            if (message != null) {
                                message.setMessageId(snapshot.getKey());
                                messageList.add(message);
                            }
                        }
                        messageAdapter.notifyDataSetChanged();

                        if (!messageList.isEmpty()) {
                            recyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Failed to load messages: " + databaseError.getMessage());
                        Toast.makeText(ChatActivity.this, "Error loading messages: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendMessage() {
        String msg = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) {
            Log.d(TAG, "Message is empty, not sending");
            return;
        }

        Log.d(TAG, "Attempting to send message: " + msg);
        Log.d(TAG, "SenderId: " + senderId + ", ReceiverId: " + receiverId);

        messageInput.setText("");

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String time = sdf.format(new Date());

        String chatId = getChatId(senderId, receiverId);
        Log.d(TAG, "Generated chatId: " + chatId);

        // Create a new message with ServerValue.TIMESTAMP
        Message message = new Message(senderId, receiverId, msg, time);

        // Generate a unique key for the message
        DatabaseReference messageRef = databaseRef.child("chats").child(chatId).child("messages").push();
        String messageId = messageRef.getKey();
        message.setMessageId(messageId);

        // Save message
        messageRef.setValue(message.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully with ID: " + messageId);
                    recyclerView.smoothScrollToPosition(messageList.size() - 1); // Scroll to latest message

                    // Update last message info
                    updateLastMessage(chatId, msg);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message: " + e.getMessage(), e);
                    Toast.makeText(ChatActivity.this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLastMessage(String chatId, String message) {
        if (senderId == null || receiverId == null) {
            return;
        }

        Map<String, Object> lastMessageData = new HashMap<>();
        lastMessageData.put("lastMessage", message);
        lastMessageData.put("timestamp", ServerValue.TIMESTAMP);

        // Update chat metadata
        databaseRef.child("chats").child(chatId).child("info").updateChildren(lastMessageData);

        // Update sender's chat list
        Map<String, Object> senderChat = new HashMap<>();
        senderChat.put("userId", receiverId);
        senderChat.put("username", receiverName);
        senderChat.put("profileImage", receiverImage);
        senderChat.put("lastMessage", message);
        senderChat.put("timestamp", ServerValue.TIMESTAMP);
        senderChat.put("unreadCount", 0); // Reset for sender

        databaseRef.child("users").child(senderId).child("chats").child(receiverId)
                .updateChildren(senderChat)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update sender chat: " + e.getMessage());
                });

        // Get sender info to update receiver's chat list
        databaseRef.child("users").child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String senderName = dataSnapshot.child("name").getValue(String.class);
                    String senderImage = dataSnapshot.child("profileImageUrl").getValue(String.class);

                    Map<String, Object> receiverChat = new HashMap<>();
                    receiverChat.put("userId", senderId);
                    receiverChat.put("username", senderName);
                    receiverChat.put("profileImage", senderImage);
                    receiverChat.put("lastMessage", message);
                    receiverChat.put("timestamp", ServerValue.TIMESTAMP);

                    // Get current unread count and increment it
                    databaseRef.child("users").child(receiverId).child("chats").child(senderId)
                            .child("unreadCount").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    int unreadCount = 0;
                                    if (snapshot.exists()) {
                                        Long count = snapshot.getValue(Long.class);
                                        if (count != null) {
                                            unreadCount = count.intValue();
                                        }
                                    }
                                    receiverChat.put("unreadCount", unreadCount + 1);

                                    databaseRef.child("users").child(receiverId).child("chats").child(senderId)
                                            .updateChildren(receiverChat)
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Failed to update receiver chat: " + e.getMessage());
                                            });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Failed to get unread count: " + error.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to get sender info: " + databaseError.getMessage());
            }
        });
    }

    private String getChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (senderId != null) {
            databaseRef.child("users").child(senderId).child("online").setValue(true)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update online status: " + e.getMessage());
                    });
        }
    }

    @Override
    protected void onPause() {
        if (senderId != null) {
            databaseRef.child("users").child(senderId).child("online").setValue(false)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update offline status: " + e.getMessage());
                    });
        }
        super.onPause();
    }
}