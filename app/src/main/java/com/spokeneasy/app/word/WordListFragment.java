package com.spokeneasy.app.word;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.spokeneasy.app.R;

import java.util.List;

public class WordListFragment extends Fragment {

    private WordViewModel viewModel;
    private WordListAdapter adapter;
    private LinearProgressIndicator loadingIndicator;
    private TextInputEditText searchEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_word_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(WordViewModel.class);

        loadingIndicator = view.findViewById(R.id.loading_indicator);
        searchEditText = view.findViewById(R.id.search_edit_text);

        RecyclerView recyclerView = view.findViewById(R.id.word_recycler_view);
        adapter = new WordListAdapter();
        adapter.setOnItemClickListener(word -> {
            NavController navController = Navigation.findNavController(view);
            Bundle args = new Bundle();
            args.putLong("wordId", word.getId());
            navController.navigate(R.id.action_wordList_to_wordDetail, args);
        });
        recyclerView.setAdapter(adapter);

        viewModel.getWords().observe(getViewLifecycleOwner(), words -> {
            if (words != null) {
                adapter.submitList(words);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim();
                if (keyword.isEmpty()) {
                    viewModel.loadWords();
                } else {
                    viewModel.search(keyword);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
