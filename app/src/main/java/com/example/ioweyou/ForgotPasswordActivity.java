package com.example.ioweyou;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmailReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmailReset = findViewById(R.id.et_email_reset);
    }

    public void sendResetEmail(View view) {
        String email = etEmailReset.getText().toString().trim();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Send reset email via backend
        Toast.makeText(this, "If this email exists, reset instructions will be sent.", Toast.LENGTH_LONG).show();
    }
}
