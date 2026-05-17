package com.spokeneasy.app.dialogue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spokeneasy.app.R;

import java.util.ArrayList;
import java.util.List;

public class ScenarioAdapter extends RecyclerView.Adapter<ScenarioAdapter.ViewHolder> {

    private List<ScenarioContent.Scenario> scenarios = new ArrayList<>();
    private Callback callback;

    public interface Callback {
        void onSelect(ScenarioContent.Scenario scenario);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void submitList(List<ScenarioContent.Scenario> list) {
        this.scenarios = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scenario_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(scenarios.get(position), callback);
    }

    @Override
    public int getItemCount() {
        return scenarios.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title, titleEn, description;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.scenario_title);
            titleEn = itemView.findViewById(R.id.scenario_title_en);
            description = itemView.findViewById(R.id.scenario_description);
        }

        void bind(ScenarioContent.Scenario scenario, Callback callback) {
            title.setText(scenario.title);
            titleEn.setText(scenario.titleEn);
            description.setText(scenario.description);
            itemView.setOnClickListener(v -> {
                if (callback != null) callback.onSelect(scenario);
            });
        }
    }
}
