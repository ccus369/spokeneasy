package com.spokeneasy.app.word;

import android.Manifest;
import android.content.Intent;
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

public class WordDetailFragment extends Fragment {

    private static final String ARG_WORD_ID = "wordId";
    private static final int REQUEST_RECORD_AUDIO = 100;

    private WordViewModel viewModel;
    private WordEntity currentWord;

    private TextView detailWord;
    private TextView detailPhonetic;
    private TextView sentence1En, sentence2En, sentence3En;
    private TextView sentence1Cn, sentence2Cn, sentence3Cn;
    private MaterialButton btnPlayWord, btnPlay1, btnPlay2, btnPlay3;
    private MaterialButton btnToggleCn;
    private MaterialButton btnRecord, btnPlayback;
    private TextView scoreText;
    private TextView detailFeedback;

    private AudioWaveformView waveformView;
    private SeekBar playbackSeekBar;
    private boolean isSeeking = false;

    private TTSEngine ttsEngine;
    private AudioRecorder audioRecorder;
    private XunfeiScorer xunfeiScorer;
    private boolean showChinese = false;
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
    private String currentAudioPath;

    public static WordDetailFragment newInstance(long wordId) {
        WordDetailFragment fragment = new WordDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_WORD_ID, wordId);
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
        return inflater.inflate(R.layout.fragment_word_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        detailWord = view.findViewById(R.id.detail_word);
        detailPhonetic = view.findViewById(R.id.detail_phonetic);
        sentence1En = view.findViewById(R.id.sentence1_en);
        sentence2En = view.findViewById(R.id.sentence2_en);
        sentence3En = view.findViewById(R.id.sentence3_en);
        sentence1Cn = view.findViewById(R.id.sentence1_cn);
        sentence2Cn = view.findViewById(R.id.sentence2_cn);
        sentence3Cn = view.findViewById(R.id.sentence3_cn);
        btnPlayWord = view.findViewById(R.id.btn_play_word);
        btnPlay1 = view.findViewById(R.id.btn_play_1);
        btnPlay2 = view.findViewById(R.id.btn_play_2);
        btnPlay3 = view.findViewById(R.id.btn_play_3);
        btnToggleCn = view.findViewById(R.id.btn_toggle_cn);
        btnRecord = view.findViewById(R.id.btn_record);
        btnPlayback = view.findViewById(R.id.btn_playback);
        scoreText = view.findViewById(R.id.score_text);
        detailFeedback = view.findViewById(R.id.detail_feedback);
        waveformView = view.findViewById(R.id.waveform_view);
        playbackSeekBar = view.findViewById(R.id.playback_seekbar);

        viewModel = new ViewModelProvider(requireActivity()).get(WordViewModel.class);

        long wordId = getArguments() != null ? getArguments().getLong(ARG_WORD_ID, -1) : -1;
        if (wordId == -1) {
            requireActivity().onBackPressed();
            return;
        }

        viewModel.getWords().observe(getViewLifecycleOwner(), words -> {
            if (words != null) {
                for (WordEntity w : words) {
                    if (w.getId() == wordId) {
                        currentWord = w;
                        bindWord(w);
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

            @Override
            public void onLanguageWarning(String message) {
                if (ttsEngine.isMissingLanguageData()) {
                    Snackbar.make(view, "需要下载英语语音数据", Snackbar.LENGTH_LONG)
                            .setAction("去下载", v -> {
                                Intent intent = new Intent(
                                        android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                                startActivity(intent);
                            })
                            .show();
                } else {
                    Snackbar.make(view, "设备不支持英语语音播放，请安装 Google 文字转语音", Snackbar.LENGTH_LONG)
                            .setAction("知道了", null)
                            .show();
                }
            }
        });

        xunfeiScorer.init(requireContext());
    }

    private void bindWord(WordEntity word) {
        detailWord.setText(word.getWord());
        detailPhonetic.setText(String.format(Locale.getDefault(), "/%s/",
                word.getPhonetic() != null ? word.getPhonetic() : ""));

        sentence1En.setText(word.getSentence1En());
        sentence2En.setText(word.getSentence2En());
        sentence3En.setText(word.getSentence3En());

        sentence1Cn.setText(word.getSentence1Cn());
        sentence2Cn.setText(word.getSentence2Cn());
        sentence3Cn.setText(word.getSentence3Cn());

        updateChineseVisibility();
    }

    private void updateChineseVisibility() {
        int visibility = showChinese ? View.VISIBLE : View.GONE;
        sentence1Cn.setVisibility(visibility);
        sentence2Cn.setVisibility(visibility);
        sentence3Cn.setVisibility(visibility);
        btnToggleCn.setText(showChinese ? "隐藏中文翻译" : "显示中文翻译");
    }

    private void setupListeners() {
        btnPlayWord.setOnClickListener(v -> {
            if (currentWord != null) {
                ttsEngine.speak(currentWord.getWord());
            }
        });

        btnPlay1.setOnClickListener(v -> {
            if (currentWord != null) ttsEngine.speak(currentWord.getSentence1En());
        });

        btnPlay2.setOnClickListener(v -> {
            if (currentWord != null) ttsEngine.speak(currentWord.getSentence2En());
        });

        btnPlay3.setOnClickListener(v -> {
            if (currentWord != null) ttsEngine.speak(currentWord.getSentence3En());
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

                        final String referenceText = currentWord != null
                                ? currentWord.getSentence1En() : "";
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
            btnRecord.setText("停止录音");
            btnPlayback.setEnabled(false);
            waveformView.setState(1);
            waveformView.post(waveformRunnable);
        }
    }

    private void stopRecording() {
        audioRecorder.stopRecording();
        btnRecord.setText("开始录音");
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
