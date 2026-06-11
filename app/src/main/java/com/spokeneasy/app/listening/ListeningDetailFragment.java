package com.spokeneasy.app.listening;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.TTSEngine;
import com.spokeneasy.app.core.database.AppDatabase;

import java.util.List;
import java.util.Locale;

public class ListeningDetailFragment extends Fragment {

    private static final String ARG_AUDIO_ID = "audioId";

    private ListeningViewModel viewModel;
    private ListeningAudioEntity currentAudio;
    private List<ListeningQuestionEntity> questions;
    private boolean submitted = false;

    // UI
    private TextView detailTitle;
    private Chip detailLevelChip;
    private MaterialCardView dialogTextCard;
    private TextView detailDialogText;
    private MaterialButton btnPlayDialog;
    private MaterialButton btnShowText;
    private TextView progressText;
    private MaterialButton btnSubmit;
    private MaterialCardView scoreCard;
    private TextView scoreText;

    // TTS
    private TTSEngine ttsEngine;
    private boolean ttsReady = false;
    private boolean isPlaying = false;
    private String[] dialogSentences;
    private int currentSentenceIndex;

    private static final int[] CARD_IDS = {
            R.id.card_question_1, R.id.card_question_2, R.id.card_question_3
    };
    private static final int[] QUESTION_TEXT_IDS = {
            R.id.question_1_text, R.id.question_2_text, R.id.question_3_text
    };
    private static final int[] RADIO_GROUP_IDS = {
            R.id.radio_group_1, R.id.radio_group_2, R.id.radio_group_3
    };
    private static final int[] RESULT_TEXT_IDS = {
            R.id.result_1_text, R.id.result_2_text, R.id.result_3_text
    };
    private static final int[][] RADIO_IDS = {
            {R.id.radio_1_a, R.id.radio_1_b, R.id.radio_1_c},
            {R.id.radio_2_a, R.id.radio_2_b, R.id.radio_2_c},
            {R.id.radio_3_a, R.id.radio_3_b, R.id.radio_3_c}
    };

    public static ListeningDetailFragment newInstance(long audioId) {
        ListeningDetailFragment fragment = new ListeningDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_AUDIO_ID, audioId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ttsEngine = new TTSEngine();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_listening_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        detailTitle = view.findViewById(R.id.detail_title);
        detailLevelChip = view.findViewById(R.id.detail_level_chip);
        dialogTextCard = view.findViewById(R.id.dialog_text_card);
        detailDialogText = view.findViewById(R.id.detail_dialog_text);
        btnPlayDialog = view.findViewById(R.id.btn_play_dialog);
        btnShowText = view.findViewById(R.id.btn_show_text);
        progressText = view.findViewById(R.id.progress_text);
        btnSubmit = view.findViewById(R.id.btn_submit);
        scoreCard = view.findViewById(R.id.score_card);
        scoreText = view.findViewById(R.id.score_text);

        viewModel = new ViewModelProvider(requireActivity()).get(ListeningViewModel.class);

        long audioId = getArguments() != null ? getArguments().getLong(ARG_AUDIO_ID, -1) : -1;
        if (audioId == -1) {
            requireActivity().onBackPressed();
            return;
        }

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null && currentAudio == null) {
                for (ListeningAudioEntity item : items) {
                    if (item.getId() == audioId) {
                        currentAudio = item;
                        bindAudio(item);
                        break;
                    }
                }
            }
        });

        loadQuestions(audioId);
        setupListeners();
        initTts();
    }

    // region TTS

    private void initTts() {
        ttsEngine.init(requireContext(), new TTSEngine.TtsCallback() {
            @Override
            public void onDone() {
                if (ttsReady) {
                    if (isPlaying) {
                        playNextSentence();
                    }
                } else {
                    ttsReady = true;
                    if (getView() != null) {
                        btnPlayDialog.setEnabled(true);
                    }
                }
            }

            @Override
            public void onError(String message) {
                ttsReady = false;
            }

            @Override
            public void onLanguageWarning(String message) {
            }
        });
    }

    private void togglePlayback() {
        if (isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    private void startPlayback() {
        if (currentAudio == null || !ttsReady || dialogSentences == null) return;
        if (dialogSentences.length == 0
                || (dialogSentences.length == 1 && dialogSentences[0].trim().isEmpty())) {
            return;
        }
        ttsEngine.stop();
        currentSentenceIndex = 0;
        isPlaying = true;
        updatePlayButton();
        playNextSentence();
    }

    private void playNextSentence() {
        if (!isPlaying) return;
        while (currentSentenceIndex < dialogSentences.length) {
            String sentence = dialogSentences[currentSentenceIndex].trim();
            currentSentenceIndex++;
            if (!sentence.isEmpty()) {
                ttsEngine.speak(sentence);
                return;
            }
        }
        isPlaying = false;
        updatePlayButton();
    }

    private void stopPlayback() {
        ttsEngine.stop();
        isPlaying = false;
        updatePlayButton();
    }

    private void updatePlayButton() {
        if (isPlaying) {
            btnPlayDialog.setText("停止播放");
            btnPlayDialog.setIconResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayDialog.setText(R.string.play_dialog);
            btnPlayDialog.setIconResource(android.R.drawable.ic_media_play);
        }
    }

    // endregion

    // region Bind

    private void bindAudio(ListeningAudioEntity item) {
        detailTitle.setText(item.getTitle());
        detailLevelChip.setText("Lv." + item.getLevel());
        detailDialogText.setText(item.getDialogText());
        dialogSentences = item.getDialogText().split("\n");
        updateProgressText();
    }

    private void updateProgressText() {
        if (progressText != null) {
            int total = questions != null ? questions.size() : 0;
            progressText.setText(String.format(Locale.getDefault(), "共 %d 题", total));
        }
    }

    // endregion

    // region Questions

    private void loadQuestions(long audioId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            questions = viewModel.getQuestionsSync(audioId);
            if (getView() != null) {
                getView().post(() -> {
                    bindQuestions();
                    updateProgressText();
                });
            }
        });
    }

    private void bindQuestions() {
        if (questions == null || getView() == null) return;

        for (int i = 0; i < questions.size() && i < 3; i++) {
            ListeningQuestionEntity q = questions.get(i);

            TextView questionText = getView().findViewById(QUESTION_TEXT_IDS[i]);
            questionText.setText(String.format(Locale.getDefault(),
                    "Q%d. %s", i + 1, q.getQuestion()));

            String[] tags = {"A", "B", "C"};
            String[] options = {q.getOptionA(), q.getOptionB(), q.getOptionC()};

            for (int j = 0; j < 3; j++) {
                RadioButton rb = getView().findViewById(RADIO_IDS[i][j]);
                rb.setText(String.format("%s. %s", tags[j], options[j]));
                rb.setTag(tags[j]);
            }
        }
    }

    // endregion

    // region Listeners

    private void setupListeners() {
        btnPlayDialog.setOnClickListener(v -> {
            com.spokeneasy.app.core.AnimationUtils.animateButtonPress(v);
            togglePlayback();
        });

        btnShowText.setOnClickListener(v -> {
            com.spokeneasy.app.core.AnimationUtils.animateButtonPress(v);
            boolean isVisible = dialogTextCard.getVisibility() == View.VISIBLE;
            if (isVisible) {
                dialogTextCard.setVisibility(View.GONE);
                btnShowText.setText(R.string.show_dialog_text);
            } else {
                dialogTextCard.setVisibility(View.VISIBLE);
                btnShowText.setText(R.string.hide_dialog_text);
                dialogTextCard.setAlpha(0f);
                dialogTextCard.animate().alpha(1f).setDuration(300).start();
            }
        });

        btnSubmit.setOnClickListener(v -> {
            com.spokeneasy.app.core.AnimationUtils.animateButtonPress(v);
            if (submitted) return;
            if (questions == null || questions.size() < 3 || getView() == null) return;

            int correctCount = 0;

            for (int i = 0; i < 3; i++) {
                RadioGroup radioGroup = getView().findViewById(RADIO_GROUP_IDS[i]);
                TextView resultText = getView().findViewById(RESULT_TEXT_IDS[i]);
                MaterialCardView card = getView().findViewById(CARD_IDS[i]);

                int selectedId = radioGroup.getCheckedRadioButtonId();
                String correctAnswer = questions.get(i).getCorrectAnswer();
                String userAnswer = null;

                if (selectedId != -1) {
                    RadioButton selectedRb = getView().findViewById(selectedId);
                    userAnswer = (String) selectedRb.getTag();
                }

                resultText.setVisibility(View.VISIBLE);
                if (correctAnswer.equals(userAnswer)) {
                    resultText.setText("✓ 正确");
                    resultText.setTextColor(ContextCompat.getColor(requireContext(), R.color.quiz_correct));
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quiz_correct_bg));
                    card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.quiz_correct));
                    card.setStrokeWidth(2);
                    correctCount++;
                } else {
                    String correctOptionText = getCorrectOptionText(i, correctAnswer);
                    resultText.setText("✗ 正确答案: " + correctOptionText);
                    resultText.setTextColor(ContextCompat.getColor(requireContext(), R.color.quiz_wrong));
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quiz_wrong_bg));
                    card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.quiz_wrong));
                    card.setStrokeWidth(2);
                    highlightCorrectAnswer(i, correctAnswer);
                }

                for (int j = 0; j < radioGroup.getChildCount(); j++) {
                    radioGroup.getChildAt(j).setEnabled(false);
                }
            }

            scoreText.setText(String.format(Locale.getDefault(), "%d / 3", correctCount));
            showScoreWithAnimation();
            btnSubmit.setEnabled(false);
            submitted = true;
        });
    }

    // endregion

    // region Submit helpers

    private String getCorrectOptionText(int questionIndex, String correctAnswer) {
        if (questions == null || questionIndex >= questions.size()) return correctAnswer;
        ListeningQuestionEntity q = questions.get(questionIndex);
        switch (correctAnswer) {
            case "A": return "A. " + q.getOptionA();
            case "B": return "B. " + q.getOptionB();
            case "C": return "C. " + q.getOptionC();
            default: return correctAnswer;
        }
    }

    private void highlightCorrectAnswer(int questionIndex, String correctAnswer) {
        int rbId;
        switch (correctAnswer) {
            case "A": rbId = RADIO_IDS[questionIndex][0]; break;
            case "B": rbId = RADIO_IDS[questionIndex][1]; break;
            default: rbId = RADIO_IDS[questionIndex][2]; break;
        }
        RadioButton correctRb = getView().findViewById(rbId);
        correctRb.setTextColor(ContextCompat.getColor(requireContext(), R.color.quiz_correct));
        correctRb.setChecked(false);
    }

    private void showScoreWithAnimation() {
        scoreCard.setVisibility(View.VISIBLE);
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

        scoreText.setScaleX(0.5f);
        scoreText.setScaleY(0.5f);
        scoreText.postDelayed(() -> scoreText.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .withEndAction(() -> scoreText.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start())
                .start(), 350);
    }

    // endregion

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsEngine != null) {
            ttsEngine.stop();
            ttsEngine.shutdown();
        }
    }
}
