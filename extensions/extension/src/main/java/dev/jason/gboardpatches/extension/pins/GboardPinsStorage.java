package dev.jason.gboardpatches.extension.pins;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class GboardPinsStorage {
    private static final String TAG = "PinsStorage";
    private static final String PINS_CACHE_DIR = "pins_cache";
    private static final String SHARE_CONTENT_DIR = "share_content";

    private GboardPinsStorage() {
    }

    public static File getPinsCacheDir(Context context) {
        File dir = new File(context.getCacheDir(), PINS_CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File downloadImageToCache(Context context, String imageUrl, String pinId) {
        if (context == null || imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        try {
            String safeId = (pinId != null && !pinId.isEmpty()) ? pinId : Integer.toHexString(imageUrl.hashCode());
            String ext = resolveExtension(imageUrl);
            File cacheFile = new File(getPinsCacheDir(context), "pin_" + safeId + ext);
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return cacheFile;
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11)");
            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                return null;
            }

            try (InputStream in = conn.getInputStream();
                 OutputStream out = new FileOutputStream(cacheFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                conn.disconnect();
            }

            return cacheFile;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to download pin image: " + imageUrl, t);
            return null;
        }
    }

    public static File prepareForShare(Context context, File cachedFile, String pinId) {
        if (context == null || cachedFile == null || !cachedFile.exists()) {
            return null;
        }
        try {
            File shareDir = new File(context.getFilesDir(), SHARE_CONTENT_DIR);
            if (!shareDir.exists()) {
                shareDir.mkdirs();
            }
            String ext = resolveExtension(cachedFile.getName());
            String fileName = "pin_" + (pinId != null ? pinId : System.currentTimeMillis()) + ext;
            File targetFile = new File(shareDir, fileName);

            try (InputStream in = new FileInputStream(cachedFile);
                 OutputStream out = new FileOutputStream(targetFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            return targetFile;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to prepare pin for share", t);
            return null;
        }
    }

    private static String resolveExtension(String path) {
        if (path == null) return ".jpg";
        String lower = path.toLowerCase();
        if (lower.contains(".gif")) return ".gif";
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".webp")) return ".webp";
        return ".jpg";
    }
}
