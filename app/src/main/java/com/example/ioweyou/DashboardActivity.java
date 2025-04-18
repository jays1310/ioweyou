package com.example.ioweyou;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    private static final int PICK_CONTACT = 3;

    private LinearLayout fabMenu;
    private boolean isFabMenuOpen = false;
    private Animation rotateOpen, rotateClose, fadeIn, fadeOut;
    private String userIdentifier;

    private GroupAdapter groupAdapter;
    private final List<Group> groupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1) Load the saved identifier (email, username, or phone)
        SharedPreferences prefs = getSharedPreferences("IOUAppPrefs", MODE_PRIVATE);
        userIdentifier = prefs.getString("user_identifier", null);
        if (userIdentifier == null) {
            // Not logged in, force login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        findViewById(R.id.imageView7).setOnClickListener(this::showSettingsMenu);

        fabMenu        = findViewById(R.id.fab_menu);
        ImageView fab  = findViewById(R.id.btn_add_transaction);
        Button btnAddContact = findViewById(R.id.btn_add_contact);
        Button btnJoinGroup  = findViewById(R.id.btn_join_group);
        Button btnCreateGroup= findViewById(R.id.btn_create_group);

        rotateOpen  = AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim);
        fadeIn      = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeOut     = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        fab.setOnClickListener(v -> {
            if (isFabMenuOpen) closeFabMenu();
            else openFabMenu();
        });

        // Add Contact flow
        btnAddContact.setOnClickListener(v -> {
            closeFabMenu();
            requestOrPickContact();
        });
        // Group flows
        btnJoinGroup.setOnClickListener(v -> {
            closeFabMenu();
            promptJoinGroup();
        });
        btnCreateGroup.setOnClickListener(v -> {
            closeFabMenu();
            promptGroupName();
        });

        RecyclerView rv = findViewById(R.id.groupRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        groupAdapter = new GroupAdapter(groupList, group -> {
            Intent i = new Intent(DashboardActivity.this, GroupDetailActivity.class);
            i.putExtra("group_id",   group.getGroupId());
            i.putExtra("group_name", group.getName());
            startActivity(i);
        });
        rv.setAdapter(groupAdapter);

        fetchGroups();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
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
        // Clear just the login token
        getSharedPreferences("IOUAppPrefs", MODE_PRIVATE)
                .edit()
                .remove("user_identifier")
                .apply();
        // Back to Login
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK));
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
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_CONTACT_PERMISSION
            );
        } else {
            startActivityForResult(
                    new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                    PICK_CONTACT
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == REQUEST_CONTACT_PERMISSION
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            requestOrPickContact();
        } else {
            Toast.makeText(this, "Contacts permission required to add a contact", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_CONTACT && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String phone = extractPhoneNumber(uri);
            if (phone != null) {
                checkUserExistsOrInvite(phone);
            }
        }
    }

    /** Query the Contacts provider to pull out the phone number. */
    private String extractPhoneNumber(Uri uri) {
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(uri,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null
        );
        if (cursor != null && cursor.moveToFirst()) {
            String num = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            );
            cursor.close();
            return num;
        }
        return null;
    }

    /** Hit your new /check_user_exists endpoint; if found, prompt name→chat, else launch SMS. */
    private void checkUserExistsOrInvite(String phone) {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/check_user_exists");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                c.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("identifier", phone);

                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        promptForContactName(phone);
                    } else {
                        sendInviteSMS(phone);
                    }
                });
                c.disconnect();
            } catch (Exception e) {
                Log.e("DashboardActivity", "Error checking user", e);
            }
        }).start();
    }

    private void promptForContactName(String phone) {
        EditText input = new EditText(this);
        input.setHint("Enter contact name");
        new AlertDialog.Builder(this)
                .setTitle("Save Contact")
                .setView(input)
                .setPositiveButton("OK", (dlg, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Intent i = new Intent(this, ContactChatActivity.class);
                        i.putExtra("contact_number", phone);
                        i.putExtra("contact_name",   name);
                        startActivity(i);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendInviteSMS(String phone) {
        Intent sms = new Intent(Intent.ACTION_SENDTO);
        sms.setData(Uri.parse("smsto:" + phone));
        sms.putExtra("sms_body",
                "Hey! Join me on IOU App to split & manage expenses: https://play.google.com/store/apps/details?id=com.example.ioweyou"
        );
        startActivity(sms);
    }

    // ─── Existing “Groups” logic ──────────────────────────────────────────

    private void promptJoinGroup() {
        EditText e = new EditText(this);
        e.setHint("Enter Group Code");
        new AlertDialog.Builder(this)
                .setTitle("Join Group")
                .setView(e)
                .setPositiveButton("Join",
                        (d, i) -> joinGroup(e.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptGroupName() {
        EditText e = new EditText(this);
        e.setHint("Enter Group Name");
        new AlertDialog.Builder(this)
                .setTitle("Create Group")
                .setView(e)
                .setPositiveButton("Next",
                        (d, i) -> fetchUsersThenCreate(e.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchUsersThenCreate(String groupName) {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/all_users");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(c.getInputStream())
                );
                StringBuilder sb = new StringBuilder();
                for (String line; (line = in.readLine()) != null; )
                    sb.append(line);
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
        List<String> emails = new ArrayList<>();
        boolean[] checks = new boolean[arr.length()];
        for (int i = 0; i < arr.length(); i++) try {
            emails.add(arr.getString(i));
        } catch (Exception e) { /* ignore */ }

        String[] items = emails.toArray(new String[0]);
        Set<String> picked = new HashSet<>();

        new AlertDialog.Builder(this)
                .setTitle("Add Members")
                .setMultiChoiceItems(items, checks,
                        (dlg, idx, checked) -> {
                            if (checked) picked.add(items[idx]);
                            else          picked.remove(items[idx]);
                        })
                .setPositiveButton("Create",
                        (dlg, w) -> createGroup(groupName, picked))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createGroup(String groupName, Set<String> members) {
        new Thread(() -> {
            try {
                // Ensure creator is in the member list
                members.add(userIdentifier);

                URL url = new URL("https://ioweyou-sk05.onrender.com/create_group");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type","application/json");
                c.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("identifier", userIdentifier);
                data.put("group_name", groupName);
                data.put("members",  new JSONArray(members));

                try (OutputStream os = c.getOutputStream()) {
                    os.write(data.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(c.getInputStream())
                );
                StringBuilder resp = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; )
                    resp.append(line);
                JSONObject json = new JSONObject(resp.toString());
                c.disconnect();

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            code == 200 ? "Group created!" : "Create failed",
                            Toast.LENGTH_SHORT
                    ).show();

                    if (code == 200) {
                        fetchGroups();
                        Intent i = new Intent(this, GroupChatActivity.class);
                        i.putExtra("group_id",   json.optString("group_id"));
                        i.putExtra("group_name", json.optString("group_name"));
                        startActivity(i);
                    }
                });
            } catch (Exception e) {
                Log.e("DashboardActivity","Error creating group",e);
                runOnUiThread(() ->
                        Toast.makeText(this,"Create failed",Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }


    private void joinGroup(String gid) {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/join_group");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type","application/json");
                c.setDoOutput(true);

                JSONObject d = new JSONObject();
                d.put("identifier", userIdentifier);
                d.put("group_id",   gid);

                try (OutputStream os = c.getOutputStream()) {
                    os.write(d.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = c.getResponseCode();
                new Scanner(c.getInputStream()).useDelimiter("\\A").forEachRemaining(s->{});
                c.disconnect();

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            code == 200 ? "Joined!" : "Join failed",
                            Toast.LENGTH_SHORT
                    ).show();
                    if (code == 200) fetchGroups();
                });
            } catch (Exception e) {
                Log.e("DashboardActivity","Error joining group",e);
                runOnUiThread(() ->
                        Toast.makeText(this,"Join failed",Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchGroups() {
        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/user_groups?identifier=" + userIdentifier);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");

                if (c.getResponseCode() == 200) {
                    Scanner sc = new Scanner(c.getInputStream()).useDelimiter("\\A");
                    String resp = sc.hasNext() ? sc.next() : "";
                    JSONArray arr = new JSONObject(resp).getJSONArray("groups");

                    runOnUiThread(() -> {
                        groupList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            try {
                                JSONObject g = arr.getJSONObject(i);
                                groupList.add(new Group(g.getString("group_id"), g.getString("name")));
                            } catch (Exception e) { /* ignore */ }
                        }
                        groupAdapter.notifyDataSetChanged();
                        // scroll new items into view
                        RecyclerView rv = findViewById(R.id.groupRecyclerView);
                        rv.scrollToPosition(0);
                    });
                }
                c.disconnect();
            } catch (Exception e) {
                Log.e("DashboardActivity","Error fetching groups",e);
            }
        }).start();
    }
}
