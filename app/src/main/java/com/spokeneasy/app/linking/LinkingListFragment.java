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

        RecyclerView recyclerView = view.findViewById(R.id.linking_recycler_view);
        adapter = new LinkingListAdapter();
        adapter.setOnItemClickListener(item -> {
            NavController navController = Navigation.findNavController(view);
            Bundle args = new Bundle();
            args.putLong("linkingId", item.getId());
            navController.navigate(R.id.action_linkingList_to_linkingDetail, args);
        });
        recyclerView.setAdapter(adapter);

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                adapter.submitList(items);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }
}
