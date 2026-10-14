package dev.jason.gboardpatches.extension.customgallery;

import android.content.ClipDescription;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class GboardCustomGalleryShare {
    private static final String TAG = "CustomGalleryShare";
    private static final String SHARE_CONTENT_DIR = "share_content";

    private GboardCustomGalleryShare() {
    }

    public static boolean shareImage(Context context, File sourceFile) {
        if (context == null || sourceFile == null || !sourceFile.exists()) {
            return false;
        }
        try {
            // 1. Copy into share_content directory matching Gboard's RecentImages pattern
            File shareDir = new File(context.getFilesDir(), SHARE_CONTENT_DIR);
            if (!shareDir.exists()) {
                shareDir.mkdirs();
            }
            File targetFile = new File(shareDir, sourceFile.getName());
            copyFile(sourceFile, targetFile);

            // 2. Resolve FileProvider URI
            String authority = context.getPackageName() + ".fileprovider";
            Uri contentUri = FileProvider.getUriForFile(context, authority, targetFile);
            String mimeType = resolveMimeType(targetFile.getName());

            // 3. Resolve active InputMethodService
            InputMethodService ims = resolveInputMethodService(context);
            if (ims != null) {
                EditorInfo editorInfo = ims.getCurrentInputEditorInfo();
                InputConnection inputConnection = ims.getCurrentInputConnection();
                if (editorInfo != null && inputConnection != null) {
                    // Grant read URI permission to target app
                    if (editorInfo.packageName != null) {
                        try {
                            context.grantUriPermission(
                                    editorInfo.packageName,
                                    contentUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Throwable ignored) {
                        }
                    }

                    // 4. Commit content directly into active text field
                    ClipDescription description = new ClipDescription(
                            targetFile.getName(),
                            new String[]{mimeType});
                    InputContentInfoCompat inputContentInfoCompat = new InputContentInfoCompat(
                            contentUri,
                            description,
                            null);

                    boolean committed = InputConnectionCompat.commitContent(
                            inputConnection,
                            editorInfo,
                            inputContentInfoCompat,
                            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                            null);

                    if (committed) {
                        Log.i(TAG, "Successfully committed image content: " + contentUri);
                        return true;
                    }
                }
            }

            // Fallback: If commitContent could not be handled by target text field, launch chooser
            launchShareIntent(context, contentUri, mimeType);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to share image: " + sourceFile.getAbsolutePath(), t);
            Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static InputMethodService resolveInputMethodService(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper wrapper) {
            if (current instanceof InputMethodService ims) {
                return ims;
            }
            current = wrapper.getBaseContext();
        }
        return GboardCustomGalleryRuntime.getActiveService();
    }

    public static String resolveMimeType(String fileName) {
        if (fileName == null) return "image/jpeg";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }

    private static void copyFile(File src, File dst) throws Exception {
        if (src.getCanonicalPath().equals(dst.getCanonicalPath())) {
            return;
        }
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private static void launchShareIntent(Context context, Uri uri, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent chooser = Intent.createChooser(intent, "Share Image");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }
}
