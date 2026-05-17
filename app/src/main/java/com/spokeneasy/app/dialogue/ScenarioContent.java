package com.spokeneasy.app.dialogue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ScenarioContent {

    public static class Scenario {
        public final String id;
        public final String title;
        public final String titleEn;
        public final String description;
        public final String systemPrompt;
        public final List<WarmupWord> warmupWords;
        public final List<DialogueLine> dialogueLines;
        public final List<String> patternSentences;

        Scenario(JSONObject obj) throws JSONException {
            id = obj.getString("id");
            title = obj.getString("title");
            titleEn = obj.optString("title_en", "");
            description = obj.optString("description", "");
            systemPrompt = obj.optString("system_prompt", "");

            JSONArray warmupArr = obj.getJSONArray("warmup_words");
            warmupWords = new ArrayList<>(warmupArr.length());
            for (int i = 0; i < warmupArr.length(); i++) {
                warmupWords.add(new WarmupWord(warmupArr.getJSONObject(i)));
            }

            JSONArray dialogArr = obj.getJSONArray("dialogue_lines");
            dialogueLines = new ArrayList<>(dialogArr.length());
            for (int i = 0; i < dialogArr.length(); i++) {
                dialogueLines.add(new DialogueLine(dialogArr.getJSONObject(i)));
            }

            JSONArray patternArr = obj.optJSONArray("pattern_sentences");
            patternSentences = new ArrayList<>();
            if (patternArr != null) {
                for (int i = 0; i < patternArr.length(); i++) {
                    patternSentences.add(patternArr.getString(i));
                }
            }
        }
    }

    public static class WarmupWord {
        public final String word;
        public final String phonetic;
        public final String meaningCn;

        WarmupWord(JSONObject obj) throws JSONException {
            word = obj.getString("word");
            phonetic = obj.optString("phonetic", "");
            meaningCn = obj.optString("meaning_cn", "");
        }
    }

    public static class DialogueLine {
        public final String speaker;
        public final String text;
        public final String translation;

        DialogueLine(JSONObject obj) throws JSONException {
            speaker = obj.getString("speaker");
            text = obj.getString("text");
            translation = obj.optString("translation", "");
        }
    }

    public static List<Scenario> parseAll(String jsonString) throws JSONException {
        JSONArray arr = new JSONArray(jsonString);
        List<Scenario> list = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            list.add(new Scenario(arr.getJSONObject(i)));
        }
        return list;
    }
}
