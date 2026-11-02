package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.List;

/** Web Mercator viewport, including selected section endpoints; independent from the live ride map. */
public record ReportMapLayout(int zoom, double left, double top, int width, int height) {
    public record Pixel(double x,double y) {}
    public record Tile(int zoom,int x,int y) {}
    public static ReportMapLayout fit(List<ReportData.Entry> entries,int width,int height) {
        List<Pixel> positions=new ArrayList<>();
        for(ReportData.Entry entry:entries) {
            positions.add(normalized(entry.latitude(),entry.longitude()));
            for(ReportData.Section section:entry.sections()) {
                positions.add(normalized(section.latitude(),section.longitude()));positions.add(normalized(section.endLatitude(),section.endLongitude()));
            }
        }
        if(positions.isEmpty())throw new IllegalArgumentException("Keine Fundstellen für die Karte.");
        double minX=1,minY=1,maxX=0,maxY=0;
        for(Pixel p:positions){minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minY=Math.min(minY,p.y);maxY=Math.max(maxY,p.y);}
        int zoom=17;
        while(zoom>0&&((maxX-minX)*world(zoom)>width-160||(maxY-minY)*world(zoom)>height-160))zoom--;
        double size=world(zoom);
        return new ReportMapLayout(zoom,(minX+maxX)*size/2-width/2.0,(minY+maxY)*size/2-height/2.0,width,height);
    }
    public Pixel project(double latitude,double longitude) {
        Pixel p=normalized(latitude,longitude);return new Pixel(p.x*world(zoom)-left,p.y*world(zoom)-top);
    }
    private static Pixel normalized(double lat,double lon) {
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new IllegalArgumentException("Ungültige Kartenkoordinate.");
        double sin=Math.sin(Math.toRadians(Math.max(-85.05112878,Math.min(85.05112878,lat))));
        return new Pixel((lon+180)/360,.5-Math.log((1+sin)/(1-sin))/(4*Math.PI));
    }
    public List<Tile> tiles() {
        List<Tile> tiles=new ArrayList<>();int max=1<<zoom;
        for(int y=(int)Math.floor(top/256);y<=(int)Math.floor((top+height-1)/256);y++)
            for(int x=(int)Math.floor(left/256);x<=(int)Math.floor((left+width-1)/256);x++)
                if(y>=0&&y<max)tiles.add(new Tile(zoom,Math.floorMod(x,max),y));
        return tiles;
    }
    public List<Pixel> labels(List<ReportData.Entry> entries) {
        List<Pixel> placed=new ArrayList<>();
        for(ReportData.Entry entry:entries) {
            Pixel anchor=project(entry.latitude(),entry.longitude()),label=anchor;
            search: for(int radius=0;radius<=220;radius+=22)for(int step=0;step<16;step++) {
                double angle=step*Math.PI/8;
                Pixel candidate=new Pixel(Math.max(22,Math.min(width-22,anchor.x+Math.cos(angle)*radius)),Math.max(22,Math.min(height-22,anchor.y+Math.sin(angle)*radius)));
                boolean free=true;for(Pixel previous:placed)if(Math.hypot(previous.x-candidate.x,previous.y-candidate.y)<38){free=false;break;}
                if(free){label=candidate;break search;}
            }
            placed.add(label);
        }
        return placed;
    }
    public static double world(int zoom){return 256.0*Math.pow(2,zoom);}
}
