package dev.jason.gboardpatches.extension.recentimageslimit;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Method;

public final class GboardRecentImagesLimitRuntime {
    public static final String FLAG_RECENT_CONTENT_SUGGESTION_SHARE_MAX_COUNT =
            "recent_content_suggestion_share_max_count";

    private static volatile Context applicationContext;

    private GboardRecentImagesLimitRuntime() {
    }

    public static Object applyOverriddenFlagValue(String flagName, Object stockResult) {
        if (!FLAG_RECENT_CONTENT_SUGGESTION_SHARE_MAX_COUNT.equals(flagName)
                || !(stockResult instanceof Long)) {
            return stockResult;
        }
        Context context = resolveContext();
        if (context == null) {
            return stockResult;
        }
        SharedPreferences preferences = GboardRecentImagesLimitSettings.preferences(context);
        return GboardRecentImagesLimitSettings.overrideFlagValue(preferences, (Long) stockResult);
    }

    private static Context resolveContext() {
        Context cached = applicationContext;
        if (cached != null) {
            return cached;
        }
        Context reflected = reflectedApplicationContext(
                "android.app.ActivityThread", "currentApplication");
        if (reflected == null) {
            reflected = reflectedApplicationContext(
                    "android.app.AppGlobals", "getInitialApplication");
        }
        if (reflected != null) {
            applicationContext = reflected;
        }
        return reflected;
    }

    private static Context reflectedApplicationContext(String className, String methodName) {
        try {
            Method method = Class.forName(className).getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object application = method.invoke(null);
            if (!(application instanceof Context)) {
                return null;
            }
            Context context = (Context) application;
            Context resolved = context.getApplicationContext();
            return resolved == null ? context : resolved;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
