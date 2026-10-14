package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class NavigationWorkflowTest {
    private Application app;
    @Before public void before() {
        app = RuntimeEnvironment.getApplication(); app.deleteDatabase("findings.db");
        RecordingService.running = false; OsmEnricher.enabled(app, false);
        new DetectionPreferences(app).setThreshold(40);
    }
    private long finding(FindingStore store, double latitude, boolean released, String note) {
        Location fix = new Location("gps"); fix.setLatitude(latitude); fix.setLongitude(13.405); fix.setAccuracy(5);
        long id = store.insert(fix, null); store.review(id, "root_heave", note, released); return id;
    }
    private MapView map(View view) {
        if (view instanceof MapView m) return m;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) { MapView m = map(group.getChildAt(i)); if (m != null) return m; }
        return null;
    }
    private int buttons(View view, int id) {
        int count = view.getId() == id ? 1 : 0;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) count += buttons(group.getChildAt(i), id);
        return count;
    }
    private boolean containsText(View view, String text) {
        if (view instanceof android.widget.TextView t && t.getText().toString().contains(text)) return true;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) if (containsText(group.getChildAt(i), text)) return true;
        return false;
    }
    private void clickDialog(int button) {
        ShadowAlertDialog.getLatestAlertDialog().getButton(button).performClick(); Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    @Test public void ridePageKeepsMapAndViewportWhenNewMeasurementsArrive() {
        try (FindingStore store = new FindingStore(app)) { finding(store,52.52,false,""); }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup(); FindingStore store = new FindingStore(app)) {
            MainActivity activity = controller.get(); MapView map = map(activity.findViewById(android.R.id.content)); assertNotNull(map);
            assertNull(activity.findViewById(R.id.finding_delete)); assertNull(activity.findViewById(R.id.sensitivity_slider));
            assertNull(activity.findViewById(R.id.geodata_toggle)); assertNotNull(activity.findViewById(R.id.ride_start_button));
            map.getController().setCenter(new GeoPoint(52.48,13.31));
            finding(store,52.53,false,""); Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertSame(map,map(activity.findViewById(android.R.id.content))); assertEquals(52.48,map.getMapCenter().getLatitude(),.00001);
            assertEquals(2,map.getOverlays().stream().filter(o->o instanceof Marker).count());
        }
    }
    @Test public void filtersPreserveMixedBundleConsentAndSurviveRotation() {
        try (FindingStore store = new FindingStore(app)) {
            long released=finding(store,52.52,true,"Freigegeben"),open=finding(store,52.52,false,"Offen");
            finding(store,52.53,false,"Einzeln"); store.mergeBundles(List.of(-released,-open));
        }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup(); FindingStore store = new FindingStore(app)) {
            controller.get().findViewById(R.id.nav_findings).performClick(); assertNull(map(controller.get().findViewById(android.R.id.content)));
            assertEquals(2,buttons(controller.get().findViewById(android.R.id.content),R.id.finding_review));
            controller.get().findViewById(R.id.filter_reviewed).performClick();
            assertEquals(1,buttons(controller.get().findViewById(android.R.id.content),R.id.finding_review));
            assertTrue(containsText(controller.get().findViewById(android.R.id.content),"Teilweise freigegeben"));
            controller.recreate(); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertTrue(controller.get().findViewById(R.id.nav_findings).isSelected()); assertTrue(controller.get().findViewById(R.id.filter_reviewed).isSelected());
            assertEquals(1,buttons(controller.get().findViewById(android.R.id.content),R.id.finding_review)); assertEquals(1,store.all().stream().filter(Finding::reviewed).count());
            controller.get().findViewById(R.id.filter_open).performClick(); assertEquals(2,buttons(controller.get().findViewById(android.R.id.content),R.id.finding_review));
        }
    }
    @Test public void threeMeterProposalOpensBundlingWithoutAutomaticallyMergingData() {
        try (FindingStore store = new FindingStore(app)) { finding(store,52.52,false,""); finding(store,52.520027,false,""); }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup(); FindingStore store = new FindingStore(app)) {
            controller.get().findViewById(R.id.nav_findings).performClick(); View proposal=controller.get().findViewById(R.id.bundle_open); assertNotNull(proposal);
            proposal.performClick(); Intent intent=Shadows.shadowOf(controller.get()).getNextStartedActivity(); assertEquals(BundleActivity.class.getName(),intent.getComponent().getClassName());
            assertEquals(2,store.bundles().size()); assertEquals(0,store.all().stream().filter(Finding::reviewed).count());
        }
    }
    @Test public void numericSensitivityIsReachableUnderMoreAndPersistsWhenReopened() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().findViewById(R.id.nav_more).performClick(); controller.get().findViewById(R.id.menu_measurement).performClick();
            AlertDialog settings=ShadowAlertDialog.getLatestAlertDialog(); ((EditText)settings.findViewById(R.id.sensitivity_number)).setText("55");
            settings.findViewById(R.id.sensitivity_apply).performClick(); assertEquals(55,new DetectionPreferences(app).threshold()); clickDialog(AlertDialog.BUTTON_POSITIVE);
            controller.get().findViewById(R.id.menu_measurement).performClick(); assertEquals(50,((SeekBar)ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.sensitivity_slider)).getProgress());
            clickDialog(AlertDialog.BUTTON_POSITIVE); controller.get().onBackPressed(); assertTrue(controller.get().findViewById(R.id.nav_ride).isSelected());
        }
    }
    @Test public void csvActionInSubmenuExportsOnlyReleasedMeasurements() throws Exception {
        try (FindingStore store=new FindingStore(app)) { finding(store,52.52,true,"Freigegebene Notiz"); finding(store,52.53,false,"Private Notiz"); }
        try (ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().findViewById(R.id.nav_findings).performClick(); controller.get().findViewById(R.id.findings_actions).performClick();
            ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null,1,1); Shadows.shadowOf(Looper.getMainLooper()).idle();
            clickDialog(AlertDialog.BUTTON_POSITIVE); org.robolectric.shadows.ShadowActivity.IntentForResult request=Shadows.shadowOf(controller.get()).getNextStartedActivityForResult();
            assertEquals(Intent.ACTION_CREATE_DOCUMENT,request.intent.getAction());
            File file=File.createTempFile("navigation-export-",".csv",app.getCacheDir());
            controller.get().onActivityResult(request.requestCode,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(file)));
            String csv=new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8); assertTrue(csv.contains("Freigegebene Notiz")); assertFalse(csv.contains("Private Notiz")); assertEquals(2,csv.lines().count());
            file.delete();
        }
    }
    @Test public void routeDeletionInDataMenuNeedsConfirmationAndKeepsMeasurements() {
        try (FindingStore store=new FindingStore(app)) { finding(store,52.52,true,""); store.insertRoutePoint(new RoutePoint(1000,500,52.52,13.405,5,0)); }
        try (ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();FindingStore store=new FindingStore(app)) {
            controller.get().findViewById(R.id.nav_more).performClick(); controller.get().findViewById(R.id.menu_data).performClick();
            ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.delete_route).performClick(); clickDialog(AlertDialog.BUTTON_NEGATIVE);
            assertEquals(1,store.routeCount()); controller.get().findViewById(R.id.menu_data).performClick();
            ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.delete_route).performClick(); clickDialog(AlertDialog.BUTTON_POSITIVE);
            assertEquals(0,store.routeCount()); assertEquals(1,store.count()); assertTrue(store.all().get(0).reviewed());
        }
    }
}
