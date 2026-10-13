package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.widget.CheckBox;
import android.view.View;
import android.view.ViewGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class ClassificationMigrationTest {
    private Context context;
    @Before public void before() {
        context = RuntimeEnvironment.getApplication(); context.deleteDatabase("findings.db");
        OsmEnricher.enabled(context, false);
        RecordingService.running = false;
    }
    @Test public void legacyCategoriesAndMeasurementsMigrateWithoutGuessingSubtypesOrHeight() {
        try (SQLiteDatabase old = context.openOrCreateDatabase("findings.db", Context.MODE_PRIVATE, null)) {
            old.execSQL("CREATE TABLE findings (id INTEGER PRIMARY KEY AUTOINCREMENT, time_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracy REAL NOT NULL, speed REAL, peak REAL, threshold REAL, source TEXT NOT NULL, category TEXT NOT NULL DEFAULT 'Ungeklärt', note TEXT NOT NULL DEFAULT '', reviewed INTEGER NOT NULL DEFAULT 0, location_age_ms INTEGER NOT NULL)");
            for (String category : new String[]{"Wurzel", "Schiefe Bodenplatte", "Bodenwelle", "Straßenschaden", "Ungeklärt", "Unbekannte alte Art"})
                old.execSQL("INSERT INTO findings (time_ms,latitude,longitude,accuracy,peak,threshold,source,category,note,reviewed,location_age_ms) VALUES (1000,52.5,13.4,5,8,3,'automatic',?,'Alte Notiz',1,500)", new Object[]{category});
            old.setVersion(1);
        }
        try (FindingStore store = new FindingStore(context)) {
            assertEquals(6, store.count());
            assertEquals("root_heave", store.get(1).category()); assertEquals("paving_displacement", store.get(2).category());
            for (long id = 3; id <= 6; id++) {
                assertEquals("other", store.get(id).category()); assertFalse(store.get(id).details().legacyCategory().isEmpty());
            }
            for (Finding f : store.all()) {
                assertEquals("Alte Notiz", f.note()); assertTrue(f.reviewed()); assertEquals(8, f.peak(), 0);
                assertNull(f.details().heightMm()); assertEquals("m/s²", f.details().intensity().unit());
                assertEquals("legacy_gps", f.position().method()); assertEquals("experimental-1", f.detectorVersion());
                assertEquals("unknown", f.details().traffic());
            }
        }
    }
    @Test public void editingMeasuredHeightRequiresConfirmationAndPreservesOriginalSensorPeak() {
        Location l = new Location("gps"); l.setLatitude(52.5); l.setLongitude(13.4); l.setAccuracy(5);
        try (FindingStore store = new FindingStore(context)) {
            long id = store.insert(l, new BumpDetector.Detection(0, 48, 40)); Finding f = store.get(id);
            FindingReviewForm form = new FindingReviewForm(context, f);
            form.height.setText("7,5"); // A typed number alone is not an asserted measured height.
            assertNull(form.details(f).heightMm());
            form.heightMeasured.setChecked(true);
            CheckBox hazard = checkbox(form.root, "Wasseransammlung"); assertNotNull(hazard); hazard.setChecked(true);
            FindingDetails edited = form.details(f);
            store.review(id, form.category(), "gemessen", true, edited);
            Finding saved = store.get(id);
            assertEquals(7.5, saved.details().heightMm(), 0); assertEquals(48, saved.peak(), 0);
            assertEquals(40, saved.threshold(), 0); assertEquals(2, saved.details().intensity().level());
            assertEquals(16, saved.details().hazards());
            form.height.setText("NaN"); assertThrows(IllegalArgumentException.class, () -> form.details(f));
            form.height.setText(""); assertThrows(IllegalArgumentException.class, () -> form.details(f));
            form.heightMeasured.setChecked(false); assertEquals("m/s²", form.details(f).intensity().unit());
        }
    }
    @Test public void serializationKeepsEmptyMeasurementsAndRawOsmTagsDistinct() {
        FindingDetails d = FindingDetails.initial("other", null);
        assertEquals(d, FindingDetails.fromJson(d.json(), "other", null));
        assertEquals("unavailable", d.intensity().method());
        assertNull(d.heightMm()); assertEquals(0, d.hazards());
    }
    @Test public void versionThreeUpgradePreservesCorrectedCoordinatesAndDetectorVersion() {
        try (SQLiteDatabase old = context.openOrCreateDatabase("findings.db", Context.MODE_PRIVATE, null)) {
            old.execSQL("CREATE TABLE findings (id INTEGER PRIMARY KEY AUTOINCREMENT,time_ms INTEGER NOT NULL,latitude REAL NOT NULL,longitude REAL NOT NULL,accuracy REAL NOT NULL,speed REAL,peak REAL,threshold REAL,source TEXT NOT NULL,category TEXT NOT NULL DEFAULT 'Ungeklärt',note TEXT NOT NULL DEFAULT '',reviewed INTEGER NOT NULL DEFAULT 0,location_age_ms INTEGER NOT NULL,gps_latitude REAL,gps_longitude REAL,bearing_deg REAL,position_distance_m REAL NOT NULL DEFAULT 0,position_method TEXT NOT NULL DEFAULT 'legacy_gps',detector_version TEXT NOT NULL DEFAULT 'experimental-1')");
            old.execSQL("CREATE TABLE route_points (id INTEGER PRIMARY KEY AUTOINCREMENT,time_ms INTEGER NOT NULL,elapsed_ms INTEGER NOT NULL,latitude REAL NOT NULL,longitude REAL NOT NULL,accuracy REAL NOT NULL,segment INTEGER NOT NULL)");
            old.execSQL("INSERT INTO findings(time_ms,latitude,longitude,accuracy,speed,peak,threshold,source,category,note,reviewed,location_age_ms,gps_latitude,gps_longitude,bearing_deg,position_distance_m,position_method,detector_version) VALUES(1000,52.50016,13.4,5,6,60,40,'automatic','Wurzel','geprüft',1,3000,52.5,13.4,0,18,'course_speed_time','experimental-2')");
            old.setVersion(3);
        }
        try (FindingStore store = new FindingStore(context)) {
            Finding f = store.get(1);
            assertEquals(52.50016, f.latitude(), 0); assertEquals(52.5, f.position().gpsLatitude(), 0);
            assertEquals(18, f.position().distanceMeters(), 0); assertEquals("experimental-2", f.detectorVersion());
            assertEquals("root_heave", f.category()); assertTrue(f.reviewed());
            assertEquals("m/s²", f.details().intensity().unit()); assertNull(f.details().heightMm());
        }
    }
    @Test public void mapMarkerColorChangesAfterReviewWithoutAddingAFinding() {
        Location l = new Location("gps"); l.setLatitude(52.5); l.setLongitude(13.4); l.setAccuracy(5);
        try (FindingStore store = new FindingStore(context)) {
            long id = store.insert(l, new BumpDetector.Detection(0, 48, 40));
            try (org.robolectric.android.controller.ActivityController<MapActivity> controller = org.robolectric.Robolectric.buildActivity(MapActivity.class).setup()) {
                org.osmdroid.views.MapView map = map(controller.get().findViewById(android.R.id.content)); assertNotNull(map);
                org.osmdroid.views.overlay.Marker before = marker(map);
                assertEquals(Intensity.evaluate(null, 48.0).color(), ((android.graphics.drawable.GradientDrawable) before.getIcon()).getColor().getDefaultColor());
                Finding f = store.get(id);
                store.review(id, "pothole", "gemessen", true, f.details().reviewed(5.0, f.peak(), 0, "unknown", false, "unknown", false));
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));
                org.osmdroid.views.overlay.Marker after = marker(map);
                assertEquals(Intensity.evaluate(5.0, 48.0).color(), ((android.graphics.drawable.GradientDrawable) after.getIcon()).getColor().getDefaultColor());
                assertTrue(after.getTitle().contains("Geprüfte")); assertEquals(1, store.count());
            }
        }
    }
    private org.osmdroid.views.overlay.Marker marker(org.osmdroid.views.MapView map) {
        return map.getOverlays().stream().filter(o -> o instanceof org.osmdroid.views.overlay.Marker).map(o -> (org.osmdroid.views.overlay.Marker) o).findFirst().orElseThrow();
    }
    private org.osmdroid.views.MapView map(View view) {
        if (view instanceof org.osmdroid.views.MapView map) return map;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            org.osmdroid.views.MapView result = map(group.getChildAt(i)); if (result != null) return result;
        }
        return null;
    }
    private CheckBox checkbox(View view, String label) {
        if (view instanceof CheckBox box && label.contentEquals(box.getText())) return box;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            CheckBox found = checkbox(group.getChildAt(i), label); if (found != null) return found;
        }
        return null;
    }
}
