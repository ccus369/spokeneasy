package com.spokeneasy.app.linking;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LinkingDao {

    @Query("SELECT * FROM linking ORDER BY id ASC")
    List<LinkingEntity> getAll();

    @Query("SELECT * FROM linking WHERE id = :id")
    LinkingEntity getById(long id);

    @Query("SELECT * FROM linking WHERE category = :category ORDER BY id ASC")
    List<LinkingEntity> getByCategory(String category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LinkingEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<LinkingEntity> entities);

    @Update
    int update(LinkingEntity entity);

    @Delete
    int delete(LinkingEntity entity);

    @Query("SELECT COUNT(*) FROM linking")
    int getCount();
}
