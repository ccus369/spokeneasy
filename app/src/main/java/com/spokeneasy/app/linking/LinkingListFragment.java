package com.spokeneasy.app.linking;

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

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.spokeneasy.app.R;

public class LinkingListFragment extends Fragment {

    private LinkingViewModel viewModel;
    private LinkingListAdapter adapter;
    private LinearProgressIndicator loadingIndicator;
    private View emptyView;
    private View skeletonLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_linking_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LinkingViewModel.class);

        loadingIndicator = view.findViewById(R.id.loading_indicator);
        skeletonLayout = view.findViewById(R.id.skeleton_layout);
        emptyView = view.findViewById(R.id.empty_view);

        RecyclerView recyclerView = view.findViewById(R.id.linking_recycler_view);
        adapter = new LinkingListAdapter();
        adapter.setOnItemClickListener(item -> {
            NavController navController = Navigation.findNavController(view);
            Bundle args = new Bundle();
            args.putLong("linkingId", item.getId());
            navController.navigate(R.id.linkingDetailFragment, args);
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
    }
}
