package com.spokeneasy.app.linking;

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

public class LinkingDetailFragment extends Fragment {

    private static final String ARG_LINKING_ID = "linkingId";

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

    private TTSEngine ttsEngine;
    private AudioRecorder audioRecorder;
    private Scorer scorer;
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
        scorer = new MockScorer();
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
    }

    private void bindItem(LinkingEntity item) {
        detailRuleName.setText(item.getRuleName());
        detailOriginal.setText(item.getOriginal());
        detailLinkingText.setText(item.getLinkingText());
        detailExampleEn.setText(item.getExampleEn());
        detailExampleCn.setText(item.getExampleCn());
        updateChineseVisibility();
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
                File cacheDir = requireContext().getCacheDir();
                File audioFile = new File(cacheDir, "recording_" + System.currentTimeMillis() + ".m4a");
                currentAudioPath = audioFile.getAbsolutePath();

                boolean started = audioRecorder.startRecording(currentAudioPath);
                if (started) {
                    btnRecord.setText(R.string.stop_record);
                    btnPlayback.setEnabled(false);
                }
            } else {
                audioRecorder.stopRecording();
                btnRecord.setText(R.string.start_record);
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
                                currentItem != null ? currentItem.getExampleEn() : "",
                                filePath);
                        scoreText.setVisibility(View.VISIBLE);
                        scoreText.setText(String.format(Locale.getDefault(), "%d 分", score));
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
