package com.spokeneasy.app.listening;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ListeningAudioDao {

    @Query("SELECT * FROM listening_audios ORDER BY level ASC, id ASC")
    List<ListeningAudioEntity> getAll();

    @Query("SELECT * FROM listening_audios WHERE id = :id")
    ListeningAudioEntity getById(long id);

    @Query("SELECT * FROM listening_audios WHERE level = :level ORDER BY id ASC")
    List<ListeningAudioEntity> getByLevel(int level);

    @Query("SELECT * FROM listening_questions WHERE audio_id = :audioId")
    List<ListeningQuestionEntity> getQuestionsByAudioId(long audioId);

    @Query("SELECT * FROM listening_questions WHERE id = :id")
    ListeningQuestionEntity getQuestionById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ListeningAudioEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertQuestion(ListeningQuestionEntity question);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<ListeningAudioEntity> entities);

    @Update
    int update(ListeningAudioEntity entity);

    @Delete
    int delete(ListeningAudioEntity entity);

    @Query("SELECT COUNT(*) FROM listening_audios")
    int getAudioCount();
}
