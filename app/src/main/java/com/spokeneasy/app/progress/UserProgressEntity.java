package com.spokeneasy.app.progress;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "user_progress",
    indices = {
        @Index(value = {"user_uuid", "module_type", "item_id"}, unique = true)
    }
)
public class UserProgressEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "user_uuid")
    private String userUuid;

    @ColumnInfo(name = "module_type")
    private String moduleType;

    @ColumnInfo(name = "item_id")
    private long itemId;

    @ColumnInfo(name = "score")
    private Integer score;

    @ColumnInfo(name = "is_completed")
    private int isCompleted;

    @ColumnInfo(name = "completed_at")
    private Long completedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }

    public String getModuleType() { return moduleType; }
    public void setModuleType(String moduleType) { this.moduleType = moduleType; }

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public int getIsCompleted() { return isCompleted; }
    public void setIsCompleted(int isCompleted) { this.isCompleted = isCompleted; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
}
