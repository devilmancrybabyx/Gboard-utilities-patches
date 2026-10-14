package dev.jason.gboardpatches.extension.customgallery;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class GboardCustomGalleryStorage {
    private static final String TAG = "CustomGalleryStorage";
    private static final String GALLERY_DIR_NAME = "custom_gallery";
    private static final String DEFAULT_CATEGORY = GboardCustomGallerySettings.DEFAULT_CATEGORY;

    private GboardCustomGalleryStorage() {
    }

    public static File getGalleryRootDir(Context context) {
        File dir = new File(context.getFilesDir(), GALLERY_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String sanitizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return DEFAULT_CATEGORY;
        }
        String clean = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return clean.isEmpty() ? DEFAULT_CATEGORY : clean;
    }

    public static List<String> listCategories(Context context) {
        File root = getGalleryRootDir(context);
        File[] dirs = root.listFiles(File::isDirectory);
        List<String> result = new ArrayList<String>();
        if (dirs != null) {
            for (File d : dirs) {
                result.add(d.getName());
            }
        }
        if (result.isEmpty()) {
            createCategory(context, DEFAULT_CATEGORY);
            result.add(DEFAULT_CATEGORY);
        }
        Collections.sort(result, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                if (DEFAULT_CATEGORY.equalsIgnoreCase(a)) return -1;
                if (DEFAULT_CATEGORY.equalsIgnoreCase(b)) return 1;
                return a.compareToIgnoreCase(b);
            }
        });
        return result;
    }

    public static File getCategoryDir(Context context, String categoryName) {
        String safeName = sanitizeName(categoryName);
        File dir = new File(getGalleryRootDir(context), safeName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static boolean createCategory(Context context, String categoryName) {
        String safeName = sanitizeName(categoryName);
        File dir = new File(getGalleryRootDir(context), safeName);
        return dir.exists() || dir.mkdirs();
    }

    public static boolean renameCategory(Context context, String oldName, String newName) {
        String safeOld = sanitizeName(oldName);
        String safeNew = sanitizeName(newName);
        if (safeOld.equals(safeNew)) {
            return true;
        }
        File oldDir = new File(getGalleryRootDir(context), safeOld);
        File newDir = new File(getGalleryRootDir(context), safeNew);
        if (!oldDir.exists() || newDir.exists()) {
            return false;
        }
        boolean success = oldDir.renameTo(newDir);
        if (success && safeOld.equals(GboardCustomGallerySettings.getActiveCategory(context))) {
            GboardCustomGallerySettings.setActiveCategory(context, safeNew);
        }
        return success;
    }

    public static boolean deleteCategory(Context context, String categoryName) {
        String safeName = sanitizeName(categoryName);
        File dir = new File(getGalleryRootDir(context), safeName);
        if (!dir.exists()) {
            return false;
        }
        deleteRecursively(dir);
        if (safeName.equals(GboardCustomGallerySettings.getActiveCategory(context))) {
            GboardCustomGallerySettings.setActiveCategory(context, DEFAULT_CATEGORY);
        }
        return true;
    }

    public static List<File> listImages(Context context, String categoryName) {
        File dir = getCategoryDir(context, categoryName);
        File[] files = dir.listFiles(file -> {
            if (file.isDirectory()) return false;
            String name = file.getName().toLowerCase();
            return name.endsWith(".jpg") || name.endsWith(".jpeg")
                    || name.endsWith(".png") || name.endsWith(".gif")
                    || name.endsWith(".webp") || name.endsWith(".bmp");
        });
        if (files == null) {
            return Collections.emptyList();
        }
        List<File> list = new ArrayList<File>(Arrays.asList(files));
        Collections.sort(list, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return list;
    }

    public static File saveImage(Context context, String categoryName, String originalName, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            File catDir = getCategoryDir(context, categoryName);
            String ext = extractExtension(originalName);
            String fileName = "img_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000) + ext;
            File target = new File(catDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(data);
            }
            return target;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to save image bytes", t);
            return null;
        }
    }

    public static File importImageFromUri(Context context, String categoryName, Uri sourceUri) {
        if (context == null || sourceUri == null) {
            return null;
        }
        try {
            File catDir = getCategoryDir(context, categoryName);
            String ext = ".jpg";
            String mime = context.getContentResolver().getType(sourceUri);
            if (mime != null) {
                if (mime.contains("gif")) ext = ".gif";
                else if (mime.contains("png")) ext = ".png";
                else if (mime.contains("webp")) ext = ".webp";
            }
            String fileName = "img_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000) + ext;
            File target = new File(catDir, fileName);
            try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(target)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return target;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to import image from URI: " + sourceUri, t);
            return null;
        }
    }

    public static boolean deleteImage(File file) {
        return file != null && file.exists() && file.delete();
    }

    private static String extractExtension(String name) {
        if (name == null) return ".jpg";
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot).toLowerCase();
        }
        return ".jpg";
    }

    private static void deleteRecursively(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        fileOrDir.delete();
    }
}
