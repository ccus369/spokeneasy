package com.spokeneasy.app.progress;

import com.spokeneasy.app.core.database.AppDatabase;

import java.util.List;

public class UserProgressRepository {

    private final UserProgressDao dao;

    private UserProgressRepository(UserProgressDao dao) {
        this.dao = dao;
    }

    public static UserProgressRepository create(AppDatabase db) {
        return new UserProgressRepository(db.userProgressDao());
    }

    public List<UserProgressEntity> getAll(String userUuid) {
        return dao.getAll(userUuid);
    }

    public List<UserProgressEntity> getByModule(String userUuid, String moduleType) {
        return dao.getByModule(userUuid, moduleType);
    }

    public UserProgressEntity getByItem(String userUuid, String moduleType, long itemId) {
        return dao.getByItem(userUuid, moduleType, itemId);
    }

    public List<UserProgressEntity> getCompleted(String userUuid) {
        return dao.getCompleted(userUuid);
    }

    public int getCompletedCount(String userUuid, String moduleType) {
        return dao.getCompletedCount(userUuid, moduleType);
    }

    public int getAttemptedCount(String userUuid, String moduleType) {
        return dao.getAttemptedCount(userUuid, moduleType);
    }

    public long insert(UserProgressEntity entity) {
        return dao.insert(entity);
    }

    public int update(UserProgressEntity entity) {
        return dao.update(entity);
    }

    public int delete(UserProgressEntity entity) {
        return dao.delete(entity);
    }
}
