package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.net.Uri;
import java.io.File;

/** Whitelisted, immutable report snapshots; never exposes the database, maps or arbitrary local paths. */
public final class ReportFiles {
    private ReportFiles(){}
    public static File file(Context context,String token,String filename) {
        if(!token.matches("[a-f0-9-]{36}")||!filename.matches("Fundstellenbericht-BW-[A-F0-9]{8}\\.pdf"))throw new IllegalArgumentException("Ungültiger Berichtspfad.");
        return new File(new File(new File(context.getNoBackupFilesDir(),"reports"),token),filename);
    }
    public static Uri uri(Context context,String token,String filename) {
        file(context,token,filename);
        return new Uri.Builder().scheme("content").authority(context.getPackageName()+".photos").appendPath("report").appendPath(token).appendPath(filename).build();
    }
    public static void cleanup(Context context) {
        File[] directories=new File(context.getNoBackupFilesDir(),"reports").listFiles();if(directories==null)return;
        for(File directory:directories)if(System.currentTimeMillis()-directory.lastModified()>7L*24*60*60*1000) {
            File[] files=directory.listFiles();if(files!=null)for(File file:files)file.delete();directory.delete();
        }
    }
}
