package com.spokeneasy.app.listening;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.spokeneasy.app.R;

public class ListeningListFragment extends Fragment {

    private ListeningViewModel viewModel;
    private ListeningListAdapter adapter;
    private LinearProgressIndicator loadingIndicator;
    private ChipGroup chipGroupLevel;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private View skeletonLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_listening_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ListeningViewModel.class);

        loadingIndicator = view.findViewById(R.id.loading_indicator);
        skeletonLayout = view.findViewById(R.id.skeleton_layout);
        chipGroupLevel = view.findViewById(R.id.chip_group_level);
        emptyView = view.findViewById(R.id.empty_view);

        recyclerView = view.findViewById(R.id.listening_recycler_view);
        adapter = new ListeningListAdapter();
        adapter.setOnItemClickListener(item -> {
            NavController navController = Navigation.findNavController(view);
            Bundle args = new Bundle();
            args.putLong("audioId", item.getId());
            navController.navigate(R.id.action_listeningList_to_listeningDetail, args);
        });
        recyclerView.setAdapter(adapter);

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                skeletonLayout.setVisibility(View.GONE);
                adapter.submitList(items);
                boolean empty = items.isEmpty();
                recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
            skeletonLayout.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        chipGroupLevel.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int level = 0;
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chip_level_1) level = 1;
                else if (id == R.id.chip_level_2) level = 2;
                else if (id == R.id.chip_level_3) level = 3;
            }
            viewModel.loadItems(level);
        });
    }
}
