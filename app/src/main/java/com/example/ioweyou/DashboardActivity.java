package com.example.ioweyou;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class DashboardActivity extends AppCompatActivity {

    private static final int REQUEST_CONTACT_PERMISSION = 1;
    private static final int REQUEST_SEND_SMS_PERMISSION = 2;
    private static final int PICK_CONTACT = 3;

    private SearchView searchView;
    private ImageButton addTransactionButton;
    private LinearLayout fabMenu;
    private boolean isFabMenuOpen = false;
    private Animation rotateOpen, rotateClose, fadeIn, fadeOut;
    private String userEmail;

    private GroupAdapter groupAdapter;
    private final List<Group> groupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        SharedPreferences sharedPreferences = getSharedPreferences("IOUAppPrefs", MODE_PRIVATE);
        userEmail = sharedPreferences.getString("user_email", null);

        if (userEmail == null) {
            Toast.makeText(this, "User email not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        searchView = findViewById(R.id.searchView);
        addTransactionButton = findViewById(R.id.btn_add_transaction);
        fabMenu = findViewById(R.id.fab_menu);
        ImageView settingsButton = findViewById(R.id.imageView7);
        settingsButton.setOnClickListener(this::showSettingsMenu);

        Button btnAddContact = findViewById(R.id.btn_add_contact);
        Button btnJoinGroup = findViewById(R.id.btn_join_group);
        Button btnCreateGroup = findViewById(R.id.btn_create_group);

        rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        addTransactionButton.setOnClickListener(view -> {
            if (isFabMenuOpen) closeFabMenu();
            else openFabMenu();
        });

        btnAddContact.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACT_PERMISSION);
            } else {
                openContactPicker();
            }
        });

        btnJoinGroup.setOnClickListener(v -> {
            closeFabMenu();
            promptJoinGroup();
        });

        btnCreateGroup.setOnClickListener(v -> {
            closeFabMenu();
            promptGroupName();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { return false; }
        });

        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) searchView.setIconified(true);
        });

        RecyclerView groupRecyclerView = findViewById(R.id.groupRecyclerView);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        groupAdapter = new GroupAdapter(groupList);
        groupRecyclerView.setAdapter(groupAdapter);

        fetchGroups();

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private void showSettingsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 0, 0, "Logout").setIcon(R.drawable.ic_logout);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 0) {
                logoutUser();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void logoutUser() {
        SharedPreferences.Editor editor = getSharedPreferences("IOUAppPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(DashboardActivity.this, MainActivity.class));
        finish();
    }

    private void openFabMenu() {
        fabMenu.setVisibility(View.VISIBLE);
        fabMenu.startAnimation(fadeIn);
        addTransactionButton.startAnimation(rotateOpen);
        isFabMenuOpen = true;
    }

    private void closeFabMenu() {
        fabMenu.startAnimation(fadeOut);
        fabMenu.setVisibility(View.GONE);
        addTransactionButton.startAnimation(rotateClose);
        isFabMenuOpen = false;
    }

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (isFabMenuOpen) closeFabMenu();
            else finish();
        }
    };

    private void promptJoinGroup() {
        EditText input = new EditText(this);
        input.setHint("Enter Group Code");

        new AlertDialog.Builder(this)
                .setTitle("Join Group")
                .setView(input)
                .setPositiveButton("Join", (dialog, which) -> joinGroup(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptGroupName() {
        EditText input = new EditText(this);
        input.setHint("Enter Group Name");

        new AlertDialog.Builder(this)
                .setTitle("Create Group")
                .setView(input)
                .setPositiveButton("Next", (dialog, which) -> fetchUserListAndCreateGroup(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchUserListAndCreateGroup(String groupName) {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/all_users");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) result.append(line);
                in.close();

                JSONObject response = new JSONObject(result.toString());
                JSONArray usersArray = response.getJSONArray("users");

                runOnUiThread(() -> showUserSelectionDialog(groupName, usersArray));
            } catch (Exception e) {
                Log.e("DashboardActivity", "Error fetching users", e);
            }
        }).start();
    }

    private void showUserSelectionDialog(String groupName, JSONArray usersArray) {
        List<String> userList = new ArrayList<>();
        boolean[] checkedItems = new boolean[usersArray.length()];

        for (int i = 0; i < usersArray.length(); i++) {
            userList.add(usersArray.optString(i));
        }

        String[] userArray = userList.toArray(new String[0]);
        Set<String> selectedUsers = new HashSet<>();

        new AlertDialog.Builder(this)
                .setTitle("Add Members")
                .setMultiChoiceItems(userArray, checkedItems, (dialog, indexSelected, isChecked) -> {
                    if (isChecked) selectedUsers.add(userArray[indexSelected]);
                    else selectedUsers.remove(userArray[indexSelected]);
                })
                .setPositiveButton("Create", (dialog, which) -> createGroup(groupName, selectedUsers))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createGroup(String groupName, Set<String> members) {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/create_group");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("email", userEmail);
                data.put("group_name", groupName);
                data.put("members", new JSONArray(members));

                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = conn.getResponseCode();
                Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                if (scanner.hasNext()) scanner.next();

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show();
                        fetchGroups();
                    } else {
                        Toast.makeText(this, "Failed to create group.", Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                Log.e("DashboardActivity", "Error creating group", e);
                runOnUiThread(() -> Toast.makeText(this, "Create group failed!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void joinGroup(String groupId) {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/join_group");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("email", userEmail);
                data.put("group_id", groupId);

                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = conn.getResponseCode();
                Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                if (scanner.hasNext()) scanner.next();

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(this, "Joined group successfully!", Toast.LENGTH_SHORT).show();
                        fetchGroups();
                    } else {
                        Toast.makeText(this, "Failed to join group.", Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                Log.e("DashboardActivity", "Error joining group", e);
                runOnUiThread(() -> Toast.makeText(this, "Join group failed!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchGroups() {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/user_groups?email=" + userEmail);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";

                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray groupsArray = jsonResponse.getJSONArray("groups");

                    runOnUiThread(() -> {
                        groupList.clear();
                        for (int i = 0; i < groupsArray.length(); i++) {
                            groupList.add(new Group(groupsArray.optString(i)));
                        }
                        groupAdapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                Log.e("DashboardActivity", "Error fetching groups", e);
            }
        }).start();
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri == null) return;

            try (Cursor cursor = getContentResolver().query(contactUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String phoneNumber = cursor.getString(phoneIndex);
                    sendSMSInvite(phoneNumber);
                }
            }
        }
    }

    private void sendSMSInvite(String phoneNumber) {
        String message = "Hey! Check out the IOU app – track who owes whom! Download here: https://play.google.com/store/apps/details?id=com.example.ioweyou";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS_PERMISSION);
        } else {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Invitation sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        }
    }
}
