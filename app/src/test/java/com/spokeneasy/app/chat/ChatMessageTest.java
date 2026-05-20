package com.spokeneasy.app.chat;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChatMessageTest {

    @Test
    public void assistantMessage_withCorrections_parsesCorrectly() {
        String content = "Yes, I went to school yesterday.\n"
                + "\n"
                + "---\n"
                + "📝 Correction / 纠正:\n"
                + "• I go → I went — past tense needed for \"yesterday\"\n";
        ChatMessage msg = new ChatMessage(ChatMessage.Role.ASSISTANT, content);

        assertEquals("Yes, I went to school yesterday.", msg.getReplyText());
        assertTrue(msg.hasCorrections());
        assertNotNull(msg.getCorrectionText());
        assertTrue(msg.getCorrectionText().contains("📝 Correction"));
        assertTrue(msg.getCorrectionContent().contains("I go → I went"));
    }

    @Test
    public void assistantMessage_withoutCorrections_returnsFullContentAsReply() {
        String content = "Hello! How can I help you today?";
        ChatMessage msg = new ChatMessage(ChatMessage.Role.ASSISTANT, content);

        assertEquals(content, msg.getReplyText());
        assertFalse(msg.hasCorrections());
        assertNull(msg.getCorrectionText());
        assertNull(msg.getCorrectionContent());
    }

    @Test
    public void userMessage_doesNotParseCorrections() {
        String content = "I go to school yesterday.";
        ChatMessage msg = new ChatMessage(ChatMessage.Role.USER, content);

        assertEquals(content, msg.getContent());
        assertNull(msg.getReplyText());
        assertNull(msg.getCorrectionText());
        assertNull(msg.getCorrectionContent());
    }

    @Test
    public void assistantMessage_emptyCorrections_parsesGracefully() {
        String content = "Reply text.\n---";
        ChatMessage msg = new ChatMessage(ChatMessage.Role.ASSISTANT, content);

        assertEquals("Reply text.", msg.getReplyText());
        assertNotNull(msg.getCorrectionText());
        assertEquals("", msg.getCorrectionContent());
    }

    @Test
    public void toJson_serializesCorrectly() throws Exception {
        ChatMessage msg = new ChatMessage(ChatMessage.Role.USER, "Hello");
        JSONObject json = msg.toJson();

        assertNotNull(json);
        assertEquals("user", json.getString("role"));
        assertEquals("Hello", json.getString("content"));
    }

    @Test
    public void toJson_forAssistant_usesAssistantRole() throws Exception {
        ChatMessage msg = new ChatMessage(ChatMessage.Role.ASSISTANT, "Hi there");
        JSONObject json = msg.toJson();

        assertEquals("assistant", json.getString("role"));
    }

    @Test
    public void constructor_timestampIsSet() {
        long before = System.currentTimeMillis() - 100;
        ChatMessage msg = new ChatMessage(ChatMessage.Role.USER, "test");
        long after = System.currentTimeMillis() + 100;

        assertTrue(msg.getTimestamp() >= before);
        assertTrue(msg.getTimestamp() <= after);
    }
}
