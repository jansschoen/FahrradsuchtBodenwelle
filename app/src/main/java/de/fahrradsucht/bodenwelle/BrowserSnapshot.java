package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.database.Cursor;
import org.json.*;
import java.io.*;
import java.util.*;

/** Read-only raw evidence and a separate, validated set of editable observations. */
final class BrowserSnapshot {
    private BrowserSnapshot(){}
    static JSONObject edit(FindingStore store,Finding f)throws JSONException {
        FindingDetails d=f.details();JSONArray photos=new JSONArray();for(FindingPhoto p:store.photos(f.id()))photos.put(p.filename());
        return new JSONObject().put("category",f.category()).put("note",f.note()).put("hazards",d.hazards())
                .put("traffic",d.trafficManual()?d.traffic():JSONObject.NULL).put("surface",d.surfaceManual()?d.surface():JSONObject.NULL)
                .put("heightMm",d.heightMm()==null?JSONObject.NULL:d.heightMm()).put("reviewed",f.reviewed()).put("district",store.reportDistrict(f.id()))
                .put("group",f.bundleId()>0?"b"+f.bundleId():"s"+f.id()).put("photos",photos);
    }
    static JSONObject snapshot(Context c,FindingStore store)throws IOException,JSONException {
        JSONObject rows=new JSONObject(),photos=new JSONObject();
        try(Cursor cursor=store.getReadableDatabase().rawQuery("SELECT * FROM findings ORDER BY id",null)) {
            while(cursor.moveToNext()) {
                JSONObject raw=new JSONObject();for(int i=0;i<cursor.getColumnCount();i++)raw.put(cursor.getColumnName(i),switch(cursor.getType(i)){case Cursor.FIELD_TYPE_NULL->JSONObject.NULL;case Cursor.FIELD_TYPE_INTEGER->cursor.getLong(i);case Cursor.FIELD_TYPE_FLOAT->cursor.getDouble(i);case Cursor.FIELD_TYPE_STRING->cursor.getString(i);case Cursor.FIELD_TYPE_BLOB->throw new IllegalStateException("Unerwartete Binärdaten in den Messwerten.");default->throw new IllegalStateException("Unbekannter Datenbanktyp.");});
                Finding f=store.get(raw.getLong("id"));JSONObject row=new JSONObject().put("raw",raw).put("edit",edit(store,f));rows.put(Long.toString(f.id()),row);
                for(FindingPhoto photo:store.photos(f.id()))if(!photos.has(photo.filename())) {
                    File file=PhotoFiles.localFile(c,photo.filename());if(!file.isFile())throw new IOException("Ein gespeichertes Foto fehlt. Bitte die Fotogalerie prüfen.");
                    photos.put(photo.filename(),new JSONObject().put("sha256",BrowserJson.sha(file)).put("width",photo.width()).put("height",photo.height()).put("bytes",file.length()));
                }
            }
        }
        JSONArray route=new JSONArray();for(RoutePoint p:store.route())route.put(new JSONObject().put("time_ms",p.timeMillis()).put("elapsed_ms",p.elapsedMillis()).put("latitude",p.latitude()).put("longitude",p.longitude()).put("accuracy",p.accuracy()).put("segment",p.segment()));
        var mail=c.getSharedPreferences("mail_reports",Context.MODE_PRIVATE);
        JSONObject settings=new JSONObject().put("threshold",new DetectionPreferences(c).threshold()).put("geodata",OsmEnricher.enabled(c))
                .put("name",mail.getString("name","")).put("introduction",mail.getString("introduction",""));
        JSONObject catalog=new JSONObject().put("bumps",choices(Classification.BUMPS)).put("roads",choices(Classification.ROADS)).put("surfaces",choices(Classification.SURFACES)).put("hazards",choices(Classification.HAZARDS));
        JSONArray districts=new JSONArray().put(new JSONObject().put("id","").put("label","Automatisch / offen"));for(var a:new DistrictCatalog(c).all())districts.put(new JSONObject().put("id",a.id()).put("label",a.name()));catalog.put("districts",districts);
        return new JSONObject().put("catalog",catalog).put("rows",rows).put("photos",photos).put("route",route).put("settings",settings);
    }
    private static JSONArray choices(Classification.Choice[] choices)throws JSONException{JSONArray a=new JSONArray();for(var c:choices)a.put(new JSONObject().put("id",c.id()).put("label",c.label()));return a;}
    static JSONObject edits(JSONObject base)throws JSONException {JSONObject edits=new JSONObject(),rows=base.getJSONObject("rows");for(Iterator<String> it=rows.keys();it.hasNext();){String id=it.next();edits.put(id,rows.getJSONObject(id).getJSONObject("edit"));}return edits;}
    static JSONObject currentEdits(FindingStore store)throws JSONException {JSONObject edits=new JSONObject();for(Finding f:store.all())edits.put(Long.toString(f.id()),edit(store,f));return edits;}
    static final Set<String> FIELDS=Set.of("category","note","hazards","traffic","surface","heightMm","reviewed","district","group","photos");
    static void validate(JSONObject base,JSONObject edits,Set<String> photoKeys)throws IOException,JSONException {
        JSONObject rows=base.getJSONObject("rows");if(rows.length()>10000||edits.length()!=rows.length())throw new IOException("Die Anzahl der Messungen stimmt nicht mit der exportierten Datei überein.");
        Map<String,List<String>> groups=new HashMap<>();
        for(Iterator<String> it=edits.keys();it.hasNext();) {
            String id=it.next();if(!rows.has(id))throw new IOException("Unbekannte Messungsnummer.");if(edits.isNull(id))continue;
            JSONObject e=edits.getJSONObject(id);if(e.length()!=FIELDS.size())throw new IOException("Unvollständige Beschreibung.");for(String k:FIELDS)if(!e.has(k))throw new IOException("Ein Beschreibungsfeld fehlt.");
            for(String key:List.of("category","note","district","group"))if(!(e.get(key) instanceof String))throw new IOException("Ungültiger Textwert.");
            if(!(e.get("hazards") instanceof Number)||e.getDouble("hazards")!=e.getInt("hazards"))throw new IOException("Ungültige Gefährdungsmerkmale.");
            if(!e.isNull("heightMm")&&!(e.get("heightMm") instanceof Number))throw new IOException("Ungültige gemessene Höhe.");
            for(String key:List.of("traffic","surface"))if(!e.isNull(key)&&!(e.get(key) instanceof String))throw new IOException("Ungültige Oberflächen-/Verkehrsflächenangabe.");
            choice(Classification.BUMPS,e.getString("category"));if(e.getString("note").length()>20000&&!BrowserJson.same(e.get("note"),rows.getJSONObject(id).getJSONObject("edit").get("note")))throw new IOException("Eine Beschreibung ist zu lang.");
            int hazards=e.getInt("hazards");if(hazards<0||hazards>=1<<Classification.HAZARDS.length)throw new IOException("Ungültige Gefährdungsmerkmale.");
            if(!e.isNull("traffic"))choice(Classification.ROADS,e.getString("traffic"));if(!e.isNull("surface"))choice(Classification.SURFACES,e.getString("surface"));
            if(!e.isNull("heightMm")){double height=e.getDouble("heightMm");if(!Double.isFinite(height)||height<0)throw new IOException("Ungültige gemessene Höhe.");}
            if(!(e.get("reviewed") instanceof Boolean))throw new IOException("Ungültige Freigabe.");
            String district=e.getString("district");if(!district.isEmpty()&&!district.matches("0[1-9]|1[0-2]"))throw new IOException("Ungültiger Bezirk.");
            String group=e.getString("group");if(!group.matches("[A-Za-z0-9_-]{1,100}"))throw new IOException("Ungültige Bündelung.");
            JSONArray photos=e.getJSONArray("photos");Set<String> unique=new HashSet<>();for(int i=0;i<photos.length();i++){String p=photos.getString(i);if(!photoKeys.contains(p)||!unique.add(p))throw new IOException("Ein Foto fehlt oder wurde doppelt zugeordnet.");}
            groups.computeIfAbsent(group,k->new ArrayList<>()).add(id);
        }
        for(List<String> members:groups.values()) {
            boolean unchanged=true;for(String id:members)unchanged&=BrowserJson.same(edits.getJSONObject(id),rows.getJSONObject(id).getJSONObject("edit"));
            JSONObject first=edits.getJSONObject(members.get(0));
            for(String id:members)for(String key:List.of("category","note","hazards","traffic","surface","photos"))
                if(!BrowserJson.same(edits.getJSONObject(id).get(key),first.get(key))&&!unchanged)throw new IOException("Die gemeinsamen Angaben einer gebündelten Stelle widersprechen sich. Bitte im Browser abgleichen.");
            if(first.getJSONArray("photos").length()>PhotoFiles.MAX_PER_OBSERVATION*members.size()) {
                for(String id:members)if(!BrowserJson.same(edits.getJSONObject(id).get("photos"),rows.getJSONObject(id).getJSONObject("edit").get("photos")))throw new IOException("Der gemeinsame Fotospeicher dieser Stelle reicht für zusätzliche Fotos nicht aus.");
            }
        }
    }
    private static void choice(Classification.Choice[] choices,String id)throws IOException {for(var c:choices)if(c.id().equals(id))return;throw new IOException("Unbekannte Beschreibungskategorie.");}
}
