package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.location.Location;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class OsmRepositoryTest {
    private Context context;
    private final AtomicLong time = new AtomicLong(1_000_000);
    private final AtomicInteger calls = new AtomicInteger();
    private static final String RESPONSE = "{\"elements\":[{\"type\":\"way\",\"id\":42,\"tags\":{\"highway\":\"cycleway\",\"surface\":\"asphalt\",\"smoothness\":\"bad\",\"tracktype\":\"grade1\",\"width\":\"2\"},\"geometry\":[{\"lat\":52.498,\"lon\":13.4},{\"lat\":52.502,\"lon\":13.4}]}]}";
    @Before public void before() {
        context = RuntimeEnvironment.getApplication(); context.deleteDatabase("findings.db");
        context.getSharedPreferences("osm_requests", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("osm_diagnostics", Context.MODE_PRIVATE).edit().clear().commit();
        OsmEnricher.enabled(context, false);
    }
    private long finding(double lon) {
        Location location = new Location("gps"); location.setLatitude(52.5); location.setLongitude(lon);
        location.setAccuracy(5); location.setSpeed(5); location.setBearing(0);
        try (FindingStore store = new FindingStore(context)) { return store.insert(location, null); }
    }
    private OverpassClient.Transport success() {
        return query -> { calls.incrementAndGet(); assertTrue(query.contains("way[highway]")); return RESPONSE; };
    }
    @Test public void offlineRecordingRemainsAvailableAndCanBeEnrichedLater() {
        long id = finding(13.4);
        try (OsmRepository repository = new OsmRepository(context, success(), time::get); FindingStore store = new FindingStore(context)) {
            repository.enrich(id, false); assertEquals(0, calls.get());
            assertEquals("offline", store.get(id).details().osm().status()); assertEquals(1, store.count());
            repository.enrich(id, true); Finding f = store.get(id);
            assertEquals("cycleway", f.details().traffic()); assertEquals("asphalt", f.details().surface());
            assertEquals("bad", f.details().osm().tags().get("smoothness"));
            assertEquals("2", f.details().osm().tags().get("width")); assertEquals("grade1", f.details().osm().tags().get("tracktype"));
            assertEquals(0, f.details().intensity().level()); assertEquals(Long.valueOf(42), f.details().osm().wayId());
        }
    }
    @Test public void nearbyFindingsUsePersistentCacheIncludingOffline() {
        long a = finding(13.4), b = finding(13.40001);
        try (OsmRepository repository = new OsmRepository(context, success(), time::get)) { repository.enrich(a, true); }
        try (OsmRepository repository = new OsmRepository(context, success(), time::get); FindingStore store = new FindingStore(context)) {
            repository.enrich(b, false); assertEquals(1, calls.get()); assertTrue(store.get(b).details().osm().assigned());
        }
    }
    @Test public void adjacentGridCellsReuseTheirOverlappingQueryArea() {
        long a = finding(13.39999), b = finding(13.40001);
        assertNotEquals(OsmRepository.cell(52.5, 13.39999), OsmRepository.cell(52.5, 13.40001));
        try (OsmRepository repository = new OsmRepository(context, success(), time::get); FindingStore store = new FindingStore(context)) {
            repository.enrich(a, true); repository.enrich(b, true);
            assertEquals(1, calls.get()); assertTrue(store.get(b).details().osm().assigned());
            assertEquals(OsmMatcher.VERSION, store.get(b).details().osm().tags().get("bodenwelle:matcher_version"));
        }
    }
    @Test public void globalRequestBudgetPreventsRequestsForEveryFinding() {
        long a = finding(13.4), b = finding(13.42);
        try (OsmRepository repository = new OsmRepository(context, success(), time::get); FindingStore store = new FindingStore(context)) {
            assertEquals(0, repository.enrich(a, true)); assertEquals(time.get() + 60_000, repository.enrich(b, true)); assertEquals(1, calls.get());
            assertEquals("rate_limited", store.get(b).details().osm().status());
            time.addAndGet(60_000); repository.enrich(b, true); assertEquals(2, calls.get());
        }
    }
    @Test public void failureBackoffNeverDropsLocalFindingOrCachesPartialResponse() {
        long id = finding(13.4);
        OverpassClient.Transport failure = query -> { calls.incrementAndGet(); throw new IOException("offline"); };
        try (OsmRepository repository = new OsmRepository(context, failure, time::get); FindingStore store = new FindingStore(context)) {
            repository.enrich(id, true); repository.enrich(id, true);
            assertEquals(1, calls.get()); assertEquals(1, store.count());
            assertNull(store.cachedOsm(OsmRepository.cell(52.5, 13.4)));
        }
        time.addAndGet(300_000);
        try (OsmRepository repository = new OsmRepository(context, q -> "{\"remark\":\"timeout\",\"elements\":[]}", time::get); FindingStore store = new FindingStore(context)) {
            repository.enrich(id, true); assertEquals("error", store.get(id).details().osm().status());
            assertNull(store.cachedOsm(OsmRepository.cell(52.5, 13.4)));
        }
    }
    @Test public void manualCorrectionsSurviveBackgroundResponsesAndReviewRace() {
        long id = finding(13.4);
        try (FindingStore store = new FindingStore(context)) {
            Finding original = store.get(id);
            FindingDetails edited = original.details().reviewed(7.0, original.peak(), 17, "other", true, "concrete", true);
            OsmMatch first = new OsmMatch("matched", 42L, Map.of("highway", "cycleway", "surface", "asphalt"), time.get(), "medium", 0.0, "test", "cycleway", "asphalt");
            store.setOsm(id, first); // Arrives after form was opened, before review is saved.
            store.review(id, "pothole", "gemessen", true, edited);
            assertEquals(Long.valueOf(42), store.get(id).details().osm().wayId());
            store.setOsm(id, new OsmMatch("matched", 43L, Map.of("highway", "residential"), time.get(), "low", 4.0, "test", "carriageway", "gravel"));
            Finding f = store.get(id);
            assertEquals("other", f.details().traffic()); assertEquals("concrete", f.details().surface());
            assertEquals(17, f.details().hazards()); assertEquals(2, f.details().intensity().level());
            assertEquals("mm", f.details().intensity().unit()); assertEquals(Long.valueOf(43), f.details().osm().wayId());
            String csv = FindingCsv.exportReviewed(store.all());
            assertTrue(csv.contains("manual_measured_height_v1")); assertTrue(csv.contains("poor_visibility;standing_water"));
            assertTrue(csv.contains("ODbL 1.0")); assertTrue(csv.contains("OpenStreetMap contributors"));
            FindingDetails automatic = f.details().reviewed(null, f.peak(), 17, "other", false, "concrete", false);
            store.review(id, "pothole", "", true, automatic);
            assertEquals("carriageway", store.get(id).details().traffic()); assertEquals("gravel", store.get(id).details().surface());
        }
    }
    @Test public void staleCacheCanBeUsedOfflineWithLowerConfidenceAndOriginalDate() {
        long id = finding(13.4);
        try (OsmRepository repository = new OsmRepository(context, success(), time::get); FindingStore store = new FindingStore(context)) {
            repository.enrich(id, true); time.addAndGet(8L * 24 * 60 * 60 * 1000);
            repository.enrich(id, false); assertEquals(1, calls.get());
            assertEquals("low", store.get(id).details().osm().confidence()); assertEquals(1_000_000, store.get(id).details().osm().queriedAt());
        }
    }
    @Test public void invalidOrMissingGeometryNeverInventsTagsOrAHeight() throws Exception {
        assertTrue(OverpassClient.parse("{\"elements\":[{\"type\":\"way\",\"id\":2,\"tags\":{\"highway\":\"cycleway\"}}]}").isEmpty());
        assertThrows(IOException.class, () -> OverpassClient.parse("not json"));
        assertThrows(IOException.class, () -> OverpassClient.parse("{\"remark\":\"timeout\",\"elements\":[]}"));
    }
    @Test public void serverRetryAfterIsHonoredBeyondLocalBackoff() {
        long id = finding(13.4);
        assertEquals(3_600_000, OverpassClient.retryAfter("3600"));
        assertEquals(0, OverpassClient.retryAfter("invalid"));
        try (OsmRepository repository = new OsmRepository(context, q -> { calls.incrementAndGet(); throw new OverpassClient.RequestFailure(429, 3_600_000); }, time::get)) {
            repository.enrich(id, true); time.addAndGet(300_000); repository.enrich(id, true);
            assertEquals(1, calls.get());
        }
    }
    @Test public void queryAndMatchingUseOriginalGpsCircleCentreAndKeepProjectedEventPosition() {
        long id=finding(13.4);
        try(FindingStore store=new FindingStore(context)) {
            store.getWritableDatabase().execSQL("UPDATE findings SET latitude=52.51,longitude=13.41,position_distance_m=1000,position_method='course_speed_time' WHERE id=?",new Object[]{id});
            Finding original=store.get(id);
            try(OsmRepository repository=new OsmRepository(context,query->{calls.incrementAndGet();assertEquals(OsmRepository.query(52.5,13.4),query);return RESPONSE;},time::get)) {
                repository.enrich(id,true);
            }
            Finding result=store.get(id);assertEquals(original.position(),result.position());assertEquals(1,calls.get());
            assertEquals(Long.valueOf(42),result.details().osm().wayId());assertEquals(0,result.details().osm().distanceMeters(),.001);
            assertEquals("gps_circle_center",result.details().osm().tags().get("bodenwelle:position_basis"));
            assertNotNull(store.cachedOsm(OsmRepository.cell(52.5,13.4)));assertNull(store.cachedOsm(OsmRepository.cell(52.51,13.41)));
        }
    }
    @Test public void manualRefreshBypassesOlderFreshCacheButSharesTheNewAnswerAndRespectsRequestBudget() {
        long a=finding(13.4),b=finding(13.40001);
        try(OsmRepository repository=new OsmRepository(context,success(),time::get)) {
            repository.enrich(a,true);time.addAndGet(1000);long generation=time.get();
            assertEquals(1_060_000,repository.enrich(a,true,generation));assertEquals(1,calls.get());
            time.addAndGet(59_000);assertEquals(0,repository.enrich(a,true,generation));
            assertEquals(0,repository.enrich(b,true,generation));assertEquals(2,calls.get());
            assertTrue(new OsmDiagnostics(context).summary().contains("aus Cache"));
        }
    }
    @Test public void serverFailuresAreDistinguishedWithoutLeakingLocationOrNotesIntoDiagnostics() {
        long id=finding(13.4);
        try(FindingStore store=new FindingStore(context)) {store.review(id,"pothole","Private Ortsbeschreibung",false);}
        try(OsmRepository repository=new OsmRepository(context,q->{throw new OverpassClient.RequestFailure(403,0);},time::get)) {
            long next=repository.enrich(id,true);assertEquals(time.get()+300_000,next);
            String diagnosis=new OsmDiagnostics(context).summary();assertTrue(diagnosis.contains("HTTP 403"));assertTrue(diagnosis.contains("Fehlerpause"));
            assertFalse(diagnosis.contains("52.5"));assertFalse(diagnosis.contains("13.4"));assertFalse(diagnosis.contains("Private Ortsbeschreibung"));
        }
    }
    @Test public void accuracyAboveFiftyMetresPreventsEvenANetworkRequest() {
        long id=finding(13.4);
        try(FindingStore store=new FindingStore(context);OsmRepository repository=new OsmRepository(context,success(),time::get)) {
            store.getWritableDatabase().execSQL("UPDATE findings SET accuracy=51 WHERE id=?",new Object[]{id});
            assertEquals(0,repository.enrich(id,true));assertEquals(0,calls.get());assertEquals("gps_uncertain",store.get(id).details().osm().status());
        }
    }
    @Test public void parkEvidenceUsesOriginalGpsCentreEvenIfEventPositionIsOutsideAndSurvivesOfflineCache() {
        long id=finding(13.4);
        String park="{\"type\":\"way\",\"id\":90,\"tags\":{\"leisure\":\"park\"},\"geometry\":[{\"lat\":52.499,\"lon\":13.399},{\"lat\":52.499,\"lon\":13.401},{\"lat\":52.501,\"lon\":13.401},{\"lat\":52.501,\"lon\":13.399},{\"lat\":52.499,\"lon\":13.399}]}";
        String response=RESPONSE.replace("{\"elements\":[","{\"elements\":["+park+",");
        try(FindingStore store=new FindingStore(context);OsmRepository repo=new OsmRepository(context,q->{assertTrue(q.contains("is_in("));assertTrue(q.contains("relation[access=private]"));return response;},time::get)) {
            store.getWritableDatabase().execSQL("UPDATE findings SET latitude=52.51 WHERE id=?",new Object[]{id});repo.enrich(id,true);
            assertEquals("way/90",store.get(id).details().osm().tags().get(OsmPlaceContext.PARK));assertEquals(52.51,store.get(id).latitude(),0);
            repo.enrich(id,false);assertTrue(OsmPlaceContext.suggested(store.get(id)));
        }
    }
}
