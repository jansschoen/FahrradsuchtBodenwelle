package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MapWorkflowTest {
    private Context context;
    @Before public void before() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("findings.db");
        RecordingService.running = false;
        OsmEnricher.enabled(RuntimeEnvironment.getApplication(), false);
    }
    @After public void after() { context.deleteDatabase("findings.db"); }

    @Test public void schemaUpgradeKeepsPreviouslyReviewedFindings() {
        try (SQLiteDatabase old = context.openOrCreateDatabase("findings.db", Context.MODE_PRIVATE, null)) {
            old.execSQL("CREATE TABLE findings (id INTEGER PRIMARY KEY AUTOINCREMENT, time_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracy REAL NOT NULL, speed REAL, peak REAL, threshold REAL, source TEXT NOT NULL, category TEXT NOT NULL DEFAULT 'Ungeklärt', note TEXT NOT NULL DEFAULT '', reviewed INTEGER NOT NULL DEFAULT 0, location_age_ms INTEGER NOT NULL)");
            old.execSQL("INSERT INTO findings (time_ms, latitude, longitude, accuracy, source, note, reviewed, location_age_ms) VALUES (1000, 52.5, 13.4, 5, 'manual', 'Bereits geprüft', 1, 500)");
            old.setVersion(1);
        }
        try (FindingStore upgraded = new FindingStore(context)) {
            assertEquals(1, upgraded.count());
            assertTrue(upgraded.all().get(0).reviewed());
            assertEquals("Bereits geprüft", upgraded.all().get(0).note());
            assertEquals(0, upgraded.routeCount());
            upgraded.insertRoutePoint(new RoutePoint(2000, 1000, 52.5, 13.4, 5, 0));
            assertEquals(1, upgraded.routeCount());
        }
    }
    @Test public void routeSurvivesReopeningAndClearingItKeepsFindings() {
        try (FindingStore first = new FindingStore(context)) {
            Location fix = new Location("gps");
            fix.setLatitude(52.5); fix.setLongitude(13.4); fix.setAccuracy(5);
            fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            first.insert(fix, null);
            first.insertRoutePoint(new RoutePoint(2000, 1000, 52.5, 13.4, 5, 0));
        }
        try (FindingStore reopened = new FindingStore(context)) {
            assertEquals(1, reopened.route().size());
            reopened.clearRoute();
            assertEquals(0, reopened.routeCount());
            assertEquals(1, reopened.count());
        }
    }
    @Test public void routePointsAreExcludedFromFindingExport() {
        try (FindingStore store = new FindingStore(context)) {
            store.insertRoutePoint(new RoutePoint(2000, 1000, 52.123456, 13.654321, 5, 0));
            String csv = FindingCsv.exportReviewed(store.all());
            assertFalse(csv.contains("52.123456"));
            assertEquals(1, csv.lines().count());
        }
    }
    @Test public void mainScreenWithEmbeddedMapStartsWithoutLocationPermission() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertEquals(PackageManager.PERMISSION_DENIED, controller.get().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION));
            MapView map = findMap(controller.get().findViewById(android.R.id.content));
            assertNotNull(map);
            assertEquals("OSM", map.getTileProvider().getTileSource().name());
        }
    }
    @Test public void fullScreenMapCanDisplaySavedRouteWithGap() {
        try (FindingStore store = new FindingStore(context)) {
            store.insertRoutePoint(new RoutePoint(2000, 1000, 52.5, 13.4, 5, 0));
            store.insertRoutePoint(new RoutePoint(3000, 2000, 52.5001, 13.4, 5, 0));
            store.insertRoutePoint(new RoutePoint(30_000, 29_000, 52.501, 13.4, 5, 1));
        }
        try (ActivityController<MapActivity> controller = Robolectric.buildActivity(MapActivity.class).setup()) {
            MapView map = findMap(controller.get().findViewById(android.R.id.content));
            assertNotNull(map);
            java.util.List<Polyline> lines = map.getOverlays().stream().filter(o -> o instanceof Polyline)
                    .map(o -> (Polyline) o).toList();
            assertEquals(1, lines.size());
            assertEquals(2, lines.get(0).getActualPoints().size());
        }
    }

    private MapView findMap(View view) {
        if (view instanceof MapView) return (MapView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                MapView found = findMap(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
}
