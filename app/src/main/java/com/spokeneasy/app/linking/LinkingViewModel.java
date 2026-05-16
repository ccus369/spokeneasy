package com.spokeneasy.app.linking;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spokeneasy.app.core.database.AppDatabase;

import java.util.List;

public class LinkingViewModel extends AndroidViewModel {

    private final LinkingRepository repository;
    private final MutableLiveData<List<LinkingEntity>> items = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LinkingViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.repository = LinkingRepository.create(db);
        loadItems();
    }

    public void loadItems() {
        isLoading.setValue(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LinkingEntity> list = repository.getAll();
            items.postValue(list);
            isLoading.postValue(false);
        });
    }

    public LiveData<List<LinkingEntity>> getItems() {
        return items;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}
