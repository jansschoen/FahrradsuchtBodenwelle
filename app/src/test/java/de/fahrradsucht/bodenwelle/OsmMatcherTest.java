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
    @Test public void parallelWaysWithTheSameTagsGiveAnExplicitUncertainAreaSuggestion() {
        OsmMatch result = match(10, 0f, north(42, 13.4), north(43, 13.4001));
        assertEquals("area_suggestion", result.status()); assertEquals("cycleway", result.traffic()); assertEquals("asphalt", result.surface()); assertNull(result.wayId());
        assertEquals("42;43", result.tags().get("bodenwelle:candidate_way_ids")); assertEquals("low", result.confidence());
    }
    @Test public void bearingCanDistinguishPerpendicularWays() {
        OsmWay east = new OsmWay(43, List.of(new OsmWay.Point(52.5, 13.399), new OsmWay.Point(52.5, 13.401)), Map.of("highway", "residential"));
        assertEquals(Long.valueOf(42), match(5, 0f, north(42, 13.4), east).wayId());
        assertEquals("ambiguous", match(5, null, north(42, 13.4), east).status());
        assertEquals(Long.valueOf(42), match(5, 180f, north(42, 13.4)).wayId());
    }
    @Test public void poorGpsMissingGeometryAndDistantWaysCannotBeMatched() {
        assertEquals("no_match", match(51, 0f, north(42, 13.4)).status());
        assertEquals("no_match", match(5, 0f, north(42, 13.41)).status());
        assertEquals("no_match", match(5, 0f, new OsmWay(2, List.of(), Map.of("highway", "cycleway"))).status());
        assertEquals("no_match", match(5, 0f).status());
    }
    @Test public void expandedRadiusAndModeratelyPoorGpsOnlyGiveUncertainSuggestions() {
        OsmMatch distant = match(5, 0f, north(42, 13.4005));
        assertTrue(distant.assigned()); assertEquals("low", distant.confidence()); assertTrue(distant.distanceMeters() > 30);
        assertEquals("low", match(40, 0f, north(42, 13.4)).confidence());
        assertEquals("no_match", match(5, 0f, north(42, 13.401)).status());
    }
    @Test public void conflictingParallelWaysOnlySupplyTheirCommonSurface() {
        OsmWay road = new OsmWay(43, north(43, 13.4001).geometry(), Map.of("highway", "residential", "surface", "asphalt"));
        OsmMatch result = match(10, 0f, north(42, 13.4), road);
        assertEquals("area_suggestion", result.status()); assertEquals("unknown", result.traffic()); assertEquals("asphalt", result.surface());
        OsmWay gravel = new OsmWay(44, road.geometry(), Map.of("highway", "residential", "surface", "gravel"));
        assertEquals("ambiguous", match(10, 0f, north(42, 13.4), gravel).status());
        OsmWay missing = new OsmWay(45, road.geometry(), Map.of("highway", "residential"));
        assertEquals("ambiguous", match(10, 0f, north(42, 13.4), missing).status());
    }
    @Test public void projectionMarginWidensUncertaintyWithoutRejectingValidGps() {
        OsmMatch result = OsmMatcher.match(52.5, 13.4, 25, 10, 0f, List.of(north(42, 13.4)), 1234, "test");
        assertTrue(result.assigned()); assertEquals("low", result.confidence());
    }
    @Test public void moderatelyPoorGpsLowersConfidenceEvenForOneWay() {
        assertEquals("low", match(20, 0f, north(42, 13.4)).confidence());
    }
}
