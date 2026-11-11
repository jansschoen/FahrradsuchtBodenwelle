package de.fahrradsucht.bodenwelle;

import android.app.*;
import android.location.Location;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35})
public class BundleMergeReviewTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");OsmEnricher.enabled(app,false);RecordingService.running=false;}
    private long add(FindingStore store,String category,String note,int hazards,String surface,Double height,boolean release) {
        Location location=new Location("gps");location.setLatitude(52.52);location.setLongitude(13.405);location.setAccuracy(5);
        long id=store.insert(location,new BumpDetector.Detection(0,48,40));Finding f=store.get(id);
        store.review(id,category,note,release,f.details().reviewed(height,48.0,hazards,"cycleway",true,surface,true));return id;
    }
    private void positive(){ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();}
    private <T extends View> List<T> views(View root,Class<T> type){List<T> result=new ArrayList<>();if(type.isInstance(root))result.add(type.cast(root));if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)result.addAll(views(group.getChildAt(i),type));return result;}
    private void select(Spinner spinner,String value){for(int i=0;i<spinner.getCount();i++)if(spinner.getItemAtPosition(i).toString().equals(value)){spinner.setSelection(i);Shadows.shadowOf(Looper.getMainLooper()).idle();return;}fail(value);}
    @Test public void matchingSharedPropertiesKeepTheShortConfirmationDespiteDifferentHeightsAndConsent() {
        try(FindingStore store=new FindingStore(app);var activity=Robolectric.buildActivity(MainActivity.class).setup()) {
            String note="G".repeat(20001);long a=add(store,"root_heave",note,1,"asphalt",4.0,true),b=add(store,"root_heave",note,1,"asphalt",9.0,false);
            BundleConfirmation.show(activity.get(),store,List.of(-a,-b),()->{});AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
            assertNull(dialog.findViewById(R.id.bundle_preset));assertNull(dialog.findViewById(R.id.bundle_result_preview));positive();assertEquals(note,store.get(a).note());
            assertEquals(store.get(a).bundleKey(),store.get(b).bundleKey());assertEquals(4.0,store.get(a).details().heightMm(),0);assertEquals(9.0,store.get(b).details().heightMm(),0);assertTrue(store.get(a).reviewed());assertFalse(store.get(b).reviewed());
        }
    }
    @Test public void differingPropertiesHaveAnEditablePreviewAndExplicitSourcePresetWhilePhotosAreUnited()throws Exception {
        try(FindingStore store=new FindingStore(app);var activity=Robolectric.buildActivity(MainActivity.class).setup()) {
            long a=add(store,"root_heave","Erste Beschreibung",1,"asphalt",4.0,true),b=add(store,"pothole","Zweite Beschreibung",16,"concrete",9.0,false);
            for(long id:List.of(a,b)){String name=UUID.randomUUID()+".jpg";var file=PhotoFiles.localFile(app,name);file.getParentFile().mkdirs();Files.write(file.toPath(),new byte[]{1,2,3});store.addPhoto(id,name,1,1,3);}
            Finding originalA=store.get(a),originalB=store.get(b);store.reportDistrict(a,"01");store.reportDistrict(b,"02");
            BundleConfirmation.show(activity.get(),store,List.of(-a,-b),()->{});positive();assertEquals(2,store.bundles().size());
            AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();select(dialog.findViewById(R.id.bundle_preset),"Angaben von Messung #"+b+" übernehmen");
            ((EditText)dialog.findViewById(R.id.bundle_note)).setText("Gemeinsam geprüft");
            assertTrue(((TextView)dialog.findViewById(R.id.bundle_result_preview)).getText().toString().contains("Schlagloch"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("Erste Beschreibung",store.get(a).note());assertEquals("asphalt",store.get(a).details().surface());
            BundleConfirmation.show(activity.get(),store,List.of(-a,-b),()->{});dialog=ShadowAlertDialog.getLatestAlertDialog();select(dialog.findViewById(R.id.bundle_preset),"Angaben von Messung #"+b+" übernehmen");((EditText)dialog.findViewById(R.id.bundle_note)).setText("Gemeinsam geprüft");positive();
            for(Finding original:List.of(originalA,originalB)){Finding actual=store.get(original.id());assertEquals("pothole",actual.category());assertEquals("Gemeinsam geprüft",actual.note());assertEquals(16,actual.details().hazards());assertEquals("concrete",actual.details().surface());assertEquals(2,store.photos(actual.id()).size());assertEquals(original.details().heightMm(),actual.details().heightMm());assertEquals(original.reviewed(),actual.reviewed());assertEquals(original.peak(),actual.peak());assertEquals(original.position(),actual.position());assertEquals(original.timeMillis(),actual.timeMillis());}
            assertEquals("01",store.reportDistrict(a));assertEquals("02",store.reportDistrict(b));
        }
    }
    @Test public void changedSourcesCannotBeOverwrittenByAnOldMergePreview() {
        try(FindingStore store=new FindingStore(app);var activity=Robolectric.buildActivity(MainActivity.class).setup()) {
            long a=add(store,"root_heave","A",1,"asphalt",null,true),b=add(store,"root_heave","B",1,"asphalt",null,false);
            BundleConfirmation.show(activity.get(),store,List.of(-a,-b),()->{});store.review(a,"pothole","Inzwischen geändert",true);positive();
            assertEquals(2,store.bundles().size());assertEquals("Inzwischen geändert",store.get(a).note());assertEquals("pothole",store.get(a).category());assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
        }
    }
    @Test public void hazardsNotesAndPhotoAssignmentsCountAsDifferencesButIndividualMeasurementsDoNot()throws Exception {
        try(FindingStore store=new FindingStore(app)) {
            long a=add(store,"root_heave","Gleich",1,"asphalt",4.0,true),b=add(store,"root_heave","Gleich",1,"asphalt",9.0,false);
            assertFalse(BundleDescription.differs(store,List.of(store.get(a),store.get(b))));
            store.review(b,"root_heave","Anders",false);assertTrue(BundleDescription.differs(store,List.of(store.get(a),store.get(b))));
            store.review(b,"root_heave","Gleich",false);String name=UUID.randomUUID()+".jpg";var file=PhotoFiles.localFile(app,name);file.getParentFile().mkdirs();Files.write(file.toPath(),new byte[]{1});store.addPhoto(a,name,1,1,1);
            assertTrue(BundleDescription.differs(store,List.of(store.get(a),store.get(b))));
            store.mergeBundles(List.of(-a,-b));assertFalse(BundleDescription.differs(store,store.observations(store.get(a).bundleKey())));
        }
    }
}
