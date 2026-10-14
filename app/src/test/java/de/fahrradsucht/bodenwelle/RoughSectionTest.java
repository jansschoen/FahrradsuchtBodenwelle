package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.location.Location;
import android.os.SystemClock;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class RoughSectionTest {
    private Application app;
    @Before public void before() { app = RuntimeEnvironment.getApplication(); app.deleteDatabase("findings.db"); }
    private Location fix(long elapsed, double lat, double lon) {
        Location f = new Location("gps"); f.setLatitude(lat); f.setLongitude(lon); f.setAccuracy(5); f.setSpeed(5); f.setBearing(0);
        f.setElapsedRealtimeNanos(elapsed * 1_000_000L); return f;
    }
    private long insert(FindingStore store) {
        long now = SystemClock.elapsedRealtime();
        return store.insertSection(fix(now - 4000, 52.52, 13.405), fix(now, 52.52018, 13.405),
                new SurfaceDetector.Section(now - 4000, now, 12, 40, 20));
    }
    @Test public void sectionMetadataSurvivesReopeningReviewOsmUpdatesAndCsvExport() {
        long id;
        try (FindingStore store = new FindingStore(app)) { id = insert(store); assertNull(FindingDetails.initial("other", 12.0).section()); }
        try (FindingStore store = new FindingStore(app)) {
            Finding f = store.get(id); assertEquals("rough_road", f.category()); assertFalse(f.reviewed());
            RoughSection section = f.details().section(); assertEquals(4000, section.durationMillis()); assertEquals(20, section.lengthMeters(), 0);
            assertEquals(section, f.details().withOsm(OsmMatch.empty("offline")).section());
            store.review(id, "rough_road", "Unebener Belag", true, f.details().reviewed(null, f.peak(), 0, "cycleway", true, "asphalt", true));
            assertEquals(section, store.get(id).details().section()); String csv = FindingCsv.exportReviewed(store.all());
            assertTrue(csv.contains("rough_section")); assertTrue(csv.contains("end_latitude")); assertTrue(csv.contains("length_m"));
            store.delete(id); assertEquals(0, store.count());
        }
    }
    @Test public void mailIncludesEndpointsAndBothCalendarWeeksButNeverFullTimestamps() {
        try (FindingStore store = new FindingStore(app)) {
            long id = insert(store); long time = Instant.parse("2026-10-18T21:59:58Z").toEpochMilli();
            store.getWritableDatabase().execSQL("UPDATE findings SET time_ms = ? WHERE id = ?", new Object[]{time, id});
            store.review(id, "rough_road", "", true); Finding f = store.get(id);
            MailReports.Group group = MailReports.groups(List.of(new MailReports.Item(f, "01", "manual")), true).get(0);
            String body = MailReports.draft(group, "Mitte", true, false, "").body();
            assertTrue(body.contains("KW 42/2026")); assertTrue(body.contains("KW 43/2026"));
            assertTrue(body.contains("Anfang:")); assertTrue(body.contains("Ende:")); assertTrue(body.contains("etwa 20 m"));
            assertFalse(body.contains("21:59")); assertFalse(body.contains("23:59")); assertFalse(body.contains("2026-10-18"));
        }
    }
    @Test public void crossingDistrictSectionNeedsManualAssignmentInsteadOfSilentlyUsingItsStart() throws Exception {
        try (FindingStore store = new FindingStore(app)) {
            long now = SystemClock.elapsedRealtime();
            long id = store.insertSection(fix(now-4000,52.52,13.405), fix(now,52.465,13.385), new SurfaceDetector.Section(now-4000,now,12,40,20));
            BerlinDistricts boundaries = new BerlinDistricts(app);
            assertEquals("section_jurisdiction_uncertain", boundaries.assign(store.get(id), "").method());
            assertEquals("", boundaries.assign(store.get(id), "").districtId());
            assertEquals("01", boundaries.assign(store.get(id), "01").districtId());
        }
    }
    @Test public void invalidSectionRollsBackTheEntireFindingAndCannotEnterPointSuggestions() {
        try (FindingStore store = new FindingStore(app)) {
            long now = SystemClock.elapsedRealtime();
            assertThrows(IllegalArgumentException.class, () -> store.insertSection(fix(now,52.52,13.405), fix(now,52.52,13.405),
                    new SurfaceDetector.Section(now, now - 1, 12, 40, 20)));
            assertEquals(0, store.count()); long id = insert(store);
            Location point = fix(now,52.52,13.405); store.insert(point,null);
            assertTrue(BundleSuggestions.suggest(store.bundles()).isEmpty()); assertNotNull(store.get(id).details().section());
        }
    }
}
