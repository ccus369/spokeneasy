package com.spokeneasy.app.linking;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spokeneasy.app.R;
import com.spokeneasy.app.core.AnimationUtils;

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
            bgColor = getCategoryColor(category, holder.itemView.getContext());
            // Show abbreviated tag
            String tag = category.length() <= 4 ? category : category.substring(0, 2);
            holder.categoryTag.setText(tag);
            holder.categoryTag.setVisibility(View.VISIBLE);
            GradientDrawable drawable = (GradientDrawable) holder.categoryTag.getBackground();
            drawable.setColor(bgColor);
            holder.categoryTag.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
        } else {
            holder.categoryTag.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });

        AnimationUtils.animateListItem(holder.itemView, position * 50, holder.itemView.getContext());
    }

    private int getCategoryColor(String category, Context context) {
        switch (category) {
            case "缩约":
            case "缩写":
                return ContextCompat.getColor(context, R.color.tag_blue);
            case "同化":
                return ContextCompat.getColor(context, R.color.tag_purple);
            case "省略":
                return ContextCompat.getColor(context, R.color.tag_orange);
            case "连读":
            case "连接":
                return ContextCompat.getColor(context, R.color.tag_green);
            default:
                return ContextCompat.getColor(context, R.color.tag_grey);
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
