package com.kronos.olympusfront;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter du ViewPager2 principal : 5 pages swipables.
 * Ordre : Oracle (chat IA), Accueil, Repas, Stats, Profil.
 */
public class MainPagerAdapter extends FragmentStateAdapter {

    public static final int PAGE_CHAT = 0;
    public static final int PAGE_HOME = 1;
    public static final int PAGE_MEALS = 2;
    public static final int PAGE_STATS = 3;
    public static final int PAGE_PROFILE = 4;
    public static final int PAGE_COUNT = 5;

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case PAGE_CHAT:
                return new ChatFragment();
            case PAGE_MEALS:
                return new MealsFragment();
            case PAGE_STATS:
                return new StatsFragment();
            case PAGE_PROFILE:
                return new ProfileFragment();
            case PAGE_HOME:
            default:
                return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
