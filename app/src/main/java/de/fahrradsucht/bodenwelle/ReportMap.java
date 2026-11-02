package de.fahrradsucht.bodenwelle;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import java.util.List;

/** A report-specific static map: only exported findings and selected section endpoints, never a ride track. */
public final class ReportMap {
    interface Tiles { Bitmap load(ReportMapLayout.Tile tile); }
    public record Snapshot(Bitmap bitmap,int loaded,int total) implements AutoCloseable {
        public boolean complete(){return loaded==total;}
        public String status(){return complete()?"Straßenkarte: OpenStreetMap":"Straßenkarte unvollständig · "+loaded+" von "+total+" Kartenteilen verfügbar";}
        @Override public void close(){bitmap.recycle();}
    }
    private ReportMap(){}
    static Snapshot render(ReportData report,Tiles tiles) {
        ReportMapLayout layout=ReportMapLayout.fit(report.entries(),1024,600);
        Bitmap image=Bitmap.createBitmap(layout.width(),layout.height(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);c.drawColor(0xffedf2f1);
        p.setColor(0xffd5dfdc);p.setStrokeWidth(1);
        for(int x=0;x<layout.width();x+=64)c.drawLine(x,0,x,layout.height(),p);
        for(int y=0;y<layout.height();y+=64)c.drawLine(0,y,layout.width(),y,p);
        int loaded=0;List<ReportMapLayout.Tile> requested=layout.tiles();
        if(requested.size()>20)throw new IllegalStateException("Kartenansicht überschreitet das begrenzte Abfragebudget.");
        for(ReportMapLayout.Tile tile:requested) {
            if(Thread.currentThread().isInterrupted()){image.recycle();throw new IllegalStateException("Berichterstellung abgebrochen.");}
            Bitmap bitmap=tiles.load(tile);if(bitmap==null)continue;
            c.drawBitmap(bitmap,null,new android.graphics.RectF((float)(tile.x()*256-layout.left()),(float)(tile.y()*256-layout.top()),(float)(tile.x()*256+256-layout.left()),(float)(tile.y()*256+256-layout.top())),p);
            bitmap.recycle();loaded++;
        }
        p.setColor(0xffce711b);p.setStrokeWidth(7);
        for(ReportData.Entry entry:report.entries())for(ReportData.Section section:entry.sections()) {
            ReportMapLayout.Pixel a=layout.project(section.latitude(),section.longitude()),b=layout.project(section.endLatitude(),section.endLongitude());
            c.drawLine((float)a.x(),(float)a.y(),(float)b.x(),(float)b.y(),p);
        }
        List<ReportMapLayout.Pixel> labels=layout.labels(report.entries());int index=0;
        for(ReportData.Entry entry:report.entries()) {
            ReportMapLayout.Pixel a=layout.project(entry.latitude(),entry.longitude()),b=labels.get(index++);
            p.setColor(0xff124b48);p.setStrokeWidth(2);c.drawLine((float)a.x(),(float)a.y(),(float)b.x(),(float)b.y(),p);
            c.drawCircle((float)a.x(),(float)a.y(),4,p);p.setColor(Color.WHITE);c.drawCircle((float)b.x(),(float)b.y(),19,p);
            p.setColor(0xff087f73);c.drawCircle((float)b.x(),(float)b.y(),16,p);
            p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(22);p.setTextAlign(Paint.Align.CENTER);
            c.drawText(Integer.toString(entry.number()),(float)b.x(),(float)b.y()-(p.ascent()+p.descent())/2,p);
        }
        p.setTextAlign(Paint.Align.LEFT);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(20);p.setColor(Color.WHITE);c.drawRect(layout.width()-65,12,layout.width()-12,90,p);
        p.setColor(0xff124b48);c.drawText("N",layout.width()-47,38,p);p.setStrokeWidth(3);c.drawLine(layout.width()-40,80,layout.width()-40,48,p);
        c.drawLine(layout.width()-40,48,layout.width()-47,58,p);c.drawLine(layout.width()-40,48,layout.width()-33,58,p);
        double metersPerPixel=40075016.686*Math.cos(Math.toRadians(report.entries().get(0).latitude()))/ReportMapLayout.world(layout.zoom());
        double raw=metersPerPixel*140;double power=Math.pow(10,Math.floor(Math.log10(raw)));double scale=Math.floor(raw/power)*power;
        float length=(float)(scale/metersPerPixel);p.setColor(Color.WHITE);c.drawRect(12,layout.height()-65,32+length,layout.height()-12,p);
        p.setColor(0xff124b48);p.setStrokeWidth(3);c.drawLine(22,layout.height()-27,22+length,layout.height()-27,p);
        p.setTextSize(18);c.drawText(scale>=1000?String.format(java.util.Locale.GERMANY,"%.1f km",scale/1000):Math.round(scale)+" m",22,layout.height()-38,p);
        return new Snapshot(image,loaded,requested.size());
    }
}
