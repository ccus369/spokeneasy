package com.spokeneasy.app.listening;

import com.spokeneasy.app.core.database.AppDatabase;

import java.util.List;

public class ListeningRepository {

    private final ListeningAudioDao dao;

    private ListeningRepository(ListeningAudioDao dao) {
        this.dao = dao;
    }

    public static ListeningRepository create(AppDatabase db) {
        return new ListeningRepository(db.listeningAudioDao());
    }

    public List<ListeningAudioEntity> getAll() {
        return dao.getAll();
    }

    public ListeningAudioEntity getById(long id) {
        return dao.getById(id);
    }

    public List<ListeningAudioEntity> getByLevel(int level) {
        return dao.getByLevel(level);
    }

    public List<ListeningQuestionEntity> getQuestionsByAudioId(long audioId) {
        return dao.getQuestionsByAudioId(audioId);
    }

    public int getCount() {
        return dao.getAudioCount();
    }
}
