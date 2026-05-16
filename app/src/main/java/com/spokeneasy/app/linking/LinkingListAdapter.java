package com.spokeneasy.app.linking;

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
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView ruleText;
        final TextView categoryText;

        ViewHolder(View itemView) {
            super(itemView);
            ruleText = itemView.findViewById(R.id.linking_rule_text);
            categoryText = itemView.findViewById(R.id.linking_category_text);
        }
    }
}
