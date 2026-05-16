package com.spokeneasy.app.word;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spokeneasy.app.core.database.AppDatabase;

import java.util.List;

public class WordViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final MutableLiveData<List<WordEntity>> words = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public WordViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.repository = WordRepository.create(db);
        loadWords();
    }

    public void loadWords() {
        isLoading.setValue(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<WordEntity> list = repository.getAll();
            words.postValue(list);
            isLoading.postValue(false);
        });
    }

    public void search(String keyword) {
        isLoading.setValue(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<WordEntity> list = repository.search(keyword);
            words.postValue(list);
            isLoading.postValue(false);
        });
    }

    public LiveData<List<WordEntity>> getWords() {
        return words;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
