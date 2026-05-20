package com.spokeneasy.app.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.spokeneasy.app.core.net.ApiConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.TTSEngine;
import com.spokeneasy.app.core.audio.TtsHelper;
import com.spokeneasy.app.progress.UserProgressViewModel;

import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private UserProgressViewModel viewModel;

    private TextView statsWordLabel;
    private TextView statsLinkingLabel;
    private TextView statsPronunciationLabel;
    private ProgressBar statsWordProgress;
    private ProgressBar statsLinkingProgress;
    private ProgressBar statsPronunciationProgress;
    private MaterialButton btnExport;
    private MaterialButton btnReset;
    private MaterialButton btnRecordHistory;
    private TextView deviceUuidText;

    // TTS section
    private TextView ttsStatusText;
    private TextView ttsEngineText;
    private MaterialButton btnTtsTest;
    private MaterialButton btnTtsSettings;
    private MaterialButton btnTtsInstall;
    private View ttsActionLayout;

    private TTSEngine ttsEngine;

    // MiMo API section
    private TextInputEditText mimoKeyInput;
    private MaterialButton btnMimoSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadStats();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statsWordLabel = view.findViewById(R.id.stats_word_label);
        statsLinkingLabel = view.findViewById(R.id.stats_linking_label);
        statsPronunciationLabel = view.findViewById(R.id.stats_pronunciation_label);
        statsWordProgress = view.findViewById(R.id.stats_word_progress);
        statsLinkingProgress = view.findViewById(R.id.stats_linking_progress);
        statsPronunciationProgress = view.findViewById(R.id.stats_pronunciation_progress);
        btnExport = view.findViewById(R.id.btn_export_data);
        btnReset = view.findViewById(R.id.btn_reset_data);
        btnRecordHistory = view.findViewById(R.id.btn_record_history);
        deviceUuidText = view.findViewById(R.id.settings_device_uuid);

        ttsStatusText = view.findViewById(R.id.tts_status_text);
        ttsEngineText = view.findViewById(R.id.tts_engine_text);
        btnTtsTest = view.findViewById(R.id.btn_tts_test);
        btnTtsSettings = view.findViewById(R.id.btn_tts_settings);
        btnTtsInstall = view.findViewById(R.id.btn_tts_install);
        ttsActionLayout = view.findViewById(R.id.tts_action_layout);

        mimoKeyInput = view.findViewById(R.id.mimo_key_input);
        btnMimoSave = view.findViewById(R.id.btn_mimo_save);

        viewModel = new ViewModelProvider(this).get(UserProgressViewModel.class);

        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && stats.size() >= 3) {
                updateStat(stats.get(0), statsWordLabel, statsWordProgress);
                updateStat(stats.get(1), statsLinkingLabel, statsLinkingProgress);
                updateStat(stats.get(2), statsPronunciationLabel, statsPronunciationProgress);
            }
        });

        deviceUuidText.setText(String.format(Locale.getDefault(),
                "设备 ID: %s", viewModel.getUserUuid()));

        setupTtsSection();
        loadMimoApiKey();
        setupListeners();
    }

    private void setupTtsSection() {
        ttsStatusText.setText(R.string.tts_status_checking);
        ttsEngineText.setText("");

        TtsHelper.check(requireContext(), (state, engineLabel) -> {
            if (!isAdded()) return;

            // Show detected engine(s) info
            String engInfo = "";
            if (engineLabel != null && !engineLabel.isEmpty()) {
                if (state == TtsHelper.TtsState.NO_ENGINE && engineLabel.startsWith("已安装引擎")) {
                    engInfo = engineLabel; // Show the list of installed engines
                } else {
                    engInfo = "引擎: " + engineLabel;
                }
            }
            ttsEngineText.setText(engInfo);

            switch (state) {
                case AVAILABLE:
                    ttsStatusText.setText(R.string.tts_status_available);
                    btnTtsInstall.setVisibility(View.GONE);
                    btnTtsTest.setVisibility(View.VISIBLE);
                    break;
                case MISSING_DATA:
                    ttsStatusText.setText(R.string.tts_status_missing_data);
                    btnTtsInstall.setVisibility(View.VISIBLE);
                    btnTtsInstall.setText(R.string.tts_install_data);
                    btnTtsTest.setVisibility(View.VISIBLE);
                    break;
                case NOT_SUPPORTED:
                    ttsStatusText.setText(R.string.tts_status_not_supported);
                    btnTtsInstall.setVisibility(View.VISIBLE);
                    btnTtsInstall.setText(R.string.tts_configure_engine);
                    btnTtsTest.setVisibility(View.VISIBLE);
                    break;
                case NO_ENGINE:
                    ttsStatusText.setText(R.string.tts_status_no_engine);
                    btnTtsInstall.setVisibility(View.VISIBLE);
                    btnTtsInstall.setText(R.string.tts_configure_engine);
                    if (engineLabel != null && engineLabel.startsWith("已安装引擎")) {
                        // Has installed engines but none worked — show test button
                        btnTtsTest.setVisibility(View.VISIBLE);
                        btnTtsTest.setText("重试检测");
                    } else {
                        btnTtsTest.setVisibility(View.GONE);
                    }
                    break;
            }
        });
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

    private void loadMimoApiKey() {
        com.spokeneasy.app.core.net.ApiConfig config =
                new com.spokeneasy.app.core.net.ApiConfig(requireContext());
        String key = config.getMiMoApiKey();
        if (!key.isEmpty()) {
            mimoKeyInput.setText(key);
        }
    }

    private void setupListeners() {
        btnMimoSave.setOnClickListener(v -> {
            String key = mimoKeyInput.getText() != null
                    ? mimoKeyInput.getText().toString().trim() : "";
            com.spokeneasy.app.core.net.ApiConfig config =
                    new com.spokeneasy.app.core.net.ApiConfig(requireContext());
            config.setMiMoApiKey(key);
            int msgRes = key.isEmpty() ? R.string.mimo_api_key_cleared : R.string.mimo_api_key_saved;
            Snackbar.make(requireView(), msgRes, Snackbar.LENGTH_SHORT).show();
        });

        btnRecordHistory.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.action_settings_to_recordHistory));

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

        btnTtsTest.setOnClickListener(v -> speakTestPhrase());

        btnTtsSettings.setOnClickListener(v -> openTtsSettings());

        btnTtsInstall.setOnClickListener(v -> {
            String text = btnTtsInstall.getText().toString();
            if (text.equals(getString(R.string.tts_install_data))) {
                // Try to install TTS data — wrap in try-catch in case no handler exists
                try {
                    Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        // No TTS data installer available, open system TTS settings instead
                        openTtsSettings();
                    }
                } catch (Exception e) {
                    openTtsSettings();
                }
            } else {
                openTtsSettings();
            }
        });
    }

    private void openTtsSettings() {
        Intent intent = new Intent();
        intent.setAction("com.android.settings.TTS_SETTINGS");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent fallback = new Intent(Settings.ACTION_SETTINGS);
            startActivity(fallback);
        }
    }

    private void speakTestPhrase() {
        // If button text is "重试检测", re-run TTS detection
        if ("重试检测".equals(btnTtsTest.getText().toString())) {
            setupTtsSection();
            return;
        }

        // Check media volume first
        android.media.AudioManager am = (android.media.AudioManager)
                requireContext().getSystemService(android.content.Context.AUDIO_SERVICE);
        int vol = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
        if (vol == 0) {
            Snackbar.make(requireView(), "媒体音量已静音，请调高音量", Snackbar.LENGTH_LONG).show();
        }

        if (ttsEngine == null) {
            ttsEngine = new TTSEngine();
            ttsEngine.init(requireContext(), new TTSEngine.TtsCallback() {
                @Override
                public void onDone() {
                    ttsEngine.speak(getString(R.string.tts_test_phrase));
                }

                @Override
                public void onError(String message) {
                    Snackbar.make(requireView(),
                            "TTS 初始化失败: " + message, Snackbar.LENGTH_LONG).show();
                    ttsEngine = null;
                }

                @Override
                public void onLanguageWarning(String message) {
                    Snackbar.make(requireView(),
                            message, Snackbar.LENGTH_LONG).show();
                    ttsEngine.speak(getString(R.string.tts_test_phrase));
                }
            });
        } else if (ttsEngine.isInitialized()) {
            ttsEngine.speak(getString(R.string.tts_test_phrase));
        } else {
            Snackbar.make(requireView(), "TTS 引擎尚未就绪，请稍后重试",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsEngine != null) {
            ttsEngine.shutdown();
            ttsEngine = null;
        }
    }
}
