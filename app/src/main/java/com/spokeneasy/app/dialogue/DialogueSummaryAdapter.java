package com.spokeneasy.app.dialogue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spokeneasy.app.R;

import java.util.List;
import java.util.Map;

public class DialogueSummaryAdapter extends RecyclerView.Adapter<DialogueSummaryAdapter.ViewHolder> {

    private List<ScenarioContent.DialogueLine> lines;
    private Map<Integer, Integer> scores;

    public DialogueSummaryAdapter(List<ScenarioContent.DialogueLine> lines,
                                  Map<Integer, Integer> scores) {
        this.lines = lines;
        this.scores = scores;
    }

    public void updateData(List<ScenarioContent.DialogueLine> lines,
                           Map<Integer, Integer> scores) {
        this.lines = lines;
        this.scores = scores;
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
        ScenarioContent.DialogueLine line = lines.get(position);
        Integer score = scores.get(position);

        holder.indexText.setText("第 " + (position + 1) + " 句");
        holder.expectedText.setText(line.speaker + ": " + line.text);

        if (score != null) {
            holder.scoreText.setText(score + " 分");
            int color;
            if (score >= 80) {
                color = 0xFF4CAF50;
            } else if (score >= 60) {
                color = 0xFFFF9800;
            } else {
                color = 0xFFF44336;
            }
            holder.scoreText.setTextColor(color);
        } else {
            holder.scoreText.setText("—");
            holder.scoreText.setTextColor(0xFF9E9E9E);
        }
    }

    @Override
    public int getItemCount() {
        return lines != null ? lines.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView indexText, expectedText, scoreText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            indexText = itemView.findViewById(R.id.summary_index);
            expectedText = itemView.findViewById(R.id.summary_expected);
            scoreText = itemView.findViewById(R.id.summary_score);
        }
    }
}
