package com.spokeneasy.app.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.spokeneasy.app.R;
import com.spokeneasy.app.dialogue.DialogueFragment;
import com.spokeneasy.app.drill.PatternDrillFragment;
import com.spokeneasy.app.pronunciation.PronunciationLabFragment;

public class PracticeRootFragment extends Fragment {

    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_practice_root, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tab_layout);

        tabLayout.addTab(tabLayout.newTab().setText("发音实验室"));
        tabLayout.addTab(tabLayout.newTab().setText("句型操练"));
        tabLayout.addTab(tabLayout.newTab().setText("情景对话"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchFragment(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Default to first tab
        switchFragment(0);
    }

    private void switchFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new PronunciationLabFragment();
                break;
            case 1:
                fragment = new PatternDrillFragment();
                break;
            case 2:
                fragment = new DialogueFragment();
                break;
            default:
                fragment = new PronunciationLabFragment();
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }
}
