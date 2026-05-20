package com.kronos.olympusfront;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.kronos.olympusfront.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewPager2 : 5 pages swipables
        binding.viewPager.setAdapter(new MainPagerAdapter(this));
        binding.viewPager.setOffscreenPageLimit(1);

        // Clics sur la barre du bas -> page correspondante
        binding.navChat.setOnClickListener(v -> binding.viewPager.setCurrentItem(MainPagerAdapter.PAGE_CHAT, true));
        binding.navHome.setOnClickListener(v -> binding.viewPager.setCurrentItem(MainPagerAdapter.PAGE_HOME, true));
        binding.navMeals.setOnClickListener(v -> binding.viewPager.setCurrentItem(MainPagerAdapter.PAGE_MEALS, true));
        binding.navStats.setOnClickListener(v -> binding.viewPager.setCurrentItem(MainPagerAdapter.PAGE_STATS, true));
        binding.navProfile.setOnClickListener(v -> binding.viewPager.setCurrentItem(MainPagerAdapter.PAGE_PROFILE, true));

        // Synchronisation barre du bas <-> swipe
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateNavigationUI(position);
            }
        });

        // Page par défaut : Accueil
        if (savedInstanceState == null) {
            binding.viewPager.setCurrentItem(MainPagerAdapter.PAGE_HOME, false);
        }
        updateNavigationUI(binding.viewPager.getCurrentItem());
    }

    private void updateNavigationUI(int position) {
        int inactiveColor = ContextCompat.getColor(this, R.color.marble_white);
        int activeColor = ContextCompat.getColor(this, R.color.on_secondary);
        int activeBg = ContextCompat.getColor(this, R.color.secondary_container);
        int inactiveBg = ContextCompat.getColor(this, android.R.color.transparent);

        styleNavItem(binding.navChat, binding.navChatIcon, binding.navChatText, inactiveColor, inactiveBg);
        styleNavItem(binding.navHome, binding.navHomeIcon, binding.navHomeText, inactiveColor, inactiveBg);
        styleNavItem(binding.navMeals, binding.navMealsIcon, binding.navMealsText, inactiveColor, inactiveBg);
        styleNavItem(binding.navStats, binding.navStatsIcon, binding.navStatsText, inactiveColor, inactiveBg);
        styleNavItem(binding.navProfile, binding.navProfileIcon, binding.navProfileText, inactiveColor, inactiveBg);

        switch (position) {
            case MainPagerAdapter.PAGE_CHAT:
                styleNavItem(binding.navChat, binding.navChatIcon, binding.navChatText, activeColor, activeBg);
                break;
            case MainPagerAdapter.PAGE_MEALS:
                styleNavItem(binding.navMeals, binding.navMealsIcon, binding.navMealsText, activeColor, activeBg);
                break;
            case MainPagerAdapter.PAGE_STATS:
                styleNavItem(binding.navStats, binding.navStatsIcon, binding.navStatsText, activeColor, activeBg);
                break;
            case MainPagerAdapter.PAGE_PROFILE:
                styleNavItem(binding.navProfile, binding.navProfileIcon, binding.navProfileText, activeColor, activeBg);
                break;
            case MainPagerAdapter.PAGE_HOME:
            default:
                styleNavItem(binding.navHome, binding.navHomeIcon, binding.navHomeText, activeColor, activeBg);
                break;
        }
    }

    private void styleNavItem(@NonNull View layout, @NonNull ImageView icon, @NonNull TextView text,
                              int color, int bg) {
        layout.setBackgroundColor(bg);
        icon.setImageTintList(ColorStateList.valueOf(color));
        text.setTextColor(color);
    }
}
