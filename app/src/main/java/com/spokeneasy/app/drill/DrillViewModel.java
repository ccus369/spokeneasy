package com.spokeneasy.app.drill;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spokeneasy.app.core.database.AppDatabase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DrillViewModel extends AndroidViewModel {

    public enum Phase { SELECTING, DRILLING, SUMMARY }

    private final MutableLiveData<List<DrillContent.DrillCollection>> grammarPoints = new MutableLiveData<>();
    private final MutableLiveData<Phase> phase = new MutableLiveData<>(Phase.SELECTING);
    private final MutableLiveData<DrillContent.DrillCollection> currentGrammar = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentStepIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private List<DrillContent.DrillStep> currentSteps = new ArrayList<>();
    private final List<StepResult> stepResults = new ArrayList<>();

    public static class StepResult {
        public final String stepId;
        public final int score;
        public final String detail;
        public final String audioPath;

        public StepResult(String stepId, int score, String detail, String audioPath) {
            this.stepId = stepId;
            this.score = score;
            this.detail = detail;
            this.audioPath = audioPath;
        }
    }

    public DrillViewModel(@NonNull Application application) {
        super(application);
        loadData();
    }

    public LiveData<List<DrillContent.DrillCollection>> getGrammarPoints() { return grammarPoints; }
    public LiveData<Phase> getPhase() { return phase; }
    public LiveData<DrillContent.DrillCollection> getCurrentGrammar() { return currentGrammar; }
    public LiveData<Integer> getCurrentStepIndex() { return currentStepIndex; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    private void loadData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String[] files = getApplication().getAssets().list("drills");
                List<DrillContent.DrillCollection> list = new ArrayList<>();
                if (files != null) {
                    for (String file : files) {
                        if (!file.endsWith(".json")) continue;
                        InputStream is = getApplication().getAssets().open("drills/" + file);
                        java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                        String json = scanner.hasNext() ? scanner.next() : "";
                        scanner.close();
                        is.close();
                        list.add(DrillContent.parseCollection(json));
                    }
                }
                grammarPoints.postValue(list);
                isLoading.postValue(false);
            } catch (Exception e) {
                errorMessage.postValue("加载句型数据失败: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    public void selectGrammar(DrillContent.DrillCollection gc) {
        currentGrammar.setValue(gc);
        currentSteps = DrillContent.flattenSteps(gc);
        currentStepIndex.setValue(0);
        stepResults.clear();
        phase.setValue(Phase.DRILLING);
    }

    public DrillContent.DrillStep getCurrentStep() {
        Integer index = currentStepIndex.getValue();
        if (index != null && index >= 0 && index < currentSteps.size()) {
            return currentSteps.get(index);
        }
        return null;
    }

    public boolean hasNextStep() {
        Integer index = currentStepIndex.getValue();
        return index != null && currentSteps != null && index < currentSteps.size() - 1;
    }

    public boolean hasPrevStep() {
        Integer index = currentStepIndex.getValue();
        return index != null && index > 0;
    }

    public boolean isStepScored(String stepId) {
        for (StepResult r : stepResults) {
            if (r.stepId.equals(stepId)) return true;
        }
        return false;
    }

    public Integer getStepScore(String stepId) {
        for (StepResult r : stepResults) {
            if (r.stepId.equals(stepId)) return r.score;
        }
        return null;
    }

    public void nextStep() {
        if (hasNextStep()) {
            currentStepIndex.setValue(currentStepIndex.getValue() + 1);
        } else {
            phase.setValue(Phase.SUMMARY);
        }
    }

    public void prevStep() {
        if (hasPrevStep()) {
            currentStepIndex.setValue(currentStepIndex.getValue() - 1);
        }
    }

    public void addStepResult(String stepId, int score, String detail, String audioPath) {
        stepResults.removeIf(r -> r.stepId.equals(stepId));
        stepResults.add(new StepResult(stepId, score, detail, audioPath));
    }

    public List<StepResult> getStepResults() {
        return new ArrayList<>(stepResults);
    }

    public int getAverageScore() {
        if (stepResults.isEmpty()) return 0;
        int sum = 0;
        for (StepResult r : stepResults) sum += r.score;
        return sum / stepResults.size();
    }

    public void backToSelection() {
        phase.setValue(Phase.SELECTING);
        currentGrammar.setValue(null);
        currentSteps = new ArrayList<>();
        stepResults.clear();
    }

    public void retryGrammar() {
        currentStepIndex.setValue(0);
        stepResults.clear();
        phase.setValue(Phase.DRILLING);
    }

    /** Total steps for progress display. */
    public int getTotalSteps() {
        return currentSteps.size();
    }

    /** Current drill type label for the active step. */
    public String getCurrentDrillTypeLabel() {
        DrillContent.DrillStep step = getCurrentStep();
        if (step == null) return "";
        // Find which set this step belongs to
        DrillContent.DrillCollection gc = currentGrammar.getValue();
        if (gc != null) {
            for (DrillContent.DrillSet set : gc.sets) {
                for (DrillContent.DrillStep s : set.steps) {
                    if (s.id.equals(step.id)) {
                        return set.drillType.labelCn;
                    }
                }
            }
        }
        return "";
    }
}
