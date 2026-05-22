package com.spokeneasy.app.shadowing;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ShadowingLoader {

    public static List<ShadowingContent> load(Context context) {
        List<ShadowingContent> list = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("data/listening.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            JSONArray audios = root.getJSONArray("audios");

            for (int i = 0; i < audios.length(); i++) {
                JSONObject obj = audios.getJSONObject(i);
                int id = obj.has("id") ? obj.getInt("id") : i;
                String title = obj.getString("title");
                int level = obj.getInt("level");
                String type = obj.optString("type", "dialogue");
                String dialogText = obj.getString("dialog_text");
                list.add(new ShadowingContent(id, title, level, type, dialogText));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<ShadowingContent> filterByLevel(List<ShadowingContent> items, int level) {
        if (level == 0) return items;
        List<ShadowingContent> filtered = new ArrayList<>();
        for (ShadowingContent item : items) {
            if (item.getLevel() == level) filtered.add(item);
        }
        return filtered;
    }

    public static ShadowingContent findById(List<ShadowingContent> items, int id) {
        for (ShadowingContent item : items) {
            if (item.getId() == id) return item;
        }
        return null;
    }
}
