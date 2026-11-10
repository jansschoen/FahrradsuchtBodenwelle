package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.location.Location;
import android.graphics.*;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35}) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BrowserExchangeTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");BrowserProject.clearLocal(app);PhotoFiles.clearLocal(app);OsmEnricher.enabled(app,false);RecordingService.running=false;}
    private long add(FindingStore s,double lat){Location l=new Location("gps");l.setLatitude(lat);l.setLongitude(13.405);l.setAccuracy(5);l.setSpeed(5);l.setBearing(0);long id=s.insert(l,new BumpDetector.Detection(0,48,40));s.review(id,"root_heave","Alte Notiz",true);return id;}
    private byte[] jpeg()throws Exception {Bitmap b=Bitmap.createBitmap(20,12,Bitmap.Config.ARGB_8888);b.eraseColor(0xff167b51);ByteArrayOutputStream out=new ByteArrayOutputStream();assertTrue(b.compress(Bitmap.CompressFormat.JPEG,90,out));b.recycle();return out.toByteArray();}
    private JSONObject payload(File file)throws Exception {String html=new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8),start="<script id=\"bodenwelle-project\" type=\"application/json\">";int at=html.indexOf(start)+start.length();return new JSONObject(html.substring(at,html.indexOf("</script>",at)));}
    private InputStream edited(File file,JSONObject data)throws Exception {String html=new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8),start="<script id=\"bodenwelle-project\" type=\"application/json\">";int at=html.indexOf(start)+start.length();return new ByteArrayInputStream((html.substring(0,at)+BrowserJson.safeJson(data)+html.substring(html.indexOf("</script>",at))).getBytes(StandardCharsets.UTF_8));}
    private void apply(BrowserProject.Loaded project,Map<String,Boolean> choices)throws Exception {try(FindingStore s=new FindingStore(app)){BrowserMerge plan=new BrowserMerge(BrowserSnapshot.edits(project.base),project.edits,BrowserSnapshot.currentEdits(s));BrowserDatabase.apply(app,project,plan.chosen(choices),plan.fingerprint);}}
    @Test public void fullFileContainsPhotosRouteAllDescriptionsAndReadOnlyEvidence()throws Exception {
        long id;try(FindingStore s=new FindingStore(app)){id=add(s,52.52);PhotoFiles.importStream(app,new ByteArrayInputStream(jpeg()),id);s.insertRoutePoint(new RoutePoint(1000,500,52.52,13.405,5,0));s.reportDistrict(id,"01");}
        File file=BrowserProject.create(app);try(var project=BrowserProject.load(app,new FileInputStream(file))) {
            assertEquals(1,project.base.getJSONObject("rows").length());assertEquals(1,project.blobs.size());assertEquals(1,project.base.getJSONArray("route").length());
            JSONObject raw=project.base.getJSONObject("rows").getJSONObject(""+id).getJSONObject("raw");assertEquals(48,raw.getDouble("peak"),0);assertEquals("01",project.edits.getJSONObject(""+id).getString("district"));
            assertTrue(project.base.getJSONObject("catalog").getJSONArray("bumps").length()>5);
        }
        assertTrue(file.getName().endsWith(".html"));
    }
    @Test public void changedDescriptionsRoundTripWithoutChangingRawMeasurementsAndKeepNewPhoneFindingsAndTrack()throws Exception {
        long id;Finding original;try(FindingStore s=new FindingStore(app)){id=add(s,52.52);original=s.get(id);s.insertRoutePoint(new RoutePoint(1000,500,52.52,13.405,5,0));}
        File file=BrowserProject.create(app);JSONObject p=payload(file),e=p.getJSONObject("edits").getJSONObject(""+id);e.put("category","pothole").put("note","Browseränderung").put("heightMm",7.5).put("hazards",17).put("traffic","shared_path").put("surface","concrete").put("reviewed",false).put("district","02");
        long fresh;try(FindingStore s=new FindingStore(app)){fresh=add(s,52.6);s.insertRoutePoint(new RoutePoint(2000,1000,52.6,13.405,5,1));new DetectionPreferences(app).setThreshold(60);}
        try(var loaded=BrowserProject.load(app,edited(file,p))){apply(loaded,Map.of());}
        try(FindingStore s=new FindingStore(app)){Finding f=s.get(id);assertEquals("Browseränderung",f.note());assertEquals("pothole",f.category());assertFalse(f.reviewed());assertEquals(17,f.details().hazards());assertEquals(7.5,f.details().heightMm(),0);assertEquals("concrete",f.details().surface());assertEquals("02",s.reportDistrict(id));assertNotNull(s.get(fresh));assertEquals(2,s.routeCount());assertEquals(original.position(),f.position());assertEquals(original.peak(),f.peak());assertEquals(original.timeMillis(),f.timeMillis());assertEquals(original.threshold(),f.threshold());assertEquals(60,new DetectionPreferences(app).threshold());}
        assertThrows(IOException.class,()->BrowserProject.load(app,edited(file,p)));
    }
    @Test public void unchangedFilePreservesNewerPhoneEditsAndDeletedPhoneMeasurements()throws Exception {
        long id;try(FindingStore s=new FindingStore(app)){id=add(s,52.52);}File file=BrowserProject.create(app);
        try(FindingStore s=new FindingStore(app)){s.review(id,"pothole","Am Handy geändert",false);}
        try(var loaded=BrowserProject.load(app,new FileInputStream(file));FindingStore s=new FindingStore(app)){BrowserMerge plan=new BrowserMerge(BrowserSnapshot.edits(loaded.base),loaded.edits,BrowserSnapshot.currentEdits(s));assertTrue(plan.parts.isEmpty());}
        try(FindingStore s=new FindingStore(app)){s.delete(id);}
        try(var loaded=BrowserProject.load(app,new FileInputStream(file));FindingStore s=new FindingStore(app)){assertTrue(new BrowserMerge(BrowserSnapshot.edits(loaded.base),loaded.edits,BrowserSnapshot.currentEdits(s)).parts.isEmpty());assertNull(s.get(id));}
    }
    @Test public void concurrentEditsRequireAnExplicitChoiceAndAPreviewCannotBeAppliedAfterAnotherChange()throws Exception {
        long id;try(FindingStore s=new FindingStore(app)){id=add(s,52.52);}File file=BrowserProject.create(app);JSONObject p=payload(file);p.getJSONObject("edits").getJSONObject(""+id).put("note","Browser");
        try(FindingStore s=new FindingStore(app)){s.review(id,"root_heave","Handy",true);}
        try(var loaded=BrowserProject.load(app,edited(file,p));FindingStore s=new FindingStore(app)) {
            BrowserMerge plan=new BrowserMerge(BrowserSnapshot.edits(loaded.base),loaded.edits,BrowserSnapshot.currentEdits(s));assertEquals(1,plan.conflicts());assertThrows(IOException.class,()->plan.chosen(Map.of()));
            assertEquals(0,plan.chosen(Map.of(plan.parts.get(0).key(),false)).length());
            JSONObject take=plan.chosen(Map.of(plan.parts.get(0).key(),true));s.review(id,"root_heave","Noch neuer",true);
            assertThrows(IOException.class,()->BrowserDatabase.apply(app,loaded,take,plan.fingerprint));assertEquals("Noch neuer",s.get(id).note());
        }
    }
    @Test public void deletingAndSplittingBundlesNeverDeletesNewPhoneMembersOrSharedPhotos()throws Exception {
        long a,b;try(FindingStore s=new FindingStore(app)){a=add(s,52.52);b=add(s,52.52001);PhotoFiles.importStream(app,new ByteArrayInputStream(jpeg()),a);s.mergeBundles(List.of(-a,-b));}
        File file=BrowserProject.create(app);JSONObject p=payload(file);p.getJSONObject("edits").put(""+a,JSONObject.NULL);p.getJSONObject("edits").getJSONObject(""+b).put("group","s"+b).put("note","Bleibt");
        long newer;try(FindingStore s=new FindingStore(app)){newer=add(s,52.52002);s.mergeBundles(List.of(s.get(b).bundleKey(),-newer));}
        try(var loaded=BrowserProject.load(app,edited(file,p));FindingStore s=new FindingStore(app)){BrowserMerge plan=new BrowserMerge(BrowserSnapshot.edits(loaded.base),loaded.edits,BrowserSnapshot.currentEdits(s));assertEquals(1,plan.conflicts());BrowserDatabase.apply(app,loaded,plan.chosen(Map.of(plan.parts.get(0).key(),true)),plan.fingerprint);}
        try(FindingStore s=new FindingStore(app)){assertNull(s.get(a));assertNotNull(s.get(newer));assertEquals("Bleibt",s.get(b).note());assertNotEquals(s.get(newer).bundleKey(),s.get(b).bundleKey());assertEquals(1,s.photos(newer).size());assertEquals(1,s.photos(b).size());assertTrue(PhotoFiles.localFile(app,s.photos(newer).get(0).filename()).isFile());}
    }
    @Test public void backupCanRestoreDeletedMeasurementsAndPhotosWhileKeepingLaterReadings()throws Exception {
        long id;try(FindingStore s=new FindingStore(app)){id=add(s,52.52);PhotoFiles.importStream(app,new ByteArrayInputStream(jpeg()),id);}File file=BrowserProject.create(app),backup=BrowserProject.backup(app);JSONObject p=payload(file);p.getJSONObject("edits").put(""+id,JSONObject.NULL);p.put("blobs",new JSONObject());
        try(var loaded=BrowserProject.load(app,edited(file,p))){apply(loaded,Map.of());}long fresh;try(FindingStore s=new FindingStore(app)){assertNull(s.get(id));fresh=add(s,52.6);}
        try(var loaded=BrowserProject.load(app,new FileInputStream(backup),true);FindingStore s=new FindingStore(app)){BrowserDatabase.apply(app,loaded,BrowserSnapshot.edits(loaded.base),BrowserJson.canonical(BrowserSnapshot.currentEdits(s)));}
        try(FindingStore s=new FindingStore(app)){assertNotNull(s.get(id));assertNotNull(s.get(fresh));assertEquals(1,s.photos(id).size());assertTrue(PhotoFiles.localFile(app,s.photos(id).get(0).filename()).isFile());}
    }
    @Test public void backupRestoresAnUnresolvedLegacyBundleWithoutDiscardingItsDifferentCategories()throws Exception {
        long a,b;try(FindingStore s=new FindingStore(app)){a=add(s,52.52);b=add(s,52.52001);s.mergeBundles(List.of(-a,-b));s.getWritableDatabase().execSQL("UPDATE findings SET category='pothole' WHERE id=?",new Object[]{b});}
        File backup=BrowserProject.backup(app);
        try(FindingStore s=new FindingStore(app)){s.review(a,"paving_displacement","Nach dem Import vereinheitlicht",true);}
        try(var loaded=BrowserProject.load(app,new FileInputStream(backup),true);FindingStore s=new FindingStore(app)){BrowserDatabase.apply(app,loaded,BrowserSnapshot.edits(loaded.base),BrowserJson.canonical(BrowserSnapshot.currentEdits(s)));}
        try(FindingStore s=new FindingStore(app)){assertEquals("root_heave",s.get(a).category());assertEquals("pothole",s.get(b).category());assertEquals(s.get(a).bundleKey(),s.get(b).bundleKey());}
    }
    @Test public void rawTamperingWrongDeviceInvalidGroupsAndUnknownPhotosAreRejectedBeforeWriting()throws Exception {
        long a,b;try(FindingStore s=new FindingStore(app)){a=add(s,52.52);b=add(s,52.52001);s.mergeBundles(List.of(-a,-b));}File file=BrowserProject.create(app);
        JSONObject p=payload(file);p.getJSONObject("base").getJSONObject("rows").getJSONObject(""+a).getJSONObject("raw").put("peak",999);
        assertThrows(IOException.class,()->BrowserProject.load(app,edited(file,p)));
        JSONObject wrong=payload(file);wrong.put("device",UUID.randomUUID().toString());assertThrows(IOException.class,()->BrowserProject.load(app,edited(file,wrong)));
        JSONObject inconsistent=payload(file);inconsistent.getJSONObject("edits").getJSONObject(""+a).put("note","Nur eine Hälfte geändert");assertThrows(IOException.class,()->BrowserProject.load(app,edited(file,inconsistent)));
        JSONObject unknown=payload(file);unknown.getJSONObject("edits").getJSONObject(""+a).getJSONArray("photos").put("new-"+UUID.randomUUID()+".jpg");assertThrows(IOException.class,()->BrowserProject.load(app,edited(file,unknown)));
        try(FindingStore s=new FindingStore(app)){assertEquals(48,s.get(a).peak(),0);assertEquals("Alte Notiz",s.get(a).note());}
    }
    @Test public void newBrowserPhotosAreDecodedAndSharedWithoutCopyingOriginalMetadata()throws Exception {
        long id;try(FindingStore s=new FindingStore(app)){id=add(s,52.52);}File file=BrowserProject.create(app);JSONObject p=payload(file);String key="new-"+UUID.randomUUID()+".jpg";p.getJSONObject("blobs").put(key,android.util.Base64.encodeToString(jpeg(),android.util.Base64.NO_WRAP));p.getJSONObject("edits").getJSONObject(""+id).getJSONArray("photos").put(key);
        try(var loaded=BrowserProject.load(app,edited(file,p))){apply(loaded,Map.of());}
        try(FindingStore s=new FindingStore(app)){assertEquals(1,s.photos(id).size());File saved=PhotoFiles.localFile(app,s.photos(id).get(0).filename());assertEquals(20,BitmapFactory.decodeFile(saved.getPath()).getWidth());assertFalse(saved.getName().startsWith("new-"));}
    }
    @Test public void unicodeAndScriptLikeNotesRemainDataAndExportEscapesTheClosingScriptTag()throws Exception {
        long id;try(FindingStore s=new FindingStore(app)){id=add(s,52.52);s.review(id,"pothole","<script>alert('x')</script> Straße 🌳 & Grüße",false);}File file=BrowserProject.create(app);
        try(var loaded=BrowserProject.load(app,new FileInputStream(file))){assertTrue(loaded.edits.getJSONObject(""+id).getString("note").contains("🌳"));assertTrue(loaded.edits.getJSONObject(""+id).getString("note").contains("</script>"));}
        String html=new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);assertFalse(html.contains("<script>alert('x')</script>"));
    }
    @Test public void schemaEightMigrationPreservesMeasurementsAndAddsImportReceipts()throws Exception {
        long id;try(FindingStore s=new FindingStore(app)){id=add(s,52.52);s.getWritableDatabase().execSQL("DROP TABLE browser_imports");}
        try(SQLiteDatabase old=app.openOrCreateDatabase("findings.db",0,null)){old.setVersion(8);}
        try(FindingStore s=new FindingStore(app)){assertEquals(9,s.getReadableDatabase().getVersion());assertNotNull(s.get(id));try(var c=s.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM browser_imports",null)){c.moveToFirst();assertEquals(0,c.getInt(0));}}
    }
    @Test public void committedBackupIsDiscoverableEvenWithoutAPreferencesUpdate()throws Exception {
        long id;try(FindingStore s=new FindingStore(app)){id=add(s,52.52);}File file=BrowserProject.create(app),backup=BrowserProject.backup(app);JSONObject p=payload(file);p.getJSONObject("edits").getJSONObject(""+id).put("note","Neu");
        try(var loaded=BrowserProject.load(app,edited(file,p));FindingStore s=new FindingStore(app)){BrowserMerge plan=new BrowserMerge(BrowserSnapshot.edits(loaded.base),loaded.edits,BrowserSnapshot.currentEdits(s));BrowserDatabase.apply(app,loaded,plan.chosen(Map.of()),plan.fingerprint,backup);}
        app.getSharedPreferences("browser_projects",0).edit().remove("backup").commit();assertEquals(backup,BrowserProject.lastBackup(app));
    }
}
