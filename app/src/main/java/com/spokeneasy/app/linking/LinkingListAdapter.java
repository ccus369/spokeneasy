package com.spokeneasy.app.linking;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spokeneasy.app.R;

public class LinkingListAdapter extends ListAdapter<LinkingEntity, LinkingListAdapter.ViewHolder> {

    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(LinkingEntity item);
    }

    public LinkingListAdapter() {
        super(new DiffUtil.ItemCallback<LinkingEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull LinkingEntity oldItem, @NonNull LinkingEntity newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull LinkingEntity oldItem, @NonNull LinkingEntity newItem) {
                return oldItem.getRuleName().equals(newItem.getRuleName());
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
                .inflate(R.layout.item_linking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LinkingEntity item = getItem(position);
        holder.ruleText.setText(item.getRuleName());
        holder.categoryText.setText(item.getCategory());

        String category = item.getCategory();
        int bgColor;
        if (category != null) {
            bgColor = getCategoryColor(category);
            // Show abbreviated tag
            String tag = category.length() <= 4 ? category : category.substring(0, 2);
            holder.categoryTag.setText(tag);
            holder.categoryTag.setVisibility(View.VISIBLE);
            GradientDrawable drawable = (GradientDrawable) holder.categoryTag.getBackground();
            drawable.setColor(bgColor);
            holder.categoryTag.setTextColor(Color.WHITE);
        } else {
            holder.categoryTag.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    private int getCategoryColor(String category) {
        switch (category) {
            case "缩约":
            case "缩写":
                return Color.parseColor("#1976D2");
            case "同化":
                return Color.parseColor("#7B1FA2");
            case "省略":
                return Color.parseColor("#F57C00");
            case "连读":
            case "连接":
                return Color.parseColor("#388E3C");
            default:
                return Color.parseColor("#757575");
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView ruleText;
        final TextView categoryText;
        final TextView categoryTag;

        ViewHolder(View itemView) {
            super(itemView);
            ruleText = itemView.findViewById(R.id.linking_rule_text);
            categoryText = itemView.findViewById(R.id.linking_category_text);
            categoryTag = itemView.findViewById(R.id.category_tag);
        }
    }
}
