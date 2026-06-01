package com.spokeneasy.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import android.content.res.ColorStateList;
import com.spokeneasy.app.R;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setScrimColor(Color.parseColor("#66000000"));

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment not found");
        }
        navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Per-tab icon tinting (Learn=blue, Practice=amber, Settings=grey)
        int unselectedColor = ContextCompat.getColor(this, R.color.blue_grey_600);
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };

        MenuItem learnItem = bottomNav.getMenu().findItem(R.id.learnFragment);
        if (learnItem != null) {
            learnItem.setIconTintList(new ColorStateList(states, new int[] {
                    ContextCompat.getColor(this, R.color.bottom_nav_learn_tint), unselectedColor
            }));
        }
        MenuItem practiceItem = bottomNav.getMenu().findItem(R.id.practiceRootFragment);
        if (practiceItem != null) {
            practiceItem.setIconTintList(new ColorStateList(states, new int[] {
                    ContextCompat.getColor(this, R.color.bottom_nav_practice_tint), unselectedColor
            }));
        }
        MenuItem settingsItem = bottomNav.getMenu().findItem(R.id.settingsFragment);
        if (settingsItem != null) {
            settingsItem.setIconTintList(new ColorStateList(states, new int[] {
                    ContextCompat.getColor(this, R.color.bottom_nav_settings_tint), unselectedColor
            }));
        }

        NavigationView navView = findViewById(R.id.nav_view);
        NavigationUI.setupWithNavController(navView, navController);

        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, drawerLayout);
    }
}
