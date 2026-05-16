package com.spokeneasy.app.linking;

import com.spokeneasy.app.core.database.AppDatabase;

import java.util.List;
import java.util.concurrent.Future;

public class LinkingRepository {

    private final LinkingDao linkingDao;

    private LinkingRepository(LinkingDao linkingDao) {
        this.linkingDao = linkingDao;
    }

    public static LinkingRepository create(AppDatabase db) {
        return new LinkingRepository(db.linkingDao());
    }

    public List<LinkingEntity> getAll() {
        return linkingDao.getAll();
    }

    public LinkingEntity getById(long id) {
        return linkingDao.getById(id);
    }

    public List<LinkingEntity> getByCategory(String category) {
        return linkingDao.getByCategory(category);
    }

    public Future<Long> insert(final LinkingEntity entity) {
        return AppDatabase.databaseWriteExecutor.submit(() -> linkingDao.insert(entity));
    }

    public int getCount() {
        return linkingDao.getCount();
    }
}
