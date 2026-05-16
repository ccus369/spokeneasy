package com.spokeneasy.app.progress;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spokeneasy.app.core.database.AppDatabase;
import com.spokeneasy.app.core.util.UuidManager;

import java.util.List;

public class RecordHistoryViewModel extends AndroidViewModel {

    private final PracticeRecordRepository repository;
    private final String userUuid;
    private final MutableLiveData<List<PracticeRecordEntity>> records = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public RecordHistoryViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.repository = new PracticeRecordRepository(db);
        this.userUuid = UuidManager.getDeviceUuid(application);
        loadRecords();
    }

    public LiveData<List<PracticeRecordEntity>> getRecords() {
        return records;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void loadRecords() {
        isLoading.setValue(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<PracticeRecordEntity> result = repository.getAll(userUuid);
            records.postValue(result);
            isLoading.postValue(false);
        });
    }
}
