package com.floatingapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "FloatingAppPrefs";

    // Window state
    public static final String KEY_WIN_X = "win_x";
    public static final String KEY_WIN_Y = "win_y";
    public static final String KEY_WIN_W = "win_w";
    public static final String KEY_WIN_H = "win_h";
    public static final String KEY_MINIMIZED = "minimized";
    public static final String KEY_OPACITY = "opacity";
    public static final String KEY_LOCKED = "locked";

    // Settings
    public static final String KEY_AUTO_START = "auto_start";
    public static final String KEY_DEFAULT_SIZE = "default_size"; // small/medium/large
    public static final String KEY_DEFAULT_CORNER = "default_corner"; // tl/tr/bl/br
    public static final String KEY_THEME_COLOR = "theme_color";
    public static final String KEY_WINDOW_TITLE = "window_title";
    public static final String KEY_SNAP_TO_EDGE = "snap_to_edge";
    public static final String KEY_HIGH_CONTRAST = "high_contrast";
    public static final String KEY_LAST_URL = "last_url";
    public static final String KEY_LAST_TAB = "last_tab"; // webview/notes/calc/timer/media

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getInt(String key, int def) { return prefs.getInt(key, def); }
    public void setInt(String key, int val) { prefs.edit().putInt(key, val).apply(); }

    public float getFloat(String key, float def) { return prefs.getFloat(key, def); }
    public void setFloat(String key, float val) { prefs.edit().putFloat(key, val).apply(); }

    public boolean getBoolean(String key, boolean def) { return prefs.getBoolean(key, def); }
    public void setBoolean(String key, boolean val) { prefs.edit().putBoolean(key, val).apply(); }

    public String getString(String key, String def) { return prefs.getString(key, def); }
    public void setString(String key, String val) { prefs.edit().putString(key, val).apply(); }

    // Convenience: window size presets
    public static int[] getSizePreset(String size) {
        switch (size) {
            case "small":  return new int[]{280, 340};
            case "large":  return new int[]{480, 600};
            default:       return new int[]{360, 460}; // medium
        }
    }

    // Convenience: corner positions
    public static int[] getCornerPosition(String corner, int screenW, int screenH, int winW, int winH) {
        int margin = 24;
        switch (corner) {
            case "tr": return new int[]{screenW - winW - margin, margin + 80};
            case "bl": return new int[]{margin, screenH - winH - margin - 80};
            case "br": return new int[]{screenW - winW - margin, screenH - winH - margin - 80};
            default:   return new int[]{margin, margin + 80}; // tl
        }
    }
}
