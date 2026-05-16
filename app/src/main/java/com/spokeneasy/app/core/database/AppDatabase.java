package com.spokeneasy.app.core.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.spokeneasy.app.linking.LinkingDao;
import com.spokeneasy.app.linking.LinkingEntity;
import com.spokeneasy.app.listening.ListeningAudioDao;
import com.spokeneasy.app.listening.ListeningAudioEntity;
import com.spokeneasy.app.listening.ListeningQuestionEntity;
import com.spokeneasy.app.progress.UserProgressDao;
import com.spokeneasy.app.progress.UserProgressEntity;
import com.spokeneasy.app.word.WordDao;
import com.spokeneasy.app.word.WordEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        WordEntity.class,
        LinkingEntity.class,
        ListeningAudioEntity.class,
        ListeningQuestionEntity.class,
        UserProgressEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WordDao wordDao();
    public abstract LinkingDao linkingDao();
    public abstract ListeningAudioDao listeningAudioDao();
    public abstract UserProgressDao userProgressDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DB_NAME = "spokeneasy.db";
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DB_NAME
                    )
                    .createFromAsset("database/spokeneasy.db")
                    .addCallback(sRoomDatabaseCallback)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final Callback sRoomDatabaseCallback = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                // Database already pre-populated from assets
            });
        }
    };
}
