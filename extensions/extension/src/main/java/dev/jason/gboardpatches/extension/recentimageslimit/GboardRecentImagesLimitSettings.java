package dev.jason.gboardpatches.extension.recentimageslimit;

import android.content.Context;
import android.content.SharedPreferences;

public final class GboardRecentImagesLimitSettings {
    public static final String PREF_FILE = "gboard_recent_images_limit";
    public static final String PREF_KEY_LIMIT = "pref_recent_images_limit";
    public static final int DEFAULT_LIMIT = 10;
    public static final int MIN_LIMIT = 1;
    public static final int MAX_LIMIT = 5000;

    private GboardRecentImagesLimitSettings() {
    }

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public static int readLimit(SharedPreferences preferences) {
        if (preferences == null) {
            return DEFAULT_LIMIT;
        }
        Object raw = preferences.getAll().get(PREF_KEY_LIMIT);
        int value = DEFAULT_LIMIT;
        if (raw instanceof Number number) {
            value = number.intValue();
        } else if (raw instanceof String stringValue) {
            try {
                value = Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                value = DEFAULT_LIMIT;
            }
        }
        if (value < MIN_LIMIT || value > MAX_LIMIT) {
            return DEFAULT_LIMIT;
        }
        return value;
    }

    public static boolean writeLimit(Context context, int limit) {
        if (context == null || limit < MIN_LIMIT || limit > MAX_LIMIT) {
            return false;
        }
        return preferences(context).edit()
                .putInt(PREF_KEY_LIMIT, limit)
                .commit();
    }

    public static Object overrideFlagValue(SharedPreferences preferences, Long stockResult) {
        int customLimit = readLimit(preferences);
        if (customLimit != DEFAULT_LIMIT) {
            return (long) customLimit;
        }
        return stockResult != null ? stockResult : (long) DEFAULT_LIMIT;
    }
}
