package com.example.ioweyou;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GroupChatActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_group);  // Uses the group chat layout

        // Get data from intent
        Intent intent = getIntent();
        String groupName = intent.getStringExtra("group_name");
        String groupId = intent.getStringExtra("group_id");

        // Initialize views
        TextView groupNameTextView = findViewById(R.id.text_group_name);
        TextView groupIdTextView = findViewById(R.id.text_group_id);
        Button addPaymentButton = findViewById(R.id.btn_add_payment);
        ImageView backArrow = findViewById(R.id.back_arrow);

        // Set data
        if (groupName != null) {
            groupNameTextView.setText("Group Name" +groupName);
        }

        if (groupId != null) {
            groupIdTextView.setText("Group ID: " + groupId);
        }

        // Back arrow click
        backArrow.setOnClickListener(v -> {
            finish(); // Close and go back
        });

        // Add payment logic
        addPaymentButton.setOnClickListener(v -> {
            // Step 1: Prompt for total expense (integer only)
            AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this);
            builder.setTitle("Enter Total Expense");

            final EditText input = new EditText(GroupChatActivity.this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setHint("Amount in ₹");
            builder.setView(input);

            builder.setPositiveButton("Next", (dialog, which) -> {
                String value = input.getText().toString().trim();
                if (!value.isEmpty()) {
                    try {
                        int totalExpense = Integer.parseInt(value);
                        showSplitOptions(totalExpense);  // Step 2: Handle splitting method
                    } catch (NumberFormatException e) {
                        Toast.makeText(GroupChatActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(GroupChatActivity.this, "Amount cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });

    }
    private void showSplitOptions(int totalExpense) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this);
        builder.setTitle("Choose Split Method");

        String[] splitMethods = {"Equally", "In Percentage", "Specific"};

        builder.setItems(splitMethods, (dialog, which) -> {
            String selectedMethod = splitMethods[which];

            // Now ask for member selection after split method is chosen
            selectGroupMembers(totalExpense, selectedMethod);
        });

        builder.show();
    }

    private void selectGroupMembers(int totalExpense, String splitMethod) {
        // For now, let's use some mock group members. Replace with real data from database.
        String[] allMembers = {"Alice", "Bob", "Charlie", "David", "Eve"};
        boolean[] checkedItems = new boolean[allMembers.length];
        ArrayList<String> selectedMembers = new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this);
        builder.setTitle("Select Members to Split With");

        builder.setMultiChoiceItems(allMembers, checkedItems, (dialog, which, isChecked) -> {
            if (isChecked) {
                selectedMembers.add(allMembers[which]);
            } else {
                selectedMembers.remove(allMembers[which]);
            }
        });

        builder.setPositiveButton("Enter", (dialog, which) -> {
            if (!selectedMembers.isEmpty()) {
                calculateSplit(totalExpense, splitMethod, selectedMembers);
            } else {
                Toast.makeText(this, "Please select at least one member.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void calculateSplit(int totalExpense, String splitMethod, ArrayList<String> members) {
        HashMap<String, Integer> splitMap = new HashMap<>();

        switch (splitMethod) {
            case "Equally":
                int equalShare = totalExpense / members.size();
                for (String member : members) {
                    splitMap.put(member, equalShare);
                }
                break;

            case "Percentage":
                // For simplicity, we'll assume equal percentage splits unless specified differently
                Toast.makeText(this, "Percentage split not implemented yet.", Toast.LENGTH_SHORT).show();
                return;

            case "Specific":
                // Placeholder for specific input per user
                Toast.makeText(this, "Specific split not implemented yet.", Toast.LENGTH_SHORT).show();
                return;
        }

        displaySplitOnScreen(splitMap);
        saveSplitToDatabase(splitMap, totalExpense);
    }

    @SuppressLint("SetTextI18n")
    private void displaySplitOnScreen(HashMap<String, Integer> splitMap) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        for (Map.Entry<String, Integer> entry : splitMap.entrySet()) {
            TextView tv = new TextView(this);
            tv.setText(entry.getKey() + ": ₹" + entry.getValue());
            tv.setTextSize(18);
            layout.addView(tv);
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Split Summary")
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .show();
    }

    private void saveSplitToDatabase(HashMap<String, Integer> splitMap, int totalExpense) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("group_name", groupName);
            payload.put("group_id", groupId);
            payload.put("total_expense", totalExpense);

            JSONArray splitArray = new JSONArray();
            for (Map.Entry<String, Integer> entry : splitMap.entrySet()) {
                JSONObject obj = new JSONObject();
                obj.put("member", entry.getKey());
                obj.put("amount", entry.getValue());
                splitArray.put(obj);
            }

            payload.put("split_details", splitArray);

            String URL = "https://your-api-url.com/save_expense";  // Change this to your Flask route

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL, payload,
                    response -> Toast.makeText(this, "Expense saved!", Toast.LENGTH_SHORT).show(),
                    error -> Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show()
            );

            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating JSON", Toast.LENGTH_SHORT).show();
        }
    }


}
