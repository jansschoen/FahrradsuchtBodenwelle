package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Native vector A4 report with a bounded raster basemap; all drawing stays on the device. */
public final class ReportPdf implements AutoCloseable {
    private static final int INK=0xff153d3a,TEAL=0xff087f73,MUTED=0xff526864,MINT=0xffedf5f1;
    private static final int W=595,H=842;private static final float M=42,BOTTOM=790;
    private final PdfSurface document;
    private final ReportData data;private boolean active;private float y;private int pages;private String chapter;
    public record Photo(int place,String name,Uri uri) {}
    private ReportPdf(Context context,ReportData data)throws IOException{this.data=data;document=new PdfSurface(context,data);}
    public static int write(Context context,File output,ReportData data,ReportMap.Snapshot map,List<Photo> photos) throws IOException {
        try(ReportPdf report=new ReportPdf(context,data)) {
            report.cover(map);report.table();report.details();report.photos(context,photos);report.finish();
            report.document.save(output);
            return report.pages;
        } catch(IOException|RuntimeException failure){output.delete();throw failure;}
    }
    private void page(String title) {
        finish();chapter=title;document.newPage();active=true;pages++;rect(0,0,W,H,Color.WHITE);
        if(!title.isEmpty()) {
            rect(0,0,W,9,TEAL);text("HINWEISE ZUM STRASSENZUSTAND",10,M,40,TEAL,true);
            text(data.reference(),9,435,40,MUTED,false);paragraph(data.district(),10,M,58,W-2*M,MUTED,false);
            text(title,21,M,103,INK,true);y=125;
        }
    }
    private void finish() {
        if(!active)return;
        if(document.escapedGlyph)text("Nicht darstellbare Zeichen sind als Unicode-Code [U+…] angegeben.",7,M,799,MUTED,false);
        rect(M,807,W-M,808,0xffdce7e1);
        text(data.reference()+"  ·  "+(data.appNotice()?"Mit Bodenwelle zusammengestellt":"Fundstellenbericht"),8,M,823,MUTED,false);
        text("Seite "+pages,8,W-80,823,MUTED,false);document.finishPage();active=false;
    }
    private void cover(ReportMap.Snapshot map) {
        page("");rect(0,0,W,177,INK);rect(M,35,M+28,39,0xffb2d9c8);
        text("BITTE UM ÖRTLICHE PRÜFUNG",10,M,60,0xffcde7dc,true);text("Hinweise zum",29,M,99,Color.WHITE,true);
        text("Straßenzustand",29,M,135,Color.WHITE,true);text(data.district(),11,M,160,0xffd4e6df,false);
        text(data.reference(),9,W-125,43,0xffd4e6df,false);
        int observations=data.entries().stream().mapToInt(ReportData.Entry::observations).sum();
        stat(M,"FUNDSTELLEN",Integer.toString(data.entries().size()),true);
        stat(216,"BEOBACHTUNGEN",Integer.toString(observations),true);
        String weeks=data.weeks();stat(390,"ZEITRAUM",weeks.length()<22?weeks:"Mehrere Kalenderwochen",false);
        text("1  |  Kartenübersicht",15,M,291,INK,true);text(map.status(),9,M,310,map.complete()?MUTED:0xff99500c,false);
        document.image(map.bitmap(),new RectF(M,323,W-M,623));
        paragraph("Nummern entsprechen der Tabelle. Kleine Punkte zeigen die GPS-Position; versetzte Nummern sind mit ihr verbunden. Orange Linien kennzeichnen geschätzte unebene Abschnitte.",9,M,639,W-2*M,MUTED,false);
        text("Kartengrundlage: © OpenStreetMap contributors · ODbL 1.0",8,M,683,MUTED,false);
        text("https://www.openstreetmap.org/copyright",8,M,695,MUTED,false);
        document.link(new RectF(M,674,350,699),"https://www.openstreetmap.org/copyright");
        rect(M,708,W-M,790,MINT);text("Erbetene Schritte",12,M+14,733,INK,true);
        paragraph("Zuständigkeit und Lage zuordnen, Zustand vor Ort prüfen und bei bestätigtem Handlungsbedarf geeignete Sicherungs- oder Instandsetzungsmaßnahmen veranlassen. Eine kurze Rückmeldung mit Aktenzeichen wäre hilfreich.",10,M+14,745,W-2*M-28,INK,false);
    }
    private void stat(float x,String label,String value,boolean large) {
        rect(x,196,x+163,266,MINT);text(label,8,x+12,215,MUTED,true);
        paragraph(value,large?25:11,x+12,large?230:234,139,INK,true);
    }
    private void tableHeader() {
        rect(M,y,W-M,y+27,TEAL);
        String[] labels={"Nr.","Lage & Karte","Befund & Fläche","Messung & Hinweise"};float[] x={M+7,78,239,399};
        for(int i=0;i<labels.length;i++)text(labels[i],9,x[i],y+18,Color.WHITE,true);y+=27;
    }
    private void table() {
        page("2  |  Fundstellen im Überblick");tableHeader();
        for(ReportData.Entry entry:data.entries()) {
            boolean appendix=needsDetails(entry);
            String location=(entry.roadName().trim().isEmpty()?"Straße vor Ort zuordnen":shortText(entry.roadName(),90))+"\n"+entry.district()+"\n"+entry.coordinates()
                    +String.format(Locale.GERMANY,"\nGPS etwa ± %.0f m",entry.accuracy())+(entry.projected()?" · korrigiert":"")+"\n"+entry.weeks();
            String condition=shortText(entry.title(),120)+"\nFläche: "+compact(entry.traffic())+"\nBelag: "+compact(entry.surface());
            String evidence="Sensor: "+entry.sensor()+"\nGemessene Höhe: "+entry.height()+"\nMessungen "+shortText(entry.references(),100)
                    +(entry.hazards().trim().isEmpty()?"":"\n"+entry.hazards())+"\n"+entry.action()+(appendix?"\nErgänzungen im Anhang.":"");
            String[] cells={Integer.toString(entry.number()),location,condition,evidence};float[] left={M+7,78,239,399};float[] widths={20,147,146,146};
            List<List<String>> lines=new ArrayList<>();int maximum=0;
            for(int i=0;i<cells.length;i++){List<String> wrapped=wrap(cells[i],9,widths[i],false);lines.add(wrapped);maximum=Math.max(maximum,wrapped.size());}
            float height=maximum*12+20;
            if(y+height>BOTTOM){page("2  |  Fundstellen · Fortsetzung");tableHeader();}
            if(height>BOTTOM-y)throw new IllegalArgumentException("Eine Fundstelle enthält zu umfangreiche Angaben für die Tabelle.");
            rect(M,y,W-M,y+height,entry.number()%2==1?MINT:Color.WHITE);
            for(int i=0;i<cells.length;i++)for(int line=0;line<lines.get(i).size();line++)text(lines.get(i).get(line),9,left[i],y+15+line*12,INK,i==0);
            document.link(new RectF(75,y,234,y+height),entry.mapLink());
            y+=height;rect(M,y,W-M,y+.6f,0xffdce7e1);
        }
        gap(14);flow("Messhinweise",11,INK,true);
        flow("Die Lagefelder der Tabelle sind mit der jeweiligen OpenStreetMap-Ansicht verlinkt.",9,MUTED,false);
        flow("GPS-Positionen und Abschnittsenden sind Schätzungen. Eine zeitliche Positionskorrektur ersetzt keine Vermessung. Sensorreaktionen sind keine Schadenshöhen oder rechtlichen Grenzwerte. Höhen in Millimetern stammen ausschließlich aus bestätigten Messungen vor Ort. OSM-Angaben sind Vorschläge und müssen überprüft werden.",9,MUTED,false);
        if(!data.name().trim().isEmpty()){gap(8);flow("Gemeldet von: "+data.name(),10,INK,false);}
    }
    private boolean needsDetails(ReportData.Entry e) {return !e.notes().isEmpty()||!e.sections().isEmpty()||e.references().length()>100||e.roadName().length()>90||e.title().length()>120;}
    private void details() {
        boolean started=false;
        for(ReportData.Entry entry:data.entries())if(needsDetails(entry)) {
            if(!started){page("3  |  Ergänzende Angaben");started=true;}else{gap(18);ensure(90);}
            flow("Stelle "+entry.number()+" · "+entry.title(),14,TEAL,true);
            if(!entry.roadName().trim().isEmpty())flow(entry.roadName(),11,INK,true);
            flow(entry.coordinates()+" · "+entry.weeks(),10,MUTED,false);
            flow("Messungen "+entry.references(),10,INK,false);
            for(ReportData.Section section:entry.sections())flow(String.format(Locale.GERMANY,"Unebener Abschnitt: etwa %.0f m, aus Geschwindigkeit und Dauer geschätzt. ",section.meters())
                    +String.format(Locale.ROOT,"Anfang %.6f, %.6f · Ende %.6f, %.6f.",section.latitude(),section.longitude(),section.endLatitude(),section.endLongitude()),10,INK,false);
            for(String note:entry.notes()){gap(7);flow("Eigene Beschreibung:",10,INK,true);flow(note,10,INK,false);}
            gap(7);flow("Kartenlink zum Kopieren:",9,MUTED,false);flow(entry.mapLink(),8,MUTED,false);
        }
    }
    private void photos(Context context,List<Photo> photos) throws IOException {
        int index=0;
        for(Photo photo:photos) {
            if(index++%2==0)page("Fotodokumentation");else gap(20);
            flow("Stelle "+photo.place()+" · "+photo.name(),11,TEAL,true);
            Bitmap bitmap;
            try(java.io.InputStream in=context.getContentResolver().openInputStream(photo.uri())) {
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;bitmap=BitmapFactory.decodeStream(in,null,options);
            }
            if(bitmap==null)throw new IOException("Ein ausgewähltes Foto ist nicht lesbar. Bitte das Foto prüfen oder abwählen.");
            float height=285,scale=Math.min((W-2*M)/bitmap.getWidth(),height/bitmap.getHeight());
            rect(M,y,W-M,y+height,MINT);
            float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;
            document.image(bitmap,new RectF((W-w)/2,y+(height-h)/2,(W+w)/2,y+(height+h)/2));bitmap.recycle();y+=height;
        }
    }
    private void ensure(float height){if(y+height>BOTTOM)page(chapter+" · Fortsetzung");}
    private void gap(float amount){y+=amount;}
    private void flow(String value,float size,int color,boolean bold) {
        for(String line:wrap(value,size,W-2*M,bold)){ensure(size*1.4f);text(line,size,M,y+size,color,bold);y+=size*1.4f;}
    }
    private float paragraph(String value,float size,float x,float top,float width,int color,boolean bold) {
        float next=top;for(String line:wrap(value,size,width,bold)){text(line,size,x,next+size,color,bold);next+=size*1.35f;}return next;
    }
    private List<String> wrap(String value,float size,float width,boolean bold) {
        List<String> lines=new ArrayList<>();
        for(String raw:value.replace("\r","").split("\n",-1)) {
            String remaining=document.safe(raw,bold);if(remaining.isEmpty()){lines.add("");continue;}
            while(!remaining.isEmpty()) {
                int count=document.breakText(remaining,size,width,bold);
                if(count<remaining.length()&&count>0&&Character.isHighSurrogate(remaining.charAt(count-1)))count--;
                if(count<=0)count=Character.charCount(remaining.codePointAt(0));
                int space=remaining.lastIndexOf(' ',Math.min(count,remaining.length()-1));
                if(count<remaining.length()&&space>count/2)count=space;
                lines.add(remaining.substring(0,count));remaining=remaining.substring(count).trim();
            }
        }
        return lines;
    }
    private void text(String value,float size,float x,float baseline,int color,boolean bold){document.text(value,size,x,baseline,color,bold);}
    private void rect(float l,float t,float r,float b,int color){document.rect(l,t,r,b,color);}
    private static String compact(String value){return value.replace(" · OSM-Vorschlag, zu prüfen"," (OSM)").replace(" · manuell"," (manuell)");}
    private static String shortText(String value,int limit){return value.length()<=limit?value:value.substring(0,limit-1)+"…";}
    @Override public void close(){document.close();}
}
