package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.List;

/** Conservative suggestions only. No sensor event is automatically merged. */
public final class BundleSuggestions {
    public static final double MAX_DISTANCE_METERS = 12;
    private BundleSuggestions() {}
    public static double distance(Finding a, Finding b) {
        double lat = Math.toRadians(b.latitude() - a.latitude()), lon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(lat/2) * Math.sin(lat/2) + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude())) * Math.sin(lon/2) * Math.sin(lon/2);
        return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, h)));
    }
    public static boolean compatible(FindingBundle a, FindingBundle b) {
        if (a.key() == b.key() || "other".equals(a.category()) || !a.category().equals(b.category())) return false;
        for (Finding x : a.observations()) for (Finding y : b.observations()) {
            if (!x.reviewed() || !y.reviewed() || !Float.isFinite(x.accuracy()) || !Float.isFinite(y.accuracy()) || x.accuracy()<0 || y.accuracy()<0
                    || !Double.isFinite(x.latitude()) || !Double.isFinite(y.latitude()) || !Double.isFinite(x.longitude()) || !Double.isFinite(y.longitude()) || x.accuracy() > 15 || y.accuracy() > 15
                    || distance(x, y) > MAX_DISTANCE_METERS) return false;
            Long wx = x.details().osm().wayId(), wy = y.details().osm().wayId();
            if (wx != null && wy != null && !wx.equals(wy)) return false;
            Float bx = x.position().bearing(), by = y.position().bearing();
            if (bx != null && by != null) {
                double angle = Math.abs(bx - by) % 360;
                if (Math.min(angle, 360 - angle) > 45) return false;
            }
        }
        return true;
    }
    public static List<List<FindingBundle>> suggest(List<FindingBundle> bundles) {
        List<List<FindingBundle>> candidates = new ArrayList<>();
        for (FindingBundle bundle : bundles) {
            List<FindingBundle> target = null;
            for (List<FindingBundle> candidate : candidates) if (candidate.stream().allMatch(b -> compatible(b, bundle))) { target = candidate; break; }
            if (target == null) { target = new ArrayList<>(); candidates.add(target); }
            target.add(bundle);
        }
        candidates.removeIf(c -> c.size() < 2); return candidates;
    }
}
