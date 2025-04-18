package com.example.ioweyou;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<UserModel> userList;

    public UserAdapter(List<UserModel> userList) {
        this.userList = userList;
    }

    public List<UserModel> getSelectedUsers() {
        List<UserModel> selected = new ArrayList<>();
        for (UserModel user : userList) {
            if (user.isSelected()) selected.add(user);
        }
        return selected;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_checkbox, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);

        Context context = holder.itemView.getContext();
        String displayText = context.getString(R.string.user_display, user.getUsername(), user.getEmail());

        holder.checkBox.setText(displayText);
        holder.checkBox.setChecked(user.isSelected());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> user.setSelected(isChecked));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxUser);
        }
    }
}
