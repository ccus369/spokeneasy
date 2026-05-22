package com.spokeneasy.app.shadowing;

public class ShadowingContent {
    private final int id;
    private final String title;
    private final int level;
    private final String type; // "dialogue" or "monologue"
    private final String dialogText;

    public ShadowingContent(int id, String title, int level, String type, String dialogText) {
        this.id = id;
        this.title = title;
        this.level = level;
        this.type = type;
        this.dialogText = dialogText;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public int getLevel() { return level; }
    public String getType() { return type; }
    public String getDialogText() { return dialogText; }

    public String[] getSentences() {
        return dialogText.split("\n");
    }

    public boolean isMonologue() {
        return "monologue".equals(type);
    }
}
