package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.SystemClock;
import android.widget.SeekBar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class FindingPositionTest {
    private Application app;
    @Before public void before() {
        app = RuntimeEnvironment.getApplication(); app.deleteDatabase("findings.db");
        new DetectionPreferences(app).setThreshold(40);
        RecordingService.running = false;
        OsmEnricher.enabled(RuntimeEnvironment.getApplication(), false);
        ShadowSystemClock.advanceBy(Duration.ofSeconds(10));
    }
    private Location fix(long elapsed, float bearing) {
        Location location = new Location("gps");
        location.setLatitude(52.5); location.setLongitude(13.4);
        location.setAccuracy(5); location.setSpeed(6); location.setBearing(bearing);
        location.setBearingAccuracyDegrees(5);
        location.setElapsedRealtimeNanos(elapsed * 1_000_000L);
        return location;
    }
    @Test public void storedFindingUsesPeakTimeAndKeepsOriginalGpsInExport() {
        long now = SystemClock.elapsedRealtime();
        long wall = System.currentTimeMillis();
        try (FindingStore store = new FindingStore(app)) {
            store.insert(fix(now - 3250, 0), new BumpDetector.Detection(now - 250, 60, 40));
            Finding f = store.all().get(0);
            // Wall time can advance by a millisecond while SQLite is opened. Bound the
            // timestamp by both clock reads, still requiring the 250-ms peak offset.
            assertTrue(f.timeMillis() >= wall - 250);
            assertTrue(f.timeMillis() <= System.currentTimeMillis() - 250);
            assertEquals(3000, f.locationAgeMillis()); assertTrue(f.position().projected());
            assertEquals(18, f.position().distanceMeters(), 0);
            assertEquals(52.5, f.position().gpsLatitude(), 0); assertTrue(f.latitude() > 52.5);
            store.review(f.id(), "Bodenwelle", "Geprüft", true);
            String csv = FindingCsv.exportReviewed(store.all());
            assertTrue(csv.contains("gps_latitude,gps_longitude,bearing_deg,position_distance_m,position_method"));
            assertTrue(csv.contains("52.5,13.4,0.0,18.0,\"course_speed_time\""));
            assertTrue(csv.contains("\"" + BumpDetector.VERSION + "\""));
        }
    }
    @Test public void newerGpsFixIsBackProjectedToEarlierPeak() {
        long now = SystemClock.elapsedRealtime();
        try (FindingStore store = new FindingStore(app)) {
            store.insert(fix(now - 50, 90), new BumpDetector.Detection(now - 250, 60, 40));
            Finding f = store.all().get(0);
            assertEquals(-200, f.locationAgeMillis()); assertTrue(f.longitude() < 13.4);
            assertEquals(-1.2, f.position().distanceMeters(), 0.00001);
        }
    }
    @Test public void upgradeFromVersionTwoPreservesFindingsAndRouteWithoutRelabellingDetector() {
        try (SQLiteDatabase old = app.openOrCreateDatabase("findings.db", Context.MODE_PRIVATE, null)) {
            old.execSQL("CREATE TABLE findings (id INTEGER PRIMARY KEY AUTOINCREMENT, time_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracy REAL NOT NULL, speed REAL, peak REAL, threshold REAL, source TEXT NOT NULL, category TEXT NOT NULL DEFAULT 'Ungeklärt', note TEXT NOT NULL DEFAULT '', reviewed INTEGER NOT NULL DEFAULT 0, location_age_ms INTEGER NOT NULL)");
            old.execSQL("CREATE TABLE route_points (id INTEGER PRIMARY KEY AUTOINCREMENT, time_ms INTEGER NOT NULL, elapsed_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracy REAL NOT NULL, segment INTEGER NOT NULL)");
            old.execSQL("INSERT INTO findings (time_ms,latitude,longitude,accuracy,speed,peak,threshold,source,note,reviewed,location_age_ms) VALUES (1000,52.5,13.4,5,6,8,3,'automatic','Alte Fundstelle',1,500)");
            old.execSQL("INSERT INTO route_points (time_ms,elapsed_ms,latitude,longitude,accuracy,segment) VALUES (1000,500,52.5,13.4,5,0)");
            old.setVersion(2);
        }
        try (FindingStore store = new FindingStore(app)) {
            Finding f = store.all().get(0);
            assertTrue(f.reviewed()); assertEquals("Alte Fundstelle", f.note());
            assertEquals(52.5, f.latitude(), 0); assertEquals(52.5, f.position().gpsLatitude(), 0);
            assertEquals("legacy_gps", f.position().method()); assertEquals("experimental-1", f.detectorVersion());
            assertEquals(1, store.routeCount());
            assertTrue(FindingCsv.exportReviewed(store.all()).contains("\"experimental-1\""));
        }
    }
    @Test public void sliderChangesPersistAcrossMapReopening() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().findViewById(R.id.map_options).performClick();
            SeekBar slider = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.sensitivity_slider);
            assertNotNull(slider); slider.setProgress(10);
            assertEquals(15, new DetectionPreferences(app).threshold());
        }
        try (ActivityController<MapActivity> controller = Robolectric.buildActivity(MapActivity.class).setup()) {
            controller.get().findViewById(R.id.map_options).performClick();
            SeekBar slider = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.sensitivity_slider);
            assertEquals(10, slider.getProgress());
        }
    }
}
