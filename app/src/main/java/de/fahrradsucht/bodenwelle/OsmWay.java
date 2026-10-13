package de.fahrradsucht.bodenwelle;

import java.util.List;
import java.util.Map;

public record OsmWay(long id, List<Point> geometry, Map<String, String> tags) {
    public OsmWay { geometry = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(geometry)); tags = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(tags)); }
    public record Point(double latitude, double longitude) {}
}
