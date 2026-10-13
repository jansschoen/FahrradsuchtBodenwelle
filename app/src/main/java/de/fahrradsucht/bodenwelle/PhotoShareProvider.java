package de.fahrradsucht.bodenwelle;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/** Private, URI-granted provider: camera scratch files are writable; prepared mail photos are read-only. */
public final class PhotoShareProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    private File file(Uri uri) {
        if (!uri.getAuthority().equals(getContext().getPackageName()+".photos")) throw new IllegalArgumentException("Wrong authority");
        List<String> p = uri.getPathSegments();
        if (p.size()==2 && "capture".equals(p.get(0))) return PhotoFiles.captureFile(getContext(),p.get(1));
        if (p.size()==3 && "mail".equals(p.get(0))) return PhotoFiles.outgoingFile(getContext(),p.get(1),p.get(2));
        throw new IllegalArgumentException("Unsupported photo path");
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        File file;
        try { file = file(uri); } catch (IllegalArgumentException e) { throw new FileNotFoundException("Invalid photo URI"); }
        boolean capture = "capture".equals(uri.getPathSegments().get(0));
        if ("r".equals(mode)) return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
        if (capture && ("w".equals(mode) || "wt".equals(mode) || "rw".equals(mode) || "rwt".equals(mode))) {
            if(!file.getParentFile().isDirectory()&&!file.getParentFile().mkdirs())throw new FileNotFoundException("Camera storage unavailable");
            return ParcelFileDescriptor.open(file,ParcelFileDescriptor.parseMode(mode) | ParcelFileDescriptor.MODE_CREATE);
        }
        throw new FileNotFoundException("Mail attachments are read-only");
    }
    @Override public String getType(Uri uri) { file(uri); return "image/jpeg"; }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
        File file = file(uri); String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE} : projection;
        MatrixCursor result = new MatrixCursor(columns); Object[] row = new Object[columns.length];
        for (int i=0;i<columns.length;i++) row[i] = OpenableColumns.DISPLAY_NAME.equals(columns[i]) ? file.getName() : OpenableColumns.SIZE.equals(columns[i]) ? file.length() : null;
        result.addRow(row); return result;
    }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String selection,String[] args) { throw new UnsupportedOperationException(); }
}
