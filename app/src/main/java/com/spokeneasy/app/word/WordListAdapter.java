package com.spokeneasy.app.word;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spokeneasy.app.R;

import java.util.List;
import java.util.Locale;

public class WordListAdapter extends ListAdapter<WordEntity, WordListAdapter.ViewHolder> {

    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(WordEntity word);
    }

    public WordListAdapter() {
        super(new DiffUtil.ItemCallback<WordEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull WordEntity oldItem, @NonNull WordEntity newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull WordEntity oldItem, @NonNull WordEntity newItem) {
                return oldItem.getWord().equals(newItem.getWord());
            }
        });
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WordEntity word = getItem(position);
        holder.wordText.setText(word.getWord());
        if (word.getPhonetic() != null && !word.getPhonetic().isEmpty()) {
            holder.phoneticText.setText(String.format(Locale.getDefault(), "/%s/", word.getPhonetic()));
            holder.phoneticText.setVisibility(View.VISIBLE);
        } else {
            holder.phoneticText.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(word);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView wordText;
        final TextView phoneticText;

        ViewHolder(View itemView) {
            super(itemView);
            wordText = itemView.findViewById(R.id.word_text);
            phoneticText = itemView.findViewById(R.id.phonetic_text);
        }
    }
}
