package de.fahrradsucht.bodenwelle;

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class OsmTagsTest {
    @Test public void ordinaryDesignatedCyclewayDoesNotImplyMandatoryUse() {
        assertEquals("cycleway", OsmTags.classify(Map.of("highway", "cycleway", "bicycle", "designated")).traffic());
        assertEquals("cycleway", OsmTags.classify(Map.of("highway", "path", "bicycle", "designated")).traffic());
    }
    @Test public void explicitGermanSignsAreOnlyMappedOnDedicatedPaths() {
        for (String sign : new String[]{"DE:237", "DE:241"})
            assertEquals("mandatory_cycleway", OsmTags.classify(Map.of("highway", "cycleway", "traffic_sign", sign)).traffic());
        assertEquals("shared_path", OsmTags.classify(Map.of("highway", "path", "traffic_sign", "DE:240")).traffic());
        assertEquals("unknown", OsmTags.classify(Map.of("highway", "residential", "traffic_sign", "DE:237")).traffic());
        assertEquals("cycleway", OsmTags.classify(Map.of("highway", "cycleway", "traffic_sign", "DE:1237")).traffic());
    }
    @Test public void sharedSeparatedAndContradictoryPathsAreDistinguished() {
        assertEquals("shared_path", OsmTags.classify(Map.of("highway", "path", "bicycle", "designated", "foot", "designated", "segregated", "no")).traffic());
        assertEquals("cycleway", OsmTags.classify(Map.of("highway", "path", "bicycle", "designated", "foot", "designated", "segregated", "yes")).traffic());
        assertEquals("unknown", OsmTags.classify(Map.of("highway", "path", "bicycle", "designated", "foot", "designated")).traffic());
        assertEquals("unknown", OsmTags.classify(Map.of("highway", "cycleway", "traffic_sign", "DE:237;DE:240")).traffic());
        assertEquals("unknown", OsmTags.classify(Map.of("highway", "cycleway", "traffic_sign", "DE:237", "bicycle", "no")).traffic());
        assertEquals("unknown", OsmTags.classify(Map.of("highway", "path", "traffic_sign", "DE:240", "segregated", "yes")).traffic());
    }
    @Test public void roadsAndSideLanesDoNotOverstatePositionCertainty() {
        assertEquals("bicycle_road", OsmTags.classify(Map.of("highway", "residential", "bicycle_road", "yes")).traffic());
        assertEquals("carriageway", OsmTags.classify(Map.of("highway", "residential")).traffic());
        assertEquals("unknown", OsmTags.classify(Map.of("highway", "residential", "cycleway:right", "track")).traffic());
        assertEquals("unknown", OsmTags.classify(Map.of("highway", "residential", "cycleway:left", "lane")).traffic());
        assertEquals("other", OsmTags.classify(Map.of("highway", "track")).traffic());
        assertEquals("unknown", OsmTags.classify(Map.of()).traffic());
    }
    @Test public void surfaceMaterialsDoNotConfuseSmoothnessWithBumpIntensity() {
        Map<String, String> expected = Map.of("asphalt", "asphalt", "concrete:plates", "concrete", "sett", "stone_paving",
                "cobblestone", "stone_paving", "compacted", "compacted", "fine_gravel", "compacted", "gravel", "gravel", "ground", "unpaved", "wood", "other");
        expected.forEach((raw, category) -> assertEquals(category, OsmTags.classify(Map.of("surface", raw, "smoothness", "very_bad")).surface()));
        assertEquals("concrete_paving", OsmTags.classify(Map.of("surface", "paving_stones", "material", "concrete")).surface());
        assertEquals("other", OsmTags.classify(Map.of("surface", "paving_stones")).surface());
        assertEquals("unknown", OsmTags.classify(Map.of("smoothness", "excellent", "tracktype", "grade1", "width", "2")).surface());
    }
}
