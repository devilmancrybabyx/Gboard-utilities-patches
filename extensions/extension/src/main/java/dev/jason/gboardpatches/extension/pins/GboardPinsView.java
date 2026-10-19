package dev.jason.gboardpatches.extension.pins;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.jason.gboardpatches.extension.customgallery.GboardCustomGalleryShare;

public final class GboardPinsView extends FrameLayout {
    private static final ExecutorService IMAGE_LOADER = Executors.newFixedThreadPool(4);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final EditText searchEditText;
    private final TextView backButton;
    private final LinearLayout historyChipsLayout;
    private final HorizontalScrollView historyScrollView;
    private final LinearLayout gridLeftColumn;
    private final LinearLayout gridRightColumn;
    private final ProgressBar loadingSpinner;
    private final TextView statusMessage;

    private String currentQuery = "aesthetic wallpaper";
    private final Stack<List<GboardPinsService.PinItem>> navigationHistory =
            new Stack<List<GboardPinsService.PinItem>>();
    private List<GboardPinsService.PinItem> currentPins = new ArrayList<GboardPinsService.PinItem>();

    public GboardPinsView(Context context) {
        super(context);
        setBackgroundColor(0xFF1E1E1E);

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 1. Search & Navigation Header
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(dp(8), dp(6), dp(8), dp(6));
        headerLayout.setBackgroundColor(0xFF252525);

        // Back button (for returning from related pins view)
        backButton = new TextView(context);
        backButton.setText("◀ Back");
        backButton.setTextColor(0xFFE60023); // Pinterest Red
        backButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        backButton.setPadding(dp(6), dp(4), dp(8), dp(4));
        backButton.setVisibility(View.GONE);
        backButton.setOnClickListener(v -> onBackClicked());
        headerLayout.addView(backButton);

        // Search Input Field
        searchEditText = new EditText(context);
        searchEditText.setHint("Search Pinterest Pins...");
        searchEditText.setHintTextColor(0xFF888888);
        searchEditText.setTextColor(0xFFFFFFFF);
        searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        searchEditText.setSingleLine(true);
        searchEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchEditText.setPadding(dp(12), dp(6), dp(12), dp(6));

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(0xFF333333);
        searchBg.setCornerRadius(dp(16));
        searchEditText.setBackground(searchBg);

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        searchParams.setMargins(dp(4), 0, dp(4), 0);
        searchEditText.setLayoutParams(searchParams);

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearch(searchEditText.getText().toString());
                hideSoftKeyboard();
                return true;
            }
            return false;
        });
        headerLayout.addView(searchEditText);

        // Search Action Button
        TextView searchActionBtn = new TextView(context);
        searchActionBtn.setText("Search");
        searchActionBtn.setTextColor(0xFFFFFFFF);
        searchActionBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        searchActionBtn.setGravity(Gravity.CENTER);
        searchActionBtn.setPadding(dp(10), dp(6), dp(10), dp(6));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(0xFFE60023); // Pinterest Red
        btnBg.setCornerRadius(dp(14));
        searchActionBtn.setBackground(btnBg);
        searchActionBtn.setOnClickListener(v -> {
            performSearch(searchEditText.getText().toString());
            hideSoftKeyboard();
        });
        headerLayout.addView(searchActionBtn);

        rootLayout.addView(headerLayout);

        // 2. History Chips Bar
        historyScrollView = new HorizontalScrollView(context);
        historyScrollView.setHorizontalScrollBarEnabled(false);
        historyScrollView.setPadding(dp(6), dp(4), dp(6), dp(4));
        historyScrollView.setBackgroundColor(0xFF222222);

        historyChipsLayout = new LinearLayout(context);
        historyChipsLayout.setOrientation(LinearLayout.HORIZONTAL);
        historyChipsLayout.setGravity(Gravity.CENTER_VERTICAL);
        historyScrollView.addView(historyChipsLayout);
        rootLayout.addView(historyScrollView);

        // 3. Content Area with 2-Column Staggered Grid
        FrameLayout contentFrame = new FrameLayout(context);
        contentFrame.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));

        ScrollView gridScrollView = new ScrollView(context);
        gridScrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout gridContainer = new LinearLayout(context);
        gridContainer.setOrientation(LinearLayout.HORIZONTAL);
        gridContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        gridContainer.setPadding(dp(4), dp(4), dp(4), dp(4));

        gridLeftColumn = new LinearLayout(context);
        gridLeftColumn.setOrientation(LinearLayout.VERTICAL);
        gridLeftColumn.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
        gridLeftColumn.setPadding(dp(2), dp(2), dp(2), dp(2));

        gridRightColumn = new LinearLayout(context);
        gridRightColumn.setOrientation(LinearLayout.VERTICAL);
        gridRightColumn.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
        gridRightColumn.setPadding(dp(2), dp(2), dp(2), dp(2));

        gridContainer.addView(gridLeftColumn);
        gridContainer.addView(gridRightColumn);
        gridScrollView.addView(gridContainer);
        contentFrame.addView(gridScrollView);

        // Loading spinner & Status Message
        loadingSpinner = new ProgressBar(context);
        LayoutParams spinnerParams = new LayoutParams(dp(40), dp(40), Gravity.CENTER);
        loadingSpinner.setLayoutParams(spinnerParams);
        loadingSpinner.setVisibility(View.GONE);
        contentFrame.addView(loadingSpinner);

        statusMessage = new TextView(context);
        statusMessage.setTextColor(0xFFAAAAAA);
        statusMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusMessage.setGravity(Gravity.CENTER);
        statusMessage.setVisibility(View.GONE);
        contentFrame.addView(statusMessage, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        rootLayout.addView(contentFrame);
        addView(rootLayout);

        refreshHistoryChips();
        performSearch(currentQuery);
    }

    public void refresh() {
        refreshHistoryChips();
        if (currentPins.isEmpty()) {
            performSearch(currentQuery);
        }
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        final String cleanQuery = query.trim();
        this.currentQuery = cleanQuery;
        this.navigationHistory.clear();
        backButton.setVisibility(View.GONE);

        GboardPinsSettings.recordSearch(getContext(), cleanQuery);
        refreshHistoryChips();

        setLoading(true);
        GboardPinsService.searchPins(cleanQuery, new GboardPinsService.Callback<List<GboardPinsService.PinItem>>() {
            @Override
            public void onSuccess(List<GboardPinsService.PinItem> result) {
                setLoading(false);
                displayPins(result);
            }

            @Override
            public void onError(Throwable throwable) {
                setLoading(false);
                showStatus("Failed to load pins. Check your connection.");
            }
        });
    }

    private void openRelatedPins(GboardPinsService.PinItem pin) {
        if (pin == null) return;
        navigationHistory.push(new ArrayList<GboardPinsService.PinItem>(currentPins));
        backButton.setVisibility(View.VISIBLE);

        setLoading(true);
        GboardPinsService.searchRelatedPins(pin.id, pin.title, new GboardPinsService.Callback<List<GboardPinsService.PinItem>>() {
            @Override
            public void onSuccess(List<GboardPinsService.PinItem> result) {
                setLoading(false);
                displayPins(result);
            }

            @Override
            public void onError(Throwable throwable) {
                setLoading(false);
                showStatus("No related pins found.");
            }
        });
    }

    private void onBackClicked() {
        if (!navigationHistory.isEmpty()) {
            List<GboardPinsService.PinItem> previous = navigationHistory.pop();
            displayPins(previous);
            if (navigationHistory.isEmpty()) {
                backButton.setVisibility(View.GONE);
            }
        } else {
            backButton.setVisibility(View.GONE);
        }
    }

    private void displayPins(List<GboardPinsService.PinItem> pins) {
        this.currentPins = pins != null ? pins : new ArrayList<GboardPinsService.PinItem>();
        gridLeftColumn.removeAllViews();
        gridRightColumn.removeAllViews();

        if (currentPins.isEmpty()) {
            showStatus("No pins found.");
            return;
        }
        hideStatus();

        for (int i = 0; i < currentPins.size(); i++) {
            GboardPinsService.PinItem pin = currentPins.get(i);
            View card = createPinCard(pin);
            if (i % 2 == 0) {
                gridLeftColumn.addView(card);
            } else {
                gridRightColumn.addView(card);
            }
        }
    }

    private View createPinCard(final GboardPinsService.PinItem pin) {
        Context context = getContext();
        FrameLayout card = new FrameLayout(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(dp(2), dp(3), dp(2), dp(3));
        card.setLayoutParams(cardParams);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF2C2C2C);
        cardBg.setCornerRadius(dp(10));
        card.setBackground(cardBg);
        card.setClipToOutline(true);

        // Pin Image View
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(150)));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(imageView);

        // Loading indicator for image
        ProgressBar imageSpinner = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
        LayoutParams spinnerParams = new LayoutParams(dp(24), dp(24), Gravity.CENTER);
        imageSpinner.setLayoutParams(spinnerParams);
        card.addView(imageSpinner);

        // Async Image Loading
        loadImageAsync(pin.thumbnailUrl, pin.id, imageView, imageSpinner);

        // Click on entire card -> Opens related pictures as in Pinterest!
        card.setOnClickListener(v -> openRelatedPins(pin));

        // Tiny send logo overlaid at bottom-right corner
        FrameLayout sendButton = createSendButton(context, pin);
        LayoutParams sendParams = new LayoutParams(dp(34), dp(34), Gravity.BOTTOM | Gravity.END);
        sendParams.setMargins(0, 0, dp(6), dp(6));
        sendButton.setLayoutParams(sendParams);
        card.addView(sendButton);

        return card;
    }

    private FrameLayout createSendButton(final Context context, final GboardPinsService.PinItem pin) {
        FrameLayout sendBtn = new FrameLayout(context);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setShape(GradientDrawable.OVAL);
        btnBg.setColor(0xFFE60023); // Pinterest red accent
        sendBtn.setBackground(btnBg);

        ImageView sendIcon = new ImageView(context);
        sendIcon.setLayoutParams(new LayoutParams(dp(18), dp(18), Gravity.CENTER));
        sendIcon.setImageBitmap(createSendIconBitmap(dp(18)));
        sendBtn.addView(sendIcon);

        sendBtn.setOnClickListener(v -> {
            Toast.makeText(context, "Sending pin...", Toast.LENGTH_SHORT).show();
            IMAGE_LOADER.execute(() -> {
                File cached = GboardPinsStorage.downloadImageToCache(context, pin.imageUrl, pin.id);
                if (cached != null) {
                    File shareFile = GboardPinsStorage.prepareForShare(context, cached, pin.id);
                    if (shareFile != null) {
                        MAIN_HANDLER.post(() -> GboardCustomGalleryShare.shareImage(context, shareFile));
                    }
                }
            });
        });

        return sendBtn;
    }

    private Bitmap createSendIconBitmap(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(dp(1));

        Path path = new Path();
        float s = (float) size;
        path.moveTo(s * 0.15f, s * 0.15f);
        path.lineTo(s * 0.85f, s * 0.5f);
        path.lineTo(s * 0.15f, s * 0.85f);
        path.lineTo(s * 0.35f, s * 0.5f);
        path.close();
        canvas.drawPath(path, paint);
        return bitmap;
    }

    private void loadImageAsync(final String url, final String id, final ImageView imageView, final ProgressBar spinner) {
        IMAGE_LOADER.execute(() -> {
            File file = GboardPinsStorage.downloadImageToCache(getContext(), url, id);
            if (file != null && file.exists()) {
                try {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = 1;
                    Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                    if (bmp != null) {
                        MAIN_HANDLER.post(() -> {
                            spinner.setVisibility(View.GONE);
                            imageView.setImageBitmap(bmp);
                        });
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
            MAIN_HANDLER.post(() -> spinner.setVisibility(View.GONE));
        });
    }

    private void refreshHistoryChips() {
        historyChipsLayout.removeAllViews();
        List<String> history = GboardPinsSettings.getSearchHistory(getContext());
        if (history.isEmpty()) {
            historyScrollView.setVisibility(View.GONE);
            return;
        }
        historyScrollView.setVisibility(View.VISIBLE);

        for (final String term : history) {
            TextView chip = new TextView(getContext());
            chip.setText(term);
            chip.setTextColor(0xFFDDDDDD);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setPadding(dp(10), dp(4), dp(10), dp(4));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(0xFF333333);
            chipBg.setCornerRadius(dp(12));
            chip.setBackground(chipBg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(3), 0, dp(3), 0);
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> {
                searchEditText.setText(term);
                performSearch(term);
            });
            historyChipsLayout.addView(chip);
        }

        // Add clear history button chip
        TextView clearChip = new TextView(getContext());
        clearChip.setText("✖ Clear");
        clearChip.setTextColor(0xFF888888);
        clearChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        clearChip.setPadding(dp(8), dp(4), dp(8), dp(4));
        clearChip.setOnClickListener(v -> {
            GboardPinsSettings.clearSearchHistory(getContext());
            refreshHistoryChips();
        });
        historyChipsLayout.addView(clearChip);
    }

    private void setLoading(boolean loading) {
        loadingSpinner.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            statusMessage.setVisibility(View.GONE);
        }
    }

    private void showStatus(String message) {
        statusMessage.setText(message);
        statusMessage.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusMessage.setVisibility(View.GONE);
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
