package de.fahrradsucht.bodenwelle;

import android.graphics.*;
import java.util.List;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.util.GeoPoint;

/** Dots retain their actual coordinates; offset labels distinguish even coincident measurements. */
final class FindingMapOverlay extends Overlay {
    private final List<Finding> findings;
    private final List<String> labels;
    private final float scale;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    FindingMapOverlay(List<Finding> findings,List<String> labels,float scale){this.findings=List.copyOf(findings);this.labels=List.copyOf(labels);this.scale=scale;}
    @Override public void draw(Canvas canvas,MapView map,boolean shadow) {
        if(shadow)return;
        for(int i=0;i<findings.size();i++) {
            Finding f=findings.get(i);Point p=map.getProjection().toPixels(new GeoPoint(f.latitude(),f.longitude()),null);
            int side=i%2==0?-1:1,color=i%2==0?0xff087f73:0xffcf6d11;
            // Two label columns, spread vertically without shifting the measured positions.
            int rows=(findings.size()+1)/2;
            float x=map.getWidth()/2f+side*64*scale,y=map.getHeight()/2f+((i/2)-(rows-1)/2f)*27*scale;
            if(findings.size()<=2)y+=side*26*scale;
            paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*scale);canvas.drawLine(p.x,p.y,x,y,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);canvas.drawCircle(p.x,p.y,7*scale,paint);
            paint.setColor(color);canvas.drawCircle(p.x,p.y,5*scale,paint);
            paint.setTextSize(13*scale);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);
            String label=labels.get(i);float half=paint.measureText(label)/2+7*scale;
            paint.setColor(Color.WHITE);canvas.drawRoundRect(new RectF(x-half,y-13*scale,x+half,y+9*scale),7*scale,7*scale,paint);
            paint.setColor(color);canvas.drawText(label,x,y+4*scale,paint);
        }
    }
    static void center(MapView map,List<Finding> findings) {
        double lat=findings.stream().mapToDouble(Finding::latitude).average().orElse(52.52);
        double lon=findings.stream().mapToDouble(Finding::longitude).average().orElse(13.405);
        map.getController().setZoom(18.0);map.getController().setCenter(new GeoPoint(lat,lon));
    }
}
