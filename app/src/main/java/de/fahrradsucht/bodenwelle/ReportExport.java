package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Build one frozen, consent-checked report and sanitized photo snapshots in a background job. */
final class ReportExport {
    record Request(MailReports.Group group,Map<String,String> districts,String name,boolean notes,boolean notice,List<MailAttachments.Choice> selected) {
        Request { districts=java.util.Collections.unmodifiableMap(new java.util.HashMap<>(districts));selected=java.util.Collections.unmodifiableList(new ArrayList<>(selected)); }
    }
    record Result(Uri pdf,ArrayList<Uri> photos,int pages,String mapStatus) {}
    static Result create(Context context,Request request) throws IOException {
        MailAttachments.validate(context,request.selected(),request.group());
        String token=UUID.randomUUID().toString(),reference="BW-"+token.replace("-","").substring(0,8).toUpperCase(java.util.Locale.ROOT);
        ReportData data=ReportData.create(request.group(),id->request.districts().getOrDefault(id,"Zuordnung offen"),request.name(),request.notes(),request.notice(),reference);
        String filename="Fundstellenbericht-"+reference+".pdf";File output=ReportFiles.file(context,token,filename);
        if(!output.getParentFile().isDirectory()&&!output.getParentFile().mkdirs())throw new IOException("Berichtsspeicher nicht verfügbar.");
        ArrayList<Uri> photos=MailAttachments.prepare(context,request.selected(),request.group());List<ReportPdf.Photo> included=new ArrayList<>();
        for(int i=0;i<photos.size();i++)included.add(new ReportPdf.Photo(request.selected().get(i).place(),request.selected().get(i).displayName(),photos.get(i)));
        try(ReportTiles tiles=new ReportTiles(context);ReportMap.Snapshot map=ReportMap.render(data,tiles)) {
            int pages=ReportPdf.write(context,output,data,map,included);
            MailAttachments.validate(context,request.selected(),request.group());
            if(Thread.currentThread().isInterrupted())throw new IOException("Berichterstellung abgebrochen.");
            return new Result(ReportFiles.uri(context,token,filename),photos,pages,map.status());
        }catch(IOException|RuntimeException failure){output.delete();output.getParentFile().delete();throw failure;}
    }
}
