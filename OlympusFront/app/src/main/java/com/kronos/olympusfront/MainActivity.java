package com.kronos.olympusfront;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.kronos.olympusfront.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Navigation setup
        binding.navHome.setOnClickListener(v -> loadFragment(new HomeFragment(), "HOME"));
        binding.navMeals.setOnClickListener(v -> loadFragment(new MealsFragment(), "MEALS"));
        binding.navStats.setOnClickListener(v -> loadFragment(new StatsFragment(), "STATS"));
        binding.navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment(), "PROFILE"));

        // Set App Icon in Top Bar
        binding.profileButton.setImageResource(R.mipmap.ic_olympus);
        binding.profileButton.setBackground(null);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "HOME");
        }
    }

    private void loadFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(binding.navHostFragment.getId(), fragment, tag)
                .commit();
        updateNavigationUI(tag);
    }

    private void updateNavigationUI(String tag) {
        // Colors
        int inactiveColor = ContextCompat.getColor(this, R.color.marble_white);
        int activeColor = ContextCompat.getColor(this, R.color.on_secondary);
        int activeBg = ContextCompat.getColor(this, R.color.secondary_container);
        int inactiveBg = ContextCompat.getColor(this, android.R.color.transparent);

        resetNavItem(binding.navHome, binding.navHomeIcon, binding.navHomeText, inactiveColor, inactiveBg);
        resetNavItem(binding.navMeals, binding.navMealsIcon, binding.navMealsText, inactiveColor, inactiveBg);
        resetNavItem(binding.navStats, binding.navStatsIcon, binding.navStatsText, inactiveColor, inactiveBg);
        resetNavItem(binding.navProfile, binding.navProfileIcon, binding.navProfileText, inactiveColor, inactiveBg);

        // Highlight selected
        switch (tag) {
            case "HOME":
                setActiveNavItem(binding.navHome, binding.navHomeIcon, binding.navHomeText, activeColor, activeBg);
                break;
            case "MEALS":
                setActiveNavItem(binding.navMeals, binding.navMealsIcon, binding.navMealsText, activeColor, activeBg);
                break;
            case "STATS":
                setActiveNavItem(binding.navStats, binding.navStatsIcon, binding.navStatsText, activeColor, activeBg);
                break;
            case "PROFILE":
                setActiveNavItem(binding.navProfile, binding.navProfileIcon, binding.navProfileText, activeColor, activeBg);
                break;
        }
    }

    private void resetNavItem(View layout, ImageView icon, TextView text, int color, int bg) {
        layout.setBackgroundColor(bg);
        icon.setImageTintList(ColorStateList.valueOf(color));
        text.setTextColor(color);
    }

    private void setActiveNavItem(View layout, ImageView icon, TextView text, int color, int bg) {
        layout.setBackgroundColor(bg);
        icon.setImageTintList(ColorStateList.valueOf(color));
        text.setTextColor(color);
    }
}