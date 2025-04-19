package com.example.chatapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<Userr> userList;
    private SimpleDateFormat dateFormat;

    public UserAdapter(Context context) {
        this.context = context;
        this.userList = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    public void setUsers(List<Userr> users) {
        this.userList = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Userr user = userList.get(position);

        holder.nameTextView.setText(user.getName());
        holder.usernameTextView.setText("@" + user.getUsername());

        if (user.getStatus() != null && !user.getStatus().isEmpty()) {
            holder.statusTextView.setText(user.getStatus());
            holder.statusTextView.setVisibility(View.VISIBLE);
        } else {
            holder.statusTextView.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("userId", user.getId());
            intent.putExtra("userName", user.getUsername());
            intent.putExtra("userImage", user.getProfileImageUrl());
            context.startActivity(intent);
        });

        // Parse and format the timestamp
        try {
            if (user.getCreatedAt() != null && !user.getCreatedAt().isEmpty()) {
                holder.createdAtTextView.setText("Created: " + user.getCreatedAt().substring(0, 10));
                holder.createdAtTextView.setVisibility(View.VISIBLE);
            } else {
                holder.createdAtTextView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            holder.createdAtTextView.setVisibility(View.GONE);
        }

        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.default_profile);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImageView;
        TextView nameTextView, usernameTextView, statusTextView, createdAtTextView;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            createdAtTextView = itemView.findViewById(R.id.createdAtTextView);
        }
    }
}