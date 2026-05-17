package com.spokeneasy.app.pronunciation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spokeneasy.app.core.database.AppDatabase;

import java.io.InputStream;

public class PronunciationLabViewModel extends AndroidViewModel {

    private final MutableLiveData<PronunciationContent.MinimalPairsData> data =
            new MutableLiveData<>();
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public PronunciationLabViewModel(@NonNull Application application) {
        super(application);
        loadData();
    }

    public LiveData<PronunciationContent.MinimalPairsData> getData() {
        return data;
    }

    public LiveData<String> getSelectedCategory() {
        return selectedCategory;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void selectCategory(String categoryKey) {
        selectedCategory.setValue(categoryKey);
    }

    private void loadData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                InputStream is = getApplication().getAssets().open("pronunciation/minimal_pairs.json");
                java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                String json = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                is.close();
                PronunciationContent.MinimalPairsData parsed =
                        PronunciationContent.parse(json);
                data.postValue(parsed);
                isLoading.postValue(false);
            } catch (Exception e) {
                errorMessage.postValue("加载发音数据失败: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
}
