package com.example.chatapplication;

public class Userr {
    private String id;
    private String createdAt;
    private String email;
    private String name;
    private String profileImageUrl;
    private String status;
    private String username;

    // Empty constructor needed for Firebase
    public Userr() {
    }

    public Userr(String id, String createdAt, String email, String name,
                String profileImageUrl, String status, String username) {
        this.id = id;
        this.createdAt = createdAt;
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.status = status;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}