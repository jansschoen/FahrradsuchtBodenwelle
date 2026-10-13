package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Conservative local association, not navigation map matching or a legal survey. */
public final class OsmMatcher {
    private OsmMatcher() {}
    private record Candidate(OsmWay way, double distance, double angle, double score) {}
    public static OsmMatch match(double lat, double lon, float accuracy, Float bearing, List<OsmWay> ways, long time, String source) {
        if (!Float.isFinite(accuracy) || accuracy < 0 || accuracy > 25) return empty("no_match", time, source);
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
            if (nearest <= Math.max(12, accuracy + 5) && angle <= 60)
                candidates.add(new Candidate(way, nearest, angle, nearest + angle / 3));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::score));
        if (candidates.isEmpty()) return empty("no_match", time, source);
        Candidate best = candidates.get(0);
        if (candidates.size() > 1 && candidates.get(1).score - best.score <= Math.max(8, accuracy))
            return empty("ambiguous", time, source);
        OsmTags.Result tags = OsmTags.classify(best.way.tags());
        String confidence = accuracy <= 8 && best.distance <= 8 && !tags.uncertain() ? "medium" : "low";
        return new OsmMatch("matched", best.way.id(), best.way.tags(), time, confidence, best.distance, source, tags.traffic(), tags.surface());
    }
    private static OsmMatch empty(String status, long time, String source) {
        return new OsmMatch(status, null, java.util.Collections.emptyMap(), time, "unknown", null, source, "unknown", "unknown");
    }
}
