package com.spokeneasy.app.dialogue;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spokeneasy.app.core.database.AppDatabase;
import com.spokeneasy.app.core.net.ApiConfig;
import com.spokeneasy.app.core.net.MiMoApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DialogueViewModel extends AndroidViewModel {

    public enum Phase { SCENE_SELECT, WARMUP, DIALOGUE, ROLEPLAY, SUMMARY }

    private final MutableLiveData<Phase> phase = new MutableLiveData<>(Phase.SCENE_SELECT);
    private final MutableLiveData<List<ScenarioContent.Scenario>> scenarios = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ScenarioContent.Scenario> currentScenario = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> currentLineIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    // Dialogue scoring results: lineId -> score
    private final Map<Integer, Integer> lineScores = new HashMap<>();
    private final Map<Integer, String> lineRecordings = new HashMap<>();

    // Roleplay messages
    private final MutableLiveData<List<RoleplayMessage>> roleplayMessages = new MutableLiveData<>(new ArrayList<>());

    private final MiMoApiService apiService;
    private final ApiConfig apiConfig;

    public DialogueViewModel(@NonNull Application application) {
        super(application);
        this.apiConfig = new ApiConfig(application);
        this.apiService = new MiMoApiService(apiConfig);
        loadScenarios();
    }

    // ===== LiveData Getters =====

    public LiveData<Phase> getPhase() { return phase; }
    public LiveData<List<ScenarioContent.Scenario>> getScenarios() { return scenarios; }
    public LiveData<ScenarioContent.Scenario> getCurrentScenario() { return currentScenario; }
    public LiveData<Integer> getCurrentLineIndex() { return currentLineIndex; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    // ===== Scenario Loading =====

    private void loadScenarios() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                android.content.Context ctx = getApplication();
                java.io.InputStream is = ctx.getAssets().open("dialogues/scenarios.json");
                java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                String json = scanner.hasNext() ? scanner.next() : "[]";
                scanner.close();

                List<ScenarioContent.Scenario> list = ScenarioContent.parseAll(json);
                scenarios.postValue(list);
            } catch (Exception e) {
                error.postValue("加载场景失败: " + e.getMessage());
            }
        });
    }

    // ===== Phase Transitions =====

    public void selectScenario(ScenarioContent.Scenario scenario) {
        currentScenario.setValue(scenario);
        phase.setValue(Phase.WARMUP);
    }

    public void startDialogue() {
        currentLineIndex.setValue(0);
        lineScores.clear();
        lineRecordings.clear();
        phase.setValue(Phase.DIALOGUE);
    }

    public void startRoleplay() {
        roleplayMessages.setValue(new ArrayList<>());
        phase.setValue(Phase.ROLEPLAY);
    }

    public void showSummary() {
        phase.setValue(Phase.SUMMARY);
    }

    public void backToSceneSelect() {
        currentScenario.setValue(null);
        phase.setValue(Phase.SCENE_SELECT);
    }

    // ===== Dialogue Navigation =====

    public void nextLine() {
        ScenarioContent.Scenario sc = currentScenario.getValue();
        if (sc == null) return;
        Integer idx = currentLineIndex.getValue();
        if (idx != null && idx < sc.dialogueLines.size() - 1) {
            currentLineIndex.setValue(idx + 1);
        }
    }

    public void prevLine() {
        Integer idx = currentLineIndex.getValue();
        if (idx != null && idx > 0) {
            currentLineIndex.setValue(idx - 1);
        }
    }

    public boolean hasNextLine() {
        ScenarioContent.Scenario sc = currentScenario.getValue();
        Integer idx = currentLineIndex.getValue();
        return sc != null && idx != null && idx < sc.dialogueLines.size() - 1;
    }

    public boolean hasPrevLine() {
        Integer idx = currentLineIndex.getValue();
        return idx != null && idx > 0;
    }

    public int getTotalLines() {
        ScenarioContent.Scenario sc = currentScenario.getValue();
        return sc != null ? sc.dialogueLines.size() : 0;
    }

    // ===== Dialogue Scoring =====

    public void addLineScore(int lineIndex, int score, String filePath) {
        lineScores.put(lineIndex, score);
        lineRecordings.put(lineIndex, filePath);
    }

    public boolean isLineScored(int lineIndex) {
        return lineScores.containsKey(lineIndex);
    }

    public int getLineScore(int lineIndex) {
        Integer s = lineScores.get(lineIndex);
        return s != null ? s : 0;
    }

    public boolean allLinesScored() {
        ScenarioContent.Scenario sc = currentScenario.getValue();
        if (sc == null) return false;
        for (int i = 0; i < sc.dialogueLines.size(); i++) {
            if (!lineScores.containsKey(i)) return false;
        }
        return true;
    }

    public int getAverageScore() {
        if (lineScores.isEmpty()) return 0;
        int sum = 0;
        for (int s : lineScores.values()) sum += s;
        return sum / lineScores.size();
    }

    public int getScoredCount() {
        return lineScores.size();
    }

    // ===== Roleplay =====

    public static class RoleplayMessage {
        public final boolean isUser;
        public final String text;

        public RoleplayMessage(boolean isUser, String text) {
            this.isUser = isUser;
            this.text = text;
        }
    }

    public LiveData<List<RoleplayMessage>> getRoleplayMessages() {
        return roleplayMessages;
    }

    public void sendRoleplayMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (isLoading.getValue() == Boolean.TRUE) return;

        String trimmed = text.trim();
        List<RoleplayMessage> current = new ArrayList<>(roleplayMessages.getValue());
        current.add(new RoleplayMessage(true, trimmed));
        roleplayMessages.setValue(current);

        ScenarioContent.Scenario sc = currentScenario.getValue();

        isLoading.setValue(true);

        List<RoleplayMessage> snapshot = new ArrayList<>(current);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Build a custom roleplay request with scenario context
                List<com.spokeneasy.app.chat.ChatMessage> chatHistory = new ArrayList<>();
                if (sc != null && sc.systemPrompt != null && !sc.systemPrompt.isEmpty()) {
                    chatHistory.add(new com.spokeneasy.app.chat.ChatMessage(
                            com.spokeneasy.app.chat.ChatMessage.Role.SYSTEM, sc.systemPrompt));
                }
                // Add recent history (last 10 exchanges)
                int start = Math.max(0, snapshot.size() - 11);
                for (int i = start; i < snapshot.size(); i++) {
                    RoleplayMessage rm = snapshot.get(i);
                    chatHistory.add(new com.spokeneasy.app.chat.ChatMessage(
                            rm.isUser ? com.spokeneasy.app.chat.ChatMessage.Role.USER
                                    : com.spokeneasy.app.chat.ChatMessage.Role.ASSISTANT,
                            rm.text));
                }

                String response = apiService.sendMessage(chatHistory);
                List<RoleplayMessage> updated = new ArrayList<>(roleplayMessages.getValue());
                updated.add(new RoleplayMessage(false, response));
                roleplayMessages.postValue(updated);
                isLoading.postValue(false);
            } catch (Exception e) {
                List<RoleplayMessage> updated = new ArrayList<>(roleplayMessages.getValue());
                updated.add(new RoleplayMessage(false,
                        "(AI 响应失败: " + e.getMessage() + ")"));
                roleplayMessages.postValue(updated);
                isLoading.postValue(false);
            }
        });
    }

    public void clearMessages() {
        roleplayMessages.setValue(new ArrayList<>());
    }

    // ===== Utils =====

    public void dismissError() {
        error.setValue(null);
    }
}
