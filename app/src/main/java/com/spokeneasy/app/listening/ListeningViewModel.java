package com.spokeneasy.app.listening;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spokeneasy.app.core.database.AppDatabase;

import java.util.List;

public class ListeningViewModel extends AndroidViewModel {

    private final ListeningRepository repository;
    private final MutableLiveData<List<ListeningAudioEntity>> items = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> currentLevel = new MutableLiveData<>(0);

    public ListeningViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.repository = ListeningRepository.create(db);
        loadItems(0);
    }

    public void loadItems(int level) {
        currentLevel.setValue(level);
        isLoading.setValue(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ListeningAudioEntity> list;
            if (level == 0) {
                list = repository.getAll();
            } else {
                list = repository.getByLevel(level);
            }
            items.postValue(list);
            isLoading.postValue(false);
        });
    }

    public LiveData<List<ListeningAudioEntity>> getItems() {
        return items;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Integer> getCurrentLevel() {
        return currentLevel;
    }

    public List<ListeningQuestionEntity> getQuestionsSync(long audioId) {
        return repository.getQuestionsByAudioId(audioId);
    }
}
