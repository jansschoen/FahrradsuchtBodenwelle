package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class BerlinDistrictsTest {
    private Application app;
    @Before public void before() { app = RuntimeEnvironment.getApplication(); app.deleteDatabase("findings.db"); }
    @Test public void offlinePolygonsRecognizeRepresentativePointsInAllTwelveDistricts() throws Exception {
        BerlinDistricts b = new BerlinDistricts(app);
        double[][] points = {{52.52,13.405},{52.499,13.430},{52.569,13.405},{52.51,13.30},{52.535,13.2},{52.443,13.260},
                {52.465,13.385},{52.467,13.440},{52.445,13.578},{52.545,13.580},{52.523,13.499},{52.605,13.305}};
        for (int i = 0; i < points.length; i++) assertEquals("district " + (i+1), String.format(java.util.Locale.ROOT, "%02d", i+1), b.locate(points[i][0], points[i][1], 5, 0).districtId());
    }
    @Test public void nearBorderOutsideAndInvalidPositionsStayUnknown() throws Exception {
        BerlinDistricts b = new BerlinDistricts(app);
        assertEquals("near_boundary", b.locate(52.540212, 13.403528, 5, 0).method());
        assertEquals("", b.locate(48.137, 11.575, 5, 0).districtId());
        assertEquals("invalid_position", b.locate(Double.NaN, 13.4, 5, 0).method());
        assertEquals("", b.locate(52.52, 13.405, 50000, 0).districtId());
    }
    @Test public void catalogContainsTwelveVerifiedSourceLinkedAddresses() throws Exception {
        DistrictCatalog c = new DistrictCatalog(app); assertEquals(12, c.all().size());
        for (DistrictCatalog.Authority a : c.all()) {
            assertTrue(a.source().startsWith("https://www.berlin.de/")); assertTrue(MailIntent.validAddress(a.email()));
            assertEquals("2026-10-13", a.checkedAt());
        }
    }
    @Test public void versionFourMigrationPreservesReviewMetadataCoordinatesAndRoute() {
        try (SQLiteDatabase old = app.openOrCreateDatabase("findings.db", 0, null)) {
            old.execSQL("CREATE TABLE findings(id INTEGER PRIMARY KEY AUTOINCREMENT,time_ms INTEGER NOT NULL,latitude REAL NOT NULL,longitude REAL NOT NULL,accuracy REAL NOT NULL,speed REAL,peak REAL,threshold REAL,source TEXT NOT NULL,category TEXT NOT NULL,note TEXT NOT NULL,reviewed INTEGER NOT NULL,location_age_ms INTEGER NOT NULL,gps_latitude REAL,gps_longitude REAL,bearing_deg REAL,position_distance_m REAL NOT NULL,position_method TEXT NOT NULL,detector_version TEXT NOT NULL,details TEXT NOT NULL)");
            old.execSQL("CREATE TABLE route_points(id INTEGER PRIMARY KEY AUTOINCREMENT,time_ms INTEGER NOT NULL,elapsed_ms INTEGER NOT NULL,latitude REAL NOT NULL,longitude REAL NOT NULL,accuracy REAL NOT NULL,segment INTEGER NOT NULL)");
            old.execSQL("CREATE TABLE osm_cache(cell TEXT PRIMARY KEY,queried_at INTEGER NOT NULL,response TEXT NOT NULL)");
            old.execSQL("INSERT INTO findings VALUES(1,1000,52.52,13.405,5,6,48,40,'automatic','root_heave','Prüfung',1,3000,52.51984,13.405,0,18,'course_speed_time','experimental-2',?)",new Object[]{FindingDetails.initial("root_heave",48.0).json()});
            old.execSQL("INSERT INTO route_points VALUES(1,1000,500,52.52,13.405,5,0)"); old.setVersion(4);
        }
        try (FindingStore s = new FindingStore(app)) {
            Finding f = s.get(1); assertTrue(f.reviewed()); assertEquals("root_heave", f.category()); assertEquals("Prüfung", f.note());
            assertEquals(18, f.position().distanceMeters(),0); assertEquals(48, f.details().intensity().value(),0); assertEquals(1,s.routeCount());
            assertEquals("",s.reportDistrict(1)); s.reportDistrict(1,"02"); assertEquals("02",s.reportDistrict(1));
            assertThrows(IllegalArgumentException.class, () -> s.reportDistrict(1,"99"));
        }
        try (FindingStore s = new FindingStore(app)) { assertEquals("02",s.reportDistrict(1)); }
    }
}
