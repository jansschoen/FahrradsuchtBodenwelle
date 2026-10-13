package de.fahrradsucht.bodenwelle;

import java.util.Map;

public record OsmMatch(String status, Long wayId, Map<String, String> tags, long queriedAt,
                       String confidence, Double distanceMeters, String source,
                       String traffic, String surface) {
    public OsmMatch { tags = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(tags)); }
    public static OsmMatch empty(String status) {
        return new OsmMatch(status, null, java.util.Collections.emptyMap(), 0, "unknown", null, "", "unknown", "unknown");
    }
    public boolean assigned() { return "matched".equals(status); }
}
