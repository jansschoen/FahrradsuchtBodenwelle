package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.location.Location;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class BundleStoreTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");}
    private long insert(FindingStore store,double lat){Location l=new Location("gps");l.setLatitude(lat);l.setLongitude(13.4);l.setAccuracy(5);long id=store.insert(l,null);store.review(id,"root_heave","Notiz "+id,true);return id;}
    @Test public void confirmationPersistsAllOriginalObservationsAndCanBeReversed() {
        long a,b,key;
        try(FindingStore s=new FindingStore(app)){a=insert(s,52.5);b=insert(s,52.50005);key=s.mergeBundles(List.of(-a,-b));assertEquals(1,s.bundles().size());assertEquals(2,s.count());assertEquals("Notiz "+a,s.get(a).note());assertTrue(s.get(b).reviewed());}
        try(FindingStore s=new FindingStore(app)){assertEquals(key,s.get(a).bundleId());assertEquals(key,s.get(b).bundleId());s.separateBundle(key);assertEquals(2,s.bundles().size());assertEquals(52.50005,s.get(b).latitude(),0);}
    }
    @Test public void mergingExistingPlacesExpandsTheirMembersAndRejectsStaleSelectionAtomically() {
        try(FindingStore s=new FindingStore(app)){long a=insert(s,52.5),b=insert(s,52.50005),c=insert(s,52.50008);long first=s.mergeBundles(List.of(-a,-b));
            long combined=s.mergeBundles(List.of(first,-c));assertEquals(3,s.bundles().get(0).observations().size());
            assertThrows(IllegalArgumentException.class,()->s.mergeBundles(List.of(combined,-99999L)));assertEquals(combined,s.get(a).bundleId());assertEquals(3,s.count());
            s.delete(a);assertEquals(2,s.bundles().get(0).observations().size());s.deleteAll();assertEquals(0,s.bundles().size());
        }
    }
    @Test public void csvKeepsOneRowPerReleasedObservationWithBundleReference() {
        try(FindingStore s=new FindingStore(app)){long a=insert(s,52.5),b=insert(s,52.50005);long key=s.mergeBundles(List.of(-a,-b));String csv=FindingCsv.exportReviewed(s.all());
            assertEquals(3,csv.lines().count());assertTrue(csv.lines().findFirst().orElseThrow().endsWith("bundle_id"));assertTrue(csv.contains(","+key+"\r\n"));
            s.review(a,"root_heave","",false);assertEquals(2,FindingCsv.exportReviewed(s.all()).lines().count());
        }
    }
    @Test public void versionFiveMigrationPreservesOriginalMetadataManualDistrictAndRoute() {
        try(android.database.sqlite.SQLiteDatabase old=app.openOrCreateDatabase("findings.db",0,null)){
            old.execSQL("CREATE TABLE findings(id INTEGER PRIMARY KEY AUTOINCREMENT,time_ms INTEGER NOT NULL,latitude REAL NOT NULL,longitude REAL NOT NULL,accuracy REAL NOT NULL,speed REAL,peak REAL,threshold REAL,source TEXT NOT NULL,category TEXT NOT NULL,note TEXT NOT NULL,reviewed INTEGER NOT NULL,location_age_ms INTEGER NOT NULL,gps_latitude REAL,gps_longitude REAL,bearing_deg REAL,position_distance_m REAL NOT NULL,position_method TEXT NOT NULL,detector_version TEXT NOT NULL,details TEXT NOT NULL,report_district TEXT NOT NULL DEFAULT '')");
            old.execSQL("CREATE TABLE route_points(id INTEGER PRIMARY KEY AUTOINCREMENT,time_ms INTEGER NOT NULL,elapsed_ms INTEGER NOT NULL,latitude REAL NOT NULL,longitude REAL NOT NULL,accuracy REAL NOT NULL,segment INTEGER NOT NULL)");
            old.execSQL("CREATE TABLE osm_cache(cell TEXT PRIMARY KEY,queried_at INTEGER NOT NULL,response TEXT NOT NULL)");
            old.execSQL("INSERT INTO findings VALUES(1,1000,52.52,13.405,5,6,48,40,'automatic','root_heave','Alte Notiz',1,3000,52.51984,13.405,0,18,'course_speed_time','experimental-2',?,'01')",new Object[]{FindingDetails.initial("root_heave",48.0).json()});
            old.execSQL("INSERT INTO route_points VALUES(1,1000,500,52.52,13.405,5,0)");old.setVersion(5);
        }
        try(FindingStore s=new FindingStore(app)){Finding f=s.get(1);assertEquals(0,f.bundleId());assertEquals("01",s.reportDistrict(1));assertEquals("Alte Notiz",f.note());assertTrue(f.reviewed());assertEquals(48,f.peak(),0);assertEquals(18,f.position().distanceMeters(),0);assertEquals(1,s.routeCount());assertTrue(s.photos(1).isEmpty());assertEquals(1,s.bundles().size());}
    }
}
