package com.spokeneasy.app.progress;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "practice_records",
        indices = {@Index("user_uuid"), @Index("created_at")})
public class PracticeRecordEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "user_uuid")
    private String userUuid;

    @NonNull
    @ColumnInfo(name = "module_type")
    private String moduleType;

    @ColumnInfo(name = "item_id")
    private long itemId;

    @ColumnInfo(name = "reference_text")
    private String referenceText;

    private int score;

    private String detail;

    @ColumnInfo(name = "audio_file_path")
    private String audioFilePath;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public PracticeRecordEntity(@NonNull String userUuid, @NonNull String moduleType,
                                long itemId, String referenceText, int score,
                                String detail, String audioFilePath, long createdAt) {
        this.userUuid = userUuid;
        this.moduleType = moduleType;
        this.itemId = itemId;
        this.referenceText = referenceText;
        this.score = score;
        this.detail = detail;
        this.audioFilePath = audioFilePath;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(@NonNull String userUuid) { this.userUuid = userUuid; }

    @NonNull
    public String getModuleType() { return moduleType; }
    public void setModuleType(@NonNull String moduleType) { this.moduleType = moduleType; }

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }

    public String getReferenceText() { return referenceText; }
    public void setReferenceText(String referenceText) { this.referenceText = referenceText; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getAudioFilePath() { return audioFilePath; }
    public void setAudioFilePath(String audioFilePath) { this.audioFilePath = audioFilePath; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
