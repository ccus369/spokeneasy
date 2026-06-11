package com.spokeneasy.app.progress;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.spokeneasy.app.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordHistoryFragment extends Fragment {

    private RecordHistoryViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.history_recycler);
        emptyText = view.findViewById(R.id.history_empty);

        adapter = new HistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(RecordHistoryViewModel.class);
        viewModel.getRecords().observe(getViewLifecycleOwner(), records -> {
            if (records == null || records.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setRecords(records);
            }
        });
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {

        private List<PracticeRecordEntity> records = new ArrayList<>();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

        void setRecords(List<PracticeRecordEntity> records) {
            this.records = records;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_record_history, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            PracticeRecordEntity record = records.get(position);
            holder.bind(record, dateFormat);
        }

        @Override
        public int getItemCount() {
            return records.size();
        }
    }

    private static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView moduleBadge;
        private final TextView scoreText;
        private final TextView referenceText;
        private final TextView dateText;
        private final MaterialButton btnPlay;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            moduleBadge = itemView.findViewById(R.id.history_module_badge);
            scoreText = itemView.findViewById(R.id.history_score);
            referenceText = itemView.findViewById(R.id.history_reference);
            dateText = itemView.findViewById(R.id.history_date);
            btnPlay = itemView.findViewById(R.id.btn_history_play);
        }

        void bind(PracticeRecordEntity record, SimpleDateFormat dateFormat) {
            // Module type
            String moduleLabel;
            int badgeColor;
            switch (record.getModuleType()) {
                case "word":
                    moduleLabel = "单词";
                    badgeColor = ContextCompat.getColor(moduleBadge.getContext(), R.color.record_badge_word);
                    break;
                case "linking":
                    moduleLabel = "连读";
                    badgeColor = ContextCompat.getColor(moduleBadge.getContext(), R.color.record_badge_linking);
                    break;
                case "listening":
                    moduleLabel = "听力";
                    badgeColor = ContextCompat.getColor(moduleBadge.getContext(), R.color.record_badge_listening);
                    break;
                default:
                    moduleLabel = record.getModuleType();
                    badgeColor = ContextCompat.getColor(moduleBadge.getContext(), R.color.record_badge_default);
            }
            moduleBadge.setText(moduleLabel);
            moduleBadge.setBackgroundColor(badgeColor);

            // Score
            scoreText.setText(String.format(Locale.getDefault(), "%d", record.getScore()));
            int scoreColorRes;
            if (record.getScore() >= 80) {
                scoreColorRes = R.color.record_score_high;
            } else if (record.getScore() >= 60) {
                scoreColorRes = R.color.record_score_medium;
            } else {
                scoreColorRes = R.color.record_score_low;
            }
            scoreText.setTextColor(ContextCompat.getColor(scoreText.getContext(), scoreColorRes));

            // Reference text (truncate to 1 line worth)
            String ref = record.getReferenceText();
            if (ref != null && ref.length() > 50) {
                ref = ref.substring(0, 50) + "…";
            }
            referenceText.setText(ref != null ? ref : "");

            // Date
            dateText.setText(dateFormat.format(new Date(record.getCreatedAt())));

            // Play button
            String audioPath = record.getAudioFilePath();
            boolean audioExists = audioPath != null && new File(audioPath).exists();
            btnPlay.setVisibility(audioExists ? View.VISIBLE : View.GONE);
            btnPlay.setOnClickListener(v -> {
                if (audioExists) {
                    android.media.MediaPlayer player = new android.media.MediaPlayer();
                    try {
                        player.setDataSource(audioPath);
                        player.setOnCompletionListener(mp -> mp.release());
                        player.setOnErrorListener((mp, what, extra) -> {
                            mp.release();
                            return true;
                        });
                        player.prepare();
                        player.start();
                    } catch (Exception e) {
                        player.release();
                    }
                }
            });
        }
    }
}
