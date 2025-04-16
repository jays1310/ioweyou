package com.example.ioweyou;

import android.content.Intent;
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

    private EditText usernameInput, emailInput, passwordInput, confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        TextInputLayout usernameLayout = findViewById(R.id.usernameInputLayout);
        TextInputLayout emailLayout = findViewById(R.id.emailInputLayout);
        TextInputLayout passwordLayout = findViewById(R.id.passwordInputLayout);
        TextInputLayout confirmPasswordLayout = findViewById(R.id.confirmPasswordInput);

        usernameInput = usernameLayout.getEditText();
        emailInput = emailLayout.getEditText();
        passwordInput = passwordLayout.getEditText();
        confirmPasswordInput = confirmPasswordLayout.getEditText();
    }

    public void registerUser(View view) {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send data to Flask backend in background thread
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:5000/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject userData = new JSONObject();
                userData.put("username", username);
                userData.put("email", email);
                userData.put("password", password);

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
                            Toast.makeText(SignUpActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                            finish();
                        } else if (responseCode == 409) {
                            Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Registration failed: " + message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("SignUpActivity", "Error parsing response", e);
                        Toast.makeText(SignUpActivity.this, "Unexpected response", Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                Log.e("SignUpActivity", "Registration failed", e);
                runOnUiThread(() ->
                        Toast.makeText(SignUpActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void goBackToMain(View view) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
