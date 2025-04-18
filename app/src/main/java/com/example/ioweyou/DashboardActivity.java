package com.example.ioweyou;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
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
    private static final int PICK_CONTACT = 3;

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

        userEmail = getSharedPreferences("IOUAppPrefs", MODE_PRIVATE)
                .getString("user_email", null);
        if (userEmail == null) {
            Toast.makeText(this, "User email not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.imageView7).setOnClickListener(this::showSettingsMenu);

        fabMenu = findViewById(R.id.fab_menu);
        ImageView fab = findViewById(R.id.btn_add_transaction);
        Button btnAddContact = findViewById(R.id.btn_add_contact);
        Button btnJoinGroup = findViewById(R.id.btn_join_group);
        Button btnCreateGroup = findViewById(R.id.btn_create_group);

        rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        fab.setOnClickListener(view -> {
            if (isFabMenuOpen) closeFabMenu();
            else openFabMenu();
        });

        btnAddContact.setOnClickListener(view -> requestOrPickContact());
        btnJoinGroup.setOnClickListener(view -> {
            closeFabMenu();
            promptJoinGroup();
        });

        btnCreateGroup.setOnClickListener(view -> {
            closeFabMenu();
            promptGroupName();
        });

        RecyclerView rv = findViewById(R.id.groupRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Updated: Use OnGroupClickListener to open GroupDetailActivity
        groupAdapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(DashboardActivity.this, GroupDetailActivity.class);
            intent.putExtra("group_name", group.getName());
            startActivity(intent);
        });

        rv.setAdapter(groupAdapter);
        fetchGroups();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFabMenuOpen) closeFabMenu();
                else finish();
            }
        });
    }

    private void showSettingsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Logout").setIcon(R.drawable.ic_logout);
        popup.setOnMenuItemClickListener(item -> {
            logoutUser();
            return true;
        });
        popup.show();
    }

    private void logoutUser() {
        getSharedPreferences("IOUAppPrefs", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void openFabMenu() {
        fabMenu.setVisibility(View.VISIBLE);
        fabMenu.startAnimation(fadeIn);
        findViewById(R.id.btn_add_transaction).startAnimation(rotateOpen);
        isFabMenuOpen = true;
    }

    private void closeFabMenu() {
        fabMenu.startAnimation(fadeOut);
        fabMenu.setVisibility(View.GONE);
        findViewById(R.id.btn_add_transaction).startAnimation(rotateClose);
        isFabMenuOpen = false;
    }

    private void requestOrPickContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_CONTACT_PERMISSION);
        } else {
            startActivityForResult(
                    new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                    PICK_CONTACT
            );
        }
    }

    private void promptJoinGroup() {
        EditText input = new EditText(this);
        input.setHint("Enter Group Code");
        new AlertDialog.Builder(this)
                .setTitle("Join Group")
                .setView(input)
                .setPositiveButton("Join", (d, i) -> joinGroup(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptGroupName() {
        EditText input = new EditText(this);
        input.setHint("Enter Group Name");
        new AlertDialog.Builder(this)
                .setTitle("Create Group")
                .setView(input)
                .setPositiveButton("Next", (d, i) -> fetchUsersThenCreate(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchUsersThenCreate(String groupName) {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/all_users");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                for (String line; (line = in.readLine()) != null; ) sb.append(line);
                in.close();

                JSONObject resp = new JSONObject(sb.toString());
                JSONArray arr = resp.getJSONArray("users");

                runOnUiThread(() -> showUserSelectionDialog(groupName, arr));
            } catch (Exception e) {
                Log.e("DashboardActivity", "Error fetching users", e);
            }
        }).start();
    }

    private void showUserSelectionDialog(String groupName, JSONArray arr) {
        List<String> users = new ArrayList<>();
        boolean[] checks = new boolean[arr.length()];
        for (int i = 0; i < arr.length(); i++) users.add(arr.optString(i));
        String[] userArray = users.toArray(new String[0]);
        Set<String> picked = new HashSet<>();

        new AlertDialog.Builder(this)
                .setTitle("Add Members")
                .setMultiChoiceItems(userArray, checks, (dlg, idx, checked) -> {
                    if (checked) picked.add(userArray[idx]);
                    else picked.remove(userArray[idx]);
                })
                .setPositiveButton("Create", (dlg, w) -> createGroup(groupName, picked))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createGroup(String groupName, Set<String> members) {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/create_group");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type", "application/json");
                c.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("email", userEmail);
                data.put("group_name", groupName);
                data.put("members", new JSONArray(members));

                try (OutputStream os = c.getOutputStream()) {
                    os.write(data.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();

                StringBuilder responseBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                }

                c.disconnect();

                JSONObject responseJson = new JSONObject(responseBuilder.toString());

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            code == 200 ? "Group created!" : "Create failed",
                            Toast.LENGTH_SHORT).show();

                    if (code == 200) {
                        fetchGroups();

                        Intent intent = new Intent(DashboardActivity.this, GroupDetailActivity.class);
                        intent.putExtra("group_id", responseJson.optString("group_id"));
                        intent.putExtra("group_name", responseJson.optString("group_name"));
                        startActivity(intent);
                    }
                });
            } catch (Exception e) {
                Log.e("DashboardActivity", "Error creating group", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Create failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void joinGroup(String gid) {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/join_group");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type", "application/json");
                c.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("email", userEmail);
                data.put("group_id", gid);

                try (OutputStream os = c.getOutputStream()) {
                    os.write(data.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();
                new Scanner(c.getInputStream()).useDelimiter("\\A").forEachRemaining(s -> {});

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            code == 200 ? "Joined!" : "Join failed",
                            Toast.LENGTH_SHORT).show();
                    if (code == 200) fetchGroups();
                });

                c.disconnect();
            } catch (Exception e) {
                Log.e("DashboardActivity", "Error joining group", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Join failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchGroups() {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/user_groups?email=" + userEmail);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");

                if (c.getResponseCode() == 200) {
                    Scanner sc = new Scanner(c.getInputStream()).useDelimiter("\\A");
                    String resp = sc.hasNext() ? sc.next() : "";
                    JSONObject jr = new JSONObject(resp);
                    JSONArray arr = jr.getJSONArray("groups");

                    runOnUiThread(() -> {
                        groupList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            try {
                                JSONObject g = arr.getJSONObject(i);
                                groupList.add(new Group(g.getString("group_id"), g.getString("name")));
                            } catch (JSONException e) {
                                Log.e("DashboardActivity", "JSON error", e);
                            }
                        }
                        groupAdapter.notifyDataSetChanged();

                        // ✅ Scroll to top after new group is added
                        RecyclerView rv = findViewById(R.id.groupRecyclerView);
                        rv.scrollToPosition(0);
                    });
                }
                c.disconnect();
            } catch (Exception e) {
                Log.e("DashboardActivity", "Error fetching groups", e);
            }
        }).start();
    }
}
