package dev.jason.gboardpatches.extension.customgallery;

import android.content.Context;
import android.content.SharedPreferences;

public final class GboardCustomGallerySettings {
    public static final String PREF_FILE = "gboard_custom_gallery";
    public static final String PREF_KEY_ENABLED = "pref_custom_gallery_enabled";
    public static final String PREF_KEY_ACTIVE_CATEGORY = "pref_custom_gallery_active_category";
    public static final String DEFAULT_CATEGORY = "Default";

    private GboardCustomGallerySettings() {
    }

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public static boolean isGalleryEnabled(Context context) {
        if (context == null) {
            return true;
        }
        return preferences(context).getBoolean(PREF_KEY_ENABLED, true);
    }

    public static boolean setGalleryEnabled(Context context, boolean enabled) {
        if (context == null) {
            return false;
        }
        return preferences(context).edit()
                .putBoolean(PREF_KEY_ENABLED, enabled)
                .commit();
    }

    public static String getActiveCategory(Context context) {
        if (context == null) {
            return DEFAULT_CATEGORY;
        }
        String category = preferences(context).getString(PREF_KEY_ACTIVE_CATEGORY, DEFAULT_CATEGORY);
        return (category != null && !category.trim().isEmpty()) ? category.trim() : DEFAULT_CATEGORY;
    }

    public static boolean setActiveCategory(Context context, String category) {
        if (context == null) {
            return false;
        }
        String safeName = (category != null && !category.trim().isEmpty()) ? category.trim() : DEFAULT_CATEGORY;
        return preferences(context).edit()
                .putString(PREF_KEY_ACTIVE_CATEGORY, safeName)
                .commit();
    }
}
