package com.spokeneasy.app.shadowing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.spokeneasy.app.R;

import java.util.ArrayList;
import java.util.List;

public class ShadowingListFragment extends Fragment {

    private ShadowingListAdapter adapter;
    private List<ShadowingContent> allItems = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ShadowingContent item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shadowing_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_level);
        RecyclerView recyclerView = view.findViewById(R.id.shadowing_recycler_view);

        adapter = new ShadowingListAdapter();
        adapter.setOnItemClickListener(item -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
        recyclerView.setAdapter(adapter);

        allItems = ShadowingLoader.load(requireContext());
        adapter.submitList(allItems);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int level = 0;
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chip_level_1) level = 1;
                else if (id == R.id.chip_level_2) level = 2;
                else if (id == R.id.chip_level_3) level = 3;
            }
            adapter.submitList(ShadowingLoader.filterByLevel(allItems, level));
        });
    }
}
