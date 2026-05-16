package com.spokeneasy.app.listening;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "listening_audios")
public class ListeningAudioEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "level")
    private int level;

    @ColumnInfo(name = "dialog_text")
    private String dialogText;

    @ColumnInfo(name = "audio_file_name")
    private String audioFileName;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getDialogText() { return dialogText; }
    public void setDialogText(String dialogText) { this.dialogText = dialogText; }

    public String getAudioFileName() { return audioFileName; }
    public void setAudioFileName(String audioFileName) { this.audioFileName = audioFileName; }
}
