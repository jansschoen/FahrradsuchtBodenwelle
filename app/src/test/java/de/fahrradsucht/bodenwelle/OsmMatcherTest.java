package de.fahrradsucht.bodenwelle;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class OsmMatcherTest {
    private OsmWay north(long id, double longitude) {
        return new OsmWay(id, List.of(new OsmWay.Point(52.499, longitude), new OsmWay.Point(52.501, longitude)), Map.of("highway", "cycleway", "surface", "asphalt"));
    }
    private OsmMatch match(float accuracy, Float bearing, OsmWay... ways) {
        return OsmMatcher.match(52.5, 13.4, accuracy, bearing, List.of(ways), 1234, "test");
    }
    @Test public void singleCloseWayKeepsSourceTagsTimeAndConfidence() {
        OsmMatch result = match(5, 0f, north(42, 13.4));
        assertEquals(Long.valueOf(42), result.wayId()); assertEquals("medium", result.confidence());
        assertEquals(0, result.distanceMeters(), .001); assertEquals("asphalt", result.tags().get("surface"));
        assertEquals(1234, result.queriedAt()); assertEquals("cycleway", result.traffic());
    }
    @Test public void parallelRoadAndCyclewayWithinGpsErrorStayUnknown() {
        OsmMatch result = match(10, 0f, north(42, 13.4), north(43, 13.4001));
        assertEquals("ambiguous", result.status()); assertEquals("unknown", result.traffic()); assertNull(result.wayId());
    }
    @Test public void bearingCanDistinguishPerpendicularWays() {
        OsmWay east = new OsmWay(43, List.of(new OsmWay.Point(52.5, 13.399), new OsmWay.Point(52.5, 13.401)), Map.of("highway", "residential"));
        assertEquals(Long.valueOf(42), match(5, 0f, north(42, 13.4), east).wayId());
        assertEquals("ambiguous", match(5, null, north(42, 13.4), east).status());
        assertEquals(Long.valueOf(42), match(5, 180f, north(42, 13.4)).wayId());
    }
    @Test public void poorGpsMissingGeometryAndDistantWaysCannotBeMatched() {
        assertEquals("no_match", match(26, 0f, north(42, 13.4)).status());
        assertEquals("no_match", match(5, 0f, north(42, 13.41)).status());
        assertEquals("no_match", match(5, 0f, new OsmWay(2, List.of(), Map.of("highway", "cycleway"))).status());
        assertEquals("no_match", match(5, 0f).status());
    }
    @Test public void moderatelyPoorGpsLowersConfidenceEvenForOneWay() {
        assertEquals("low", match(20, 0f, north(42, 13.4)).confidence());
    }
}
