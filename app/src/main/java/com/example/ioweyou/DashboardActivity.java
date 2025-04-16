package com.example.ioweyou;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

public class DashboardActivity extends AppCompatActivity {

    private SearchView searchView;
    private ImageButton addTransactionButton;
    private LinearLayout fabMenu;
    private boolean isFabMenuOpen = false;
    private Animation rotateOpen, rotateClose, fadeIn, fadeOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize UI elements
        searchView = findViewById(R.id.searchView);
        addTransactionButton = findViewById(R.id.btn_add_transaction);
        fabMenu = findViewById(R.id.fab_menu);

        Button btnAddContact = findViewById(R.id.btn_add_contact);
        Button btnJoinGroup = findViewById(R.id.btn_join_group);
        Button btnCreateGroup = findViewById(R.id.btn_create_group);

        // Load animations
        rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        // Floating Action Button click listener
        addTransactionButton.setOnClickListener(view -> {
            if (isFabMenuOpen) {
                closeFabMenu();
            } else {
                openFabMenu();
            }
        });

        // Click listeners for additional buttons (currently just placeholders)
        btnAddContact.setOnClickListener(v -> closeFabMenu());
        btnJoinGroup.setOnClickListener(v -> closeFabMenu());
        btnCreateGroup.setOnClickListener(v -> closeFabMenu());

        // SearchView functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // Clear SearchView focus when clicking outside
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                searchView.setIconified(true);
            }
        });

        // Register the OnBackPressedCallback
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    // Method to show the FAB menu with animation
    private void openFabMenu() {
        fabMenu.setVisibility(View.VISIBLE);
        fabMenu.startAnimation(fadeIn);
        addTransactionButton.startAnimation(rotateOpen);
        isFabMenuOpen = true;
    }

    // Method to hide the FAB menu with animation
    private void closeFabMenu() {
        fabMenu.startAnimation(fadeOut);
        fabMenu.setVisibility(View.GONE);
        addTransactionButton.startAnimation(rotateClose);
        isFabMenuOpen = false;
    }

    // OnBackPressedCallback (Fix: Now it's registered properly)
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (isFabMenuOpen) {
                closeFabMenu();
            } else {
                finish();
            }
        }
    };
}
