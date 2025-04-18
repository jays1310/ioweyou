package com.example.ioweyou;

public class UserModel {
    private final String username;
    private final String email;
    private boolean isSelected;

    public UserModel(String username, String email) {
        this.username = username;
        this.email = email;
        this.isSelected = false;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}

