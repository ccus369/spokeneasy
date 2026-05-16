package com.spokeneasy.app.core.net;

import android.content.Context;
import android.content.SharedPreferences;

public class ApiConfig {

    private static final String PREFS_NAME = "spokeneasy_prefs";
    private static final String KEY_MIMO_API_KEY = "mimo_api_key";

    private final SharedPreferences prefs;

    public ApiConfig(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getMiMoApiKey() {
        return prefs.getString(KEY_MIMO_API_KEY, "");
    }

    public void setMiMoApiKey(String key) {
        prefs.edit().putString(KEY_MIMO_API_KEY, key).apply();
    }

    public boolean hasMiMoApiKey() {
        return !getMiMoApiKey().isEmpty();
    }
}
