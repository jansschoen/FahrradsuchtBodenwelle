package de.fahrradsucht.bodenwelle;

import org.junit.Test;
import static org.junit.Assert.*;

public class RouteTrackerTest {
    private RoutePoint sample(RouteTracker tracker, long elapsed, double latitude, float accuracy) {
        return tracker.accept(1_000_000 + elapsed, elapsed, elapsed + 100, latitude, 13.4, accuracy);
    }
    @Test public void ordinaryRideProducesContinuousRoute() {
        RouteTracker t = new RouteTracker();
        RoutePoint start = sample(t, 1000, 52.5, 5);
        RoutePoint next = sample(t, 2000, 52.50005, 5);
        assertNotNull(start); assertNotNull(next);
        assertEquals(start.segment(), next.segment());
    }
    @Test public void gpsGapStartsSeparateSegment() {
        RouteTracker t = new RouteTracker();
        sample(t, 1000, 52.5, 5);
        RoutePoint afterGap = sample(t, 20_000, 52.505, 5);
        assertNotNull(afterGap); assertEquals(1, afterGap.segment());
    }
    @Test public void gpsTeleportIsNotRecordedAsTravel() {
        RouteTracker t = new RouteTracker();
        sample(t, 1000, 52.5, 5);
        assertNull(sample(t, 2000, 52.51, 5));
        assertNotNull(sample(t, 3000, 52.50008, 5));
    }
    @Test public void stationaryGpsNoiseIsThrottled() {
        RouteTracker t = new RouteTracker();
        sample(t, 1000, 52.5, 5);
        assertNull(sample(t, 2000, 52.500001, 5));
        assertNotNull(sample(t, 6000, 52.500001, 5));
    }
    @Test public void staleInaccurateAndInvalidCoordinatesAreRejected() {
        RouteTracker t = new RouteTracker();
        assertNull(t.accept(1, 1000, 7000, 52.5, 13.4, 5));
        assertNull(sample(t, 1000, 52.5, 51));
        assertNull(sample(t, 1000, Double.NaN, 5));
        assertNull(sample(t, 1000, 91, 5));
        assertNull(t.accept(1, 1000, 1100, 52.5, 181, 5));
        assertNull(sample(t, 1000, 52.5, Float.NaN));
    }
    @Test public void repeatedOrOutOfOrderFixCannotExtendRoute() {
        RouteTracker t = new RouteTracker();
        sample(t, 1000, 52.5, 5);
        assertNull(sample(t, 1000, 52.5001, 5));
        assertNull(sample(t, 900, 52.5001, 5));
    }
    @Test public void newTrackerStartsFreshSessionWithoutOldConnections() {
        RouteTracker old = new RouteTracker(); sample(old, 1000, 52.5, 5);
        RouteTracker current = new RouteTracker();
        RoutePoint start = sample(current, 9000, 52.51, 5);
        assertNotNull(start); assertEquals(0, start.segment());
    }
}
