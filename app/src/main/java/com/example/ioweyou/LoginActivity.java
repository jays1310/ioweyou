package com.example.ioweyou;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

    private EditText etEmail, etPassword;
    private CheckBox rememberMeCheckBox;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // assuming you're still using activity_login.xml

        // Initialize EditText fields
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        rememberMeCheckBox = findViewById(R.id.cb_remember_me);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);

        // Check if "Remember Me" is selected and auto-login
        if (sharedPreferences.getBoolean("rememberMe", false)) {
            String savedEmail = sharedPreferences.getString("email", "");
            String savedPassword = sharedPreferences.getString("password", "");
            etEmail.setText(savedEmail);
            etPassword.setText(savedPassword);
            rememberMeCheckBox.setChecked(true);
        }
    }

    // Called when login button is clicked
    public void loginUser(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send login credentials to Flask backend
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:5000/login"); // Adjust to your backend URL
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject loginData = new JSONObject();
                loginData.put("email", email);
                loginData.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(loginData.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

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
                            // Save credentials if "Remember Me" is checked
                            if (rememberMeCheckBox.isChecked()) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean("rememberMe", true);
                                editor.putString("email", email);
                                editor.putString("password", password);
                                editor.apply();
                            } else {
                                // Clear credentials if "Remember Me" is not checked
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean("rememberMe", false);
                                editor.remove("email");
                                editor.remove("password");
                                editor.apply();
                            }

                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Error parsing response", e);
                        Toast.makeText(LoginActivity.this, "Unexpected response", Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                Log.e("LoginActivity", "Login failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Called when "Don't have an account? Sign Up" is clicked
    public void openSignUp(View view) {
        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        startActivity(intent);
    }

    // Custom back button in UI
    public void goBackToMain(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: close the current activity
    }
}
