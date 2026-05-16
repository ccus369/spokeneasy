package com.spokeneasy.app.progress;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserProgressDao {

    @Query("SELECT * FROM user_progress WHERE user_uuid = :userUuid ORDER BY id ASC")
    List<UserProgressEntity> getAll(String userUuid);

    @Query("SELECT * FROM user_progress WHERE user_uuid = :userUuid AND module_type = :moduleType ORDER BY id ASC")
    List<UserProgressEntity> getByModule(String userUuid, String moduleType);

    @Query("SELECT * FROM user_progress WHERE user_uuid = :userUuid AND module_type = :moduleType AND item_id = :itemId")
    UserProgressEntity getByItem(String userUuid, String moduleType, long itemId);

    @Query("SELECT * FROM user_progress WHERE user_uuid = :userUuid AND is_completed = 1")
    List<UserProgressEntity> getCompleted(String userUuid);

    @Query("SELECT COUNT(*) FROM user_progress WHERE user_uuid = :userUuid AND module_type = :moduleType AND is_completed = 1")
    int getCompletedCount(String userUuid, String moduleType);

    @Query("SELECT COUNT(*) FROM user_progress WHERE user_uuid = :userUuid AND module_type = :moduleType")
    int getAttemptedCount(String userUuid, String moduleType);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserProgressEntity entity);

    @Update
    int update(UserProgressEntity entity);

    @Delete
    int delete(UserProgressEntity entity);
}
