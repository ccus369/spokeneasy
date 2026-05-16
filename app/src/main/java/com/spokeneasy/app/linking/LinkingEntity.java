package com.spokeneasy.app.linking;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "linking")
public class LinkingEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "rule_name")
    private String ruleName;

    @ColumnInfo(name = "original")
    private String original;

    @ColumnInfo(name = "linking_text")
    private String linkingText;

    @ColumnInfo(name = "example_en")
    private String exampleEn;

    @ColumnInfo(name = "example_cn")
    private String exampleCn;

    @ColumnInfo(name = "category")
    private String category;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getOriginal() { return original; }
    public void setOriginal(String original) { this.original = original; }

    public String getLinkingText() { return linkingText; }
    public void setLinkingText(String linkingText) { this.linkingText = linkingText; }

    public String getExampleEn() { return exampleEn; }
    public void setExampleEn(String exampleEn) { this.exampleEn = exampleEn; }

    public String getExampleCn() { return exampleCn; }
    public void setExampleCn(String exampleCn) { this.exampleCn = exampleCn; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
