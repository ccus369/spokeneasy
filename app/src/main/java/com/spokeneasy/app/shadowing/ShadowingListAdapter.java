package com.spokeneasy.app.shadowing;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.AnimationUtils;

public class ShadowingListAdapter extends ListAdapter<ShadowingContent, ShadowingListAdapter.ViewHolder> {

    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(ShadowingContent item);
    }

    public ShadowingListAdapter() {
        super(new DiffUtil.ItemCallback<ShadowingContent>() {
            @Override
            public boolean areItemsTheSame(@NonNull ShadowingContent oldItem,
                                           @NonNull ShadowingContent newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull ShadowingContent oldItem,
                                              @NonNull ShadowingContent newItem) {
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
                .inflate(R.layout.item_shadowing_audio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShadowingContent item = getItem(position);
        holder.titleText.setText(item.getTitle());

        String preview = item.getDialogText().replace("\n", " ").trim();
        if (preview.length() > 80) preview = preview.substring(0, 80) + "…";
        holder.dialogPreview.setText(preview);

        holder.levelChip.setText("Lv." + item.getLevel());

        int levelColor;
        switch (item.getLevel()) {
            case 1: levelColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.level_easy); break;
            case 2: levelColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.level_medium); break;
            case 3: levelColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.level_hard); break;
            default: levelColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.level_easy);
        }
        holder.difficultyBar.setBackgroundColor(levelColor);

        if (item.isMonologue()) {
            holder.typeChip.setText("独白");
            holder.typeChip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.chip_grammar_bg)));
            holder.typeChip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.chip_grammar_text));
        } else {
            holder.typeChip.setText("对话");
            holder.typeChip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.chip_scene_bg)));
            holder.typeChip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.chip_scene_text));
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });

        AnimationUtils.animateListItem(holder.itemView, position * 50, holder.itemView.getContext());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView dialogPreview;
        final Chip levelChip;
        final Chip typeChip;
        final View difficultyBar;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.shadowing_title_text);
            dialogPreview = itemView.findViewById(R.id.shadowing_dialog_preview);
            levelChip = itemView.findViewById(R.id.shadowing_level_chip);
            typeChip = itemView.findViewById(R.id.type_chip);
            difficultyBar = itemView.findViewById(R.id.difficulty_bar);
        }
    }
}
