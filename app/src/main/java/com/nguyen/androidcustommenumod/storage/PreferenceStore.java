package com.nguyen.androidcustommenumod.storage;

import android.content.Context;
import android.content.SharedPreferences;

public final class PreferenceStore {
    private static final String FILE_NAME = "nebula_menu_preferences";
    private final SharedPreferences preferences;

    public PreferenceStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public boolean getBoolean(String key, boolean fallback) {
        return preferences.getBoolean(key, fallback);
    }

    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    public float getFloat(String key, float fallback) {
        return preferences.getFloat(key, fallback);
    }

    public void putFloat(String key, float value) {
        preferences.edit().putFloat(key, value).apply();
    }

    public int getInt(String key, int fallback) {
        return preferences.getInt(key, fallback);
    }

    public void putInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    public void clearMenuValues() {
        int bubbleX = getInt("bubble_x", 24);
        int bubbleY = getInt("bubble_y", 180);
        preferences.edit().clear()
                .putInt("bubble_x", bubbleX)
                .putInt("bubble_y", bubbleY)
                .apply();
    }
}
