package com.example.ioweyou;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Find the logo view
        View splashView = findViewById(R.id.logo);

        // Start fade-out animation
        splashView.setAlpha(1f); // Ensure it's fully visible
        splashView.animate()
                .alpha(0f) // Fade to invisible
                .setDuration(2000) // 2 seconds fade
                .withEndAction(() -> {
                    // After fade-out, start MainActivity
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                });
    }
}
