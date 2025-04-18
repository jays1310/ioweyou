package com.example.ioweyou;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ContactChatActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_contact); // Use your actual XML filename

        TextView contactNameTextView = findViewById(R.id.contact_name);
        String contactName = getIntent().getStringExtra("contact_name");
        if (contactName != null) {
            contactNameTextView.setText(contactName);
        }

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> finish());

        Button addPaymentBtn = findViewById(R.id.btn_add_payment);
        addPaymentBtn.setOnClickListener(v -> {
            // TODO: Implement Add Payment logic here (maybe open a dialog or another activity)
        });
    }
}

