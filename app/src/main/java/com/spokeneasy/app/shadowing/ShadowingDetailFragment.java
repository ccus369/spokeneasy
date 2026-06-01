package com.spokeneasy.app.shadowing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.AudioRecorder;
import com.spokeneasy.app.core.audio.TTSEngine;
import com.spokeneasy.app.core.scorer.XunfeiScorer;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ShadowingDetailFragment extends Fragment {

    private static final String ARG_AUDIO_ID = "audioId";

    private ShadowingContent content;
    private String[] sentences;
    private int currentIndex = 0;
    private boolean ttsReady = false;
    private boolean isPlayingAll = false;
    private int[] sentenceScores;
    private int scoredCount = 0;

    // UI
    private TextView detailTitle, progressText, currentSentence, speakerLabel;
    private TextView scoreDisplay, overallScoreText;
    private Chip detailLevelChip, detailTypeChip;
    private MaterialButton btnPlayAll, btnPlaySentence, btnRecord, btnPrev, btnNext;
    private MaterialButton btnShowText;
    private MaterialCardView dialogTextCard, scoreCard;
    private TextView detailDialogText;
    private LinearLayout scoreSection;
    private TextView recordingIndicator;

    // TTS
    private TTSEngine ttsEngine;

    // Recording
    private AudioRecorder audioRecorder;
    private XunfeiScorer xunfeiScorer;
    private boolean isRecording = false;

    public static ShadowingDetailFragment newInstance(int audioId) {
        ShadowingDetailFragment fragment = new ShadowingDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_AUDIO_ID, audioId);
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
        return inflater.inflate(R.layout.fragment_shadowing_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initTts();
        xunfeiScorer.init(requireContext());

        int audioId = getArguments() != null ? getArguments().getInt(ARG_AUDIO_ID, -1) : -1;
        if (audioId == -1) {
            requireActivity().onBackPressed();
            return;
        }

        List<ShadowingContent> items = ShadowingLoader.load(requireContext());
        content = ShadowingLoader.findById(items, audioId);
        if (content == null) {
            requireActivity().onBackPressed();
            return;
        }

        sentences = content.getSentences();
        sentenceScores = new int[sentences.length];
        for (int i = 0; i < sentenceScores.length; i++) sentenceScores[i] = -1;

        bindContent();
        updateSentenceUI();
        setupListeners();
    }

    private void initViews(View view) {
        detailTitle = view.findViewById(R.id.detail_title);
        detailLevelChip = view.findViewById(R.id.detail_level_chip);
        detailTypeChip = view.findViewById(R.id.detail_type_chip);
        btnPlayAll = view.findViewById(R.id.btn_play_all);
        btnShowText = view.findViewById(R.id.btn_show_text);
        dialogTextCard = view.findViewById(R.id.dialog_text_card);
        detailDialogText = view.findViewById(R.id.detail_dialog_text);
        progressText = view.findViewById(R.id.progress_text);
        currentSentence = view.findViewById(R.id.current_sentence);
        speakerLabel = view.findViewById(R.id.speaker_label);
        btnPlaySentence = view.findViewById(R.id.btn_play_sentence);
        btnRecord = view.findViewById(R.id.btn_record);
        recordingIndicator = view.findViewById(R.id.recording_indicator);
        scoreSection = view.findViewById(R.id.score_section);
        scoreDisplay = view.findViewById(R.id.score_display);
        btnPrev = view.findViewById(R.id.btn_prev);
        btnNext = view.findViewById(R.id.btn_next);
        scoreCard = view.findViewById(R.id.score_card);
        overallScoreText = view.findViewById(R.id.overall_score_text);
    }

    private void initTts() {
        ttsEngine.init(requireContext(), new TTSEngine.TtsCallback() {
            @Override
            public void onDone() {
                if (getView() != null) {
                    if (!ttsReady) {
                        ttsReady = true;
                    }
                    if (isPlayingAll) {
                        playAllNextSentence();
                    }
                }
            }

            @Override
            public void onError(String message) {
                ttsReady = false;
            }

            @Override
            public void onLanguageWarning(String message) {}
        });
    }

    private void bindContent() {
        detailTitle.setText(content.getTitle());
        detailLevelChip.setText("Lv." + content.getLevel());
        detailDialogText.setText(content.getDialogText());

        if (content.isMonologue()) {
            detailTypeChip.setText("独白");
            detailTypeChip.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.chip_grammar_bg)));
            detailTypeChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_grammar_text));
            speakerLabel.setVisibility(View.GONE);
        } else {
            detailTypeChip.setText("对话");
            detailTypeChip.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.chip_scene_bg)));
            detailTypeChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_scene_text));
        }
    }

    // region Sentence UI

    private void updateSentenceUI() {
        if (sentences == null || currentIndex >= sentences.length) return;

        String rawSentence = sentences[currentIndex].trim();
        updateProgressText();

        if (content.isMonologue()) {
            currentSentence.setText(rawSentence);
        } else {
            // Dialogue: extract speaker prefix (e.g., "A: ")
            if (rawSentence.length() >= 2 && rawSentence.charAt(1) == ':') {
                speakerLabel.setText("角色 " + rawSentence.charAt(0));
                speakerLabel.setVisibility(View.VISIBLE);
                currentSentence.setText(rawSentence.substring(2).trim());
            } else {
                speakerLabel.setVisibility(View.GONE);
                currentSentence.setText(rawSentence);
            }
        }

        // Update score display if this sentence was already scored
        if (sentenceScores[currentIndex] >= 0) {
            showScore(sentenceScores[currentIndex]);
        } else {
            hideScore();
        }

        // Prev/Next visibility
        btnPrev.setVisibility(currentIndex > 0 ? View.VISIBLE : View.INVISIBLE);
        if (currentIndex == sentences.length - 1) {
            btnNext.setText("完成");
        } else {
            btnNext.setText("下一句");
        }
    }

    private void updateProgressText() {
        progressText.setText(String.format(Locale.getDefault(),
                "第 %d / %d 句", currentIndex + 1, sentences.length));
    }

    // endregion

    // region Play All

    private void togglePlayAll() {
        if (isPlayingAll) {
            stopPlayAll();
        } else {
            startPlayAll();
        }
    }

    private void startPlayAll() {
        if (!ttsReady || sentences == null || sentences.length == 0) return;
        if (sentences.length == 1 && sentences[0].trim().isEmpty()) return;
        ttsEngine.stop();
        isPlayingAll = true;
        btnPlayAll.setText("停止播放");
        btnPlayAll.setIconResource(android.R.drawable.ic_media_pause);
        ttsPlayIndex = 0;
        playAllNextSentence();
    }

    private int ttsPlayIndex = 0;

    private void playAllNextSentence() {
        if (!isPlayingAll) return;
        while (ttsPlayIndex < sentences.length) {
            String sentence = sentences[ttsPlayIndex].trim();
            ttsPlayIndex++;
            if (!sentence.isEmpty()) {
                ttsEngine.speak(stripSpeakerPrefix(sentence));
                return;
            }
        }
        stopPlayAll();
    }

    private void stopPlayAll() {
        ttsEngine.stop();
        isPlayingAll = false;
        btnPlayAll.setText("播放全文");
        btnPlayAll.setIconResource(android.R.drawable.ic_media_play);
    }

    private String stripSpeakerPrefix(String sentence) {
        if (sentence.length() >= 2 && sentence.charAt(1) == ':') {
            return sentence.substring(2).trim();
        }
        return sentence;
    }

    // endregion

    // region Sentence Playback

    private void playCurrentSentence() {
        if (!ttsReady || currentIndex >= sentences.length) return;
        String sentence = stripSpeakerPrefix(sentences[currentIndex].trim());
        if (!sentence.isEmpty()) {
            ttsEngine.speak(sentence);
        }
    }

    // endregion

    // region Recording

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        File cacheDir = requireContext().getCacheDir();
        File audioFile = new File(cacheDir, "shadowing_" + System.currentTimeMillis() + ".wav");
        audioRecorder.startRecording(audioFile.getAbsolutePath());
        isRecording = true;
        recordingIndicator.setVisibility(View.VISIBLE);
        btnRecord.setIconResource(android.R.drawable.ic_media_pause);
    }

    private void stopRecording() {
        isRecording = false;
        recordingIndicator.setVisibility(View.GONE);
        btnRecord.setIconResource(android.R.drawable.ic_btn_speak_now);

        String filePath = audioRecorder.stopRecording();
        if (filePath == null) return;

        String expected = stripSpeakerPrefix(sentences[currentIndex].trim());
        if (expected.isEmpty()) return;

        btnRecord.setEnabled(false);
        final int idx = currentIndex;

        xunfeiScorer.scoreAsync(expected, filePath, new XunfeiScorer.ScoreCallback() {
            @Override
            public void onResult(int score, String detail) {
                if (!isAdded() || getView() == null) return;
                requireActivity().runOnUiThread(() -> {
                    sentenceScores[idx] = score;
                    scoredCount++;
                    showScore(score);
                    btnRecord.setEnabled(true);
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || getView() == null) return;
                requireActivity().runOnUiThread(() -> {
                    btnRecord.setEnabled(true);
                });
            }
        });
    }

    // endregion

    // region Score

    private void showScore(int score) {
        scoreSection.setVisibility(View.VISIBLE);
        int color;
        String label;
        if (score >= 85) { color = ContextCompat.getColor(requireContext(), R.color.score_excellent); label = "优秀"; }
        else if (score >= 70) { color = ContextCompat.getColor(requireContext(), R.color.score_good); label = "良好"; }
        else if (score >= 50) { color = ContextCompat.getColor(requireContext(), R.color.score_fair); label = "一般"; }
        else { color = ContextCompat.getColor(requireContext(), R.color.score_poor); label = "需努力"; }
        scoreDisplay.setTextColor(color);
        final String suffix = " 分 · " + label;
        com.spokeneasy.app.core.AnimationUtils.animateScoreCount(
                scoreDisplay, score, "%d 分 · " + label,
                () -> { /* counting ends with pulse already */ });
    }

    private void hideScore() {
        scoreSection.setVisibility(View.GONE);
    }

    private void showOverallScore() {
        if (scoredCount == 0) return;
        int total = 0;
        for (int s : sentenceScores) {
            if (s >= 0) total += s;
        }
        int avg = total / scoredCount;
        scoreCard.setVisibility(View.VISIBLE);
        com.spokeneasy.app.core.AnimationUtils.animateScoreCount(
                overallScoreText, avg, "%d 分");
        scoreCard.setScaleX(0.8f);
        scoreCard.setScaleY(0.8f);
        scoreCard.setAlpha(0f);
        scoreCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    // endregion

    // region Listeners

    private void setupListeners() {
        btnPlayAll.setOnClickListener(v -> {
            com.spokeneasy.app.core.AnimationUtils.animateButtonAction(v);
            togglePlayAll();
        });

        btnShowText.setOnClickListener(v -> {
            boolean isVisible = dialogTextCard.getVisibility() == View.VISIBLE;
            dialogTextCard.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            btnShowText.setText(isVisible
                    ? R.string.show_dialog_text
                    : R.string.hide_dialog_text);
        });

        btnPlaySentence.setOnClickListener(v -> {
            com.spokeneasy.app.core.AnimationUtils.animateButtonAction(v);
            playCurrentSentence();
        });

        btnRecord.setOnClickListener(v -> toggleRecording());

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateSentenceUI();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < sentences.length - 1) {
                currentIndex++;
                updateSentenceUI();
            } else {
                // All done — show overall score
                showOverallScore();
            }
        });
    }

    // endregion

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsEngine != null) {
            ttsEngine.stop();
            ttsEngine.shutdown();
        }
        if (audioRecorder != null) {
            audioRecorder.release();
        }
        if (xunfeiScorer != null) {
            xunfeiScorer.destroy();
        }
    }
}
