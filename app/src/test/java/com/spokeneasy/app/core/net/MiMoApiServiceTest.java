package com.spokeneasy.app.core.net;

import com.spokeneasy.app.chat.ChatMessage;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MiMoApiServiceTest {

    // --- parseResponse ---

    @Test
    public void parseResponse_validJson_returnsContent() throws Exception {
        String json = "{\"choices\":[{\"message\":{\"content\":\"Hello, how are you?\"}}]}";
        MiMoApiService service = new MiMoApiService(null);

        String result = service.parseResponse(json);

        assertEquals("Hello, how are you?", result);
    }

    @Test(expected = MiMoApiService.MiMoException.class)
    public void parseResponse_emptyChoices_throwsException() throws Exception {
        String json = "{\"choices\":[]}";
        MiMoApiService service = new MiMoApiService(null);

        service.parseResponse(json);
    }

    @Test(expected = MiMoApiService.MiMoException.class)
    public void parseResponse_malformedJson_throwsException() throws Exception {
        String json = "not valid json";
        MiMoApiService service = new MiMoApiService(null);

        service.parseResponse(json);
    }

    @Test(expected = MiMoApiService.MiMoException.class)
    public void parseResponse_missingChoices_throwsException() throws Exception {
        String json = "{\"foo\":\"bar\"}";
        MiMoApiService service = new MiMoApiService(null);

        service.parseResponse(json);
    }

    // --- buildRequestBody ---

    @Test
    public void buildRequestBody_emptyHistory_containsSystemPrompt() throws Exception {
        MiMoApiService service = new MiMoApiService(null);
        List<ChatMessage> history = new ArrayList<>();

        JSONObject body = service.buildRequestBody(history);

        JSONArray messages = body.getJSONArray("messages");
        assertTrue(messages.length() >= 1);
        assertEquals("system", messages.getJSONObject(0).getString("role"));
        assertEquals("mimo-v2-flash", body.getString("model"));
    }

    @Test
    public void buildRequestBody_withMessages_includesAllMessages() throws Exception {
        MiMoApiService service = new MiMoApiService(null);
        List<ChatMessage> history = new ArrayList<>();
        history.add(new ChatMessage(ChatMessage.Role.USER, "Hello"));
        history.add(new ChatMessage(ChatMessage.Role.ASSISTANT, "Hi there"));

        JSONObject body = service.buildRequestBody(history);

        JSONArray messages = body.getJSONArray("messages");
        // system + user + assistant = 3
        assertEquals(3, messages.length());
        assertEquals("user", messages.getJSONObject(1).getString("role"));
        assertEquals("assistant", messages.getJSONObject(2).getString("role"));
    }

    @Test
    public void buildRequestBody_moreThan20Messages_truncatesToLatest20() throws Exception {
        MiMoApiService service = new MiMoApiService(null);
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            history.add(new ChatMessage(ChatMessage.Role.USER, "msg" + i));
        }

        JSONObject body = service.buildRequestBody(history);

        JSONArray messages = body.getJSONArray("messages");
        // system (1) + last 20 = 21
        assertEquals(21, messages.length());
        // The first user message after system should be "msg5" (index 5 in 0-based, since we keep last 20 of 25)
        assertEquals("msg5", messages.getJSONObject(1).getString("content"));
    }

    // --- parseErrorMessage ---

    @Test
    public void parseErrorMessage_withErrorJson_extractsMessage() {
        MiMoApiService service = new MiMoApiService(null);
        String body = "{\"error\":{\"message\":\"Rate limit exceeded\",\"type\":\"rate_limit\"}}";

        String msg = service.parseErrorMessage(429, body);

        assertTrue(msg.contains("Rate limit exceeded"));
    }

    @Test
    public void parseErrorMessage_noErrorJson_returnsGenericMessage() {
        MiMoApiService service = new MiMoApiService(null);

        String msg = service.parseErrorMessage(401, "");

        assertTrue(msg.contains("401"));
    }

    @Test
    public void parseErrorMessage_invalidJsonBody_returnsGenericMessage() {
        MiMoApiService service = new MiMoApiService(null);

        String msg = service.parseErrorMessage(500, "Internal Server Error");

        assertTrue(msg.contains("500"));
    }
}
