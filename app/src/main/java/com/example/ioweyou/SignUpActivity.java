package com.example.ioweyou;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SignUpActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput, confirmPasswordInput, contactInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        usernameInput = getInputEditText(R.id.usernameInputLayout);
        emailInput = getInputEditText(R.id.emailInputLayout);
        passwordInput = getInputEditText(R.id.passwordInputLayout);
        confirmPasswordInput = getInputEditText(R.id.confirmPasswordInput);
        contactInput = getInputEditText(R.id.contactInputLayout); // 💬 Fetch contact field
    }

    private EditText getInputEditText(int layoutId) {
        TextInputLayout layout = findViewById(layoutId);
        return layout != null ? layout.getEditText() : null;
    }

    public void registerUser(View view) {
        if (usernameInput == null || emailInput == null || passwordInput == null || confirmPasswordInput == null || contactInput == null) {
            Toast.makeText(this, "Error loading input fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        String contact = contactInput.getText().toString().trim(); // 📞 Get contact number

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Email format validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Phone number validation (basic 10 digits)
        if (!contact.matches("\\d{10}")) {
            Toast.makeText(this, "Enter a valid 10-digit contact number!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL("https://ioweyou-sk05.onrender.com/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject userData = new JSONObject();
                userData.put("username", username);
                userData.put("email", email);
                userData.put("password", password);
                userData.put("contact", contact); // 🧾 Add contact to request

                OutputStream os = conn.getOutputStream();
                os.write(userData.toString().getBytes(StandardCharsets.UTF_8));
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

                        if (responseCode == 201 && "success".equals(status)) {
                            SharedPreferences.Editor editor = getSharedPreferences("IOUAppPrefs", MODE_PRIVATE).edit();
                            editor.putString("user_email", email);
                            editor.apply();

                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Registration failed: " + message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("SignUpActivity", "Error parsing response", e);
                        Toast.makeText(this, "Unexpected response", Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                Log.e("SignUpActivity", "Registration failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void goBackToMain(View view) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
