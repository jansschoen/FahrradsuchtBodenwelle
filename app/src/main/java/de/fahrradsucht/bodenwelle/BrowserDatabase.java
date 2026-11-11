package de.fahrradsucht.bodenwelle;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** One transaction changes editable fields, memberships and links; raw evidence comes only from the trusted export. */
final class BrowserDatabase {
    private BrowserDatabase(){}
    static void apply(Context c,BrowserProject.Loaded project,JSONObject chosen,String fingerprint)throws Exception {apply(c,project,chosen,fingerprint,null);}
    static void apply(Context c,BrowserProject.Loaded project,JSONObject chosen,String fingerprint,File backup)throws Exception {
        if(RecordingService.running)throw new IOException("Zum Rückimportieren bitte die Fahrt beenden.");
        Map<String,BrowserPhotos.Prepared> prepared=new HashMap<>();Set<String> needed=new HashSet<>();
        for(String id:BrowserMerge.keys(chosen))if(!chosen.isNull(id)){JSONArray a=chosen.getJSONObject(id).getJSONArray("photos");for(int i=0;i<a.length();i++)needed.add(a.getString(i));}
        for(String key:needed) {
            File source=project.blobs.get(key);if(source==null){prepared.put(key,phonePhoto(c,key));continue;}
            if(key.startsWith("new-"))prepared.put(key,BrowserPhotos.prepare(source,project.staging));
            else {JSONObject spec=project.base.getJSONObject("photos").getJSONObject(key);prepared.put(key,new BrowserPhotos.Prepared(key,source,spec.getInt("width"),spec.getInt("height"),source.length()));}
        }
        List<File> installed=new ArrayList<>();Set<String> obsolete=new HashSet<>();boolean success=false;
        try(FindingStore store=new FindingStore(c)) {
            SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
            try {
                if(RecordingService.running||!BrowserJson.canonical(BrowserSnapshot.currentEdits(store)).equals(fingerprint))throw new IOException("Die Handydaten haben sich seit der Vorschau geändert. Bitte die Datei erneut prüfen.");
                if(!project.allowConsumed)try(Cursor used=db.rawQuery("SELECT 1 FROM browser_imports WHERE export_id=?",new String[]{project.data.getString("export")})){if(used.moveToFirst())throw new IOException("Diese Projektdatei wurde bereits übernommen. Bitte neu exportieren.");}
                Map<Long,Set<Long>> previous=new LinkedHashMap<>();for(Finding f:store.all())if(f.bundleId()>0&&(!chosen.has(Long.toString(f.id()))||!chosen.isNull(Long.toString(f.id()))))previous.computeIfAbsent(f.bundleId(),k->new LinkedHashSet<>()).add(f.id());
                protectUnchangedGroups(project,chosen,previous);
                Set<String> beforeFiles=photoNames(db);
                for(String id:BrowserMerge.keys(chosen))db.delete("photo_links","finding_id=?",new String[]{id});
                db.execSQL("UPDATE finding_photos SET finding_id=(SELECT MIN(finding_id) FROM photo_links WHERE photo_id=finding_photos.id) WHERE EXISTS(SELECT 1 FROM photo_links WHERE photo_id=finding_photos.id)");
                db.execSQL("DELETE FROM finding_photos WHERE NOT EXISTS(SELECT 1 FROM photo_links WHERE photo_id=finding_photos.id)");
                Map<String,List<Long>> groups=new LinkedHashMap<>();
                for(String id:BrowserMerge.keys(chosen)) {
                    long number=Long.parseLong(id);
                    if(chosen.isNull(id)){db.delete("findings","id=?",new String[]{id});continue;}
                    JSONObject edit=chosen.getJSONObject(id);Finding current=store.get(number);
                    if(current==null){JSONObject raw=project.base.getJSONObject("rows").getJSONObject(id).getJSONObject("raw");ContentValues row=rawValues(raw);row.putNull("bundle_id");db.insertOrThrow("findings",null,row);current=store.get(number);}
                    FindingDetails details=current.details().reviewed(edit.isNull("heightMm")?null:edit.getDouble("heightMm"),current.peak(),edit.getInt("hazards"),edit.isNull("traffic")?"unknown":edit.getString("traffic"),!edit.isNull("traffic"),edit.isNull("surface")?"unknown":edit.getString("surface"),!edit.isNull("surface"));
                    ContentValues values=new ContentValues();values.put("category",edit.getString("category"));values.put("note",edit.getString("note"));values.put("reviewed",edit.getBoolean("reviewed")?1:0);values.put("report_district",edit.getString("district"));values.put("details",details.json());values.putNull("bundle_id");db.update("findings",values,"id=?",new String[]{id});
                    groups.computeIfAbsent(edit.getString("group"),k->new ArrayList<>()).add(number);
                }
                for(var group:groups.entrySet()) {
                    List<Long> ids=group.getValue();long bundle=targetBundle(db,project,group.getKey(),ids,previous);
                    for(long id:ids)if(bundle>0){ContentValues v=new ContentValues();v.put("bundle_id",bundle);db.update("findings",v,"id=?",new String[]{Long.toString(id)});}
                    List<Finding> members=new ArrayList<>();for(long id:ids)members.add(store.get(id));
                    SharedFindingDetails common=SharedFindingDetails.from(members);
                    // A backup must also restore an unresolved legacy bundle exactly as exported.
                    // Edited groups still require coherent shared fields, as validated on load.
                    if(common.conflicting()) {
                        for(long id:ids)if(!BrowserJson.same(chosen.getJSONObject(Long.toString(id)),project.base.getJSONObject("rows").getJSONObject(Long.toString(id)).getJSONObject("edit")))throw new IOException("Widersprüchliche gemeinsame Angaben.");
                    } else for(Finding member:members){ContentValues v=new ContentValues();v.put("details",common.apply(member.details()).json());db.update("findings",v,"id=?",new String[]{Long.toString(member.id())});}
                    JSONObject edit=chosen.getJSONObject(Long.toString(ids.get(0)));JSONArray photos=edit.getJSONArray("photos");
                    for(int i=0;i<photos.length();i++){
                        BrowserPhotos.Prepared p=prepared.get(photos.getString(i));long photo=photoId(db,p.filename());
                        if(photo==0){File target=PhotoFiles.localFile(c,p.filename());File parent=target.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Fotospeicher nicht verfügbar.");
                            if(!target.isFile()){Files.copy(p.source().toPath(),target.toPath());installed.add(target);}
                            ContentValues v=new ContentValues();v.put("finding_id",ids.get(0));v.put("filename",p.filename());v.put("width",p.width());v.put("height",p.height());v.put("bytes",p.bytes());photo=db.insertOrThrow("finding_photos",null,v);
                        }
                        for(long id:ids)db.execSQL("INSERT OR IGNORE INTO photo_links(photo_id,finding_id) VALUES(?,?)",new Object[]{photo,id});
                    }
                }
                db.execSQL("DELETE FROM finding_bundles WHERE id NOT IN(SELECT bundle_id FROM findings WHERE bundle_id IS NOT NULL)");
                db.execSQL("INSERT OR REPLACE INTO browser_imports(export_id,applied_ms,backup) VALUES(?,?,?)",new Object[]{project.data.getString("export"),System.currentTimeMillis(),backup==null?"":backup.getName()});
                db.setTransactionSuccessful();
                // Physical files are removed only after commit, so rollback can never lose a shared original.
                beforeFiles.removeAll(photoNames(db));obsolete.addAll(beforeFiles);
            }finally{db.endTransaction();}
            success=true;for(String name:obsolete)PhotoFiles.localFile(c,name).delete();FindingStore.browserChanged();
        }finally{if(!success)for(File file:installed)file.delete();}
    }
    private static BrowserPhotos.Prepared phonePhoto(Context c,String key)throws IOException {
        if(!key.matches("[a-f0-9-]{36}\\.jpg"))throw new IOException("Ein ausgewähltes Foto fehlt.");
        try(FindingStore store=new FindingStore(c);Cursor row=store.getReadableDatabase().rawQuery("SELECT width,height,bytes FROM finding_photos WHERE filename=?",new String[]{key})){
            File file=PhotoFiles.localFile(c,key);if(!row.moveToFirst()||!file.isFile())throw new IOException("Ein auf dem Handy ergänztes Foto fehlt. Bitte die Galerie prüfen.");
            return new BrowserPhotos.Prepared(key,file,row.getInt(0),row.getInt(1),row.getLong(2));
        }
    }
    private static long targetBundle(SQLiteDatabase db,BrowserProject.Loaded project,String label,List<Long> ids,Map<Long,Set<Long>> previous)throws JSONException {
        if(ids.size()==1&&label.equals("s"+ids.get(0)))return 0;
        Set<Long> desired=new HashSet<>(ids);long candidate=0;
        for(var old:previous.entrySet())if(desired.containsAll(old.getValue())) {
            if(label.equals("b"+old.getKey()))return old.getKey();
            if(candidate==0||old.getKey()<candidate)candidate=old.getKey();
        }
        if(candidate>0)return candidate;
        // Backups may restore a removed, trusted identity; never trust an arbitrary imported ID.
        if(label.matches("b[1-9][0-9]{0,17}")){
            long requested=Long.parseLong(label.substring(1));boolean trusted=true;
            for(long id:ids){JSONObject row=project.base.getJSONObject("rows").optJSONObject(Long.toString(id));trusted&=row!=null&&label.equals(row.getJSONObject("edit").getString("group"));}
            try(Cursor existing=db.rawQuery("SELECT 1 FROM finding_bundles WHERE id=?",new String[]{Long.toString(requested)})){if(trusted&&!existing.moveToFirst()){ContentValues value=new ContentValues();value.put("id",requested);db.insertOrThrow("finding_bundles",null,value);return requested;}}
        }
        return db.insertOrThrow("finding_bundles","id",new ContentValues());
    }
    private static void protectUnchangedGroups(BrowserProject.Loaded project,JSONObject chosen,Map<Long,Set<Long>> previous)throws JSONException,IOException {
        for(Set<Long> ids:previous.values()) {
            boolean remainingOutside=false,touched=false;for(long id:ids){String key=Long.toString(id);remainingOutside|=!chosen.has(key);touched|=chosen.optJSONObject(key)!=null;}
            if(!remainingOutside||!touched)continue;
            boolean explicitSplit=false;Map<String,Set<String>> destinations=new HashMap<>();
            for(long id:ids){String key=Long.toString(id);JSONObject row=project.base.getJSONObject("rows").optJSONObject(key),next=project.edits.optJSONObject(key);if(row==null||next==null)continue;String original=row.getJSONObject("edit").getString("group");destinations.computeIfAbsent(original,k->new HashSet<>()).add(next.getString("group"));if(original.startsWith("b")&&next.getString("group").equals("s"+key))explicitSplit=true;}
            explicitSplit|=destinations.values().stream().anyMatch(values->values.size()>1);
            if(!explicitSplit)throw new IOException("Die Fundstelle enthält inzwischen weitere gemeinsam gespeicherte Messungen. Ihre Bündelung bleibt erhalten. Bitte eine aktuelle Projektdatei exportieren und diese Fundstelle gemeinsam bearbeiten.");
        }
    }
    private static ContentValues rawValues(JSONObject raw)throws JSONException {ContentValues values=new ContentValues();for(String key:BrowserMerge.keys(raw)){Object v=raw.get(key);if(v==JSONObject.NULL)values.putNull(key);else if(v instanceof Number n){if(v instanceof Float||v instanceof Double)values.put(key,n.doubleValue());else values.put(key,n.longValue());}else values.put(key,v.toString());}return values;}
    private static Set<String> photoNames(SQLiteDatabase db){Set<String> result=new HashSet<>();try(Cursor c=db.rawQuery("SELECT filename FROM finding_photos",null)){while(c.moveToNext())result.add(c.getString(0));}return result;}
    private static long photoId(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT id FROM finding_photos WHERE filename=?",new String[]{name})){return c.moveToFirst()?c.getLong(0):0;}}
}
