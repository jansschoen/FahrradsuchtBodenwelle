package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Prepare immutable sanitized snapshots with place-linked names and no observation timestamps. */
public final class MailAttachments {
    public static final int MAX_PHOTOS=10;
    public static final long MAX_BYTES=10*1024*1024L;
    private MailAttachments() {}
    public record Choice(FindingPhoto photo,int place,String displayName) { public String key() { return place+":"+photo.id(); } }
    public static List<Choice> choices(MailReports.Group group,FindingStore store) {
        List<Choice> result=new ArrayList<>();int place=0;
        for(MailReports.Point point:group.points()) {
            place++;int photoNumber=0;java.util.Set<Long> seen=new java.util.LinkedHashSet<>();
            for(MailReports.Item item:point.observations())for(FindingPhoto photo:store.photos(item.finding().id()))
                if(seen.add(photo.id()))result.add(new Choice(photo,place,String.format(Locale.ROOT,"Stelle-%02d-Foto-%02d.jpg",place,++photoNumber)));
        }
        return result;
    }
    public static ArrayList<Uri> prepare(Context c,List<Choice> selected,MailReports.Group group) throws IOException {
        if(selected.size()>MAX_PHOTOS)throw new IllegalArgumentException("Bitte höchstens zehn Fotos für diese Mail auswählen.");
        long bytes=0;for(Choice choice:selected)bytes+=choice.photo().bytes();
        if(bytes>MAX_BYTES)throw new IllegalArgumentException("Die ausgewählten Fotos sind zusammen zu groß (höchstens 10 MB). Bitte weniger Fotos auswählen.");
        String token=UUID.randomUUID().toString();ArrayList<Uri> uris=new ArrayList<>();List<File> copies=new ArrayList<>();
        try(FindingStore store=new FindingStore(c)) {
            for(MailReports.Item item:group.items()) {
                Finding current=store.get(item.finding().id());
                if(current==null||!current.reviewed()||current.bundleKey()!=item.finding().bundleKey())
                    throw new IOException("Eine Freigabe oder Bündelung hat sich geändert. Bitte den Entwurf neu erzeugen.");
            }
            java.util.Set<String> included=new java.util.HashSet<>();
            for(Choice choice:selected) {
                if(!included.add(choice.key()))throw new IOException("Ein Foto wurde für dieselbe Fundstelle mehrfach ausgewählt.");
                FindingPhoto photo=store.photo(choice.photo().id());
                boolean linked=false;
                if(choice.place()>0&&choice.place()<=group.points().size())for(MailReports.Item item:group.points().get(choice.place()-1).observations()) {
                    Finding current=store.get(item.finding().id());
                    if(current!=null&&current.reviewed()&&current.bundleKey()==item.finding().bundleKey()
                            &&store.hasPhoto(current.id(),choice.photo().id()))linked=true;
                }
                if(photo==null||!linked||!photo.filename().equals(choice.photo().filename()))
                    throw new IOException("Ein ausgewähltes Foto oder seine Freigabe hat sich geändert. Bitte den Entwurf neu erzeugen.");
                File source=PhotoFiles.localFile(c,photo.filename()),target=PhotoFiles.outgoingFile(c,token,choice.displayName());
                if(!source.isFile()||source.length()!=photo.bytes())throw new IOException("Ein ausgewähltes Foto ist nicht mehr vollständig vorhanden.");
                if(!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Anhänge konnten nicht vorbereitet werden.");
                copies.add(target);
                try(FileInputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(target)) {
                    byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
                }
                uris.add(new Uri.Builder().scheme("content").authority(c.getPackageName()+".photos").appendPath("mail").appendPath(token).appendPath(choice.displayName()).build());
            }
            return uris;
        }catch(IOException|RuntimeException e){for(File file:copies)file.delete();throw e;}
    }
}
