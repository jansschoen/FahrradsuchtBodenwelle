package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** One local HTML file: offline editor, immutable evidence, editable observations and sanitized photos. */
final class BrowserProject {
    static final long MAX_FILE=256L*1024*1024,MAX_PHOTOS=128L*1024*1024;
    private static final String START="<script id=\"bodenwelle-project\" type=\"application/json\">",END="</script>",TOKEN="__BODENWELLE_PROJECT_DATA__";
    private BrowserProject(){}
    static File directory(Context c,String name)throws IOException{File f=new File(c.getNoBackupFilesDir(),name);if(!f.isDirectory()&&!f.mkdirs())throw new IOException("Der Projektspeicher konnte nicht angelegt werden.");return f;}
    static String device(Context c){var p=c.getSharedPreferences("browser_projects",0);String id=p.getString("device","");if(id.isEmpty()){id=UUID.randomUUID().toString();p.edit().putString("device",id).apply();}return id;}
    static File create(Context c)throws Exception {
        if(RecordingService.running)throw new IOException("Zum Exportieren bitte zuerst die Fahrt beenden.");
        String token=UUID.randomUUID().toString();File output=new File(directory(c,"browser-out"),token+".html");
        try(FindingStore store=new FindingStore(c)) {
            var db=store.getWritableDatabase();db.beginTransaction();
            try {
                JSONObject base=BrowserSnapshot.snapshot(c,store);
                if(base.toString().getBytes(StandardCharsets.UTF_8).length>16*1024*1024||base.getJSONObject("rows").length()>10000||base.getJSONArray("route").length()>100000)throw new IOException("Für diesen Prototyp ist das Projekt zu groß. Es wurde nichts weggelassen.");
                long bytes=0;JSONObject photos=base.getJSONObject("photos");if(photos.length()>1000)throw new IOException("Das Projekt enthält mehr als 1000 Fotos. Es wurde nichts weggelassen.");for(Iterator<String> it=photos.keys();it.hasNext();)bytes+=photos.getJSONObject(it.next()).getLong("bytes");
                if(bytes>MAX_PHOTOS)throw new IOException("Das Projekt enthält mehr als 128 MB Fotos. Es wurde nichts weggelassen.");
                JSONObject payload=new JSONObject().put("format","bodenwelle-project").put("version",1).put("device",device(c)).put("export",token).put("base",base).put("edits",BrowserSnapshot.edits(base));
                String template=template(c);
                int split=template.indexOf(TOKEN);if(split<0)throw new IOException("Die Browseroberfläche fehlt.");
                try(Writer w=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output),StandardCharsets.UTF_8))) {
                    w.write(template.substring(0,split));String data=BrowserJson.safeJson(payload);w.write(data.substring(0,data.length()-1));w.write(",\"blobs\":{");boolean first=true;
                    for(Iterator<String> it=photos.keys();it.hasNext();) {String key=it.next();if(!first)w.write(',');first=false;w.write(JSONObject.quote(key));w.write(':');w.write('"');w.write(Base64.encodeToString(readSmall(new FileInputStream(PhotoFiles.localFile(c,key)),2*1024*1024),Base64.NO_WRAP));w.write('"');}
                    w.write("}}");w.write(template.substring(split+TOKEN.length()));
                }
                if(output.length()>MAX_FILE)throw new IOException("Die Projektdatei ist größer als 256 MB. Es wurde nichts weggelassen.");
                Files.write(new File(directory(c,"browser-baselines"),token+".json").toPath(),base.toString().getBytes(StandardCharsets.UTF_8));
                db.setTransactionSuccessful();return output;
            }finally{db.endTransaction();}
        }catch(Exception e){output.delete();throw e;}
    }
    static String template(Context c)throws IOException {
        String html=asset(c,"browser/editor.html");
        // Keep the exported file independent of CDNs and neighbouring files.
        String script=asset(c,"browser/vendor/maplibre-gl.js").replace("</script", "<\\/script");
        return html.replace("__BODENWELLE_MAP_LICENSE__",asset(c,"browser/vendor/LICENSE.txt").replace("</script", "<\\/script"))
                .replace("__BODENWELLE_MAP_STYLE__",asset(c,"browser/vendor/maplibre-gl.css"))
                .replace("__BODENWELLE_MAP_LIBRARY__",script);
    }
    private static String asset(Context c,String path)throws IOException {
        try(InputStream in=c.getAssets().open(path)){return new String(readSmall(in,2*1024*1024),StandardCharsets.UTF_8);}
    }
    static byte[] readSmall(InputStream source,int limit)throws IOException{try(InputStream in=source;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Eine Datei ist zu groß.");out.write(b,0,n);}return out.toByteArray();}}
    static final class Loaded implements AutoCloseable {
        final JSONObject data,base,edits;final Map<String,File> blobs;final File staging;final boolean allowConsumed;
        Loaded(JSONObject data,Map<String,File> blobs,File staging,boolean allowConsumed)throws JSONException{this.data=data;this.base=data.getJSONObject("base");this.edits=data.getJSONObject("edits");this.blobs=blobs;this.staging=staging;this.allowConsumed=allowConsumed;}
        @Override public void close(){remove(staging);}
    }
    static Loaded load(Context c,InputStream input)throws Exception{return load(c,input,false);}
    static Loaded load(Context c,InputStream input,boolean allowConsumed)throws Exception {
        File staging=new File(directory(c,"browser-stage"),UUID.randomUUID().toString());if(!staging.mkdirs())throw new IOException("Die Datei konnte nicht vorbereitet werden.");
        try {
            File json=new File(staging,"project.json");extract(input,json);
            JSONObject data=new JSONObject();Map<String,File> blobs=new LinkedHashMap<>();long photoBytes=0;
            try(var bounded=new BrowserJson.BoundedReader(new InputStreamReader(new FileInputStream(json),StandardCharsets.UTF_8));JsonReader r=new JsonReader(bounded)) {
                r.beginObject();Set<String> seen=new HashSet<>();
                while(r.hasNext()) {
                    bounded.sectionLimit(16*1024*1024);String key=r.nextName();if(!seen.add(key))throw new IOException("Doppeltes Projektfeld.");
                    if("blobs".equals(key)) {
                        bounded.sectionLimit(MAX_FILE);r.beginObject();
                        while(r.hasNext()) {
                            String name=r.nextName();if(!name.matches("(?:new-)?[a-f0-9-]{36}\\.jpg")||blobs.containsKey(name)||blobs.size()>=1000)throw new IOException("Ungültige Fotoliste.");
                            byte[] bytes;try{bytes=Base64.decode(r.nextString(),Base64.NO_WRAP);}catch(IllegalArgumentException e){throw new IOException("Ein Foto ist beschädigt.");}
                            photoBytes+=bytes.length;if(bytes.length>PhotoFiles.MAX_JPEG_BYTES||photoBytes>MAX_PHOTOS)throw new IOException("Die Foto-Daten sind zu groß.");
                            File file=new File(staging,UUID.randomUUID()+".jpg");Files.write(file.toPath(),bytes);blobs.put(name,file);
                        }r.endObject();
                    } else {
                        if(!Set.of("format","version","device","export","base","edits").contains(key))throw new IOException("Unbekanntes Projektfeld.");
                        data.put(key,BrowserJson.read(r,0,new int[]{1_000_000}));
                    }
                }r.endObject();if(r.peek()!=JsonToken.END_DOCUMENT)throw new IOException("Unerwartete zusätzliche Daten.");
            }
            if(!"bodenwelle-project".equals(data.getString("format"))||data.getInt("version")!=1)throw new IOException("Diese Projektversion wird nicht unterstützt.");
            String token=data.getString("export");if(!token.matches("[a-f0-9-]{36}")||!device(c).equals(data.getString("device")))throw new IOException("Diese Datei stammt nicht aus dieser App-Installation.");
            if(!allowConsumed)try(FindingStore store=new FindingStore(c);var cursor=store.getReadableDatabase().rawQuery("SELECT 1 FROM browser_imports WHERE export_id=?",new String[]{token})){if(cursor.moveToFirst())throw new IOException("Diese Bearbeitungsrunde wurde bereits importiert. Bitte für weitere Änderungen eine neue Projektdatei vom Handy exportieren.");}
            File anchor=new File(directory(c,"browser-baselines"),token+".json");if(!anchor.isFile())throw new IOException("Die Exportgrundlage fehlt auf diesem Handy. Bitte die ursprüngliche App-Installation verwenden.");
            JSONObject trusted=new JSONObject(new String(readSmall(new FileInputStream(anchor),32*1024*1024),StandardCharsets.UTF_8));
            if(!BrowserJson.same(trusted,data.getJSONObject("base")))throw new IOException("Ursprüngliche Messdaten oder die Exportgrundlage wurden verändert. Der Rückimport wurde abgebrochen.");
            JSONObject originalPhotos=trusted.getJSONObject("photos");for(Iterator<String> it=originalPhotos.keys();it.hasNext();) {String key=it.next();File file=blobs.get(key);if(file!=null&&!BrowserJson.sha(file).equals(originalPhotos.getJSONObject(key).getString("sha256")))throw new IOException("Ein ursprüngliches Foto fehlt oder wurde verändert.");}
            for(String key:blobs.keySet())if(!key.startsWith("new-")&&!originalPhotos.has(key))throw new IOException("Ein Foto gehört nicht zu diesem Projekt.");
            BrowserSnapshot.validate(trusted,data.getJSONObject("edits"),blobs.keySet());return new Loaded(data,blobs,staging,allowConsumed);
        }catch(Exception e){remove(staging);throw e;}
    }
    private static void extract(InputStream source,File output)throws IOException {
        try(InputStream in=new BufferedInputStream(source);OutputStream out=new BufferedOutputStream(new FileOutputStream(output))) {
            byte[] start=START.getBytes(StandardCharsets.US_ASCII),end=END.getBytes(StandardCharsets.US_ASCII);int matched=0,c;long count=0;
            while((c=in.read())!=-1){if(++count>MAX_FILE)throw new IOException("Die Projektdatei ist zu groß.");if(c==(start[matched]&255)){if(++matched==start.length)break;}else matched=c==(start[0]&255)?1:0;}
            if(matched!=start.length)throw new IOException("Keine Bodenwelle-Projektdatei erkannt.");matched=0;
            while((c=in.read())!=-1){if(++count>MAX_FILE)throw new IOException("Die Projektdatei ist zu groß.");if(c==(end[matched]&255)){if(++matched==end.length)return;}else{if(matched>0){out.write(end,0,matched);matched=0;}if(c==(end[0]&255))matched=1;else out.write(c);}}
            throw new IOException("Die Projektdatei ist unvollständig.");
        }
    }
    static File backup(Context c)throws Exception {File source=create(c),target=new File(directory(c,"browser-backups"),source.getName());Files.move(source.toPath(),target.toPath());return target;}
    static void rememberBackup(Context c,File file)throws IOException {
        c.getSharedPreferences("browser_projects",0).edit().putString("backup",file.getName()).apply();
        File[] saved=directory(c,"browser-backups").listFiles((d,n)->n.endsWith(".html"));if(saved!=null){Arrays.sort(saved,Comparator.comparingLong(File::lastModified).reversed());for(int i=3;i<saved.length;i++)if(!saved[i].equals(file))saved[i].delete();}
    }
    static File lastBackup(Context c)throws IOException {
        try(FindingStore store=new FindingStore(c);var rows=store.getReadableDatabase().rawQuery("SELECT backup FROM browser_imports WHERE backup<>'' ORDER BY applied_ms DESC,rowid DESC",null)) {
            while(rows.moveToNext()){String candidate=rows.getString(0);if(candidate.matches("[a-f0-9-]{36}\\.html")){File file=new File(directory(c,"browser-backups"),candidate);if(file.isFile())return file;}}
        }
        String name=c.getSharedPreferences("browser_projects",0).getString("backup","");if(!name.matches("[a-f0-9-]{36}\\.html"))return null;File f=new File(directory(c,"browser-backups"),name);return f.isFile()?f:null;}
    static void cleanup(Context c){for(String dir:List.of("browser-out","browser-stage")){File[] files=new File(c.getNoBackupFilesDir(),dir).listFiles();if(files!=null)for(File f:files)if(System.currentTimeMillis()-f.lastModified()>24*60*60*1000L)remove(f);}}
    static void clearLocal(Context c){for(String dir:List.of("browser-baselines","browser-backups","browser-out","browser-stage"))remove(new File(c.getNoBackupFilesDir(),dir));c.getSharedPreferences("browser_projects",0).edit().remove("backup").apply();}
    private static void remove(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);file.delete();}
}
