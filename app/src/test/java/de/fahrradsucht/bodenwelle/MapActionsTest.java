package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class MapActionsTest {
    private Application app;
    @Before public void before() {
        app = RuntimeEnvironment.getApplication(); app.deleteDatabase("findings.db");
        OsmEnricher.enabled(app, false); RecordingService.running = false;
        try (FindingStore store = new FindingStore(app)) {
            Location fix = new Location("gps"); fix.setLatitude(52.52); fix.setLongitude(13.405); fix.setAccuracy(5);
            store.insert(fix, null); store.insertRoutePoint(new RoutePoint(1000, 500, 52.52, 13.405, 5, 0));
        }
    }
    private MapView map(View v) {
        if (v instanceof MapView m) return m;
        if (v instanceof ViewGroup g) for (int i = 0; i < g.getChildCount(); i++) { MapView m = map(g.getChildAt(i)); if (m != null) return m; }
        return null;
    }
    private CheckBox release(View v) {
        if (v instanceof CheckBox b && b.getText().toString().equals(app.getString(R.string.review_release))) return b;
        if (v instanceof ViewGroup g) for (int i = 0; i < g.getChildCount(); i++) { CheckBox b = release(g.getChildAt(i)); if (b != null) return b; }
        return null;
    }
    private void tapMarker(Activity activity) {
        MapView map = map(activity.findViewById(android.R.id.content)); assertNotNull(map);
        Marker marker = (Marker) map.getOverlays().stream().filter(o -> o instanceof Marker).findFirst().orElseThrow();
        Marker.OnMarkerClickListener click = ReflectionHelpers.getField(marker, "mOnMarkerClickListener");
        assertTrue(click.onMarkerClick(marker, map));
    }
    private void clickDialog(int button) {
        ShadowAlertDialog.getLatestAlertDialog().getButton(button).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    private void editor(Class<? extends Activity> type) {
        try (ActivityController<? extends Activity> controller = Robolectric.buildActivity(type).setup(); FindingStore store = new FindingStore(app)) {
            tapMarker(controller.get());
            assertEquals("Stelle prüfen und Fotos ergänzen", ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
            clickDialog(AlertDialog.BUTTON_POSITIVE);
            AlertDialog editor = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(editor.findViewById(R.id.photo_pick)); assertNotNull(editor.findViewById(R.id.photo_capture));
            CheckBox release = release(editor.findViewById(android.R.id.content)); assertNotNull(release); release.setChecked(true);
            clickDialog(AlertDialog.BUTTON_POSITIVE);
            assertTrue(store.all().get(0).reviewed()); assertEquals(1, store.routeCount());
        }
    }
    @Test public void embeddedMapPointOpensTheSharedEditorAndSavesIndividualConsent() { editor(MainActivity.class); }
    @Test public void fullScreenMapPointOpensTheSharedEditorAndSavesIndividualConsent() { editor(MapActivity.class); }
    @Test public void fullScreenMapDeleteRemovesMarkerButKeepsRideTrack() {
        try (ActivityController<MapActivity> controller = Robolectric.buildActivity(MapActivity.class).setup(); FindingStore store = new FindingStore(app)) {
            tapMarker(controller.get()); clickDialog(AlertDialog.BUTTON_NEUTRAL); clickDialog(AlertDialog.BUTTON_NEGATIVE);
            assertEquals(1, store.count()); tapMarker(controller.get()); clickDialog(AlertDialog.BUTTON_NEUTRAL); clickDialog(AlertDialog.BUTTON_POSITIVE);
            assertEquals(0, store.count()); assertEquals(1, store.routeCount());
            assertFalse(map(controller.get().findViewById(android.R.id.content)).getOverlays().stream().anyMatch(o -> o instanceof Marker));
        }
    }
    @Test public void fullScreenMapPhotoPickerAndRecalibrationUseNativeActivityAndServiceRequests() {
        try (ActivityController<MapActivity> controller = Robolectric.buildActivity(MapActivity.class).setup()) {
            tapMarker(controller.get()); clickDialog(AlertDialog.BUTTON_POSITIVE);
            ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.photo_pick).performClick();
            org.robolectric.shadows.ShadowActivity.IntentForResult request = Shadows.shadowOf(controller.get()).getNextStartedActivityForResult();
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.intent.getAction());
            controller.get().onActivityResult(request.requestCode, Activity.RESULT_CANCELED, null);
            ShadowAlertDialog.getLatestAlertDialog().dismiss();
            RecordingService.running = true;
            controller.get().findViewById(R.id.recalibrate).performClick();
            assertEquals(RecordingService.CALIBRATE, Shadows.shadowOf(app).getNextStartedService().getAction());
            RecordingService.running = false;
        }
    }
}
