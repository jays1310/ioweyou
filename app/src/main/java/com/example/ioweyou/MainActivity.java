package com.example.ioweyou;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button loginButton, signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences("IOUAppPrefs", MODE_PRIVATE);
        String identifier = prefs.getString("user_identifier", null);

        if (identifier != null) {
            // User already logged in — go to dashboard
            startActivity(new Intent(this, DashboardActivity.class));
            finish(); // Prevent going back here
            return;
        }

        // Else show the normal main layout with login/signup buttons
        setContentView(R.layout.activity_main); // Ensure this matches your XML file

        loginButton = findViewById(R.id.btnLogin);
        signUpButton = findViewById(R.id.btnSignUp);

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}
