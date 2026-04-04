package com.kronos.olympusfront;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.kronos.olympusfront.databinding.ActivityStatsBinding;

public class StatsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStatsBinding binding = ActivityStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Navigation
        binding.navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        binding.navMeals.setOnClickListener(v -> {
            Intent intent = new Intent(this, MealsActivity.class);
            startActivity(intent);
            finish();
        });

        binding.navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            finish();
        });

        binding.profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }
}