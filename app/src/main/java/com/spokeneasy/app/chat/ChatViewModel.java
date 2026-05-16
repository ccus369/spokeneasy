package com.spokeneasy.app.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spokeneasy.app.core.database.AppDatabase;
import com.spokeneasy.app.core.net.ApiConfig;
import com.spokeneasy.app.core.net.MiMoApiService;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private static final String WELCOME_MESSAGE =
            "Hello! I'm your English conversation partner. "
            + "What would you like to talk about today? 😊\n\n"
            + "You can practice any topic — daily life, travel, hobbies, work... "
            + "I'll help you improve your English along the way!";

    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> hasApiKey = new MutableLiveData<>(false);

    private final MiMoApiService apiService;
    private final ApiConfig apiConfig;

    private boolean welcomeShown = false;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.apiConfig = new ApiConfig(application);
        this.apiService = new MiMoApiService(apiConfig);
        checkApiKey();
        addWelcomeMessage();
    }

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getHasApiKey() { return hasApiKey; }

    public void checkApiKey() {
        hasApiKey.setValue(apiConfig.hasMiMoApiKey());
    }

    private void addWelcomeMessage() {
        if (welcomeShown) return;
        welcomeShown = true;
        List<ChatMessage> current = new ArrayList<>(messages.getValue());
        current.add(new ChatMessage(ChatMessage.Role.ASSISTANT, WELCOME_MESSAGE));
        messages.setValue(current);
    }

    public void sendText(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (isLoading.getValue() == Boolean.TRUE) return;

        // Add user message
        List<ChatMessage> current = new ArrayList<>(messages.getValue());
        ChatMessage userMsg = new ChatMessage(ChatMessage.Role.USER, text.trim());
        current.add(userMsg);
        messages.setValue(current);

        // Start loading
        isLoading.setValue(true);
        error.setValue(null);

        // Call API on background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String response = apiService.sendMessage(current);

                ChatMessage assistantMsg = new ChatMessage(ChatMessage.Role.ASSISTANT, response);

                List<ChatMessage> updated = new ArrayList<>(messages.getValue());
                updated.add(assistantMsg);

                // Post to main thread
                messages.postValue(updated);
                isLoading.postValue(false);
            } catch (MiMoApiService.MiMoException e) {
                error.postValue(e.getMessage());
                isLoading.postValue(false);
            } catch (Exception e) {
                error.postValue("网络请求失败，请检查网络连接后重试");
                isLoading.postValue(false);
            }
        });
    }

    public void dismissError() {
        error.setValue(null);
    }

    public void clearMessages() {
        welcomeShown = false;
        messages.setValue(new ArrayList<>());
        addWelcomeMessage();
    }
}
