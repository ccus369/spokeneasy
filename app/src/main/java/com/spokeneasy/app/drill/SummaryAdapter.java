package com.spokeneasy.app.drill;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.spokeneasy.app.R;

import java.util.List;

public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.ViewHolder> {

    private List<DrillViewModel.StepResult> results;
    private List<DrillContent.DrillStep> steps;

    public SummaryAdapter(List<DrillViewModel.StepResult> results,
                          List<DrillContent.DrillStep> steps) {
        this.results = results;
        this.steps = steps;
    }

    public void updateData(List<DrillViewModel.StepResult> results,
                           List<DrillContent.DrillStep> steps) {
        this.results = results;
        this.steps = steps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_summary_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < results.size()) {
            DrillViewModel.StepResult result = results.get(position);
            DrillContent.DrillStep step = steps.get(position);
            holder.bind(position, step, result);
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView indexText, expectedText, scoreText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            indexText = itemView.findViewById(R.id.summary_index);
            expectedText = itemView.findViewById(R.id.summary_expected);
            scoreText = itemView.findViewById(R.id.summary_score);
        }

        void bind(int index, DrillContent.DrillStep step, DrillViewModel.StepResult result) {
            indexText.setText("第 " + (index + 1) + " 题");
            expectedText.setText(step.expected);
            scoreText.setText(result.score + " 分");

            // Color based on score
            int color;
            if (result.score >= 80) {
                color = ContextCompat.getColor(itemView.getContext(), R.color.score_excellent);
            } else if (result.score >= 60) {
                color = ContextCompat.getColor(itemView.getContext(), R.color.score_fair);
            } else {
                color = ContextCompat.getColor(itemView.getContext(), R.color.score_poor);
            }
            scoreText.setTextColor(color);
        }
    }
}
