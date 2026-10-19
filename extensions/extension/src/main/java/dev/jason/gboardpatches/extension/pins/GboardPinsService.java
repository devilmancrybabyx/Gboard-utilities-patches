package dev.jason.gboardpatches.extension.pins;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GboardPinsService {
    private static final String TAG = "PinsService";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static final class PinItem {
        public final String id;
        public final String title;
        public final String imageUrl;
        public final String thumbnailUrl;

        public PinItem(String id, String title, String imageUrl, String thumbnailUrl) {
            this.id = id;
            this.title = title != null ? title : "";
            this.imageUrl = imageUrl != null ? imageUrl : "";
            this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : this.imageUrl;
        }
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }

    private GboardPinsService() {
    }

    public static void searchPins(final String query, final Callback<List<PinItem>> callback) {
        EXECUTOR.execute(() -> {
            try {
                List<PinItem> results = executePinterestSearch(query);
                if (results.isEmpty()) {
                    results = executeFallbackSearch(query);
                }
                final List<PinItem> finalResults = results;
                MAIN_HANDLER.post(() -> callback.onSuccess(finalResults));
            } catch (Throwable t) {
                Log.w(TAG, "Search failed for: " + query, t);
                MAIN_HANDLER.post(() -> {
                    try {
                        List<PinItem> fallback = executeFallbackSearch(query);
                        callback.onSuccess(fallback);
                    } catch (Throwable fallbackError) {
                        callback.onError(t);
                    }
                });
            }
        });
    }

    public static void searchRelatedPins(final String pinId, final String title, final Callback<List<PinItem>> callback) {
        EXECUTOR.execute(() -> {
            try {
                List<PinItem> results = executeRelatedPinsSearch(pinId);
                if (results.isEmpty() && title != null && !title.isEmpty()) {
                    results = executePinterestSearch(title);
                }
                if (results.isEmpty()) {
                    results = executeFallbackSearch(title != null ? title : "aesthetic wallpaper");
                }
                final List<PinItem> finalResults = results;
                MAIN_HANDLER.post(() -> callback.onSuccess(finalResults));
            } catch (Throwable t) {
                Log.w(TAG, "Related pins search failed for: " + pinId, t);
                MAIN_HANDLER.post(() -> {
                    try {
                        List<PinItem> fallback = executeFallbackSearch(title != null ? title : "aesthetic wallpaper");
                        callback.onSuccess(fallback);
                    } catch (Throwable fallbackError) {
                        callback.onError(t);
                    }
                });
            }
        });
    }

    private static List<PinItem> executePinterestSearch(String query) throws Exception {
        List<PinItem> list = new ArrayList<PinItem>();
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        String dataPayload = "{\"options\":{\"query\":\"" + query.replace("\"", "\\\"") + "\",\"scope\":\"pins\",\"page_size\":30},\"context\":{}}";
        String encodedData = URLEncoder.encode(dataPayload, StandardCharsets.UTF_8.name());
        String endpoint = "https://www.pinterest.com/resource/BaseSearchResource/get/?source_url=/search/pins/?q=" + encoded + "&data=" + encodedData;

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
        conn.connect();

        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            String jsonStr = readStream(conn.getInputStream());
            JSONObject root = new JSONObject(jsonStr);
            JSONObject resourceResponse = root.optJSONObject("resource_response");
            if (resourceResponse != null) {
                JSONObject data = resourceResponse.optJSONObject("data");
                if (data != null) {
                    JSONArray results = data.optJSONArray("results");
                    if (results != null) {
                        parsePinterestResults(results, list);
                    }
                }
            }
        }
        conn.disconnect();
        return list;
    }

    private static List<PinItem> executeRelatedPinsSearch(String pinId) throws Exception {
        List<PinItem> list = new ArrayList<PinItem>();
        if (pinId == null || pinId.isEmpty()) return list;

        String dataPayload = "{\"options\":{\"pin_id\":\"" + pinId + "\",\"page_size\":30},\"context\":{}}";
        String encodedData = URLEncoder.encode(dataPayload, StandardCharsets.UTF_8.name());
        String endpoint = "https://www.pinterest.com/resource/RelatedPinFeedResource/get/?source_url=/pin/" + pinId + "/&data=" + encodedData;

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.connect();

        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            String jsonStr = readStream(conn.getInputStream());
            JSONObject root = new JSONObject(jsonStr);
            JSONObject resourceResponse = root.optJSONObject("resource_response");
            if (resourceResponse != null) {
                JSONObject data = resourceResponse.optJSONObject("data");
                if (data != null) {
                    JSONArray results = data.optJSONArray("results");
                    if (results != null) {
                        parsePinterestResults(results, list);
                    }
                }
            }
        }
        conn.disconnect();
        return list;
    }

    private static void parsePinterestResults(JSONArray results, List<PinItem> out) {
        for (int i = 0; i < results.length(); i++) {
            JSONObject pin = results.optJSONObject(i);
            if (pin == null) continue;
            String id = pin.optString("id", "");
            String title = pin.optString("grid_title", "");
            if (title.isEmpty()) {
                title = pin.optString("title", "");
            }
            if (title.isEmpty()) {
                title = pin.optString("description", "");
            }

            JSONObject images = pin.optJSONObject("images");
            if (images != null) {
                String origUrl = null;
                String thumbUrl = null;
                if (images.has("orig")) {
                    origUrl = images.optJSONObject("orig").optString("url", "");
                }
                if (origUrl == null || origUrl.isEmpty()) {
                    if (images.has("736x")) {
                        origUrl = images.optJSONObject("736x").optString("url", "");
                    }
                }
                if (images.has("474x")) {
                    thumbUrl = images.optJSONObject("474x").optString("url", "");
                } else if (images.has("236x")) {
                    thumbUrl = images.optJSONObject("236x").optString("url", "");
                }
                if (origUrl != null && !origUrl.isEmpty()) {
                    out.add(new PinItem(id, title, origUrl, thumbUrl != null ? thumbUrl : origUrl));
                }
            }
        }
    }

    private static List<PinItem> executeFallbackSearch(String query) {
        List<PinItem> list = new ArrayList<PinItem>();
        try {
            // Direct scraping of Pinterest public search page for image tags
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String url = "https://www.pinterest.com/search/pins/?q=" + encoded;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String html = readStream(conn.getInputStream());
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("https://i\\.pinimg\\.com/[^\"'\\s<>]+");
                java.util.regex.Matcher matcher = pattern.matcher(html);
                int count = 0;
                while (matcher.find() && count < 30) {
                    String imgUrl = matcher.group(0);
                    if (imgUrl.contains("/236x/") || imgUrl.contains("/474x/") || imgUrl.contains("/736x/")) {
                        String fullImg = imgUrl.replace("/236x/", "/736x/").replace("/474x/", "/736x/");
                        String pinId = "pin_" + Integer.toHexString(imgUrl.hashCode());
                        list.add(new PinItem(pinId, query, fullImg, imgUrl));
                        count++;
                    }
                }
            }
            conn.disconnect();
        } catch (Throwable ignored) {
        }
        return list;
    }

    private static String readStream(InputStream in) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
