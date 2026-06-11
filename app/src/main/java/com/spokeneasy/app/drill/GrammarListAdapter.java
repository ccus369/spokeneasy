package com.spokeneasy.app.drill;

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

public class GrammarListAdapter extends
        ListAdapter<DrillContent.DrillCollection, GrammarListAdapter.ViewHolder> {

    public interface Callback {
        void onSelect(DrillContent.DrillCollection grammar);
    }

    private Callback callback;

    public GrammarListAdapter() {
        super(new DiffUtil.ItemCallback<DrillContent.DrillCollection>() {
            @Override
            public boolean areItemsTheSame(
                    @NonNull DrillContent.DrillCollection oldItem,
                    @NonNull DrillContent.DrillCollection newItem) {
                return oldItem.grammarPoint.equals(newItem.grammarPoint);
            }

            @Override
            public boolean areContentsTheSame(
                    @NonNull DrillContent.DrillCollection oldItem,
                    @NonNull DrillContent.DrillCollection newItem) {
                return oldItem.grammarPoint.equals(newItem.grammarPoint);
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
                .inflate(R.layout.item_grammar_point, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DrillContent.DrillCollection gc = getItem(position);
        holder.bind(gc);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, typeText, stepsText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.grammar_title);
            typeText = itemView.findViewById(R.id.grammar_types);
            stepsText = itemView.findViewById(R.id.grammar_steps);
        }

        void bind(DrillContent.DrillCollection gc) {
            titleText.setText(gc.grammarPoint);
            stepsText.setText("共 " + gc.totalSteps() + " 题");

            // Build drill type summary
            StringBuilder types = new StringBuilder();
            for (DrillContent.DrillSet set : gc.sets) {
                if (types.length() > 0) types.append("  ·  ");
                types.append(set.drillType.labelCn);
            }
            typeText.setText(types.toString());

            itemView.setOnClickListener(v -> {
                if (callback != null) callback.onSelect(gc);
            });
        }
    }
}
