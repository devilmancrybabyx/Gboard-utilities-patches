package dev.jason.gboardpatches.extension.pins;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GboardPinsSettings {
    public static final String PREF_NAME = "gboard_patches_pins";
    public static final String KEY_PINS_ENABLED = "pref_pins_enabled";
    public static final String KEY_SEARCH_HISTORY = "pref_pins_search_history";
    private static final int MAX_HISTORY_ITEMS = 20;

    private GboardPinsSettings() {
    }

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isPinsEnabled(Context context) {
        if (context == null) return true;
        return preferences(context).getBoolean(KEY_PINS_ENABLED, true);
    }

    public static void setPinsEnabled(Context context, boolean enabled) {
        if (context == null) return;
        preferences(context).edit().putBoolean(KEY_PINS_ENABLED, enabled).apply();
    }

    public static List<String> getSearchHistory(Context context) {
        if (context == null) return Collections.emptyList();
        String jsonStr = preferences(context).getString(KEY_SEARCH_HISTORY, "[]");
        List<String> list = new ArrayList<String>();
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                String item = arr.optString(i, "").trim();
                if (!item.isEmpty() && !list.contains(item)) {
                    list.add(item);
                }
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    public static void recordSearch(Context context, String query) {
        if (context == null || query == null) return;
        String trimmed = query.trim();
        if (trimmed.isEmpty()) return;

        List<String> history = new ArrayList<String>(getSearchHistory(context));
        history.remove(trimmed);
        history.add(0, trimmed);
        if (history.size() > MAX_HISTORY_ITEMS) {
            history = history.subList(0, MAX_HISTORY_ITEMS);
        }

        JSONArray arr = new JSONArray(history);
        preferences(context).edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply();
    }

    public static void clearSearchHistory(Context context) {
        if (context == null) return;
        preferences(context).edit().remove(KEY_SEARCH_HISTORY).apply();
    }
}
