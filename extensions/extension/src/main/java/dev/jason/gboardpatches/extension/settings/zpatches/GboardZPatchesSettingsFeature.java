package dev.jason.gboardpatches.extension.settings.zpatches;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.jason.gboardpatches.extension.R;
import dev.jason.gboardpatches.extension.customgallery.GboardCustomGallerySettings;
import dev.jason.gboardpatches.extension.customgallery.GboardCustomGalleryStorage;
import dev.jason.gboardpatches.extension.recentimageslimit.GboardRecentImagesLimitSettings;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettingsContract;
import dev.jason.gboardpatches.extension.settings.GboardSettingsText;

public final class GboardZPatchesSettingsFeature implements GboardPatchesSettingsContract.Feature {
    public static final String FEATURE_ID = "z_patches";

    @Override
    public String getEntryTitle() {
        return "z-patches";
    }

    @Override
    public String getEntrySummary() {
        return "Recent stickers & GIFs limit and Custom Gallery settings.";
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host.getContext();
        List<GboardPatchesSettingsContract.Section> sections = new ArrayList<GboardPatchesSettingsContract.Section>();

        // 1. Recent Stickers & GIFs Limit Section
        List<GboardPatchesSettingsContract.Row> limitRows = new ArrayList<GboardPatchesSettingsContract.Row>();
        SharedPreferences limitPrefs = GboardRecentImagesLimitSettings.preferences(context);
        int currentLimit = GboardRecentImagesLimitSettings.readLimit(limitPrefs);
        String limitSummaryTemplate = GboardSettingsText.get(context,
                R.string.gboard_patches_recent_images_limit_summary);
        String limitSummary = String.format(limitSummaryTemplate, currentLimit);

        limitRows.add(new GboardPatchesSettingsContract.SelectorRow(
                GboardSettingsText.get(context, R.string.gboard_patches_recent_images_limit_title),
                limitSummary,
                Integer.toString(currentLimit),
                true,
                () -> GboardPatchesSettingsContract.showPositiveIntegerDialog(
                        host,
                        GboardSettingsText.get(context, R.string.gboard_patches_recent_images_limit_dialog_title),
                        GboardSettingsText.get(context, R.string.gboard_patches_recent_images_limit_dialog_hint),
                        currentLimit,
                        value -> {
                            GboardRecentImagesLimitSettings.writeLimit(context, value);
                            GboardPatchesSettingsContract.refresh(host);
                        })));

        sections.add(new GboardPatchesSettingsContract.Section(
                GboardSettingsText.get(context, R.string.gboard_patches_recent_images_limit_section),
                Collections.unmodifiableList(limitRows)));

        // 2. Custom Gallery Section
        List<GboardPatchesSettingsContract.Row> galleryRows = new ArrayList<GboardPatchesSettingsContract.Row>();
        boolean galleryEnabled = GboardCustomGallerySettings.isGalleryEnabled(context);
        String activeCategory = GboardCustomGallerySettings.getActiveCategory(context);

        galleryRows.add(new GboardPatchesSettingsContract.ToggleRow(
                GboardSettingsText.get(context, R.string.gboard_patches_custom_gallery_enabled_title),
                GboardSettingsText.get(context, R.string.gboard_patches_custom_gallery_enabled_summary),
                galleryEnabled,
                true,
                enabled -> {
                    GboardCustomGallerySettings.setGalleryEnabled(context, enabled);
                    GboardPatchesSettingsContract.refresh(host);
                }));

        galleryRows.add(new GboardPatchesSettingsContract.ActionRow(
                GboardSettingsText.get(context, R.string.gboard_patches_custom_gallery_manage_categories_title),
                GboardSettingsText.get(context, R.string.gboard_patches_custom_gallery_manage_categories_summary),
                galleryEnabled,
                () -> showCategoryManagementDialog(host)));

        galleryRows.add(new GboardPatchesSettingsContract.ActionRow(
                GboardSettingsText.get(context, R.string.gboard_patches_custom_gallery_add_image_title),
                "Import pictures into active category: " + activeCategory,
                galleryEnabled,
                () -> importImageAction(host)));

        sections.add(new GboardPatchesSettingsContract.Section(
                GboardSettingsText.get(context, R.string.gboard_patches_custom_gallery_section),
                Collections.unmodifiableList(galleryRows)));

        return new GboardPatchesSettingsContract.Screen(
                "z-patches",
                "Settings for Recent stickers limit and Custom Gallery",
                Collections.unmodifiableList(sections));
    }

    private void showCategoryManagementDialog(GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host.getContext();
        List<String> categories = GboardCustomGalleryStorage.listCategories(context);
        String[] items = new String[categories.size() + 1];
        for (int i = 0; i < categories.size(); i++) {
            items[i] = categories.get(i);
        }
        items[categories.size()] = "[+] Create New Category";

        new AlertDialog.Builder(context)
                .setTitle("Manage Categories")
                .setItems(items, (dialog, which) -> {
                    if (which == categories.size()) {
                        showCreateCategoryDialog(host);
                    } else {
                        showCategoryDetailDialog(host, categories.get(which));
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showCreateCategoryDialog(GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host.getContext();
        EditText input = new EditText(context);
        input.setHint("Category name");
        new AlertDialog.Builder(context)
                .setTitle("Create Category")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        GboardCustomGalleryStorage.createCategory(context, name);
                        GboardCustomGallerySettings.setActiveCategory(context, name);
                        GboardPatchesSettingsContract.refresh(host);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCategoryDetailDialog(GboardPatchesSettingsContract.FeatureHost host, String categoryName) {
        Context context = host.getContext();
        boolean isDefault = GboardCustomGallerySettings.DEFAULT_CATEGORY.equalsIgnoreCase(categoryName);
        CharSequence[] options = isDefault
                ? new CharSequence[]{"Set as Active Category"}
                : new CharSequence[]{"Set as Active Category", "Rename", "Delete"};

        new AlertDialog.Builder(context)
                .setTitle("Category: " + categoryName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        GboardCustomGallerySettings.setActiveCategory(context, categoryName);
                        Toast.makeText(context, "Active category set to: " + categoryName, Toast.LENGTH_SHORT).show();
                        GboardPatchesSettingsContract.refresh(host);
                    } else if (which == 1) {
                        showRenameCategoryDialog(host, categoryName);
                    } else if (which == 2) {
                        new AlertDialog.Builder(context)
                                .setTitle("Delete Category")
                                .setMessage("Delete \"" + categoryName + "\" and all its images?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    GboardCustomGalleryStorage.deleteCategory(context, categoryName);
                                    GboardPatchesSettingsContract.refresh(host);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .setNegativeButton("Back", (dialog, which) -> showCategoryManagementDialog(host))
                .show();
    }

    private void showRenameCategoryDialog(GboardPatchesSettingsContract.FeatureHost host, String oldName) {
        Context context = host.getContext();
        EditText input = new EditText(context);
        input.setText(oldName);
        input.setSelection(oldName.length());
        new AlertDialog.Builder(context)
                .setTitle("Rename Category")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equalsIgnoreCase(oldName)) {
                        GboardCustomGalleryStorage.renameCategory(context, oldName, newName);
                        GboardPatchesSettingsContract.refresh(host);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void importImageAction(GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host.getContext();
        String active = GboardCustomGallerySettings.getActiveCategory(context);
        try {
            host.submit(h -> h.openBinaryDocument(new String[]{"image/*"}, doc -> {
                if (doc != null && doc.data() != null) {
                    File saved = GboardCustomGalleryStorage.saveImage(context, active, doc.name(), doc.data());
                    if (saved != null) {
                        Toast.makeText(context, "Saved image to " + active, Toast.LENGTH_SHORT).show();
                        GboardPatchesSettingsContract.refresh(host);
                    }
                }
            }));
        } catch (Throwable t) {
            // Fallback: launch system photo picker directly
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(Intent.createChooser(intent, "Import Images").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Throwable ignored) {
                Toast.makeText(context, "Could not open photo picker", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
