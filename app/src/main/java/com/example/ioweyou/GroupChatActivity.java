package com.example.ioweyou;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
            groupNameTextView.setText(groupName);
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
            // TODO: Implement add payment logic here
        });
    }
}
