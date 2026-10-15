package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Conservative local association, not navigation map matching or a legal survey. */
public final class OsmMatcher {
    public static final String VERSION = "2";
    private OsmMatcher() {}
    private record Candidate(OsmWay way, double distance, double angle, double score) {}
    public static OsmMatch match(double lat, double lon, float accuracy, Float bearing, List<OsmWay> ways, long time, String source) {
        return match(lat, lon, accuracy, 0, bearing, ways, time, source);
    }
    public static OsmMatch match(double lat, double lon, float accuracy, float projectionMargin, Float bearing,
                                 List<OsmWay> ways, long time, String source) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180
                || !Float.isFinite(accuracy) || accuracy < 0 || accuracy > 50) return empty("no_match", time, source);
        float uncertainty = accuracy + Math.max(0, Math.min(10, projectionMargin));
        List<Candidate> candidates = new ArrayList<>();
        for (OsmWay way : ways) {
            if (!way.tags().containsKey("highway") || way.geometry().size() < 2) continue;
            double nearest = Double.POSITIVE_INFINITY, angle = 0;
            for (int i = 1; i < way.geometry().size(); i++) {
                OsmWay.Point a = way.geometry().get(i - 1), b = way.geometry().get(i);
                double ax = Math.toRadians(a.longitude() - lon) * 6371000 * Math.cos(Math.toRadians(lat));
                double ay = Math.toRadians(a.latitude() - lat) * 6371000;
                double bx = Math.toRadians(b.longitude() - lon) * 6371000 * Math.cos(Math.toRadians(lat));
                double by = Math.toRadians(b.latitude() - lat) * 6371000;
                double dx = bx - ax, dy = by - ay, length2 = dx * dx + dy * dy;
                if (length2 < 0.01) continue;
                double t = Math.max(0, Math.min(1, -(ax * dx + ay * dy) / length2));
                double distance = Math.hypot(ax + t * dx, ay + t * dy);
                if (distance < nearest) {
                    nearest = distance;
                    double direction = (Math.toDegrees(Math.atan2(dx, dy)) + 360) % 360;
                    double difference = bearing == null || !Float.isFinite(bearing) ? 0 : Math.abs(direction - bearing) % 180;
                    angle = Math.min(difference, 180 - difference);
                }
            }
            if (nearest <= 50 && angle <= 60)
                candidates.add(new Candidate(way, nearest, angle, nearest + angle / 3));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::score));
        if (candidates.isEmpty()) return empty("no_match", time, source);
        Candidate best = candidates.get(0);
        List<Candidate> plausible = new ArrayList<>();
        for (Candidate candidate : candidates) if (candidate.score - best.score <= Math.max(8, uncertainty)) plausible.add(candidate);
        if (plausible.size() > 1) return consensus(plausible, time, source);
        OsmTags.Result tags = OsmTags.classify(best.way.tags());
        String confidence = accuracy <= 8 && projectionMargin <= 2 && best.distance <= 8 && !tags.uncertain() ? "medium" : "low";
        return new OsmMatch("matched", best.way.id(), best.way.tags(), time, confidence, best.distance, source, tags.traffic(), tags.surface());
    }
    private static OsmMatch consensus(List<Candidate> candidates, long time, String source) {
        OsmTags.Result first = OsmTags.classify(candidates.get(0).way.tags());
        String traffic = first.traffic(), surface = first.surface();
        java.util.Map<String, String> sharedTags = new java.util.HashMap<>(candidates.get(0).way.tags());
        for (Candidate candidate : candidates) {
            OsmTags.Result tags = OsmTags.classify(candidate.way.tags());
            if (!traffic.equals(tags.traffic())) traffic = "unknown";
            if (!surface.equals(tags.surface())) surface = "unknown";
            sharedTags.entrySet().removeIf(e -> !e.getValue().equals(candidate.way.tags().get(e.getKey())));
        }
        if ("unknown".equals(traffic) && "unknown".equals(surface)) return empty("ambiguous", time, source);
        sharedTags.put("bodenwelle:candidate_way_ids", candidates.stream().map(c -> Long.toString(c.way.id()))
                .collect(java.util.stream.Collectors.joining(";")));
        return new OsmMatch("area_suggestion", null, sharedTags, time, "low", null, source, traffic, surface);
    }
    private static OsmMatch empty(String status, long time, String source) {
        return new OsmMatch(status, null, java.util.Collections.emptyMap(), time, "unknown", null, source, "unknown", "unknown");
    }
}
