package com.spokeneasy.app.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.spokeneasy.app.R;
import com.spokeneasy.app.progress.UserProgressViewModel;

import java.util.List;
import java.util.Locale;

public class LearnFragment extends Fragment {

    private UserProgressViewModel viewModel;

    private TextView greetingText;
    private TextView greetingSubtitle;
    private TextView overallProgressText;
    private ProgressBar overallProgressBar;
    private TextView statsWord;
    private TextView statsLinking;
    private TextView statsPronunciation;

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

        // --- Greeting ---
        greetingText = view.findViewById(R.id.greeting_text);
        greetingSubtitle = view.findViewById(R.id.greeting_subtitle);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        String emoji;
        String phrase;
        if (hour < 12) {
            emoji = "🌅"; // 🌅
            phrase = "早上好";
        } else if (hour < 18) {
            emoji = "☀️"; // ☀️
            phrase = "下午好";
        } else {
            emoji = "🌙"; // 🌙
            phrase = "晚上好";
        }
        greetingText.setText(phrase + " " + emoji);
        greetingSubtitle.setText("今天也要加油哦 💪");

        // --- Overall progress ---
        overallProgressText = view.findViewById(R.id.overall_progress_text);
        overallProgressBar = view.findViewById(R.id.overall_progress_bar);

        // --- Stats chips ---
        statsWord = view.findViewById(R.id.stats_word);
        statsLinking = view.findViewById(R.id.stats_linking);
        statsPronunciation = view.findViewById(R.id.stats_pronunciation);

        // --- Bento tiles ---
        setupTile(view, R.id.tile_word, R.drawable.ic_book_outline,
                R.drawable.bg_icon_indigo, "单词学习", "逐词学习，AI 评分",
                R.id.action_learn_to_wordList);

        setupTile(view, R.id.tile_linking, R.drawable.ic_link_outline,
                R.drawable.bg_icon_amber, "连读练习", "地道发音，连读技巧",
                R.id.action_learn_to_linkingList);

        setupTile(view, R.id.tile_listening, R.drawable.ic_headphone_outline,
                R.drawable.bg_icon_blue, "听力跟读", "真实场景，沉浸练习",
                R.id.action_learn_to_listeningList);

        setupTile(view, R.id.tile_chat, R.drawable.ic_chat_outline,
                R.drawable.bg_icon_purple, "AI 对话", "智能陪练，随时对话",
                R.id.action_learn_to_chat);

        // AI Chat 没有进度数据，隐藏进度条区域
        requireView().findViewById(R.id.tile_chat)
                .findViewById(R.id.tile_progress_section)
                .setVisibility(View.GONE);

        // --- Observe stats ---
        viewModel = new ViewModelProvider(this).get(UserProgressViewModel.class);
        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && stats.size() >= 3) {
                updateStats(stats);
            }
        });
    }

    private void setupTile(View rootView, int tileId, int iconRes, int bgRes,
                           String title, String subtitle, int actionId) {
        View tile = rootView.findViewById(tileId);
        ImageView icon = tile.findViewById(R.id.tile_icon);
        TextView titleView = tile.findViewById(R.id.tile_title);
        TextView subtitleView = tile.findViewById(R.id.tile_subtitle);

        icon.setImageResource(iconRes);
        icon.setBackgroundResource(bgRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(R.color.white, getContext().getTheme())));
        titleView.setText(title);
        subtitleView.setText(subtitle);

        tile.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(actionId));
    }

    private void updateStats(List<UserProgressViewModel.ModuleStats> stats) {
        UserProgressViewModel.ModuleStats word = stats.get(0);
        UserProgressViewModel.ModuleStats linking = stats.get(1);
        UserProgressViewModel.ModuleStats pronunciation = stats.get(2);

        // Overall progress
        int totalTotal = word.getTotalCount() + linking.getTotalCount()
                + pronunciation.getTotalCount();
        int totalCompleted = word.getCompletedCount() + linking.getCompletedCount()
                + pronunciation.getCompletedCount();

        if (totalTotal > 0) {
            int percent = totalCompleted * 100 / totalTotal;
            overallProgressBar.setProgress(percent);
        }
        overallProgressText.setText(String.format(Locale.getDefault(),
                "%d/%d", totalCompleted, totalTotal));

        // Stats chips
        statsWord.setText(String.format(Locale.getDefault(),
                "单词 %d/%d", word.getCompletedCount(), word.getTotalCount()));
        statsLinking.setText(String.format(Locale.getDefault(),
                "连读 %d/%d", linking.getCompletedCount(), linking.getTotalCount()));
        statsPronunciation.setText(String.format(Locale.getDefault(),
                "发音 %d/%d", pronunciation.getCompletedCount(),
                pronunciation.getTotalCount()));

        // Per-tile progress
        updateTileProgress(R.id.tile_word,
                word.getCompletedCount(), word.getTotalCount());
        updateTileProgress(R.id.tile_linking,
                linking.getCompletedCount(), linking.getTotalCount());
        updateTileProgress(R.id.tile_listening,
                pronunciation.getCompletedCount(), pronunciation.getTotalCount());
    }

    private void updateTileProgress(int tileId, int completed, int total) {
        View tile = requireView().findViewById(tileId);
        ProgressBar bar = tile.findViewById(R.id.tile_progress_bar);
        TextView text = tile.findViewById(R.id.tile_progress_text);

        if (total > 0) {
            bar.setProgress(completed * 100 / total);
        }
        text.setText(String.format(Locale.getDefault(), "%d/%d", completed, total));
    }
}
