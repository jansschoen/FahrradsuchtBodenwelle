package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.graphics.*;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.util.*;

/** Incoming new images are decoded, rotated and recompressed before entering the private gallery. */
final class BrowserPhotos {
    record Prepared(String filename,File source,int width,int height,long bytes){}
    private BrowserPhotos(){}
    static Prepared prepare(File source,File staging)throws IOException {
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(source.getPath(),bounds);
        if(!"image/jpeg".equals(bounds.outMimeType)||bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>4096||bounds.outHeight>4096)throw new IOException("Ein neues Browserfoto ist kein unterstütztes JPEG (höchstens 4096 Pixel je Seite).");
        Bitmap decoded=null,rotated=null,scaled=null,opaque=null;File output=new File(staging,UUID.randomUUID()+".jpg");
        try {
            BitmapFactory.Options sampling=new BitmapFactory.Options();sampling.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/sampling.inSampleSize>2048)sampling.inSampleSize*=2;
            decoded=BitmapFactory.decodeFile(source.getPath(),sampling);if(decoded==null)throw new IOException("Ein Browserfoto konnte nicht gelesen werden.");
            int orientation=new ExifInterface(source.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            rotated=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),PhotoFiles.orientation(orientation),true);
            double factor=Math.min(1,2048.0/Math.max(rotated.getWidth(),rotated.getHeight()));scaled=Bitmap.createScaledBitmap(rotated,Math.max(1,(int)Math.round(rotated.getWidth()*factor)),Math.max(1,(int)Math.round(rotated.getHeight()*factor)),true);
            opaque=Bitmap.createBitmap(scaled.getWidth(),scaled.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(opaque);canvas.drawColor(Color.WHITE);canvas.drawBitmap(scaled,0,0,null);
            for(int quality=85;quality>=45;quality-=10){try(FileOutputStream out=new FileOutputStream(output)){if(!opaque.compress(Bitmap.CompressFormat.JPEG,quality,out))throw new IOException("Foto konnte nicht gespeichert werden.");}if(output.length()<=PhotoFiles.MAX_JPEG_BYTES)break;}
            if(output.length()>PhotoFiles.MAX_JPEG_BYTES)throw new IOException("Ein Browserfoto ist zu groß.");return new Prepared(UUID.randomUUID()+".jpg",output,opaque.getWidth(),opaque.getHeight(),output.length());
        }finally{Set<Bitmap> seen=Collections.newSetFromMap(new IdentityHashMap<>());for(Bitmap b:new Bitmap[]{decoded,rotated,scaled,opaque})if(b!=null&&seen.add(b))b.recycle();}
    }
}
