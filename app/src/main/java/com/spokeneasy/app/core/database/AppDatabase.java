package com.spokeneasy.app.core.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.spokeneasy.app.linking.LinkingDao;
import com.spokeneasy.app.linking.LinkingEntity;
import com.spokeneasy.app.listening.ListeningAudioDao;
import com.spokeneasy.app.listening.ListeningAudioEntity;
import com.spokeneasy.app.listening.ListeningQuestionEntity;
import com.spokeneasy.app.progress.PracticeRecordDao;
import com.spokeneasy.app.progress.PracticeRecordEntity;
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
        UserProgressEntity.class,
        PracticeRecordEntity.class
    },
    version = 2,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WordDao wordDao();
    public abstract LinkingDao linkingDao();
    public abstract ListeningAudioDao listeningAudioDao();
    public abstract UserProgressDao userProgressDao();
    public abstract PracticeRecordDao practiceRecordDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DB_NAME = "spokeneasy.db";
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);
    public static final ExecutorService networkExecutor =
            Executors.newCachedThreadPool();
    public static final ExecutorService scoringExecutor =
            Executors.newSingleThreadExecutor();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS practice_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "user_uuid TEXT NOT NULL, " +
                "module_type TEXT NOT NULL, " +
                "item_id INTEGER NOT NULL DEFAULT 0, " +
                "reference_text TEXT, " +
                "score INTEGER NOT NULL DEFAULT 0, " +
                "detail TEXT, " +
                "audio_file_path TEXT, " +
                "created_at INTEGER NOT NULL)");
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_practice_records_user_uuid " +
                "ON practice_records(user_uuid)");
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_practice_records_created_at " +
                "ON practice_records(created_at)");
        }
    };

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
                    .addMigrations(MIGRATION_1_2)
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
