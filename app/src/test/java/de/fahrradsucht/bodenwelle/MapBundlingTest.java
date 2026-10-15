package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class MapBundlingTest {
    private Application app;
    @Before public void before() {
        app = RuntimeEnvironment.getApplication(); app.deleteDatabase("findings.db"); OsmEnricher.enabled(app, false); RecordingService.running = false;
        try (FindingStore store = new FindingStore(app)) {
            Location fix = new Location("gps"); fix.setLatitude(52.52); fix.setLongitude(13.405); fix.setAccuracy(5);
            store.insert(fix, null); store.insert(fix, null); store.review(1, "root_heave", "Erhalten", true);
        }
    }
    private MapView map(View view) {
        if (view instanceof MapView m) return m;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) { MapView m = map(group.getChildAt(i)); if (m != null) return m; }
        return null;
    }
    private void tap(Activity activity) {
        MapView map = map(activity.findViewById(android.R.id.content));
        Marker marker = (Marker) map.getOverlays().stream().filter(o -> o instanceof Marker).findFirst().orElseThrow();
        Marker.OnMarkerClickListener click = ReflectionHelpers.getField(marker, "mOnMarkerClickListener"); click.onMarkerClick(marker, map);
    }
    private void button(int which) { ShadowAlertDialog.getLatestAlertDialog().getButton(which).performClick(); Shadows.shadowOf(Looper.getMainLooper()).idle(); }
    private void selectBoth(Activity activity) {
        activity.findViewById(R.id.map_options).performClick(); ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.map_bundle_start).performClick();
        tap(activity); AlertDialog picker = ShadowAlertDialog.getLatestAlertDialog();
        picker.getListView().performItemClick(picker.getListView().getChildAt(0), 0, 0);
        picker.getListView().performItemClick(picker.getListView().getChildAt(1), 1, 1);
        button(AlertDialog.BUTTON_POSITIVE); assertTrue(activity.findViewById(R.id.map_bundle_confirm).isEnabled());
    }
    private void merge(Class<? extends Activity> type) {
        try (var controller = Robolectric.buildActivity(type).setup(); FindingStore store = new FindingStore(app)) {
            Activity activity = controller.get(); selectBoth(activity); activity.findViewById(R.id.map_bundle_confirm).performClick();
            button(AlertDialog.BUTTON_NEGATIVE); assertEquals(2, store.bundles().size());
            activity.findViewById(R.id.map_bundle_confirm).performClick(); button(AlertDialog.BUTTON_POSITIVE);
            assertEquals(2, store.count()); assertEquals(1, store.bundles().size()); assertTrue(store.get(1).reviewed());
            assertFalse(store.get(2).reviewed()); assertEquals("Erhalten", store.get(1).note());
            assertEquals("#1, #2", store.bundles().get(0).references());
        }
    }
    @Test public void embeddedMapCanMergeIdenticalCoordinatesAfterExplicitConfirmation() { merge(MainActivity.class); }
    @Test public void fullScreenMapCanMergeIdenticalCoordinatesAfterExplicitConfirmation() { merge(MapActivity.class); }
    @Test public void pointDialogStartsMapSelectionWithThatFindingAlreadySelected() {
        try (var controller = Robolectric.buildActivity(MapActivity.class).setup(); FindingStore store = new FindingStore(app)) {
            tap(controller.get()); AlertDialog chooser = ShadowAlertDialog.getLatestAlertDialog();
            chooser.getListView().performItemClick(null, 0, 0); Shadows.shadowOf(Looper.getMainLooper()).idle();
            ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.map_bundle_start).performClick();
            assertEquals(View.VISIBLE, ((View) controller.get().findViewById(R.id.map_bundle_confirm).getParent()).getVisibility());
            assertFalse(controller.get().findViewById(R.id.map_bundle_confirm).isEnabled());
            controller.get().findViewById(R.id.map_bundle_cancel).performClick(); assertEquals(2, store.bundles().size());
        }
    }
    @Test public void selectionSurvivesRotationAndCanBeCancelledWithoutChangingMeasurements() {
        try (var controller = Robolectric.buildActivity(MapActivity.class).setup(); FindingStore store = new FindingStore(app)) {
            selectBoth(controller.get()); Bundle state = new Bundle(); controller.saveInstanceState(state).pause().stop().destroy();
            try (var restored = Robolectric.buildActivity(MapActivity.class).create(state).start().resume().visible()) {
                assertTrue(restored.get().findViewById(R.id.map_bundle_confirm).isEnabled());
                restored.get().findViewById(R.id.map_bundle_cancel).performClick(); assertEquals(2, store.bundles().size());
                assertEquals(View.GONE, ((View) restored.get().findViewById(R.id.map_bundle_confirm).getParent().getParent()).getVisibility());
            }
        }
    }
    @Test public void mapStillIncludesTheOldestMeasurementBeyondFiveHundredFindingsAndAfterClearingTheTrack() {
        try (FindingStore store = new FindingStore(app)) {
            Location fix = new Location("gps"); fix.setLatitude(52.52); fix.setLongitude(13.405); fix.setAccuracy(5);
            for (int i = 0; i < 510; i++) store.insert(fix, null);
            store.clearRoute();
            try (var controller = Robolectric.buildActivity(MapActivity.class).setup()) {
                MapView map = map(controller.get().findViewById(android.R.id.content));
                List<MapClusters.Cluster> clusters = map.getOverlays().stream().filter(o -> o instanceof Marker)
                        .map(o -> (MapClusters.Cluster) ((Marker)o).getRelatedObject()).toList();
                assertEquals(512, clusters.stream().mapToInt(MapClusters.Cluster::count).sum());
                assertTrue(clusters.stream().flatMap(c -> c.bundles().stream()).flatMap(b -> b.observations().stream()).anyMatch(f -> f.id() == 1));
            }
        }
    }
    @Test public void olderMeasurementsCanBeLoadedInTheListAndRemainAvailableAfterRotation() {
        try (FindingStore store = new FindingStore(app)) {
            Location fix = new Location("gps"); fix.setLatitude(52.52); fix.setLongitude(13.405); fix.setAccuracy(50);
            for (int i = 0; i < 105; i++) store.insert(fix, null);
        }
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get(); activity.findViewById(R.id.nav_findings).performClick();
            assertNotNull(activity.findViewById(R.id.findings_more)); activity.findViewById(R.id.findings_more).performClick();
            assertNull(activity.findViewById(R.id.findings_more)); controller.recreate();
            assertNull(controller.get().findViewById(R.id.findings_more));
            try (FindingStore store = new FindingStore(app)) { assertEquals(107, store.count()); }
        }
    }
    @Test public void numbersSurviveSplittingDeletionAndNewRidesWithoutReusingDeletedIds() {
        try (FindingStore store = new FindingStore(app)) {
            long key = store.mergeBundles(List.of(-1L, -2L)); store.separateBundle(key); store.clearRoute(); store.delete(1);
            Location fix = new Location("gps"); fix.setLatitude(52.52); fix.setLongitude(13.405); fix.setAccuracy(5);
            long next = store.insert(fix, null); assertEquals(3, next); assertNotNull(store.get(2));
            store.deleteAll(); assertEquals(4, store.insert(fix, null));
        }
    }
}
