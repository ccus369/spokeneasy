package com.spokeneasy.app.progress;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PracticeRecordDao {

    @Query("SELECT * FROM practice_records WHERE user_uuid = :userUuid ORDER BY created_at DESC")
    List<PracticeRecordEntity> getAll(@NonNull String userUuid);

    @Query("SELECT * FROM practice_records WHERE user_uuid = :userUuid ORDER BY created_at DESC LIMIT :limit")
    List<PracticeRecordEntity> getRecent(@NonNull String userUuid, int limit);

    @Query("SELECT * FROM practice_records WHERE user_uuid = :userUuid AND module_type = :moduleType ORDER BY created_at DESC")
    List<PracticeRecordEntity> getByModule(@NonNull String userUuid, @NonNull String moduleType);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PracticeRecordEntity record);
}
