package com.spokeneasy.app.word;

import com.spokeneasy.app.core.database.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class WordRepository {

    private final WordDao wordDao;

    public WordRepository() {
        // Initialized via getInstance — dao obtained lazily
        this.wordDao = null;
    }

    private WordRepository(WordDao wordDao) {
        this.wordDao = wordDao;
    }

    public static WordRepository create(AppDatabase db) {
        return new WordRepository(db.wordDao());
    }

    public List<WordEntity> getAll() {
        return wordDao.getAll();
    }

    public WordEntity getById(long id) {
        return wordDao.getById(id);
    }

    public List<WordEntity> getByCategory(String category) {
        return wordDao.getByCategory(category);
    }

    public List<WordEntity> search(String keyword) {
        return wordDao.search(keyword);
    }

    public Future<Long> insert(final WordEntity entity) {
        return AppDatabase.databaseWriteExecutor.submit(() -> wordDao.insert(entity));
    }

    public Future<Integer> update(final WordEntity entity) {
        return AppDatabase.databaseWriteExecutor.submit(() -> wordDao.update(entity));
    }

    public int getCount() {
        return wordDao.getCount();
    }
}
