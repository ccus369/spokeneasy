package com.spokeneasy.app.word;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "words")
public class WordEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "word")
    private String word;

    @ColumnInfo(name = "phonetic")
    private String phonetic;

    @ColumnInfo(name = "sentence1_en")
    private String sentence1En;

    @ColumnInfo(name = "sentence1_cn")
    private String sentence1Cn;

    @ColumnInfo(name = "sentence2_en")
    private String sentence2En;

    @ColumnInfo(name = "sentence2_cn")
    private String sentence2Cn;

    @ColumnInfo(name = "sentence3_en")
    private String sentence3En;

    @ColumnInfo(name = "sentence3_cn")
    private String sentence3Cn;

    @ColumnInfo(name = "category")
    private String category;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getPhonetic() { return phonetic; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

    public String getSentence1En() { return sentence1En; }
    public void setSentence1En(String sentence1En) { this.sentence1En = sentence1En; }

    public String getSentence1Cn() { return sentence1Cn; }
    public void setSentence1Cn(String sentence1Cn) { this.sentence1Cn = sentence1Cn; }

    public String getSentence2En() { return sentence2En; }
    public void setSentence2En(String sentence2En) { this.sentence2En = sentence2En; }

    public String getSentence2Cn() { return sentence2Cn; }
    public void setSentence2Cn(String sentence2Cn) { this.sentence2Cn = sentence2Cn; }

    public String getSentence3En() { return sentence3En; }
    public void setSentence3En(String sentence3En) { this.sentence3En = sentence3En; }

    public String getSentence3Cn() { return sentence3Cn; }
    public void setSentence3Cn(String sentence3Cn) { this.sentence3Cn = sentence3Cn; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
