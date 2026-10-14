package dev.jason.gboardpatches.extension.customgallery;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
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

public final class GboardCustomGalleryRuntime {
    private static final String TAG = "CustomGalleryRuntime";
    public static final String CUSTOM_GALLERY_KEYBOARD_DATA = "custom_gallery";

    private static volatile WeakReference<InputMethodService> activeServiceRef =
            new WeakReference<InputMethodService>(null);
    private static volatile WeakReference<GboardCustomGalleryView> activeGalleryViewRef =
            new WeakReference<GboardCustomGalleryView>(null);

    private GboardCustomGalleryRuntime() {
    }

    public static void rememberService(InputMethodService service) {
        if (service != null) {
            activeServiceRef = new WeakReference<InputMethodService>(service);
        }
    }

    public static InputMethodService getActiveService() {
        return activeServiceRef.get();
    }

    public static Object appendCustomCorpusItem(Object receiver, Object corpusList) {
        if (corpusList == null || !(corpusList instanceof List<?> list) || list.isEmpty()) {
            return corpusList;
        }
        try {
            Context context = resolveContext(receiver, corpusList);
            if (context == null || !GboardCustomGallerySettings.isGalleryEnabled(context)) {
                return corpusList;
            }

            ClassLoader classLoader = corpusList.getClass().getClassLoader();
            if (classLoader == null) {
                return corpusList;
            }

            // Check if already present
            for (Object item : list) {
                if (isCustomGalleryCorpusItem(item)) {
                    return corpusList;
                }
            }

            // Clone first item as template and configure for custom_gallery
            Object template = list.get(0);
            Class<?> itemClass = template.getClass();
            String builderClassName = itemClass.getName() + "$a";
            Class<?> builderClass = Class.forName(builderClassName, false, classLoader);
            Constructor<?> builderConstructor = builderClass.getDeclaredConstructor(itemClass);
            builderConstructor.setAccessible(true);
            Object builder = builderConstructor.newInstance(template);

            // Build custom keyboard type
            Class<?> keyboardTypeClass = Class.forName("ppa", false, classLoader);
            Method keyboardTypeFromString = keyboardTypeClass.getDeclaredMethod("a", String.class);
            keyboardTypeFromString.setAccessible(true);
            Object customKeyboardType = keyboardTypeFromString.invoke(null, CUSTOM_GALLERY_KEYBOARD_DATA);

            // Set default keyboard type
            Method setDefaultKeyboardType = builderClass.getDeclaredMethod("b", keyboardTypeClass);
            setDefaultKeyboardType.setAccessible(true);
            setDefaultKeyboardType.invoke(builder, customKeyboardType);

            // Set eligible keyboard types
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
            // Insert custom gallery near stickers/GIFs (index 2 or 3)
            int insertIndex = Math.min(updatedList.size(), 2);
            updatedList.add(insertIndex, customItem);

            Object rebuilt = immutableListCopyOf.invoke(null, updatedList);
            Log.i(TAG, "Appended custom gallery corpus tab at index " + insertIndex);
            return rebuilt != null ? rebuilt : corpusList;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to append custom gallery corpus item", t);
            return corpusList;
        }
    }

    public static void onExpressionCorpusFooterTabClick(Object footerTabClickConsumer, Object corpusItem) {
        if (footerTabClickConsumer == null || corpusItem == null) {
            return;
        }
        try {
            boolean isGalleryTab = isCustomGalleryCorpusItem(corpusItem);
            View rootView = findRootView(footerTabClickConsumer);

            if (isGalleryTab) {
                Log.i(TAG, "Custom gallery tab clicked; showing gallery view");
                showGalleryView(rootView);
            } else {
                hideGalleryView();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error handling expression corpus footer tab click", t);
        }
    }

    private static boolean isCustomGalleryCorpusItem(Object corpusItem) {
        if (corpusItem == null) return false;
        try {
            for (Field field : corpusItem.getClass().getDeclaredFields()) {
                if ("ppa".equals(field.getType().getSimpleName())
                        || "ppa".equals(field.getType().getName())) {
                    field.setAccessible(true);
                    Object kbType = field.get(corpusItem);
                    if (kbType != null) {
                        for (Field nameField : kbType.getClass().getDeclaredFields()) {
                            if (nameField.getType() == String.class) {
                                nameField.setAccessible(true);
                                String name = (String) nameField.get(kbType);
                                if (CUSTOM_GALLERY_KEYBOARD_DATA.equals(name)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void showGalleryView(View rootView) {
        if (rootView == null) return;
        ViewGroup container = findExpressionBodyContainer(rootView);
        if (container == null) return;

        GboardCustomGalleryView existing = activeGalleryViewRef.get();
        if (existing != null && existing.getParent() == container) {
            existing.setVisibility(View.VISIBLE);
            existing.bringToFront();
            existing.refresh();
            return;
        }

        if (existing != null && existing.getParent() != null) {
            ((ViewGroup) existing.getParent()).removeView(existing);
        }

        GboardCustomGalleryView galleryView = new GboardCustomGalleryView(container.getContext());
        galleryView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(galleryView);
        galleryView.bringToFront();
        activeGalleryViewRef = new WeakReference<GboardCustomGalleryView>(galleryView);
    }

    private static void hideGalleryView() {
        GboardCustomGalleryView existing = activeGalleryViewRef.get();
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
            // Find appropriate container for expression body
            View candidate = rootView.findViewWithTag(".keyboard-base-area");
            if (candidate instanceof ViewGroup cvg) {
                return cvg;
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
                        if (ctx instanceof Context context) {
                            return context;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return getActiveService();
    }
}
