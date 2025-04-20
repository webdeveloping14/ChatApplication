package com.example.chatapplication;

import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String senderId;
    private String receiverId;
    private String message;
    private Object timestamp; // Changed to Object to support ServerValue.TIMESTAMP
    private String time;
    private String messageId;

    public Message() {
        // Required empty constructor for Firebase
    }

    public Message(String senderId, String receiverId, String message, String time) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = ServerValue.TIMESTAMP; // Will be resolved on the server
        this.time = time;
    }

    // Constructor for local use with timestamp already converted to long
    public Message(String senderId, String receiverId, String message, long timestamp, String time) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.time = time;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    // Helper method to convert to Map for Firebase
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("senderId", senderId);
        result.put("receiverId", receiverId);
        result.put("message", message);
        result.put("timestamp", timestamp);
        result.put("time", time);
        return result;
    }
}