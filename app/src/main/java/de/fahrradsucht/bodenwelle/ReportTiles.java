package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.MapTileIndex;

/** At most the current report viewport; reuses the live map cache, never downloads a route or extra zooms. */
final class ReportTiles implements ReportMap.Tiles, AutoCloseable {
    private final SqlTileWriter cache;
    private final XYTileSource source=new XYTileSource("OSM",0,19,256,".png",new String[]{"https://tile.openstreetmap.org/"});
    private final long deadline=System.nanoTime()+15_000_000_000L;
    private boolean network=true;
    ReportTiles(Context context) {
        File base=new File(context.getNoBackupFilesDir(),"osm"),tiles=new File(base,"tiles");
        if(!tiles.isDirectory()&&!tiles.mkdirs())throw new IllegalStateException("Kartenspeicher nicht verfügbar.");
        Configuration.getInstance().setOsmdroidBasePath(base);Configuration.getInstance().setOsmdroidTileCache(tiles);
        cache=new SqlTileWriter();
        network=DeviceConnectivity.canRequest(context);
    }
    @Override public Bitmap load(ReportMapLayout.Tile tile) {
        long index=MapTileIndex.getTileIndex(tile.zoom(),tile.x(),tile.y());Bitmap cached=null;
        try {
            Drawable drawable=cache.loadTile(source,index);
            if(drawable instanceof BitmapDrawable bitmap)cached=bitmap.getBitmap().copy(Bitmap.Config.ARGB_8888,false);
            Long expires=cache.getExpirationTimestamp(source,index);
            if(cached!=null&&expires!=null&&expires>System.currentTimeMillis())return cached;
        } catch(Exception ignored) { /* A missing cache never prevents a locally generated report. */ }
        if(!network||System.nanoTime()>deadline||Thread.currentThread().isInterrupted())return cached;
        HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)new URL("https://tile.openstreetmap.org/"+tile.zoom()+"/"+tile.x()+"/"+tile.y()+".png").openConnection();
            connection.setConnectTimeout(3500);connection.setReadTimeout(4000);connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent","Bodenwelle/"+BuildConfig.VERSION_NAME+" (Android report map; https://github.com/jansschoen/FahrradsuchtBodenwelle)");
            if(connection.getResponseCode()!=200)throw new IOException("Kartenteil nicht erreichbar.");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(java.io.InputStream in=connection.getInputStream()) {byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(System.nanoTime()>deadline)throw new IOException("Kartenabruf dauert zu lange.");if(bytes.size()+n>1_000_000)throw new IOException("Kartenteil zu groß.");bytes.write(buffer,0,n);}}
            byte[] png=bytes.toByteArray();Bitmap bitmap=BitmapFactory.decodeByteArray(png,0,png.length);
            if(bitmap==null||bitmap.getWidth()!=256||bitmap.getHeight()!=256){if(bitmap!=null)bitmap.recycle();throw new IOException("Ungültiger Kartenteil.");}
            long expires=connection.getExpiration();String control=connection.getHeaderField("Cache-Control");
            if(control!=null)for(String directive:control.split(","))if(directive.trim().startsWith("max-age="))try{expires=System.currentTimeMillis()+Long.parseLong(directive.trim().substring(8))*1000;}catch(NumberFormatException ignored){}
            if(expires<=0)expires=System.currentTimeMillis()+7L*24*60*60*1000;
            if(control==null||!control.contains("no-store"))cache.saveFile(source,index,new ByteArrayInputStream(png),expires);
            if(cached!=null)cached.recycle();return bitmap;
        } catch(IOException|RuntimeException failure) {network=false;return cached;}
        finally {if(connection!=null)connection.disconnect();}
    }
    @Override public void close(){cache.onDetach();}
}
