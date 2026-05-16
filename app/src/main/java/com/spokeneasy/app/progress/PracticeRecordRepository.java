package com.spokeneasy.app.progress;

import com.spokeneasy.app.core.database.AppDatabase;

import java.util.List;

public class PracticeRecordRepository {

    private final PracticeRecordDao dao;

    public PracticeRecordRepository(AppDatabase db) {
        this.dao = db.practiceRecordDao();
    }

    public List<PracticeRecordEntity> getAll(String userUuid) {
        return dao.getAll(userUuid);
    }

    public List<PracticeRecordEntity> getRecent(String userUuid, int limit) {
        return dao.getRecent(userUuid, limit);
    }

    public List<PracticeRecordEntity> getByModule(String userUuid, String moduleType) {
        return dao.getByModule(userUuid, moduleType);
    }

    public long insert(PracticeRecordEntity record) {
        return dao.insert(record);
    }
}
