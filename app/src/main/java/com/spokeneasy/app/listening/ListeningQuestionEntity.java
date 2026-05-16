package com.spokeneasy.app.listening;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "listening_questions",
    foreignKeys = @ForeignKey(
        entity = ListeningAudioEntity.class,
        parentColumns = "id",
        childColumns = "audio_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("audio_id")}
)
public class ListeningQuestionEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "audio_id")
    private long audioId;

    @ColumnInfo(name = "question")
    private String question;

    @ColumnInfo(name = "option_a")
    private String optionA;

    @ColumnInfo(name = "option_b")
    private String optionB;

    @ColumnInfo(name = "option_c")
    private String optionC;

    @ColumnInfo(name = "correct_answer")
    private String correctAnswer;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAudioId() { return audioId; }
    public void setAudioId(long audioId) { this.audioId = audioId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
}
