package com.spokeneasy.app.drill;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.AudioRecorder;
import com.spokeneasy.app.core.audio.TTSEngine;
import com.spokeneasy.app.core.scorer.XunfeiScorer;

import java.io.File;
import java.util.List;

public class PatternDrillFragment extends Fragment {

    // ViewModel
    private DrillViewModel viewModel;

    // Audio components
    private TTSEngine ttsEngine;
    private AudioRecorder audioRecorder;
    private XunfeiScorer xunfeiScorer;

    // Phase sections
    private LinearLayout selectSection;
    private ScrollView drillSection;
    private LinearLayout summarySection;

    // Selecting views
    private RecyclerView grammarList;
    private GrammarListAdapter grammarAdapter;

    // Drilling views
    private TextView drillTitle, drillProgress;
    private com.google.android.material.chip.Chip drillTypeBadge;
    private TextView baseSentence;
    private TextView cueLabel, cueText, hintText;
    private MaterialButton btnListen, btnRecord;
    private TextView recordingIndicator;

    private LinearLayout scoreSection;
    private TextView scoreDisplay, expectedText;
    private MaterialButton btnPlayExpected, btnPlayRecording;

    private MaterialButton btnPrev, btnNext;

    // Summary views
    private TextView summaryScore;
    private RecyclerView summaryList;
    private SummaryAdapter summaryAdapter;
    private MaterialButton btnBackSelection, btnRetry;

    // Recording state
    private String currentRecordingStepId;

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
        return inflater.inflate(R.layout.fragment_pattern_drill, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initTts();
        initScorer();

        viewModel = new ViewModelProvider(this).get(DrillViewModel.class);

        grammarAdapter = new GrammarListAdapter();
        grammarAdapter.setCallback(gc -> viewModel.selectGrammar(gc));
        grammarList.setLayoutManager(new LinearLayoutManager(requireContext()));
        grammarList.setAdapter(grammarAdapter);

        summaryAdapter = new SummaryAdapter(null, null);
        summaryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        summaryList.setAdapter(summaryAdapter);

        // Observe ViewModel
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && !loading) {
                grammarAdapter.submitList(viewModel.getGrammarPoints().getValue());
            }
        });

        viewModel.getGrammarPoints().observe(getViewLifecycleOwner(), list -> {
            if (list != null) grammarAdapter.submitList(list);
        });

        viewModel.getPhase().observe(getViewLifecycleOwner(), phase -> {
            if (phase == null) return;
            selectSection.setVisibility(phase == DrillViewModel.Phase.SELECTING ? View.VISIBLE : View.GONE);
            drillSection.setVisibility(phase == DrillViewModel.Phase.DRILLING ? View.VISIBLE : View.GONE);
            summarySection.setVisibility(phase == DrillViewModel.Phase.SUMMARY ? View.VISIBLE : View.GONE);

            if (phase == DrillViewModel.Phase.DRILLING) {
                refreshStepUi();
            } else if (phase == DrillViewModel.Phase.SUMMARY) {
                refreshSummaryUi();
            }
        });

        viewModel.getCurrentStepIndex().observe(getViewLifecycleOwner(), index -> {
            if (viewModel.getPhase().getValue() == DrillViewModel.Phase.DRILLING) {
                refreshStepUi();
            }
        });

        // Action buttons
        btnListen.setOnClickListener(v -> playCurrentStep());
        btnRecord.setOnClickListener(v -> toggleRecording());
        btnPlayExpected.setOnClickListener(v -> playExpectedAnswer());
        btnPlayRecording.setOnClickListener(v -> playUserRecording());
        btnPrev.setOnClickListener(v -> viewModel.prevStep());
        btnNext.setOnClickListener(v -> viewModel.nextStep());
        btnBackSelection.setOnClickListener(v -> viewModel.backToSelection());
        btnRetry.setOnClickListener(v -> viewModel.retryGrammar());
    }

    private void initViews(View view) {
        selectSection = view.findViewById(R.id.select_section);
        drillSection = view.findViewById(R.id.drill_section);
        summarySection = view.findViewById(R.id.summary_section);

        grammarList = view.findViewById(R.id.grammar_list);

        drillTitle = view.findViewById(R.id.drill_title);
        drillProgress = view.findViewById(R.id.drill_progress);
        drillTypeBadge = view.findViewById(R.id.drill_type_badge);
        baseSentence = view.findViewById(R.id.base_sentence);
        cueLabel = view.findViewById(R.id.cue_label);
        cueText = view.findViewById(R.id.cue_text);
        hintText = view.findViewById(R.id.hint_text);
        btnListen = view.findViewById(R.id.btn_listen);
        btnRecord = view.findViewById(R.id.btn_record);
        recordingIndicator = view.findViewById(R.id.recording_indicator);

        scoreSection = view.findViewById(R.id.score_section);
        scoreDisplay = view.findViewById(R.id.score_display);
        expectedText = view.findViewById(R.id.expected_text);
        btnPlayExpected = view.findViewById(R.id.btn_play_expected);
        btnPlayRecording = view.findViewById(R.id.btn_play_recording);

        btnPrev = view.findViewById(R.id.btn_prev);
        btnNext = view.findViewById(R.id.btn_next);

        summaryScore = view.findViewById(R.id.summary_score);
        summaryList = view.findViewById(R.id.summary_list);
        btnBackSelection = view.findViewById(R.id.btn_back_selection);
        btnRetry = view.findViewById(R.id.btn_retry);
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

    // ===== Phase: DRILLING =====

    private void refreshStepUi() {
        DrillContent.DrillStep step = viewModel.getCurrentStep();
        if (step == null) return;

        Integer index = viewModel.getCurrentStepIndex().getValue();
        int total = viewModel.getTotalSteps();

        drillTitle.setText(viewModel.getCurrentGrammar().getValue() != null
                ? viewModel.getCurrentGrammar().getValue().grammarPoint : "");
        drillProgress.setText((index != null ? index + 1 : 1) + " / " + total);

        drillTypeBadge.setText(viewModel.getCurrentDrillTypeLabel());

        // Base sentence with highlighted brackets
        baseSentence.setText(highlightBrackets(step.base));

        cueLabel.setText("提示");
        cueText.setText(step.cue);

        if (step.hintCn != null && !step.hintCn.isEmpty()) {
            hintText.setText("💡 " + step.hintCn);
            hintText.setVisibility(View.VISIBLE);
        } else {
            hintText.setVisibility(View.GONE);
        }

        // Check if this step was already scored
        if (viewModel.isStepScored(step.id)) {
            Integer score = viewModel.getStepScore(step.id);
            showScore(score != null ? score : 0, step.expected);
        } else {
            hideScore();
        }

        // Show play-recording button if recording exists
        boolean hasRecording = getRecordingPath(step.id) != null;
        btnPlayRecording.setVisibility(hasRecording ? View.VISIBLE : View.GONE);

        // Recording state
        recordingIndicator.setVisibility(View.GONE);
        btnRecord.setText(audioRecorder.isRecording() ? "停止" : "录音");

        // Nav buttons
        btnPrev.setEnabled(viewModel.hasPrevStep());
        btnPrev.setVisibility(viewModel.hasPrevStep() ? View.VISIBLE : View.INVISIBLE);

        boolean scored = viewModel.isStepScored(step.id);
        btnNext.setText(scored && viewModel.hasNextStep() ? "下一题"
                : !scored ? "跳过"
                : "查看总结");
    }

    /** Parse [highlighted] text in base sentence. */
    private SpannableString highlightBrackets(String text) {
        SpannableString ss = new SpannableString(text);
        int start = text.indexOf('[');
        int end = text.indexOf(']');

        if (start >= 0 && end > start) {
            // Remove brackets from display
            String clean = text.substring(0, start)
                    + text.substring(start + 1, end)
                    + text.substring(end + 1);
            ss = new SpannableString(clean);

            // Highlight the previously-bracketed text
            int cleanStart = start;
            int cleanEnd = cleanStart + (end - start - 1);
            int color = 0xFFFF6F00; // amber
            ss.setSpan(new ForegroundColorSpan(color), cleanStart, cleanEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ss;
    }

    private void toggleRecording() {
        String stepId = viewModel.getCurrentStep().id;
        if (audioRecorder.isRecording()) {
            stopRecording(stepId);
        } else {
            startRecording(stepId);
        }
    }

    private void startRecording(String stepId) {
        currentRecordingStepId = stepId;
        File cacheDir = requireContext().getCacheDir();
        File audioFile = new File(cacheDir, "drill_" + System.currentTimeMillis() + ".wav");
        audioRecorder.startRecording(audioFile.getAbsolutePath());
        recordingIndicator.setVisibility(View.VISIBLE);
        btnRecord.setText("停止");
    }

    private void stopRecording(String stepId) {
        String filePath = audioRecorder.stopRecording();
        recordingIndicator.setVisibility(View.GONE);
        btnRecord.setText("录音");

        if (filePath == null) return;

        DrillContent.DrillStep step = viewModel.getCurrentStep();
        if (step == null) return;

        btnRecord.setEnabled(false);
        btnRecord.setText("评分中…");

        xunfeiScorer.scoreAsync(step.expected, filePath, new XunfeiScorer.ScoreCallback() {
            @Override
            public void onResult(int score, String detail) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    viewModel.addStepResult(stepId, score, detail, filePath);
                    showScore(score, step.expected);
                    btnRecord.setEnabled(true);
                    btnRecord.setText("录音");
                    refreshStepUi(); // update next button
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnRecord.setEnabled(true);
                    btnRecord.setText("录音");
                });
            }
        });
    }

    private void playCurrentStep() {
        DrillContent.DrillStep step = viewModel.getCurrentStep();
        if (step != null) {
            // Play the expected answer as a model
            ttsEngine.speak(step.expected);
        }
    }

    private void playExpectedAnswer() {
        DrillContent.DrillStep step = viewModel.getCurrentStep();
        if (step != null) {
            ttsEngine.speak(step.expected);
        }
    }

    private void playUserRecording() {
        DrillContent.DrillStep step = viewModel.getCurrentStep();
        if (step == null) return;
        String audioPath = getRecordingPath(step.id);
        if (audioPath != null) {
            audioRecorder.playBack(audioPath, new AudioRecorder.AudioCallback() {
                @Override
                public void onStart() {
                    btnPlayRecording.setEnabled(false);
                }

                @Override
                public void onStop(String filePath) {
                    btnPlayRecording.setEnabled(true);
                }

                @Override
                public void onError(String message) {
                    btnPlayRecording.setEnabled(true);
                }
            });
        }
    }

    private String getRecordingPath(String stepId) {
        for (DrillViewModel.StepResult r : viewModel.getStepResults()) {
            if (r.stepId.equals(stepId) && r.audioPath != null) {
                return r.audioPath;
            }
        }
        return null;
    }

    private void showScore(int score, String expected) {
        scoreSection.setVisibility(View.VISIBLE);
        scoreDisplay.setText(score + " 分");

        // Color
        int color;
        String label;
        if (score >= 85) {
            color = 0xFF4CAF50;
            label = "优秀";
        } else if (score >= 70) {
            color = 0xFF2196F3;
            label = "良好";
        } else if (score >= 50) {
            color = 0xFFFF9800;
            label = "一般";
        } else {
            color = 0xFFF44336;
            label = "需努力";
        }
        scoreDisplay.setTextColor(color);
        scoreDisplay.setText(score + " 分 · " + label);

        expectedText.setText(expected);
    }

    private void hideScore() {
        scoreSection.setVisibility(View.GONE);
    }

    // ===== Phase: SUMMARY =====

    private void refreshSummaryUi() {
        int avg = viewModel.getAverageScore();
        summaryScore.setText(avg + " 分");

        int color;
        if (avg >= 85) color = 0xFF4CAF50;
        else if (avg >= 70) color = 0xFF2196F3;
        else if (avg >= 50) color = 0xFFFF9800;
        else color = 0xFFF44336;
        summaryScore.setTextColor(color);

        List<DrillViewModel.StepResult> results = viewModel.getStepResults();
        List<DrillContent.DrillStep> steps = DrillContent.flattenSteps(
                viewModel.getCurrentGrammar().getValue());

        summaryAdapter.updateData(results, steps);
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
