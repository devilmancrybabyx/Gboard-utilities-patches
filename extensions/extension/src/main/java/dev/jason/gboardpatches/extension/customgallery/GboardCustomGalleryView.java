package dev.jason.gboardpatches.extension.customgallery;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GboardCustomGalleryView extends LinearLayout {
    private static final int COLOR_ACCENT = 0xFF1A73E8;
    private static final int COLOR_SURFACE = 0xFF2A2B2E;
    private static final int COLOR_TEXT_PRIMARY = 0xFFEEEEEE;
    private static final int COLOR_TEXT_SECONDARY = 0xFF9E9E9E;
    private static final ExecutorService THUMBNAIL_EXECUTOR = Executors.newFixedThreadPool(4);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private HorizontalScrollView categoryScrollView;
    private LinearLayout categoryContainer;
    private ScrollView gridScrollView;
    private GridLayout imageGrid;
    private TextView emptyTextView;

    public GboardCustomGalleryView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF1E1F22);
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Top bar: Category chips + action buttons
        categoryScrollView = new HorizontalScrollView(getContext());
        categoryScrollView.setHorizontalScrollBarEnabled(false);
        categoryScrollView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(48)));
        categoryScrollView.setPadding(dp(8), dp(6), dp(8), dp(6));

        categoryContainer = new LinearLayout(getContext());
        categoryContainer.setOrientation(HORIZONTAL);
        categoryContainer.setGravity(Gravity.CENTER_VERTICAL);
        categoryScrollView.addView(categoryContainer);
        addView(categoryScrollView);

        // Separator line
        View divider = new View(getContext());
        divider.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        divider.setBackgroundColor(0xFF333333);
        addView(divider);

        // 2. Body: Scrollable image grid or empty text
        FrameLayout bodyContainer = new FrameLayout(getContext());
        bodyContainer.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        gridScrollView = new ScrollView(getContext());
        gridScrollView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        imageGrid = new GridLayout(getContext());
        imageGrid.setColumnCount(4);
        imageGrid.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        imageGrid.setPadding(dp(4), dp(4), dp(4), dp(4));
        gridScrollView.addView(imageGrid);
        bodyContainer.addView(gridScrollView);

        emptyTextView = new TextView(getContext());
        emptyTextView.setTextColor(COLOR_TEXT_SECONDARY);
        emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        emptyTextView.setGravity(Gravity.CENTER);
        emptyTextView.setText("No images in this category.\nTap [+] to import images from gallery.");
        FrameLayout.LayoutParams emptyLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        emptyLp.gravity = Gravity.CENTER;
        emptyTextView.setLayoutParams(emptyLp);
        emptyTextView.setVisibility(GONE);
        bodyContainer.addView(emptyTextView);

        addView(bodyContainer);

        refresh();
    }

    public void refresh() {
        refreshCategories();
        refreshImages();
    }

    private void refreshCategories() {
        categoryContainer.removeAllViews();
        List<String> categories = GboardCustomGalleryStorage.listCategories(getContext());
        String active = GboardCustomGallerySettings.getActiveCategory(getContext());

        for (String cat : categories) {
            boolean isSelected = cat.equalsIgnoreCase(active);
            TextView chip = new TextView(getContext());
            chip.setText(cat);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setTextColor(isSelected ? Color.WHITE : COLOR_TEXT_PRIMARY);
            chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(dp(16));
            shape.setColor(isSelected ? COLOR_ACCENT : COLOR_SURFACE);
            chip.setBackground(shape);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(4), 0, dp(4), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                GboardCustomGallerySettings.setActiveCategory(getContext(), cat);
                refresh();
            });

            chip.setOnLongClickListener(v -> {
                showCategoryOptionsDialog(cat);
                return true;
            });

            categoryContainer.addView(chip);
        }

        // [+] Add Image button
        TextView addImageBtn = createActionButton("[+] Add Image", v -> launchSystemPhotoPicker());
        categoryContainer.addView(addImageBtn);

        // [+] Add Category button
        TextView addCategoryBtn = createActionButton("[+] Category", v -> showCreateCategoryDialog());
        categoryContainer.addView(addCategoryBtn);
    }

    private TextView createActionButton(String text, OnClickListener listener) {
        TextView btn = new TextView(getContext());
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setTextColor(COLOR_ACCENT);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(dp(10), dp(6), dp(10), dp(6));

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(16));
        shape.setColor(0xFF222831);
        shape.setStroke(dp(1), COLOR_ACCENT);
        btn.setBackground(shape);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), 0, dp(4), 0);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(listener);
        return btn;
    }

    private void refreshImages() {
        imageGrid.removeAllViews();
        String active = GboardCustomGallerySettings.getActiveCategory(getContext());
        List<File> images = GboardCustomGalleryStorage.listImages(getContext(), active);

        if (images.isEmpty()) {
            emptyTextView.setVisibility(VISIBLE);
            gridScrollView.setVisibility(GONE);
            return;
        }

        emptyTextView.setVisibility(GONE);
        gridScrollView.setVisibility(VISIBLE);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int itemSize = Math.max(dp(72), (screenWidth - dp(32)) / 4);

        for (File img : images) {
            FrameLayout frame = new FrameLayout(getContext());
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = itemSize;
            glp.height = itemSize;
            glp.setMargins(dp(3), dp(3), dp(3), dp(3));
            frame.setLayoutParams(glp);

            ImageView iv = new ImageView(getContext());
            iv.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(0xFF18191B);
            frame.setBackground(bg);
            iv.setClipToOutline(true);

            frame.addView(iv);
            imageGrid.addView(frame);

            // Async thumbnail load
            THUMBNAIL_EXECUTOR.execute(() -> {
                Bitmap thumb = decodeSampledBitmap(img, itemSize, itemSize);
                if (thumb != null) {
                    mainHandler.post(() -> iv.setImageBitmap(thumb));
                }
            });

            frame.setOnClickListener(v -> {
                boolean sent = GboardCustomGalleryShare.shareImage(getContext(), img);
                if (sent) {
                    Toast.makeText(getContext(), "Image shared", Toast.LENGTH_SHORT).show();
                }
            });

            frame.setOnLongClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Delete Image")
                        .setMessage("Delete this image from " + active + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            GboardCustomGalleryStorage.deleteImage(img);
                            refreshImages();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        }
    }

    private void launchSystemPhotoPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(Intent.createChooser(intent, "Select Pictures").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Toast.makeText(getContext(), "Select images to import into " + GboardCustomGallerySettings.getActiveCategory(getContext()), Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Toast.makeText(getContext(), "Could not launch gallery picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCreateCategoryDialog() {
        EditText input = new EditText(getContext());
        input.setHint("Category name");
        new AlertDialog.Builder(getContext())
                .setTitle("New Category")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        GboardCustomGalleryStorage.createCategory(getContext(), name);
                        GboardCustomGallerySettings.setActiveCategory(getContext(), name);
                        refresh();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCategoryOptionsDialog(String categoryName) {
        if (GboardCustomGallerySettings.DEFAULT_CATEGORY.equalsIgnoreCase(categoryName)) {
            Toast.makeText(getContext(), "Cannot rename or delete Default category", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] options = new CharSequence[]{"Rename", "Delete"};
        new AlertDialog.Builder(getContext())
                .setTitle("Category: " + categoryName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRenameCategoryDialog(categoryName);
                    } else if (which == 1) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Delete Category")
                                .setMessage("Delete \"" + categoryName + "\" and all its images?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    GboardCustomGalleryStorage.deleteCategory(getContext(), categoryName);
                                    refresh();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    private void showRenameCategoryDialog(String oldName) {
        EditText input = new EditText(getContext());
        input.setText(oldName);
        input.setSelection(oldName.length());
        new AlertDialog.Builder(getContext())
                .setTitle("Rename Category")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equalsIgnoreCase(oldName)) {
                        GboardCustomGalleryStorage.renameCategory(getContext(), oldName, newName);
                        refresh();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static Bitmap decodeSampledBitmap(File file, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics());
    }
}
