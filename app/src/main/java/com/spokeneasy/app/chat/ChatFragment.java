package com.spokeneasy.app.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.audio.TTSEngine;

public class ChatFragment extends Fragment {

    private ChatViewModel viewModel;
    private RecyclerView recyclerView;
    private EditText inputEdit;
    private ImageButton btnSend;
    private ImageButton btnClear;
    private ImageButton btnSettings;
    private ProgressBar loadingBar;
    private MaterialCardView apiKeyBanner;
    private ChatAdapter adapter;
    private TTSEngine ttsEngine;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.chat_recycler);
        inputEdit = view.findViewById(R.id.chat_input);
        btnSend = view.findViewById(R.id.btn_chat_send);
        btnClear = view.findViewById(R.id.btn_chat_clear);
        btnSettings = view.findViewById(R.id.btn_chat_settings);
        loadingBar = view.findViewById(R.id.chat_loading);
        apiKeyBanner = view.findViewById(R.id.chat_api_key_banner);

        ImageButton btnBack = view.findViewById(R.id.btn_chat_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Setup RecyclerView
        adapter = new ChatAdapter(this::speakText);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.setMessages(messages);
            if (!messages.isEmpty()) {
                recyclerView.smoothScrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = loading == Boolean.TRUE;
            loadingBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnSend.setEnabled(!isLoading);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(view, error, Snackbar.LENGTH_LONG)
                        .setAction("知道了", null)
                        .show();
                viewModel.dismissError();
            }
        });

        viewModel.getHasApiKey().observe(getViewLifecycleOwner(), hasKey -> {
            apiKeyBanner.setVisibility(hasKey ? View.GONE : View.VISIBLE);
        });

        // Send button
        btnSend.setOnClickListener(v -> sendMessage());

        // Enter key to send
        inputEdit.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // Send on Enter, allow Shift+Enter for newline
                if (!event.isShiftPressed()) {
                    sendMessage();
                    return true;
                }
            }
            return false;
        });

        // Enable/disable send button based on input
        inputEdit.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                btnSend.setEnabled(s != null && s.toString().trim().length() > 0);
            }
        });

        // Clear button
        btnClear.setOnClickListener(v -> {
            if (adapter.getItemCount() > 1) { // Don't count welcome message as "having messages"
                viewModel.clearMessages();
                Snackbar.make(view, "对话已清空", Snackbar.LENGTH_SHORT).show();
            }
        });

        // Settings button → navigate to SettingsFragment
        btnSettings.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                    .navigate(R.id.action_chat_to_settings);
        });

        // Init TTS
        initTtsEngine();
    }

    private void sendMessage() {
        String text = inputEdit.getText().toString().trim();
        if (text.isEmpty()) return;

        viewModel.sendText(text);
        inputEdit.setText("");
    }

    private void initTtsEngine() {
        ttsEngine = new TTSEngine();
        ttsEngine.init(requireContext(), new TTSEngine.TtsCallback() {
            @Override
            public void onDone() {}

            @Override
            public void onError(String message) {}

            @Override
            public void onLanguageWarning(String message) {
                // Try to speak anyway
            }
        });
    }

    private void speakText(String text) {
        if (ttsEngine != null) {
            ttsEngine.speak(text);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsEngine != null) {
            ttsEngine.shutdown();
            ttsEngine = null;
        }
    }
}
