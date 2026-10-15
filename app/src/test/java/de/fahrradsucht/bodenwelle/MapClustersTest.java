package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MapClustersTest {
    private FindingBundle single(long id, double latitude) {
        Finding f = new Finding(id, 1000, latitude, 13.4, 5, 5f, 48.0, 40.0, "automatic", "root_heave", "", false, 0);
        return new FindingBundle(-id, List.of(f));
    }
    @Test public void everyObservationBeyondTheOldFiveHundredLimitIsRepresented() {
        List<FindingBundle> saved = new ArrayList<>();
        for (int i = 1; i <= 1200; i++) saved.add(single(i, 52.5 + i * .00001));
        List<MapClusters.Cluster> groups = MapClusters.group(saved, 16, 256, 44);
        assertEquals(1200, groups.stream().mapToInt(MapClusters.Cluster::count).sum());
        assertEquals(1200, groups.stream().flatMap(c -> c.bundles().stream()).map(FindingBundle::key).distinct().count());
    }
    @Test public void zoomingOnlyChangesDisplayGroupsAndNeverConfirmedMembership() {
        FindingBundle a = single(1, 52.5), b = single(2, 52.50008);
        FindingBundle confirmed = new FindingBundle(7, List.of(a.representative(), b.representative()));
        List<MapClusters.Cluster> wide = MapClusters.group(List.of(a, b), 10, 256, 44);
        assertEquals(1, wide.size()); assertEquals(2, wide.get(0).count());
        assertEquals(2, MapClusters.group(List.of(a, b), 20, 256, 44).size());
        assertEquals(2, MapClusters.group(List.of(confirmed), 20, 256, 44).get(0).count());
        assertEquals(-1, a.key()); assertEquals(7, confirmed.key());
    }
    @Test public void pickerReachesOverlappingBadgesAcrossGridBoundaries() {
        List<FindingBundle> saved = new ArrayList<>();
        for (int i = 1; i <= 100; i++) saved.add(single(i, 52.5 + i * .000001));
        List<MapClusters.Cluster> groups = MapClusters.group(saved, 19, 256, 44);
        assertTrue(groups.size() > 1);
        assertTrue(MapClusters.atMarker(groups.get(0), groups, 19, 256, 50).size() > groups.get(0).bundles().size());
    }
}
