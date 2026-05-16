package com.spokeneasy.app.core.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public final class UuidManager {

    private static final String PREFS_NAME = "spokeneasy_prefs";
    private static final String KEY_DEVICE_UUID = "device_uuid";

    private static String cachedUuid;

    private UuidManager() {}

    public static String getDeviceUuid(Context context) {
        if (cachedUuid != null) {
            return cachedUuid;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uuid = prefs.getString(KEY_DEVICE_UUID, null);

        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_UUID, uuid).apply();
        }

        cachedUuid = uuid;
        return uuid;
    }
}
