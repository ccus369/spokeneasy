package com.spokeneasy.app.progress;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spokeneasy.app.core.database.AppDatabase;
import com.spokeneasy.app.core.util.UuidManager;
import com.spokeneasy.app.linking.LinkingRepository;
import com.spokeneasy.app.pronunciation.PronunciationContent;
import com.spokeneasy.app.word.WordRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UserProgressViewModel extends AndroidViewModel {

    private final UserProgressRepository progressRepo;
    private final WordRepository wordRepo;
    private final LinkingRepository linkingRepo;
    private final String userUuid;

    private final MutableLiveData<List<ModuleStats>> stats = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public UserProgressViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.progressRepo = UserProgressRepository.create(db);
        this.wordRepo = WordRepository.create(db);
        this.linkingRepo = LinkingRepository.create(db);
        this.userUuid = UuidManager.getDeviceUuid(application);
        loadStats();
    }

    public void loadStats() {
        isLoading.setValue(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ModuleStats> list = new ArrayList<>(3);

            int wordTotal = wordRepo.getCount();
            int wordCompleted = progressRepo.getWordCompletedCount(userUuid);
            int wordAttempted = progressRepo.getWordAttemptedCount(userUuid);
            list.add(new ModuleStats("word", wordTotal, wordCompleted, wordAttempted));

            int linkingTotal = linkingRepo.getCount();
            int linkingCompleted = progressRepo.getCompletedCount(userUuid, "linking");
            int linkingAttempted = progressRepo.getAttemptedCount(userUuid, "linking");
            list.add(new ModuleStats("linking", linkingTotal, linkingCompleted, linkingAttempted));

            // Pronunciation: count from JSON assets
            int pronunciationTotal = 0;
            try {
                InputStream is = getApplication().getAssets()
                        .open("pronunciation/minimal_pairs.json");
                java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                String json = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                is.close();
                PronunciationContent.MinimalPairsData parsed =
                        PronunciationContent.parse(json);
                pronunciationTotal = parsed.pairs.size();
            } catch (Exception e) {
                pronunciationTotal = 0;
            }
            int pronunciationCompleted = progressRepo.getCompletedCount(userUuid, "pronunciation");
            int pronunciationAttempted = progressRepo.getAttemptedCount(userUuid, "pronunciation");
            list.add(new ModuleStats("pronunciation", pronunciationTotal,
                    pronunciationCompleted, pronunciationAttempted));

            stats.postValue(list);
            isLoading.postValue(false);
        });
    }

    public void resetAllProgress(Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<UserProgressEntity> all = progressRepo.getAll(userUuid);
            for (UserProgressEntity entity : all) {
                progressRepo.delete(entity);
            }
            if (onComplete != null) {
                android.os.Handler mainHandler = new android.os.Handler(
                        getApplication().getMainLooper());
                mainHandler.post(() -> {
                    onComplete.run();
                    loadStats();
                });
            }
        });
    }

    public LiveData<List<ModuleStats>> getStats() {
        return stats;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public static class ModuleStats {
        private final String moduleType;
        private final int totalCount;
        private final int completedCount;
        private final int attemptedCount;

        public ModuleStats(String moduleType, int totalCount,
                           int completedCount, int attemptedCount) {
            this.moduleType = moduleType;
            this.totalCount = totalCount;
            this.completedCount = completedCount;
            this.attemptedCount = attemptedCount;
        }

        public String getModuleType() { return moduleType; }
        public int getTotalCount() { return totalCount; }
        public int getCompletedCount() { return completedCount; }
        public int getAttemptedCount() { return attemptedCount; }
    }
}
