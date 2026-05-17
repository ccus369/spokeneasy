package com.spokeneasy.app.pronunciation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PronunciationContent {

    public static class PhonemeCategory {
        public final String key;
        public final String nameCn;
        public final String nameEn;
        public final String description;
        public final int order;

        PhonemeCategory(JSONObject obj) throws JSONException {
            key = obj.getString("key");
            nameCn = obj.getString("name_cn");
            nameEn = obj.getString("name_en");
            description = obj.optString("description", "");
            order = obj.optInt("order", 99);
        }
    }

    public static class MinimalPair {
        public final String id;
        public final String category;
        public final String wordA;
        public final String wordB;
        public final String phonemeA;
        public final String phonemeB;
        public final String sentenceA;
        public final String sentenceB;
        public final String tipCn;

        MinimalPair(JSONObject obj) throws JSONException {
            id = obj.getString("id");
            category = obj.getString("category");
            wordA = obj.getString("word_a");
            wordB = obj.getString("word_b");
            phonemeA = obj.optString("phoneme_a", "");
            phonemeB = obj.optString("phoneme_b", "");
            sentenceA = obj.optString("sentence_a", "");
            sentenceB = obj.optString("sentence_b", "");
            tipCn = obj.optString("tip_cn", "");
        }
    }

    public static class MinimalPairsData {
        public final List<PhonemeCategory> categories;
        public final List<MinimalPair> pairs;

        MinimalPairsData(JSONObject root) throws JSONException {
            JSONArray catArr = root.getJSONArray("categories");
            categories = new ArrayList<>(catArr.length());
            for (int i = 0; i < catArr.length(); i++) {
                categories.add(new PhonemeCategory(catArr.getJSONObject(i)));
            }

            JSONArray pairArr = root.getJSONArray("pairs");
            pairs = new ArrayList<>(pairArr.length());
            for (int i = 0; i < pairArr.length(); i++) {
                pairs.add(new MinimalPair(pairArr.getJSONObject(i)));
            }
        }

        /** Get pairs belonging to a specific category */
        public List<MinimalPair> getPairsByCategory(String categoryKey) {
            List<MinimalPair> result = new ArrayList<>();
            for (MinimalPair p : pairs) {
                if (p.category.equals(categoryKey)) {
                    result.add(p);
                }
            }
            return result;
        }
    }

    /** Parse from a JSON string (contents of minimal_pairs.json) */
    public static MinimalPairsData parse(String jsonString) throws JSONException {
        return new MinimalPairsData(new JSONObject(jsonString));
    }
}
