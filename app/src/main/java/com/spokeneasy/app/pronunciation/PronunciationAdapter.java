package com.spokeneasy.app.pronunciation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.AudioWaveformView;

public class PronunciationAdapter extends
        ListAdapter<PronunciationContent.MinimalPair, PronunciationAdapter.ViewHolder> {

    public interface Callback {
        void onPlayWord(String word, String sentence);
        void onRecord(String pairId);
        boolean isRecording();
    }

    private Callback callback;

    public PronunciationAdapter() {
        super(new DiffUtil.ItemCallback<PronunciationContent.MinimalPair>() {
            @Override
            public boolean areItemsTheSame(
                    @NonNull PronunciationContent.MinimalPair oldItem,
                    @NonNull PronunciationContent.MinimalPair newItem) {
                return oldItem.id.equals(newItem.id);
            }

            @Override
            public boolean areContentsTheSame(
                    @NonNull PronunciationContent.MinimalPair oldItem,
                    @NonNull PronunciationContent.MinimalPair newItem) {
                return oldItem.id.equals(newItem.id);
            }
        });
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pronunciation_pair, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PronunciationContent.MinimalPair pair = getItem(position);
        holder.bind(pair);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView wordAText, wordAPhonetic, wordBText, wordBPhonetic;
        TextView tipText, scoreText;
        MaterialButton btnPlayA, btnPlayB, btnRecord;
        AudioWaveformView waveformView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            wordAText = itemView.findViewById(R.id.word_a_text);
            wordAPhonetic = itemView.findViewById(R.id.word_a_phonetic);
            wordBText = itemView.findViewById(R.id.word_b_text);
            wordBPhonetic = itemView.findViewById(R.id.word_b_phonetic);
            btnPlayA = itemView.findViewById(R.id.btn_play_a);
            btnPlayB = itemView.findViewById(R.id.btn_play_b);
            btnRecord = itemView.findViewById(R.id.btn_record);
            waveformView = itemView.findViewById(R.id.waveform_view);
            scoreText = itemView.findViewById(R.id.score_text);
            tipText = itemView.findViewById(R.id.tip_text);
        }

        void bind(PronunciationContent.MinimalPair pair) {
            wordAText.setText(pair.wordA);
            wordAPhonetic.setText(pair.phonemeA);
            wordBText.setText(pair.wordB);
            wordBPhonetic.setText(pair.phonemeB);

            btnPlayA.setOnClickListener(v -> {
                if (callback != null) callback.onPlayWord(pair.wordA, pair.sentenceA);
            });
            btnPlayB.setOnClickListener(v -> {
                if (callback != null) callback.onPlayWord(pair.wordB, pair.sentenceB);
            });
            btnRecord.setOnClickListener(v -> {
                if (callback != null) callback.onRecord(pair.id);
            });

            if (tipText != null && pair.tipCn != null && !pair.tipCn.isEmpty()) {
                tipText.setText(pair.tipCn);
                tipText.setVisibility(View.VISIBLE);
            } else {
                tipText.setVisibility(View.GONE);
            }
        }
    }
}
