package com.spokeneasy.app.chat;

import androidx.annotation.Nullable;

public class ChatMessage {

    public enum Role { USER, ASSISTANT, SYSTEM }

    private final Role role;
    private final String content;
    private final long timestamp;

    // Parsed fields for ASSISTANT messages
    @Nullable private String replyText;
    @Nullable private String correctionText;
    @Nullable private String correctionContent;

    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        if (role == Role.ASSISTANT) {
            parseCorrections();
        }
    }

    private void parseCorrections() {
        int separator = content.indexOf("\n---");
        if (separator != -1) {
            replyText = content.substring(0, separator).trim();
            correctionText = content.substring(separator).trim();
            // Extract just the content after "---" header
            String corr = correctionText;
            int firstNewline = corr.indexOf('\n');
            if (firstNewline != -1) {
                correctionContent = corr.substring(firstNewline).trim();
            } else {
                correctionContent = "";
            }
        } else {
            replyText = content;
            correctionText = null;
            correctionContent = null;
        }
    }

    public Role getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }

    @Nullable
    public String getReplyText() { return replyText; }

    @Nullable
    public String getCorrectionText() { return correctionText; }

    @Nullable
    public String getCorrectionContent() { return correctionContent; }

    public boolean hasCorrections() {
        return correctionContent != null && !correctionContent.isEmpty();
    }

    /** Serialize to JSON map for MiMo API (OpenAI format). */
    public org.json.JSONObject toJson() {
        try {
            org.json.JSONObject obj = new org.json.JSONObject();
            String roleStr;
            switch (role) {
                case USER: roleStr = "user"; break;
                case ASSISTANT: roleStr = "assistant"; break;
                default: roleStr = "system"; break;
            }
            obj.put("role", roleStr);
            obj.put("content", content);
            return obj;
        } catch (org.json.JSONException e) {
            return null;
        }
    }
}
