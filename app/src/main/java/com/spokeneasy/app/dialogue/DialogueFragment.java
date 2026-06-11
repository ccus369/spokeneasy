package com.spokeneasy.app.dialogue;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.AudioRecorder;
import com.spokeneasy.app.core.audio.TTSEngine;
import com.spokeneasy.app.core.scorer.XunfeiScorer;

import java.io.File;
import java.util.HashMap;

public class DialogueFragment extends Fragment {

    private DialogueViewModel viewModel;

    // Audio
    private TTSEngine ttsEngine;
    private AudioRecorder audioRecorder;
    private XunfeiScorer xunfeiScorer;

    // Phase sections
    private LinearLayout selectSection, warmupSection, dialogueSection, roleplaySection, summarySection;

    // Select
    private RecyclerView scenarioList;
    private ScenarioAdapter scenarioAdapter;

    // Warmup
    private TextView warmupTitle;
    private LinearLayout warmupList;
    private MaterialButton btnStartDialogue;

    // Dialogue
    private TextView dialogueScenarioTitle, dialogueProgress;
    private Chip speakerBadge;
    private TextView dialogueText, dialogueTranslation;
    private MaterialButton btnDialogueListen, btnDialogueRecord;
    private TextView dialogueRecordingIndicator;
    private LinearLayout dialogueScoreSection;
    private TextView dialogueScoreDisplay, dialogueExpectedText;
    private MaterialButton btnDialoguePlayExpected;
    private MaterialButton btnDialoguePrev, btnDialogueNext;

    // Roleplay
    private TextView roleplayTitle, roleplayContext;
    private RecyclerView roleplayRecycler;
    private ProgressBar roleplayLoading;
    private EditText roleplayInput;
    private MaterialButton btnRoleplaySend, btnFinishRoleplay;
    private RoleplayAdapter roleplayAdapter;

    // Summary
    private TextView dialogueSummaryTitle, dialogueSummaryScore;
    private RecyclerView dialogueSummaryList;
    private TextView patternSentencesText;
    private View patternCard;
    private MaterialButton btnDialogueBack, btnDialogueRetry;

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
        return inflater.inflate(R.layout.fragment_dialogue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initTts();
        initScorer();

        viewModel = new ViewModelProvider(this).get(DialogueViewModel.class);

        // Scenario adapter
        scenarioAdapter = new ScenarioAdapter();
        scenarioAdapter.setCallback(scenario -> viewModel.selectScenario(scenario));
        scenarioList.setLayoutManager(new LinearLayoutManager(requireContext()));
        scenarioList.setAdapter(scenarioAdapter);

        // Roleplay adapter
        roleplayAdapter = new RoleplayAdapter(this::speakText);
        roleplayRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        roleplayRecycler.setAdapter(roleplayAdapter);

        // Summary adapter
        DialogueSummaryAdapter summaryAdapter = new DialogueSummaryAdapter(null, null);
        dialogueSummaryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        dialogueSummaryList.setAdapter(summaryAdapter);

        // Observe ViewModel
        viewModel.getScenarios().observe(getViewLifecycleOwner(), list -> {
            if (list != null) scenarioAdapter.submitList(list);
        });

        viewModel.getPhase().observe(getViewLifecycleOwner(), phase -> {
            if (phase == null) return;
            selectSection.setVisibility(phase == DialogueViewModel.Phase.SCENE_SELECT ? View.VISIBLE : View.GONE);
            warmupSection.setVisibility(phase == DialogueViewModel.Phase.WARMUP ? View.VISIBLE : View.GONE);
            dialogueSection.setVisibility(phase == DialogueViewModel.Phase.DIALOGUE ? View.VISIBLE : View.GONE);
            roleplaySection.setVisibility(phase == DialogueViewModel.Phase.ROLEPLAY ? View.VISIBLE : View.GONE);
            summarySection.setVisibility(phase == DialogueViewModel.Phase.SUMMARY ? View.VISIBLE : View.GONE);

            if (phase == DialogueViewModel.Phase.WARMUP) refreshWarmupUi();
            else if (phase == DialogueViewModel.Phase.DIALOGUE) refreshDialogueUi();
            else if (phase == DialogueViewModel.Phase.SUMMARY) refreshSummaryUi(summaryAdapter);
        });

        viewModel.getCurrentLineIndex().observe(getViewLifecycleOwner(), index -> {
            if (viewModel.getPhase().getValue() == DialogueViewModel.Phase.DIALOGUE) {
                refreshDialogueUi();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean l = loading == Boolean.TRUE;
            roleplayLoading.setVisibility(l ? View.VISIBLE : View.GONE);
            btnRoleplaySend.setEnabled(!l);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(view, error, Snackbar.LENGTH_LONG).show();
                viewModel.dismissError();
            }
        });

        viewModel.getRoleplayMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                roleplayAdapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    roleplayRecycler.smoothScrollToPosition(messages.size() - 1);
                }
            }
        });

        // Dialogue buttons
        btnDialogueListen.setOnClickListener(v -> playCurrentLine());
        btnDialogueRecord.setOnClickListener(v -> toggleRecording());
        btnDialoguePlayExpected.setOnClickListener(v -> playCurrentLine());
        btnDialoguePrev.setOnClickListener(v -> viewModel.prevLine());
        btnDialogueNext.setOnClickListener(v -> {
            if (viewModel.hasNextLine()) {
                viewModel.nextLine();
            } else {
                viewModel.startRoleplay();
            }
        });

        // Warmup
        btnStartDialogue.setOnClickListener(v -> viewModel.startDialogue());

        // Roleplay
        btnRoleplaySend.setOnClickListener(v -> sendRoleplayMessage());
        btnFinishRoleplay.setOnClickListener(v -> viewModel.showSummary());

        roleplayInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && keyCode == KeyEvent.KEYCODE_ENTER
                    && !event.isShiftPressed()) {
                sendRoleplayMessage();
                return true;
            }
            return false;
        });

        // Summary
        btnDialogueBack.setOnClickListener(v -> viewModel.backToSceneSelect());
        btnDialogueRetry.setOnClickListener(v -> {
            ScenarioContent.Scenario sc = viewModel.getCurrentScenario().getValue();
            viewModel.backToSceneSelect();
            if (sc != null) viewModel.selectScenario(sc);
        });

        // Send button state
        roleplayInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                btnRoleplaySend.setEnabled(s != null && s.toString().trim().length() > 0);
            }
        });
    }

    private void initViews(View view) {
        selectSection = view.findViewById(R.id.select_section);
        warmupSection = view.findViewById(R.id.warmup_section);
        dialogueSection = view.findViewById(R.id.dialogue_section);
        roleplaySection = view.findViewById(R.id.roleplay_section);
        summarySection = view.findViewById(R.id.summary_section);

        scenarioList = view.findViewById(R.id.scenario_list);

        warmupTitle = view.findViewById(R.id.warmup_title);
        warmupList = view.findViewById(R.id.warmup_list);
        btnStartDialogue = view.findViewById(R.id.btn_start_dialogue);

        dialogueScenarioTitle = view.findViewById(R.id.dialogue_scenario_title);
        dialogueProgress = view.findViewById(R.id.dialogue_progress);
        speakerBadge = view.findViewById(R.id.speaker_badge);
        dialogueText = view.findViewById(R.id.dialogue_text);
        dialogueTranslation = view.findViewById(R.id.dialogue_translation);
        btnDialogueListen = view.findViewById(R.id.btn_dialogue_listen);
        btnDialogueRecord = view.findViewById(R.id.btn_dialogue_record);
        dialogueRecordingIndicator = view.findViewById(R.id.dialogue_recording_indicator);
        dialogueScoreSection = view.findViewById(R.id.dialogue_score_section);
        dialogueScoreDisplay = view.findViewById(R.id.dialogue_score_display);
        dialogueExpectedText = view.findViewById(R.id.dialogue_expected_text);
        btnDialoguePlayExpected = view.findViewById(R.id.btn_dialogue_play_expected);
        btnDialoguePrev = view.findViewById(R.id.btn_dialogue_prev);
        btnDialogueNext = view.findViewById(R.id.btn_dialogue_next);

        roleplayTitle = view.findViewById(R.id.roleplay_title);
        roleplayContext = view.findViewById(R.id.roleplay_context);
        roleplayRecycler = view.findViewById(R.id.roleplay_recycler);
        roleplayLoading = view.findViewById(R.id.roleplay_loading);
        roleplayInput = view.findViewById(R.id.roleplay_input);
        btnRoleplaySend = view.findViewById(R.id.btn_roleplay_send);
        btnFinishRoleplay = view.findViewById(R.id.btn_finish_roleplay);

        dialogueSummaryTitle = view.findViewById(R.id.dialogue_summary_title);
        dialogueSummaryScore = view.findViewById(R.id.dialogue_summary_score);
        dialogueSummaryList = view.findViewById(R.id.dialogue_summary_list);
        patternSentencesText = view.findViewById(R.id.pattern_sentences_text);
        patternCard = view.findViewById(R.id.pattern_card);
        btnDialogueBack = view.findViewById(R.id.btn_dialogue_back);
        btnDialogueRetry = view.findViewById(R.id.btn_dialogue_retry);
    }

    private void initTts() {
        ttsEngine.init(requireContext(), new TTSEngine.TtsCallback() {
            @Override public void onDone() {}
            @Override public void onError(String message) {}
            @Override public void onLanguageWarning(String message) {}
        });
    }

    private void initScorer() {
        xunfeiScorer.init(requireContext());
    }

    // ===== WARMUP =====

    private void refreshWarmupUi() {
        ScenarioContent.Scenario sc = viewModel.getCurrentScenario().getValue();
        if (sc == null) return;

        warmupTitle.setText(sc.title + " · 预热词汇");
        warmupList.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (ScenarioContent.WarmupWord word : sc.warmupWords) {
            View row = inflater.inflate(R.layout.item_summary_row, warmupList, false);
            TextView indexText = row.findViewById(R.id.summary_index);
            TextView expectedText = row.findViewById(R.id.summary_expected);
            TextView scoreText = row.findViewById(R.id.summary_score);

            indexText.setText(word.word);
            indexText.setTextSize(16);
            indexText.setTextColor(ContextCompat.getColor(requireContext(), R.color.dialogue_index_text));

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) indexText.getLayoutParams();
            lp.width = 0;
            lp.weight = 1;
            indexText.setLayoutParams(lp);

            expectedText.setText(word.phonetic);
            expectedText.setTextSize(13);

            scoreText.setText("▶");
            scoreText.setTextSize(18);
            scoreText.setTextColor(ContextCompat.getColor(requireContext(), R.color.dialogue_play_button));
            scoreText.setOnClickListener(v -> ttsEngine.speak(word.word));

            row.setClickable(false);
            row.setBackground(null);
            warmupList.addView(row);
        }
    }

    // ===== DIALOGUE =====

    private void refreshDialogueUi() {
        ScenarioContent.Scenario sc = viewModel.getCurrentScenario().getValue();
        Integer idx = viewModel.getCurrentLineIndex().getValue();
        if (sc == null || idx == null || idx >= sc.dialogueLines.size()) return;

        ScenarioContent.DialogueLine line = sc.dialogueLines.get(idx);
        int total = viewModel.getTotalLines();

        dialogueScenarioTitle.setText(sc.title);
        dialogueProgress.setText((idx + 1) + " / " + total);

        speakerBadge.setText(line.speaker.equals("A") ? "角色 A" : "角色 B");
        dialogueText.setText(line.text);
        dialogueTranslation.setText(line.translation);

        if (viewModel.isLineScored(idx)) {
            int score = viewModel.getLineScore(idx);
            showDialogueScore(score, line.text);
        } else {
            hideDialogueScore();
        }

        dialogueRecordingIndicator.setVisibility(View.GONE);
        btnDialogueRecord.setText(audioRecorder.isRecording() ? "停止" : "录音");

        btnDialoguePrev.setEnabled(viewModel.hasPrevLine());
        btnDialoguePrev.setVisibility(viewModel.hasPrevLine() ? View.VISIBLE : View.INVISIBLE);

        if (!viewModel.hasNextLine()) {
            btnDialogueNext.setText("进入角色扮演");
        } else {
            btnDialogueNext.setText(viewModel.isLineScored(idx) ? "下一句" : "跳过");
        }

        roleplayTitle.setText(sc.title + " · AI 角色扮演");
        roleplayContext.setText("场景: " + sc.description);
    }

    private void toggleRecording() {
        Integer idx = viewModel.getCurrentLineIndex().getValue();
        if (idx == null) return;

        if (audioRecorder.isRecording()) {
            stopDialogueRecording(idx);
        } else {
            startDialogueRecording(idx);
        }
    }

    private void startDialogueRecording(int lineIndex) {
        File cacheDir = requireContext().getCacheDir();
        File audioFile = new File(cacheDir, "dialogue_" + System.currentTimeMillis() + ".wav");
        audioRecorder.startRecording(audioFile.getAbsolutePath());
        dialogueRecordingIndicator.setVisibility(View.VISIBLE);
        btnDialogueRecord.setText("停止");
    }

    private void stopDialogueRecording(int lineIndex) {
        String filePath = audioRecorder.stopRecording();
        dialogueRecordingIndicator.setVisibility(View.GONE);
        btnDialogueRecord.setText("录音");

        if (filePath == null) return;

        ScenarioContent.Scenario sc = viewModel.getCurrentScenario().getValue();
        if (sc == null || lineIndex >= sc.dialogueLines.size()) return;

        String expected = sc.dialogueLines.get(lineIndex).text;
        btnDialogueRecord.setEnabled(false);
        btnDialogueRecord.setText("评分中…");

        String finalFilePath = filePath;
        int finalLineIndex = lineIndex;
        xunfeiScorer.scoreAsync(expected, filePath, new XunfeiScorer.ScoreCallback() {
            @Override
            public void onResult(int score, String detail) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    viewModel.addLineScore(finalLineIndex, score, finalFilePath);
                    showDialogueScore(score, expected);
                    btnDialogueRecord.setEnabled(true);
                    btnDialogueRecord.setText("录音");
                    refreshDialogueUi();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnDialogueRecord.setEnabled(true);
                    btnDialogueRecord.setText("录音");
                });
            }
        });
    }

    private void showDialogueScore(int score, String expected) {
        dialogueScoreSection.setVisibility(View.VISIBLE);
        int color;
        String label;
        if (score >= 85) { color = ContextCompat.getColor(requireContext(), R.color.score_excellent); label = "优秀"; }
        else if (score >= 70) { color = ContextCompat.getColor(requireContext(), R.color.score_good); label = "良好"; }
        else if (score >= 50) { color = ContextCompat.getColor(requireContext(), R.color.score_fair); label = "一般"; }
        else { color = ContextCompat.getColor(requireContext(), R.color.score_poor); label = "需努力"; }
        dialogueScoreDisplay.setTextColor(color);
        dialogueScoreDisplay.setText(score + " 分 · " + label);
        dialogueExpectedText.setText(expected);
    }

    private void hideDialogueScore() {
        dialogueScoreSection.setVisibility(View.GONE);
    }

    private void playCurrentLine() {
        Integer idx = viewModel.getCurrentLineIndex().getValue();
        ScenarioContent.Scenario sc = viewModel.getCurrentScenario().getValue();
        if (sc != null && idx != null && idx < sc.dialogueLines.size()) {
            ttsEngine.speak(sc.dialogueLines.get(idx).text);
        }
    }

    // ===== ROLEPLAY =====

    private void sendRoleplayMessage() {
        String text = roleplayInput.getText().toString().trim();
        if (text.isEmpty()) return;

        viewModel.sendRoleplayMessage(text);
        roleplayInput.setText("");
    }

    // ===== SUMMARY =====

    private void refreshSummaryUi(DialogueSummaryAdapter adapter) {
        ScenarioContent.Scenario sc = viewModel.getCurrentScenario().getValue();
        if (sc == null) return;

        dialogueSummaryTitle.setText(sc.title);

        int avg = viewModel.getAverageScore();
        dialogueSummaryScore.setText(avg + " 分");

        int color;
        if (avg >= 85) color = ContextCompat.getColor(requireContext(), R.color.score_excellent);
        else if (avg >= 70) color = ContextCompat.getColor(requireContext(), R.color.score_good);
        else if (avg >= 50) color = ContextCompat.getColor(requireContext(), R.color.score_fair);
        else color = ContextCompat.getColor(requireContext(), R.color.score_poor);
        dialogueSummaryScore.setTextColor(color);

        // Build scores map for adapter
        HashMap<Integer, Integer> scores = new HashMap<>();
        for (int i = 0; i < sc.dialogueLines.size(); i++) {
            if (viewModel.isLineScored(i)) {
                scores.put(i, viewModel.getLineScore(i));
            }
        }
        adapter.updateData(sc.dialogueLines, scores);

        // Pattern sentences
        if (sc.patternSentences != null && !sc.patternSentences.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String ps : sc.patternSentences) {
                sb.append("• ").append(ps).append("\n");
            }
            patternSentencesText.setText(sb.toString().trim());
            patternCard.setVisibility(View.VISIBLE);
        } else {
            patternCard.setVisibility(View.GONE);
        }
    }

    // ===== TTS =====

    private void speakText(String text) {
        if (ttsEngine != null) {
            ttsEngine.speak(text);
        }
    }

    // ===== Lifecycle =====

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsEngine != null) ttsEngine.shutdown();
        if (audioRecorder != null) audioRecorder.release();
        if (xunfeiScorer != null) xunfeiScorer.destroy();
    }
}
