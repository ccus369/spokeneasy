package com.spokeneasy.app.word;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.AudioRecorder;
import com.spokeneasy.app.core.audio.TTSEngine;
import com.spokeneasy.app.core.scorer.MockScorer;
import com.spokeneasy.app.core.scorer.Scorer;

import java.io.File;
import java.util.Locale;

public class WordDetailFragment extends Fragment {

    private static final String ARG_WORD_ID = "wordId";

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

    private TTSEngine ttsEngine;
    private AudioRecorder audioRecorder;
    private Scorer scorer;
    private boolean showChinese = false;
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
        scorer = new MockScorer();
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
        });
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
                File cacheDir = requireContext().getCacheDir();
                File audioFile = new File(cacheDir, "recording_" + System.currentTimeMillis() + ".m4a");
                currentAudioPath = audioFile.getAbsolutePath();

                boolean started = audioRecorder.startRecording(currentAudioPath);
                if (started) {
                    btnRecord.setText("停止录音");
                    btnPlayback.setEnabled(false);
                }
            } else {
                audioRecorder.stopRecording();
                btnRecord.setText("开始录音");
                btnPlayback.setEnabled(true);
            }
        });

        btnPlayback.setOnClickListener(v -> {
            if (currentAudioPath != null) {
                audioRecorder.playBack(currentAudioPath, new AudioRecorder.AudioCallback() {
                    @Override
                    public void onStart() {
                        btnPlayback.setEnabled(false);
                    }

                    @Override
                    public void onStop(String filePath) {
                        btnPlayback.setEnabled(true);
                        int score = scorer.score(
                                currentWord != null ? currentWord.getSentence1En() : "",
                                filePath);
                        scoreText.setVisibility(View.VISIBLE);
                        scoreText.setText(String.format(Locale.getDefault(), "%d 分", score));
                        com.spokeneasy.app.core.AnimationUtils.animateScorePulse(scoreText);
                    }

                    @Override
                    public void onError(String message) {
                        btnPlayback.setEnabled(true);
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsEngine != null) ttsEngine.shutdown();
        if (audioRecorder != null) audioRecorder.release();
    }
}
