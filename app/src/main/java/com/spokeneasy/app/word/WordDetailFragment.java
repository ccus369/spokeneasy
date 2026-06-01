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
import com.spokeneasy.app.core.database.AppDatabase;
import com.spokeneasy.app.core.util.UuidManager;
import com.spokeneasy.app.core.scorer.XunfeiScorer;
import com.spokeneasy.app.progress.PracticeRecordEntity;
import com.spokeneasy.app.progress.UserProgressEntity;

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

    // Per-sentence recording UI
    private TextView[] sentenceScoreTexts;
    private MaterialButton[] btnPlaybackSentence;
    private String[] sentenceAudioPaths;
    private int recordingSentenceIndex = -1;

    // Bottom recording section
    private MaterialButton btnRecord, btnPlayback;
    private AudioWaveformView waveformView;
    private SeekBar playbackSeekBar;
    private boolean isSeeking = false;

    private TTSEngine ttsEngine;
    private AudioRecorder audioRecorder;
    private XunfeiScorer xunfeiScorer;
    private boolean showChinese = false;
    private String currentAudioPath;

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
        sentenceAudioPaths = new String[3];
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

        // Per-sentence recording views
        sentenceScoreTexts = new TextView[]{
                view.findViewById(R.id.score_text_1),
                view.findViewById(R.id.score_text_2),
                view.findViewById(R.id.score_text_3)
        };
        btnPlaybackSentence = new MaterialButton[]{
                view.findViewById(R.id.btn_pb_1),
                view.findViewById(R.id.btn_pb_2),
                view.findViewById(R.id.btn_pb_3)
        };

        btnRecord = view.findViewById(R.id.btn_record);
        btnPlayback = view.findViewById(R.id.btn_playback);
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
                        loadExistingProgress(wordId);
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
            public void onError(String message) {
                Snackbar.make(view, "语音引擎不可用: " + message, Snackbar.LENGTH_LONG)
                        .setAction("设置", v -> {
                            Intent intent = new Intent();
                            intent.setAction("com.android.settings.TTS_SETTINGS");
                            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                                startActivity(intent);
                            } else {
                                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                            }
                        })
                        .show();
            }

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
            com.spokeneasy.app.core.AnimationUtils.animateButtonAction(v);
            if (currentWord != null) {
                ttsEngine.speak(currentWord.getWord());
            }
        });

        btnPlay1.setOnClickListener(v -> {
            com.spokeneasy.app.core.AnimationUtils.animateButtonAction(v);
            if (currentWord != null) ttsEngine.speak(currentWord.getSentence1En());
        });

        btnPlay2.setOnClickListener(v -> {
            com.spokeneasy.app.core.AnimationUtils.animateButtonAction(v);
            if (currentWord != null) ttsEngine.speak(currentWord.getSentence2En());
        });

        btnPlay3.setOnClickListener(v -> {
            com.spokeneasy.app.core.AnimationUtils.animateButtonAction(v);
            if (currentWord != null) ttsEngine.speak(currentWord.getSentence3En());
        });

        btnToggleCn.setOnClickListener(v -> {
            showChinese = !showChinese;
            updateChineseVisibility();
        });

        // Per-sentence playback buttons
        for (int i = 0; i < 3; i++) {
            final int sentenceIdx = i;
            btnPlaybackSentence[i].setOnClickListener(v -> {
                if (sentenceAudioPaths[sentenceIdx] != null) {
                    playbackRecording(sentenceAudioPaths[sentenceIdx]);
                }
            });
        }

        // Bottom recording button (records the next unscored sentence)
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
                int targetIdx = findNextUnscoredSentence();
                startRecording(targetIdx);
            } else {
                stopRecording();
            }
        });

        btnPlayback.setOnClickListener(v -> {
            if (currentAudioPath != null) {
                playbackRecording(currentAudioPath);
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

    private int findNextUnscoredSentence() {
        for (int i = 0; i < 3; i++) {
            if (sentenceAudioPaths[i] == null) return i;
        }
        return 0; // All scored, re-record the first
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                int targetIdx = findNextUnscoredSentence();
                startRecording(targetIdx);
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

    private void loadExistingProgress(long wordId) {
        String uuid = UuidManager.getDeviceUuid(requireContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            for (int i = 0; i < 3; i++) {
                long itemId = wordId * 10 + i + 1;
                UserProgressEntity progress = db.userProgressDao().getByItem(uuid, "word", itemId);
                final int index = i;
                if (progress != null && progress.getScore() > 0) {
                    PracticeRecordEntity record = db.practiceRecordDao().getLatestByItem(uuid, "word", itemId);
                    final String audioPath = record != null ? record.getAudioFilePath() : null;
                    final int score = progress.getScore();
                    requireActivity().runOnUiThread(() -> {
                        sentenceScoreTexts[index].setVisibility(View.VISIBLE);
                        sentenceScoreTexts[index].setText(score + "分");
                        if (audioPath != null && new java.io.File(audioPath).exists()) {
                            sentenceAudioPaths[index] = audioPath;
                            btnPlaybackSentence[index].setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
    }

    private void startRecording(int sentenceIndex) {
        recordingSentenceIndex = sentenceIndex;
        File cacheDir = requireContext().getCacheDir();
        File audioFile = new File(cacheDir, "recording_" + System.currentTimeMillis() + ".wav");
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
        String filePath = audioRecorder.stopRecording();
        btnRecord.setText("开始录音");
        waveformView.removeCallbacks(waveformRunnable);
        waveformView.setState(0);

        if (filePath == null) return;

        int sentenceIdx = recordingSentenceIndex;
        if (sentenceIdx < 0) return;

        final String referenceText = getSentenceText(sentenceIdx);
        if (referenceText == null || referenceText.isEmpty()) return;

        btnRecord.setEnabled(false);
        btnPlayback.setEnabled(false);

        xunfeiScorer.scoreAsync(referenceText, filePath,
                new XunfeiScorer.ScoreCallback() {
            @Override
            public void onResult(int score, String detail) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    sentenceAudioPaths[sentenceIdx] = filePath;
                    currentAudioPath = filePath;

                    // Show score and playback button next to this sentence
                    sentenceScoreTexts[sentenceIdx].setVisibility(View.VISIBLE);
                    btnPlaybackSentence[sentenceIdx].setVisibility(View.VISIBLE);

                    com.spokeneasy.app.core.AnimationUtils.animateScoreCount(
                            sentenceScoreTexts[sentenceIdx], score, "%d分");

                    if (waveformView != null) {
                        waveformView.showSuccess();
                    }

                    savePracticeRecord(sentenceIdx, referenceText, score, detail, filePath);
                    btnRecord.setEnabled(true);
                    btnPlayback.setEnabled(true);
                    recordingSentenceIndex = -1;
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    recordingSentenceIndex = -1;
                    btnRecord.setEnabled(true);
                    btnPlayback.setEnabled(true);
                });
            }
        });
    }

    private String getSentenceText(int sentenceIndex) {
        if (currentWord == null) return "";
        switch (sentenceIndex) {
            case 0: return currentWord.getSentence1En();
            case 1: return currentWord.getSentence2En();
            case 2: return currentWord.getSentence3En();
            default: return "";
        }
    }

    private void playbackRecording(String audioPath) {
        audioRecorder.playBack(audioPath, new AudioRecorder.AudioCallback() {
            @Override
            public void onStart() {
                btnPlayback.setEnabled(false);
                for (int i = 0; i < 3; i++) {
                    btnPlaybackSentence[i].setEnabled(false);
                }
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
                btnPlayback.setEnabled(true);
                for (int i = 0; i < 3; i++) {
                    btnPlaybackSentence[i].setEnabled(true);
                }
            }

            @Override
            public void onError(String message) {
                playbackSeekBar.removeCallbacks(playbackRunnable);
                playbackSeekBar.setVisibility(View.GONE);
                waveformView.setState(0);
                btnPlayback.setEnabled(true);
                for (int i = 0; i < 3; i++) {
                    btnPlaybackSentence[i].setEnabled(true);
                }
            }
        });
    }

    private void savePracticeRecord(int sentenceIndex, String referenceText,
                                     int score, String detail, String audioFilePath) {
        String uuid = UuidManager.getDeviceUuid(requireContext());
        long wordId = currentWord != null ? currentWord.getId() : 0;
        // Encode sentence index into itemId: wordId * 10 + sentenceIndex + 1
        long itemId = wordId * 10 + sentenceIndex + 1;

        PracticeRecordEntity record = new PracticeRecordEntity(
                uuid, "word", itemId, referenceText, score, detail,
                audioFilePath, System.currentTimeMillis());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.practiceRecordDao().insert(record);
            // Update user progress per sentence
            UserProgressEntity progress = new UserProgressEntity();
            progress.setUserUuid(uuid);
            progress.setModuleType("word");
            progress.setItemId(itemId);
            progress.setScore(score);
            progress.setIsCompleted(score >= 60 ? 1 : 0);
            progress.setCompletedAt(System.currentTimeMillis());
            db.userProgressDao().insert(progress);
        });
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
