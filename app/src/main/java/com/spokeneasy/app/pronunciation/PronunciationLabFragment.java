package com.spokeneasy.app.pronunciation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.AudioRecorder;
import com.spokeneasy.app.core.audio.TTSEngine;
import com.spokeneasy.app.core.database.AppDatabase;
import com.spokeneasy.app.core.scorer.XunfeiScorer;
import com.spokeneasy.app.core.util.UuidManager;
import com.spokeneasy.app.progress.PracticeRecordEntity;
import com.spokeneasy.app.progress.UserProgressEntity;

import java.io.File;
import java.util.List;

public class PronunciationLabFragment extends Fragment {

    private PronunciationLabViewModel viewModel;
    private PronunciationAdapter adapter;
    private ChipGroup categoryChipGroup;
    private RecyclerView pairList;

    private static final int REQUEST_RECORD_AUDIO = 100;

    private TTSEngine ttsEngine;
    private AudioRecorder audioRecorder;
    private XunfeiScorer xunfeiScorer;

    private String currentRecordingPairId;
    private String pendingPermissionPairId;

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
        return inflater.inflate(R.layout.fragment_pronunciation_lab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categoryChipGroup = view.findViewById(R.id.category_chip_group);
        pairList = view.findViewById(R.id.pair_list);

        pairList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PronunciationAdapter();
        adapter.setCallback(new PronunciationAdapter.Callback() {
            @Override
            public void onPlayWord(String word, String sentence) {
                ttsEngine.speak(word);
            }

            @Override
            public void onRecord(String pairId) {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    pendingPermissionPairId = pairId;
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_AUDIO);
                    return;
                }
                toggleRecording(pairId);
            }
        });
        pairList.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(PronunciationLabViewModel.class);

        ttsEngine.init(requireContext(), new TTSEngine.TtsCallback() {
            @Override public void onDone() {}
            @Override public void onError(String message) {}
            @Override public void onLanguageWarning(String message) {}
        });

        xunfeiScorer.init(requireContext());

        viewModel.getData().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            setupCategoryChips(data.categories);
            updatePairList(data);
        });

        viewModel.getSelectedCategory().observe(getViewLifecycleOwner(), cat -> {
            PronunciationContent.MinimalPairsData data = viewModel.getData().getValue();
            if (data != null) updatePairList(data);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String pairId = pendingPermissionPairId;
                pendingPermissionPairId = null;
                if (pairId != null) toggleRecording(pairId);
            } else {
                Snackbar.make(requireView(), "需要录音权限才能使用跟读功能",
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void setupCategoryChips(List<PronunciationContent.PhonemeCategory> categories) {
        categoryChipGroup.removeAllViews();

        // Add "全部" chip as default selection
        Chip allChip = new Chip(requireContext());
        allChip.setText("全部");
        allChip.setCheckable(true);
        allChip.setClickable(true);
        allChip.setChecked(true);
        allChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                viewModel.selectCategory(null);
            }
        });
        categoryChipGroup.addView(allChip);

        for (PronunciationContent.PhonemeCategory cat : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(cat.nameCn);
            chip.setTag(cat.key);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    viewModel.selectCategory(cat.key);
                }
            });
            categoryChipGroup.addView(chip);
        }
    }

    private void updatePairList(PronunciationContent.MinimalPairsData data) {
        String selected = viewModel.getSelectedCategory().getValue();
        List<PronunciationContent.MinimalPair> pairs;
        if (selected == null) {
            pairs = data.pairs;
        } else {
            pairs = data.getPairsByCategory(selected);
        }
        adapter.submitList(pairs);
    }

    private void toggleRecording(String pairId) {
        if (audioRecorder.isRecording()) {
            stopRecording(pairId);
        } else {
            startRecording(pairId);
        }
    }

    private void startRecording(String pairId) {
        currentRecordingPairId = pairId;
        File cacheDir = requireContext().getCacheDir();
        File audioFile = new File(cacheDir,
                "pron_" + System.currentTimeMillis() + ".wav");
        boolean started = audioRecorder.startRecording(audioFile.getAbsolutePath());
        if (started) {
            adapter.setRecordingPairId(pairId);
        }
    }

    private void stopRecording(String pairId) {
        String filePath = audioRecorder.stopRecording();
        if (filePath == null) {
            adapter.clearRecording();
            return;
        }

        adapter.clearRecording();

        // Find the pair to get reference text
        PronunciationContent.MinimalPairsData data = viewModel.getData().getValue();
        if (data == null) return;

        PronunciationContent.MinimalPair pair = null;
        for (PronunciationContent.MinimalPair p : data.pairs) {
            if (p.id.equals(pairId)) {
                pair = p;
                break;
            }
        }
        if (pair == null) return;

        final PronunciationContent.MinimalPair finalPair = pair;
        xunfeiScorer.scoreAsync(finalPair.wordA + " " + finalPair.wordB,
                filePath, new XunfeiScorer.ScoreCallback() {
            @Override
            public void onResult(int score, String detail) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    adapter.setScore(pairId, score, detail);
                    savePracticeRecord(pairId, score, detail, filePath);
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Snackbar.make(requireView(), "评分失败: " + message,
                            Snackbar.LENGTH_SHORT).show();
                });
            }
        });
    }

    private int findItemId(PronunciationContent.MinimalPairsData data, String pairId) {
        if (data == null) return 0;
        for (int i = 0; i < data.pairs.size(); i++) {
            if (data.pairs.get(i).id.equals(pairId)) {
                return i + 1;
            }
        }
        return 0;
    }

    private void savePracticeRecord(String pairId, int score, String detail, String audioPath) {
        String uuid = UuidManager.getDeviceUuid(requireContext());

        PronunciationContent.MinimalPairsData data = viewModel.getData().getValue();
        final int numericItemId = findItemId(data, pairId);

        PracticeRecordEntity record = new PracticeRecordEntity(
                uuid, "pronunciation", numericItemId, pairId, score,
                detail, audioPath, System.currentTimeMillis());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.practiceRecordDao().insert(record);
            // Also update user progress for statistics
            UserProgressEntity progress = new UserProgressEntity();
            progress.setUserUuid(uuid);
            progress.setModuleType("pronunciation");
            progress.setItemId(numericItemId);
            progress.setScore(score);
            progress.setIsCompleted(score >= 60 ? 1 : 0);
            progress.setCompletedAt(System.currentTimeMillis());
            db.userProgressDao().insert(progress);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsEngine != null) ttsEngine.shutdown();
        if (audioRecorder != null) audioRecorder.release();
        if (xunfeiScorer != null) xunfeiScorer.destroy();
    }
}
