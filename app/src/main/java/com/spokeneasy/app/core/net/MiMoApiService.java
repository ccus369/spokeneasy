package com.spokeneasy.app.core.net;

import com.spokeneasy.app.chat.ChatMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MiMoApiService {

    private static final String BASE_URL = "https://api.xiaomimimo.com/v1/chat/completions";
    private static final String MODEL = "mimo-v2-flash";
    private static final int TIMEOUT_SECONDS = 30;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final String SYSTEM_PROMPT =
            "You are an English conversation partner for Chinese learners. "
            + "Rules:\n"
            + "1. Respond naturally in English as a conversation partner.\n"
            + "2. Keep responses concise (2-4 sentences).\n"
            + "3. After your response, if the user made grammar/vocabulary errors, "
            + "provide corrections in this format:\n"
            + "---\n"
            + "📝 Correction / 纠正:\n"
            + "• [original] → [corrected] — [explanation]\n"
            + "\n"
            + "💡 More natural / 更地道:\n"
            + "• [natural alternative]\n"
            + "4. If the user writes in Chinese, help translate it to English and respond.\n"
            + "5. Adjust language difficulty to the user's level. Use simpler English for beginners.\n"
            + "6. Be encouraging and patient.";

    private final OkHttpClient client;
    private final ApiConfig apiConfig;

    public MiMoApiService(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Send conversation history to MiMo API and return the response text.
     * Must be called from a background thread.
     */
    public String sendMessage(List<ChatMessage> history) throws IOException, MiMoException {
        String apiKey = apiConfig.getMiMoApiKey();
        if (apiKey.isEmpty()) {
            throw new MiMoException("请先在设置页面配置 MiMo API Key");
        }

        JSONObject body = buildRequestBody(history);
        String jsonBody = body.toString();

        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                String msg = parseErrorMessage(response.code(), errorBody);
                throw new MiMoException(msg);
            }
            String responseBody = response.body() != null ? response.body().string() : "";
            return parseResponse(responseBody);
        }
    }

    JSONObject buildRequestBody(List<ChatMessage> history) throws MiMoException {
        try {
            JSONObject root = new JSONObject();
            root.put("model", MODEL);

            JSONArray messages = new JSONArray();

            // System prompt
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", SYSTEM_PROMPT);
            messages.put(sysMsg);

            // Conversation history (last 20 messages)
            int start = Math.max(0, history.size() - 20);
            for (int i = start; i < history.size(); i++) {
                JSONObject json = history.get(i).toJson();
                if (json != null) {
                    messages.put(json);
                }
            }

            root.put("messages", messages);
            return root;
        } catch (org.json.JSONException e) {
            throw new MiMoException("请求构建失败: " + e.getMessage());
        }
    }

    String parseResponse(String json) throws MiMoException {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray choices = root.getJSONArray("choices");
            if (choices.length() == 0) {
                throw new MiMoException("AI 返回为空");
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            return message.getString("content");
        } catch (org.json.JSONException e) {
            throw new MiMoException("响应解析失败: " + e.getMessage());
        }
    }

    String parseErrorMessage(int code, String body) {
        // Try to extract error from MiMo API response
        try {
            if (body != null && !body.isEmpty()) {
                JSONObject err = new JSONObject(body);
                if (err.has("error")) {
                    JSONObject errorObj = err.getJSONObject("error");
                    return errorObj.optString("message", "API 错误 (" + code + ")");
                }
            }
        } catch (org.json.JSONException ignored) {}
        return "API 请求失败 (" + code + ")";
    }

    public static class MiMoException extends Exception {
        public MiMoException(String message) {
            super(message);
        }
    }
}
