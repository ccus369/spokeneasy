package com.spokeneasy.app.word;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WordDao {

    @Query("SELECT * FROM words ORDER BY id ASC")
    List<WordEntity> getAll();

    @Query("SELECT * FROM words WHERE id = :id")
    WordEntity getById(long id);

    @Query("SELECT * FROM words WHERE category = :category ORDER BY id ASC")
    List<WordEntity> getByCategory(String category);

    @Query("SELECT * FROM words WHERE word LIKE '%' || :keyword || '%' ORDER BY id ASC")
    List<WordEntity> search(String keyword);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(WordEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<WordEntity> entities);

    @Update
    int update(WordEntity entity);

    @Delete
    int delete(WordEntity entity);

    @Query("SELECT COUNT(*) FROM words")
    int getCount();
}
