package com.spokeneasy.app.listening;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.spokeneasy.app.R;

public class ListeningListAdapter extends ListAdapter<ListeningAudioEntity, ListeningListAdapter.ViewHolder> {

    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(ListeningAudioEntity item);
    }

    public ListeningListAdapter() {
        super(new DiffUtil.ItemCallback<ListeningAudioEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull ListeningAudioEntity oldItem,
                                           @NonNull ListeningAudioEntity newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull ListeningAudioEntity oldItem,
                                              @NonNull ListeningAudioEntity newItem) {
                return oldItem.getTitle().equals(newItem.getTitle());
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
                .inflate(R.layout.item_listening_audio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ListeningAudioEntity item = getItem(position);
        holder.titleText.setText(item.getTitle());
        holder.dialogPreview.setText(item.getDialogText());
        holder.levelChip.setText("Lv." + item.getLevel());

        int levelColor;
        switch (item.getLevel()) {
            case 1: levelColor = Color.parseColor("#4CAF50"); break;
            case 2: levelColor = Color.parseColor("#FF9800"); break;
            case 3: levelColor = Color.parseColor("#F44336"); break;
            default: levelColor = Color.parseColor("#4CAF50");
        }
        holder.difficultyBar.setBackgroundColor(levelColor);

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView dialogPreview;
        final Chip levelChip;
        final View difficultyBar;
        final ImageView headsetIcon;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.listening_title_text);
            dialogPreview = itemView.findViewById(R.id.listening_dialog_preview);
            levelChip = itemView.findViewById(R.id.listening_level_chip);
            difficultyBar = itemView.findViewById(R.id.difficulty_bar);
            headsetIcon = itemView.findViewById(R.id.headset_icon);
        }
    }
}
