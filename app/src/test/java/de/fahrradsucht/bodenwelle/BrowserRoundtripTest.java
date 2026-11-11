package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.location.Location;
import android.graphics.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Two explicit phases around a real desktop-browser run (tools/browser/test-editor.py). */
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BrowserRoundtripTest {
    private Application app;private File fixture;
    @Before public void before(){String path=System.getProperty("bodenwelle.browser.fixture");Assume.assumeTrue(path!=null);fixture=new File(path);fixture.mkdirs();app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");BrowserProject.clearLocal(app);PhotoFiles.clearLocal(app);OsmEnricher.enabled(app,false);RecordingService.running=false;}
    private long add(FindingStore s,double lat,double lon,String category,String note,boolean released){Location l=new Location("gps");l.setLatitude(lat);l.setLongitude(lon);l.setAccuracy(5);l.setSpeed(5);long id=s.insert(l,new BumpDetector.Detection(0,48+lat%2,40));s.review(id,category,note,released);return id;}
    private JSONObject data(File file)throws Exception {String text=new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8),prefix="<script id=\"bodenwelle-project\" type=\"application/json\">";int p=text.indexOf(prefix)+prefix.length();return new JSONObject(text.substring(p,text.indexOf("</script>",p)));}
    @Test public void exportFixture()throws Exception {
        try(FindingStore s=new FindingStore(app)) {
            add(s,52.5192,13.398,"root_heave","Beispiel: Aufwölbung am rechten Rand.",false);
            add(s,52.51921,13.398,"root_heave","Beispiel: dieselbe Stelle erneut beobachtet.",true);
            add(s,52.522,13.412,"paving_displacement","Beispiel: versetzte Pflasterkante.",false);
            add(s,52.52,13.405,"pothole","Beispiel: Vertiefung an der Querung.",true);
            for(int i=0;i<30;i++)s.insertRoutePoint(new RoutePoint(1_800_000_000_000L+i*1000,i*1000,52.5192+i*.0001,13.398+i*.0002,5,0));
            Bitmap b=Bitmap.createBitmap(480,300,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(0xffc8dcd0);Paint p=new Paint(3);p.setColor(0xff476b59);p.setTextSize(28);canvas.drawText("BEISPIELFOTO",50,150,p);ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,85,out);b.recycle();PhotoFiles.importStream(app,new ByteArrayInputStream(out.toByteArray()),1);
        }
        File html=BrowserProject.create(app);JSONObject project=data(html);project.getJSONObject("base").put("example",true);
        String prefix="<script id=\"bodenwelle-project\" type=\"application/json\">",text=new String(Files.readAllBytes(html.toPath()),StandardCharsets.UTF_8);int at=text.indexOf(prefix)+prefix.length();
        Files.write(new File(fixture,"original.html").toPath(),(text.substring(0,at)+BrowserJson.safeJson(project)+text.substring(text.indexOf("</script>",at))).getBytes(StandardCharsets.UTF_8));
        Files.write(new File(fixture,"baseline.json").toPath(),project.getJSONObject("base").toString().getBytes(StandardCharsets.UTF_8));
        Files.write(new File(fixture,"identity.json").toPath(),new JSONObject().put("device",project.getString("device")).put("export",project.getString("export")).toString().getBytes(StandardCharsets.UTF_8));
    }
    @Test public void importEditedFixture()throws Exception {
        JSONObject identity=new JSONObject(new String(Files.readAllBytes(new File(fixture,"identity.json").toPath()),StandardCharsets.UTF_8));
        app.getSharedPreferences("browser_projects",0).edit().putString("device",identity.getString("device")).commit();
        Files.copy(new File(fixture,"baseline.json").toPath(),new File(BrowserProject.directory(app,"browser-baselines"),identity.getString("export")+".json").toPath());
        // Recreate the exact trusted phone snapshot from the first phase in a fresh test application.
        try(var initial=BrowserProject.load(app,new FileInputStream(new File(fixture,"original.html")),true);FindingStore s=new FindingStore(app)) {
            BrowserDatabase.apply(app,initial,BrowserSnapshot.edits(initial.base),BrowserJson.canonical(BrowserSnapshot.currentEdits(s)));
            s.getWritableDatabase().delete("browser_imports",null,null);
            JSONArray route=initial.base.getJSONArray("route");for(int i=0;i<route.length();i++){JSONObject p=route.getJSONObject(i);s.insertRoutePoint(new RoutePoint(p.getLong("time_ms"),p.getLong("elapsed_ms"),p.getDouble("latitude"),p.getDouble("longitude"),(float)p.getDouble("accuracy"),p.getInt("segment")));}
        }
        Finding original;long fresh;try(FindingStore s=new FindingStore(app)){original=s.get(1);fresh=add(s,52.53,13.42,"other","Neu auf dem Handy",false);}
        try(var changed=BrowserProject.load(app,new FileInputStream(new File(fixture,"edited.html")));FindingStore s=new FindingStore(app)) {
            BrowserMerge plan=new BrowserMerge(BrowserSnapshot.edits(changed.base),changed.edits,BrowserSnapshot.currentEdits(s));assertEquals(0,plan.conflicts());
            BrowserDatabase.apply(app,changed,plan.chosen(Map.of()),plan.fingerprint);
        }
        try(FindingStore s=new FindingStore(app)) {
            assertEquals("Im Browser geprüft – sichere Rückfahrt 🌳",s.get(1).note());assertEquals("pothole",s.get(1).category());assertEquals(7.5,s.get(1).details().heightMm(),0);assertTrue(s.get(1).reviewed());assertEquals("02",s.reportDistrict(1));assertEquals(2,s.photos(1).size());
            assertEquals(s.get(2).bundleKey(),s.get(3).bundleKey());assertEquals("Gemeinsame Browserbeschreibung",s.get(3).note());assertNull(s.get(4));assertNotNull(s.get(fresh));assertEquals("Neu auf dem Handy",s.get(fresh).note());assertEquals(30,s.routeCount());assertEquals(original.position(),s.get(1).position());assertEquals(original.peak(),s.get(1).peak());assertEquals(original.timeMillis(),s.get(1).timeMillis());
            for(long id:List.of(2L,3L)){assertEquals("asphalt",s.get(id).details().surface());assertEquals("cycleway",s.get(id).details().traffic());assertEquals(2,s.get(id).details().hazards());}
        }
        Files.write(new File(fixture,"roundtrip-ok.txt").toPath(),"Android → lokales HTML → Chromium → gespeichertes HTML → Android: OK\n".getBytes(StandardCharsets.UTF_8));
    }
}
