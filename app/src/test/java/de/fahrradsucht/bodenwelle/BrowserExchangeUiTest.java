package de.fahrradsucht.bodenwelle;

import android.app.*;
import android.content.Intent;
import android.net.Uri;
import android.location.Location;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35})
public class BrowserExchangeUiTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");BrowserProject.clearLocal(app);OsmEnricher.enabled(app,false);RecordingService.running=false;}
    private long finding(FindingStore s){Location l=new Location("gps");l.setLatitude(52.52);l.setLongitude(13.405);l.setAccuracy(5);long id=s.insert(l,null);s.review(id,"root_heave","Original",false);return id;}
    private void waitFor(BrowserExchangeActivity activity)throws Exception {long deadline=System.nanoTime()+15_000_000_000L;while(System.nanoTime()<deadline){Shadows.shadowOf(Looper.getMainLooper()).idle();if(!ReflectionHelpers.<Boolean>getField(activity,"busy")){Shadows.shadowOf(Looper.getMainLooper()).idle();return;}Thread.sleep(10);}fail("Browser project operation did not complete");}
    private <T extends View>List<T> views(View v,Class<T> type){List<T> result=new ArrayList<>();if(type.isInstance(v))result.add(type.cast(v));if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)result.addAll(views(g.getChildAt(i),type));return result;}
    private File modified(File exported,String note)throws Exception {
        String html=new String(Files.readAllBytes(exported.toPath()),StandardCharsets.UTF_8),start="<script id=\"bodenwelle-project\" type=\"application/json\">";int at=html.indexOf(start)+start.length(),end=html.indexOf("</script>",at);JSONObject p=new JSONObject(html.substring(at,end));p.getJSONObject("edits").getJSONObject("1").put("note",note);File f=new File(app.getCacheDir(),"edited.html");Files.write(f.toPath(),(html.substring(0,at)+BrowserJson.safeJson(p)+html.substring(end)).getBytes(StandardCharsets.UTF_8));return f;
    }
    @Test public void exportButtonPreparesStandaloneFileAndUsesAndroidDocumentPicker()throws Exception {
        try(FindingStore s=new FindingStore(app)){finding(s);}
        try(var c=Robolectric.buildActivity(BrowserExchangeActivity.class).setup()) {
            c.get().findViewById(R.id.browser_export).performClick();waitFor(c.get());Intent picker=Shadows.shadowOf(c.get()).getNextStartedActivity();assertNotNull(picker);assertEquals(Intent.ACTION_CREATE_DOCUMENT,picker.getAction());assertEquals("text/html",picker.getType());
            File pending=ReflectionHelpers.getField(c.get(),"pendingFile");assertTrue(pending.isFile());try(var project=BrowserProject.load(app,new FileInputStream(pending))){assertEquals(1,project.edits.length());}
        }
    }
    @Test public void conflictChoiceSurvivesRotationConfirmationCanBeCancelledAndBackupRestoresThePhoneState()throws Exception {
        try(FindingStore s=new FindingStore(app)){finding(s);}File edited=modified(BrowserProject.create(app),"Browser");
        try(FindingStore s=new FindingStore(app)){s.review(1,"pothole","Neuer Handystand",true);}
        try(var c=Robolectric.buildActivity(BrowserExchangeActivity.class).setup();FindingStore s=new FindingStore(app)) {
            c.get().onActivityResult(71,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(edited)));waitFor(c.get());assertNotNull(c.get().findViewById(R.id.browser_apply));
            c.get().findViewById(R.id.browser_apply).performClick();assertTrue(((TextView)c.get().findViewById(R.id.browser_status)).getText().toString().contains("für jeden Konflikt"));assertEquals("Neuer Handystand",s.get(1).note());
            List<Spinner> fields=views(c.get().findViewById(android.R.id.content),Spinner.class);assertEquals(1,fields.size());fields.get(0).setSelection(2);
            c.recreate();waitFor(c.get());assertEquals(2,views(c.get().findViewById(android.R.id.content),Spinner.class).get(0).getSelectedItemPosition());
            c.get().findViewById(R.id.browser_apply).performClick();ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("Neuer Handystand",s.get(1).note());
            c.get().findViewById(R.id.browser_apply).performClick();ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitFor(c.get());assertEquals("Browser",s.get(1).note());assertNotNull(BrowserProject.lastBackup(app));long fresh=finding(s);
            c.get().findViewById(R.id.browser_restore).performClick();ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitFor(c.get());assertEquals("Neuer Handystand",s.get(1).note());assertEquals("pothole",s.get(1).category());assertNotNull(s.get(fresh));
        }
    }
    @Test public void deleteAllAlsoInvalidatesBrowserBaselinesAndRemovesBackupCopies()throws Exception {
        File external;try(FindingStore s=new FindingStore(app)){finding(s);}File exported=BrowserProject.create(app);external=new File(app.getCacheDir(),"outside-project.html");Files.copy(exported.toPath(),external.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);File backup=BrowserProject.backup(app);BrowserProject.rememberBackup(app,backup);
        try(FindingStore s=new FindingStore(app)){s.deleteAll();}assertFalse(backup.exists());assertThrows(IOException.class,()->BrowserProject.load(app,new FileInputStream(external)));
    }
}
