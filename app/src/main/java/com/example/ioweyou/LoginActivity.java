package com.example.ioweyou;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class LoginActivity extends AppCompatActivity {

    private EditText etIdentifier, etPassword;
    private CheckBox rememberMeCheckBox;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etIdentifier = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        rememberMeCheckBox = findViewById(R.id.cb_remember_me);
        sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);

        if (sharedPreferences.getBoolean("rememberMe", false)) {
            etIdentifier.setText(sharedPreferences.getString("identifier", ""));
            etPassword.setText(sharedPreferences.getString("password", ""));
            rememberMeCheckBox.setChecked(true);
        }
    }

    public void loginUser(View view) {
        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both identifier and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (identifier.contains("@")) {
            if (!Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                Toast.makeText(this, "Invalid email address!", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (identifier.matches("\\d+")) {
            if (identifier.length() != 10) {
                Toast.makeText(this, "Invalid contact number! Must be 10 digits.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject loginData = new JSONObject();
                loginData.put("identifier", identifier);
                loginData.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(loginData.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";
                JSONObject jsonResponse = new JSONObject(response);

                runOnUiThread(() -> {
                    try {
                        String status = jsonResponse.getString("status");
                        String message = jsonResponse.getString("message");

                        if (responseCode == 200 && "success".equals(status)) {
                            // Save identifier for login session
                            SharedPreferences.Editor iouEditor = getSharedPreferences("IOUAppPrefs", MODE_PRIVATE).edit();
                            iouEditor.putString("user_identifier", identifier);
                            iouEditor.apply();

                            // Handle Remember Me
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            if (rememberMeCheckBox.isChecked()) {
                                editor.putBoolean("rememberMe", true);
                                editor.putString("identifier", identifier);
                                editor.putString("password", password);
                            } else {
                                editor.clear();
                            }
                            editor.apply();

                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Error parsing response", e);
                        Toast.makeText(this, "Unexpected response", Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                Log.e("LoginActivity", "Login failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void openSignUp(View view) {
        startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
    }

    public void goBackToMain(View view) {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
