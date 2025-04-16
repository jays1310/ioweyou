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
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        rememberMeCheckBox = findViewById(R.id.cb_remember_me);
        sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);

        if (sharedPreferences.getBoolean("rememberMe", false)) {
            etEmail.setText(sharedPreferences.getString("email", ""));
            etPassword.setText(sharedPreferences.getString("password", ""));
            rememberMeCheckBox.setChecked(true);
        }
    }

    public void loginUser(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
            return;
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
                            // Store user_email in shared preferences
                            SharedPreferences.Editor iouEditor = getSharedPreferences("IOUAppPrefs", MODE_PRIVATE).edit();
                            iouEditor.putString("user_email", email);
                            iouEditor.apply();

                            // Optionally remember login credentials
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            if (rememberMeCheckBox.isChecked()) {
                                editor.putBoolean("rememberMe", true);
                                editor.putString("email", email);
                                editor.putString("password", password);
                            } else {
                                editor.clear();
                            }
                            editor.apply();

                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
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
        startActivity(new Intent(this, SignUpActivity.class));
    }

    public void goBackToMain(View view) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
