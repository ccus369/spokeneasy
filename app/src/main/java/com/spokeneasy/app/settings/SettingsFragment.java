package com.spokeneasy.app.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.spokeneasy.app.R;
import com.spokeneasy.app.progress.UserProgressViewModel;

import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private UserProgressViewModel viewModel;

    private TextView statsWordLabel;
    private TextView statsLinkingLabel;
    private TextView statsListeningLabel;
    private ProgressBar statsWordProgress;
    private ProgressBar statsLinkingProgress;
    private ProgressBar statsListeningProgress;
    private MaterialButton btnExport;
    private MaterialButton btnReset;
    private TextView deviceUuidText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statsWordLabel = view.findViewById(R.id.stats_word_label);
        statsLinkingLabel = view.findViewById(R.id.stats_linking_label);
        statsListeningLabel = view.findViewById(R.id.stats_listening_label);
        statsWordProgress = view.findViewById(R.id.stats_word_progress);
        statsLinkingProgress = view.findViewById(R.id.stats_linking_progress);
        statsListeningProgress = view.findViewById(R.id.stats_listening_progress);
        btnExport = view.findViewById(R.id.btn_export_data);
        btnReset = view.findViewById(R.id.btn_reset_data);
        deviceUuidText = view.findViewById(R.id.settings_device_uuid);

        viewModel = new ViewModelProvider(this).get(UserProgressViewModel.class);

        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && stats.size() >= 3) {
                updateStat(stats.get(0), statsWordLabel, statsWordProgress);
                updateStat(stats.get(1), statsLinkingLabel, statsLinkingProgress);
                updateStat(stats.get(2), statsListeningLabel, statsListeningProgress);
            }
        });

        deviceUuidText.setText(String.format(Locale.getDefault(),
                "设备 ID: %s", viewModel.getUserUuid()));

        setupListeners();
    }

    private void updateStat(UserProgressViewModel.ModuleStats stat,
                            TextView label, ProgressBar progressBar) {
        label.setText(String.format(Locale.getDefault(), "%d/%d 已完成",
                stat.getCompletedCount(), stat.getTotalCount()));

        int pct = stat.getTotalCount() > 0
                ? (stat.getCompletedCount() * 100 / stat.getTotalCount())
                : 0;
        progressBar.setProgress(pct);
    }

    private void setupListeners() {
        btnExport.setOnClickListener(v -> {
            List<UserProgressViewModel.ModuleStats> stats =
                    viewModel.getStats().getValue();
            if (stats == null || stats.size() < 3) return;

            String body = String.format(Locale.getDefault(),
                    getString(R.string.settings_export_body),
                    stats.get(0).getCompletedCount(), stats.get(0).getTotalCount(),
                    stats.get(1).getCompletedCount(), stats.get(1).getTotalCount(),
                    stats.get(2).getCompletedCount(), stats.get(2).getTotalCount());

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                    getString(R.string.settings_export_subject));
            shareIntent.putExtra(Intent.EXTRA_TEXT, body);
            startActivity(Intent.createChooser(shareIntent,
                    getString(R.string.settings_export_data)));
        });

        btnReset.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.settings_reset_confirm_title)
                    .setMessage(R.string.settings_reset_confirm_message)
                    .setPositiveButton(R.string.settings_confirm,
                            (DialogInterface dialog, int which) -> {
                                viewModel.resetAllProgress(null);
                            })
                    .setNegativeButton(R.string.settings_cancel, null)
                    .show();
        });
    }
}
