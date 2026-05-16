package com.spokeneasy.app.linking;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.AudioRecorder;
import com.spokeneasy.app.core.audio.AudioWaveformView;
import com.spokeneasy.app.core.audio.TTSEngine;
import com.spokeneasy.app.core.scorer.XunfeiScorer;

import java.io.File;
import java.util.Locale;

public class LinkingDetailFragment extends Fragment {

    private static final String ARG_LINKING_ID = "linkingId";
    private static final int REQUEST_RECORD_AUDIO = 100;

    private LinkingViewModel viewModel;
    private LinkingEntity currentItem;

    private TextView detailRuleName;
    private TextView detailOriginal;
    private TextView detailLinkingText;
    private TextView detailExampleEn;
    private TextView detailExampleCn;
    private MaterialButton btnPlayRule, btnPlayExample;
    private MaterialButton btnToggleCn;
    private MaterialButton btnRecord, btnPlayback;
    private TextView scoreText;
    private TextView detailFeedback;

    private AudioWaveformView waveformView;
    private SeekBar playbackSeekBar;
    private boolean isSeeking = false;
    private final Runnable waveformRunnable = new Runnable() {
        @Override
        public void run() {
            if (audioRecorder != null && audioRecorder.isRecording()) {
                int amp = audioRecorder.getMaxAmplitude();
                float normalized = amp > 0 ? Math.min(1f, (float) (Math.log10(1 + amp) / 4.5)) : 0f;
                waveformView.setAmplitude(normalized);
                waveformView.postDelayed(this, 80);
            }
        }
    };
    private final Runnable playbackRunnable = new Runnable() {
        @Override
        public void run() {
            if (audioRecorder != null && audioRecorder.isPlaying()) {
                int pos = audioRecorder.getPlaybackPosition();
                int dur = audioRecorder.getPlaybackDuration();
                if (dur > 0 && !isSeeking) {
                    playbackSeekBar.setProgress(pos * 1000 / dur);
                }
                playbackSeekBar.postDelayed(this, 100);
            }
        }
    };

    private TTSEngine ttsEngine;
    private AudioRecorder audioRecorder;
    private XunfeiScorer xunfeiScorer;
    private boolean showChinese = false;
    private String currentAudioPath;

    public static LinkingDetailFragment newInstance(long linkingId) {
        LinkingDetailFragment fragment = new LinkingDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_LINKING_ID, linkingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ttsEngine = new TTSEngine();
        audioRecorder = new AudioRecorder();
        xunfeiScorer = new XunfeiScorer();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_linking_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        detailRuleName = view.findViewById(R.id.detail_rule_name);
        detailOriginal = view.findViewById(R.id.detail_original);
        detailLinkingText = view.findViewById(R.id.detail_linking_text);
        detailExampleEn = view.findViewById(R.id.detail_example_en);
        detailExampleCn = view.findViewById(R.id.detail_example_cn);
        btnPlayRule = view.findViewById(R.id.btn_play_rule);
        btnPlayExample = view.findViewById(R.id.btn_play_example);
        btnToggleCn = view.findViewById(R.id.btn_toggle_cn);
        btnRecord = view.findViewById(R.id.btn_record);
        btnPlayback = view.findViewById(R.id.btn_playback);
        scoreText = view.findViewById(R.id.score_text);
        detailFeedback = view.findViewById(R.id.detail_feedback);
        waveformView = view.findViewById(R.id.waveform_view);
        playbackSeekBar = view.findViewById(R.id.playback_seekbar);

        viewModel = new ViewModelProvider(requireActivity()).get(LinkingViewModel.class);

        long linkingId = getArguments() != null ? getArguments().getLong(ARG_LINKING_ID, -1) : -1;
        if (linkingId == -1) {
            requireActivity().onBackPressed();
            return;
        }

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                for (LinkingEntity item : items) {
                    if (item.getId() == linkingId) {
                        currentItem = item;
                        bindItem(item);
                        break;
                    }
                }
            }
        });

        setupListeners();

        ttsEngine.init(requireContext(), new TTSEngine.TtsCallback() {
            @Override
            public void onDone() {}

            @Override
            public void onError(String message) {}
        });

        xunfeiScorer.init(requireContext());
    }

    private void bindItem(LinkingEntity item) {
        detailRuleName.setText(item.getRuleName());
        detailOriginal.setText(item.getOriginal());
        detailLinkingText.setText(item.getLinkingText());
        detailExampleEn.setText(item.getExampleEn());
        detailExampleCn.setText(item.getExampleCn());
        updateChineseVisibility();
        animateArrow();
    }

    private void animateArrow() {
        View arrow = getView().findViewById(R.id.detail_arrow);
        if (arrow != null) {
            arrow.setTranslationX(-20f);
            arrow.setAlpha(0f);
            arrow.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .start();
        }
    }

    private void updateChineseVisibility() {
        int visibility = showChinese ? View.VISIBLE : View.GONE;
        detailExampleCn.setVisibility(visibility);
        btnToggleCn.setText(showChinese ? R.string.hide_translation : R.string.show_translation);
    }

    private void setupListeners() {
        btnPlayRule.setOnClickListener(v -> {
            if (currentItem != null) {
                ttsEngine.speak(currentItem.getLinkingText());
            }
        });

        btnPlayExample.setOnClickListener(v -> {
            if (currentItem != null) ttsEngine.speak(currentItem.getExampleEn());
        });

        btnToggleCn.setOnClickListener(v -> {
            showChinese = !showChinese;
            updateChineseVisibility();
        });

        btnRecord.setOnClickListener(v -> {
            if (!audioRecorder.isRecording()) {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_AUDIO);
                    return;
                }
                startRecording();
            } else {
                stopRecording();
            }
        });

        btnPlayback.setOnClickListener(v -> {
            if (currentAudioPath != null) {
                audioRecorder.playBack(currentAudioPath, new AudioRecorder.AudioCallback() {
                    @Override
                    public void onStart() {
                        btnPlayback.setEnabled(false);
                        waveformView.setState(2);
                        playbackSeekBar.setVisibility(View.VISIBLE);
                        playbackSeekBar.setProgress(0);
                        playbackSeekBar.post(playbackRunnable);
                    }

                    @Override
                    public void onStop(String filePath) {
                        playbackSeekBar.removeCallbacks(playbackRunnable);
                        playbackSeekBar.setVisibility(View.GONE);
                        playbackSeekBar.setProgress(0);
                        waveformView.setState(0);

                        if (!xunfeiScorer.isInitialized()) {
                            btnPlayback.setEnabled(true);
                            return;
                        }

                        scoreText.setVisibility(View.VISIBLE);
                        scoreText.setText("评分中...");

                        final String referenceText = currentItem != null
                                ? currentItem.getExampleEn() : "";
                        xunfeiScorer.scoreAsync(referenceText, filePath,
                                new XunfeiScorer.ScoreCallback() {
                            @Override
                            public void onResult(int score, String detail) {
                                scoreText.setText(String.format(Locale.getDefault(),
                                        "%d 分", score));
                                com.spokeneasy.app.core.AnimationUtils.animateScorePulse(scoreText);

                                if (detail != null && !detail.isEmpty()) {
                                    detailFeedback.setVisibility(View.VISIBLE);
                                    detailFeedback.setText(detail);
                                }
                                btnPlayback.setEnabled(true);
                            }

                            @Override
                            public void onError(String message) {
                                scoreText.setText("评分失败");
                                detailFeedback.setVisibility(View.VISIBLE);
                                detailFeedback.setText(message);
                                btnPlayback.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        playbackSeekBar.removeCallbacks(playbackRunnable);
                        playbackSeekBar.setVisibility(View.GONE);
                        waveformView.setState(0);
                        btnPlayback.setEnabled(true);
                    }
                });
            }
        });

        playbackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && audioRecorder != null) {
                    int dur = audioRecorder.getPlaybackDuration();
                    if (dur > 0) {
                        audioRecorder.seekTo(progress * dur / 1000);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Snackbar.make(requireView(), "需要录音权限才能使用跟读功能",
                        Snackbar.LENGTH_LONG)
                        .setAction("去设置", v -> {
                            android.content.Intent intent = new android.content.Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(android.net.Uri.parse("package:" + requireContext().getPackageName()));
                            startActivity(intent);
                        })
                        .show();
            }
        }
    }

    private void startRecording() {
        File cacheDir = requireContext().getCacheDir();
        File audioFile = new File(cacheDir, "recording_" + System.currentTimeMillis() + ".m4a");
        currentAudioPath = audioFile.getAbsolutePath();

        boolean started = audioRecorder.startRecording(currentAudioPath);
        if (started) {
            btnRecord.setText(R.string.stop_record);
            btnPlayback.setEnabled(false);
            waveformView.setState(1);
            waveformView.post(waveformRunnable);
        }
    }

    private void stopRecording() {
        audioRecorder.stopRecording();
        btnRecord.setText(R.string.start_record);
        btnPlayback.setEnabled(true);
        waveformView.removeCallbacks(waveformRunnable);
        waveformView.setState(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (waveformView != null) {
            waveformView.removeCallbacks(waveformRunnable);
        }
        if (playbackSeekBar != null) {
            playbackSeekBar.removeCallbacks(playbackRunnable);
        }
        if (ttsEngine != null) ttsEngine.shutdown();
        if (audioRecorder != null) audioRecorder.release();
        if (xunfeiScorer != null) xunfeiScorer.destroy();
    }
}
