package com.example.android_project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottom_navbar);
        fragmentManager = getSupportFragmentManager();

        // Check if we should open file tab
        boolean openFileTab = getIntent().getBooleanExtra("openFileTab", false);

        // Load appropriate fragmentxxxxxxxxxxxx
        if (savedInstanceState == null) {
            if (openFileTab) {
                loadFragment(new FileManageFragment());
                bottomNavigation.setSelectedItemId(R.id.filePage);
            } else {
                loadFragment(new HomeFragment());
                bottomNavigation.setSelectedItemId(R.id.homePage);
            }
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();

            if (itemId == bottomNavigation.getSelectedItemId()) {
                return false; // The event is not consumed, no re-selection animation.
            }

            Fragment fragment = null;

            if (itemId == R.id.homePage) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.filePage) {
                fragment = new FileManageFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
