package dev.jason.gboardpatches.extension.pins;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GboardPinsRuntime {
    private static final String TAG = "PinsRuntime";
    public static final String PINS_KEYBOARD_DATA = "pins";

    private static volatile WeakReference<GboardPinsView> activePinsViewRef =
            new WeakReference<GboardPinsView>(null);

    private GboardPinsRuntime() {
    }

    public static Object appendPinsCorpusItem(Object receiver, Object corpusList) {
        if (corpusList == null || !(corpusList instanceof List<?> list) || list.isEmpty()) {
            return corpusList;
        }
        try {
            Context context = resolveContext(receiver, corpusList);
            if (context == null || !GboardPinsSettings.isPinsEnabled(context)) {
                return corpusList;
            }

            ClassLoader classLoader = corpusList.getClass().getClassLoader();
            if (classLoader == null) {
                return corpusList;
            }

            for (Object item : list) {
                if (isPinsCorpusItem(item)) {
                    return corpusList;
                }
            }

            Object template = list.get(0);
            Class<?> itemClass = template.getClass();
            String builderClassName = itemClass.getName() + "$a";
            Class<?> builderClass = Class.forName(builderClassName, false, classLoader);
            Constructor<?> builderConstructor = builderClass.getDeclaredConstructor(itemClass);
            builderConstructor.setAccessible(true);
            Object builder = builderConstructor.newInstance(template);

            Class<?> keyboardTypeClass = Class.forName("ppa", false, classLoader);
            Method keyboardTypeFromString = keyboardTypeClass.getDeclaredMethod("a", String.class);
            keyboardTypeFromString.setAccessible(true);
            Object customKeyboardType = keyboardTypeFromString.invoke(null, PINS_KEYBOARD_DATA);

            Method setDefaultKeyboardType = builderClass.getDeclaredMethod("b", keyboardTypeClass);
            setDefaultKeyboardType.setAccessible(true);
            setDefaultKeyboardType.invoke(builder, customKeyboardType);

            Class<?> immutableListClass = Class.forName("vvw", false, classLoader);
            Method immutableListCopyOf = immutableListClass.getDeclaredMethod("o", Iterable.class);
            immutableListCopyOf.setAccessible(true);
            Object eligibleTypes = immutableListCopyOf.invoke(null, Collections.singletonList(customKeyboardType));

            for (Field field : builderClass.getDeclaredFields()) {
                if (field.getType().isAssignableFrom(immutableListClass)) {
                    field.setAccessible(true);
                    field.set(builder, eligibleTypes);
                    break;
                }
            }

            Method buildMethod = builderClass.getDeclaredMethod("a");
            buildMethod.setAccessible(true);
            Object customItem = buildMethod.invoke(builder);

            List<Object> updatedList = new ArrayList<Object>(list);
            int insertIndex = Math.min(updatedList.size(), 3);
            updatedList.add(insertIndex, customItem);

            Object rebuilt = immutableListCopyOf.invoke(null, updatedList);
            Log.i(TAG, "Appended Pins corpus tab at index " + insertIndex);
            return rebuilt != null ? rebuilt : corpusList;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to append pins corpus item", t);
            return corpusList;
        }
    }

    public static boolean onExpressionCorpusFooterTabClick(Object footerTabClickConsumer, Object corpusItem) {
        if (footerTabClickConsumer == null || corpusItem == null) {
            return false;
        }
        try {
            boolean isPinsTab = isPinsCorpusItem(corpusItem);
            View rootView = findRootView(footerTabClickConsumer);

            if (isPinsTab) {
                Log.i(TAG, "Pins tab clicked; showing pins view");
                showPinsView(rootView);
                return true;
            } else {
                hidePinsView();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error handling pins footer tab click", t);
        }
        return false;
    }

    public static boolean isPinsCorpusItem(Object corpusItem) {
        if (corpusItem == null) return false;
        try {
            for (Field field : corpusItem.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object val = field.get(corpusItem);
                if (val != null) {
                    if (PINS_KEYBOARD_DATA.equals(val.toString())) {
                        return true;
                    }
                    for (Field nameField : val.getClass().getDeclaredFields()) {
                        if (nameField.getType() == String.class) {
                            nameField.setAccessible(true);
                            String name = (String) nameField.get(val);
                            if (PINS_KEYBOARD_DATA.equals(name)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static void showPinsView(View rootView) {
        if (rootView == null) return;
        ViewGroup container = findExpressionBodyContainer(rootView);
        if (container == null) return;

        GboardPinsView existing = activePinsViewRef.get();
        if (existing != null && existing.getParent() == container) {
            existing.setVisibility(View.VISIBLE);
            existing.bringToFront();
            existing.refresh();
            return;
        }

        if (existing != null && existing.getParent() != null) {
            ((ViewGroup) existing.getParent()).removeView(existing);
        }

        GboardPinsView pinsView = new GboardPinsView(container.getContext());
        pinsView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(pinsView);
        pinsView.bringToFront();
        activePinsViewRef = new WeakReference<GboardPinsView>(pinsView);
    }

    public static void hidePinsView() {
        GboardPinsView existing = activePinsViewRef.get();
        if (existing != null) {
            existing.setVisibility(View.GONE);
        }
    }

    private static View findRootView(Object consumer) {
        if (consumer instanceof View view) {
            return view.getRootView();
        }
        try {
            for (Field f : consumer.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(consumer);
                if (val instanceof View view) {
                    return view.getRootView();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ViewGroup findExpressionBodyContainer(View rootView) {
        if (rootView instanceof ViewGroup vg) {
            View candidate = rootView.findViewWithTag(".keyboard-body-area");
            if (candidate instanceof ViewGroup cvg) {
                return cvg;
            }
            candidate = rootView.findViewWithTag(".keyboard-base-area");
            if (candidate instanceof ViewGroup cvg) {
                return cvg;
            }
            Context context = rootView.getContext();
            int holderId = context.getResources().getIdentifier("keyboard_holder", "id", context.getPackageName());
            if (holderId != 0) {
                View found = rootView.findViewById(holderId);
                if (found instanceof ViewGroup hvg) {
                    return hvg;
                }
            }
            return vg;
        }
        return null;
    }

    private static Context resolveContext(Object receiver, Object corpusList) {
        if (receiver != null) {
            try {
                for (Field f : receiver.getClass().getDeclaredFields()) {
                    if (Context.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object ctx = f.get(receiver);
                        if (ctx instanceof Context c) {
                            return c;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return dev.jason.gboardpatches.extension.customgallery.GboardCustomGalleryRuntime.getActiveService();
    }
}
