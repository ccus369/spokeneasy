package com.spokeneasy.app.word;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spokeneasy.app.R;
import com.spokeneasy.app.core.AnimationUtils;

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

        // Meaning preview: use first Chinese sentence translation if available
        if (word.getSentence1Cn() != null && !word.getSentence1Cn().isEmpty()) {
            holder.meaningPreview.setText(word.getSentence1Cn());
            holder.meaningPreview.setVisibility(View.VISIBLE);
        } else {
            holder.meaningPreview.setVisibility(View.GONE);
        }

        // Category accent bar coloring
        String category = word.getCategory();
        if (category != null && !category.isEmpty()) {
            int color = getCategoryColor(category);
            holder.categoryBar.setBackgroundColor(color);
            holder.categoryBar.setVisibility(View.VISIBLE);
        } else {
            holder.categoryBar.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(word);
            }
        });

        AnimationUtils.animateListItem(holder.itemView, position * 50, holder.itemView.getContext());
    }

    private int getCategoryColor(String category) {
        switch (category) {
            case "生活": return Color.parseColor("#1976D2");
            case "工作": return Color.parseColor("#7B1FA2");
            case "学习": return Color.parseColor("#F57C00");
            case "旅行": return Color.parseColor("#388E3C");
            default: return Color.parseColor("#757575");
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView wordText;
        final TextView phoneticText;
        final TextView meaningPreview;
        final View categoryBar;

        ViewHolder(View itemView) {
            super(itemView);
            wordText = itemView.findViewById(R.id.word_text);
            phoneticText = itemView.findViewById(R.id.phonetic_text);
            meaningPreview = itemView.findViewById(R.id.meaning_preview);
            categoryBar = itemView.findViewById(R.id.category_bar);
        }
    }
}
