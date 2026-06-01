package com.spokeneasy.app.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.tabs.TabLayout;
import com.spokeneasy.app.R;
import com.spokeneasy.app.linking.LinkingListFragment;
import com.spokeneasy.app.progress.UserProgressViewModel;
import com.spokeneasy.app.shadowing.ShadowingListFragment;
import com.spokeneasy.app.word.WordListFragment;

import java.util.List;
import java.util.Locale;

public class LearnFragment extends Fragment {

    private TabLayout tabLayout;
    private Fragment currentFragment;
    private UserProgressViewModel viewModel;

    private TextView greetingText;
    private TextView greetingWordCount;
    private TextView greetingLinkingCount;
    private TextView greetingPronunciationCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_learn, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        greetingText = view.findViewById(R.id.greeting_text);
        greetingWordCount = view.findViewById(R.id.greeting_word_count);
        greetingLinkingCount = view.findViewById(R.id.greeting_linking_count);
        greetingPronunciationCount = view.findViewById(R.id.greeting_pronunciation_count);

        // Set greeting based on time of day
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "早上好 🌅";
        } else if (hour < 18) {
            greeting = "下午好 ☀️";
        } else {
            greeting = "晚上好 🌙";
        }
        greetingText.setText(greeting);

        tabLayout = view.findViewById(R.id.tab_layout);

        tabLayout.addTab(tabLayout.newTab().setText("单词学习"));
        tabLayout.addTab(tabLayout.newTab().setText("连读练习"));
        tabLayout.addTab(tabLayout.newTab().setText("听力跟读"));

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

        // Observe stats for greeting bar
        viewModel = new ViewModelProvider(this).get(UserProgressViewModel.class);
        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && stats.size() >= 3) {
                updateGreetingStats(stats);
            }
        });

        // Default to first tab
        switchFragment(0);
    }

    private void updateGreetingStats(List<UserProgressViewModel.ModuleStats> stats) {
        UserProgressViewModel.ModuleStats word = stats.get(0);
        UserProgressViewModel.ModuleStats linking = stats.get(1);
        UserProgressViewModel.ModuleStats pronunciation = stats.get(2);

        greetingWordCount.setText(String.format(Locale.getDefault(),
                "单词 %d/%d", word.getCompletedCount(), word.getTotalCount()));
        greetingLinkingCount.setText(String.format(Locale.getDefault(),
                "连读 %d/%d", linking.getCompletedCount(), linking.getTotalCount()));
        greetingPronunciationCount.setText(String.format(Locale.getDefault(),
                "发音 %d/%d", pronunciation.getCompletedCount(), pronunciation.getTotalCount()));
    }

    private void switchFragment(int position) {
        Fragment fragment;
        if (position == 0) {
            fragment = new WordListFragment();
        } else if (position == 1) {
            fragment = new LinkingListFragment();
        } else {
            ShadowingListFragment sf = new ShadowingListFragment();
            sf.setOnItemClickListener(item -> {
                Bundle args = new Bundle();
                args.putInt("audioId", item.getId());
                NavHostFragment.findNavController(this)
                        .navigate(R.id.shadowingDetailFragment, args);
            });
            fragment = sf;
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
        currentFragment = fragment;
    }
}
