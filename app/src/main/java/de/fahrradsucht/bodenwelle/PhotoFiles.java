package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Only recompressed pixel data enters the photo library. Original EXIF is never copied. */
public final class PhotoFiles {
    public static final int MAX_PER_OBSERVATION = 5;
    public static final long MAX_JPEG_BYTES = 1_500_000;
    private PhotoFiles() {}
    private static File directory(Context c, String name) {
        File dir = new File(c.getNoBackupFilesDir(), name);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("Fotospeicher konnte nicht angelegt werden.");
        return dir;
    }
    public static File localFile(Context c, String filename) {
        if (!filename.matches("[a-f0-9-]{36}\\.jpg")) throw new IllegalArgumentException("Invalid photo filename");
        return new File(new File(c.getNoBackupFilesDir(),"photos"),filename);
    }
    public static File captureFile(Context c, String token) {
        if (!token.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("Invalid capture token");
        return new File(new File(c.getNoBackupFilesDir(),"captures"),token + ".jpg");
    }
    public static Uri captureUri(Context c, String token) {
        return new Uri.Builder().scheme("content").authority(c.getPackageName()+".photos").appendPath("capture").appendPath(token).build();
    }
    public static long importPhoto(Context c, Uri source, long findingId) throws IOException {
        try (InputStream in = c.getContentResolver().openInputStream(source)) {
            if (in == null) throw new IOException("Foto konnte nicht geöffnet werden.");
            return importStream(c,in,findingId);
        }
    }
    public static long importCapture(Context c, String token, long findingId) throws IOException {
        File file = captureFile(c,token);
        try (InputStream in = new FileInputStream(file)) { return importStream(c,in,findingId); }
        finally { file.delete(); }
    }
    public static long importStream(Context c, InputStream in, long findingId) throws IOException {
        String filename = UUID.randomUUID()+".jpg";
        directory(c,"photos");
        File output = localFile(c,filename), temporary = File.createTempFile("photo-", ".source",directory(c,"captures"));
        Bitmap decoded = null, rotated = null, scaled = null, opaque = null;
        boolean stored = false;
        try {
            try (FileOutputStream out = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[8192]; int n; long bytes = 0;
                while ((n = in.read(buffer)) != -1) {
                    bytes += n; if (bytes > 32 * 1024 * 1024L) throw new IOException("Das Foto ist zu groß (höchstens 32 MB). "); out.write(buffer,0,n);
                }
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(temporary.getPath(),bounds);
            if (bounds.outWidth < 1 || bounds.outHeight < 1 || bounds.outWidth > 40000 || bounds.outHeight > 40000
                    || (long) bounds.outWidth * bounds.outHeight > 200_000_000) throw new IOException("Das Bildformat oder die Bildgröße wird nicht unterstützt.");
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try { orientation = new ExifInterface(temporary.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,orientation); }
            catch (IOException ignored) { /* No usable EXIF; pixel data may still be a valid PNG/WebP. */ }
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = 1;
            while (Math.max(bounds.outWidth,bounds.outHeight) / options.inSampleSize > 4096) options.inSampleSize *= 2;
            decoded = BitmapFactory.decodeFile(temporary.getPath(),options);
            if (decoded == null) throw new IOException("Das Foto konnte nicht gelesen werden.");
            Matrix matrix = orientation(orientation);
            rotated = Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),matrix,true);
            double factor = Math.min(1, 2048.0 / Math.max(rotated.getWidth(),rotated.getHeight()));
            scaled = Bitmap.createScaledBitmap(rotated,Math.max(1,(int)Math.round(rotated.getWidth()*factor)),Math.max(1,(int)Math.round(rotated.getHeight()*factor)),true);
            opaque = Bitmap.createBitmap(scaled.getWidth(),scaled.getHeight(),Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(opaque); canvas.drawColor(Color.WHITE); canvas.drawBitmap(scaled,0,0,null);
            for (int quality = 85; quality >= 45; quality -= 10) {
                try (FileOutputStream out = new FileOutputStream(output)) {
                    if (!opaque.compress(Bitmap.CompressFormat.JPEG,quality,out)) throw new IOException("Foto konnte nicht gespeichert werden.");
                }
                if (output.length() <= MAX_JPEG_BYTES) break;
            }
            if (output.length() > MAX_JPEG_BYTES) throw new IOException("Foto konnte nicht ausreichend verkleinert werden.");
            try (FindingStore store = new FindingStore(c)) {
                if (store.get(findingId) == null) throw new IOException("Die Beobachtung wurde inzwischen gelöscht.");
                long id = store.addPhoto(findingId,filename,opaque.getWidth(),opaque.getHeight(),output.length()); stored = true; return id;
            }
        } finally {
            temporary.delete(); if (!stored) output.delete();
            java.util.Set<Bitmap> bitmaps = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Bitmap b : new Bitmap[]{decoded,rotated,scaled,opaque}) if (b != null && bitmaps.add(b)) b.recycle();
        }
    }
    static Matrix orientation(int code) {
        Matrix m = new Matrix();
        switch (code) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.setScale(-1,1);
            case ExifInterface.ORIENTATION_ROTATE_180 -> m.setRotate(180);
            case ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.setScale(1,-1);
            case ExifInterface.ORIENTATION_TRANSPOSE -> { m.setRotate(90); m.postScale(-1,1); }
            case ExifInterface.ORIENTATION_ROTATE_90 -> m.setRotate(90);
            case ExifInterface.ORIENTATION_TRANSVERSE -> { m.setRotate(-90); m.postScale(-1,1); }
            case ExifInterface.ORIENTATION_ROTATE_270 -> m.setRotate(-90);
            default -> { }
        }
        return m;
    }
    public static void clearLocal(Context c) {
        File[] files = directory(c,"photos").listFiles(); if (files != null) for (File file : files) file.delete();
    }
    public static File outgoingFile(Context c, String token, String filename) {
        if (!token.matches("[a-f0-9-]{36}") || !filename.matches("Stelle-[0-9]{2}-Foto-[0-9]{2,6}\\.jpg")) throw new IllegalArgumentException("Invalid attachment path");
        File dir = new File(new File(c.getNoBackupFilesDir(),"mail-photos"),token);
        return new File(dir,filename);
    }
    public static void cleanup(Context c) {
        prune(new File(c.getNoBackupFilesDir(),"captures"),24 * 60 * 60 * 1000L);
        prune(new File(c.getNoBackupFilesDir(),"mail-photos"),7 * 24 * 60 * 60 * 1000L);
    }
    private static void prune(File dir,long age) {
        File[] files = dir.listFiles(); if (files == null) return;
        for (File file : files) if (System.currentTimeMillis()-file.lastModified() > age) {
            if (file.isDirectory()) { File[] children = file.listFiles(); if (children != null) for (File child : children) child.delete(); }
            file.delete();
        }
    }
}
