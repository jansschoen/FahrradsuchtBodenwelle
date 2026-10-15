package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Zoom-dependent display groups only: never changes saved measurements or confirmed bundles. */
public final class MapClusters {
    private MapClusters() {}
    public record Cluster(List<FindingBundle> bundles) {
        public Cluster { bundles = java.util.Collections.unmodifiableList(new ArrayList<>(bundles)); }
        public int count() { return bundles.stream().mapToInt(b -> b.observations().size()).sum(); }
        public Finding representative() { return bundles.get(0).representative(); }
        public Intensity intensity() {
            return bundles.stream().map(FindingBundle::intensity).max(java.util.Comparator
                    .comparingInt((Intensity i) -> i.heightBased() ? 1 : 0)
                    .thenComparingDouble(i -> i.value() == null ? -1 : i.value())).orElseThrow();
        }
    }
    public static List<Cluster> group(List<FindingBundle> bundles, double zoom, double tilePixels, double cellPixels) {
        double world = tilePixels * Math.pow(2, zoom);
        Map<String, List<FindingBundle>> cells = new LinkedHashMap<>();
        for (FindingBundle bundle : bundles) {
            Finding f = bundle.representative();
            String key = (long) Math.floor(x(f.longitude(), world) / cellPixels) + ":" + (long) Math.floor(y(f.latitude(), world) / cellPixels);
            cells.computeIfAbsent(key, unused -> new ArrayList<>()).add(bundle);
        }
        List<Cluster> result = new ArrayList<>();
        for (List<FindingBundle> cell : cells.values()) result.add(new Cluster(cell));
        return result;
    }
    /** Includes adjacent grid cells so overlapping badges cannot hide points at a cell boundary. */
    public static List<FindingBundle> atMarker(Cluster tapped, List<Cluster> clusters, double zoom, double tilePixels, double pixels) {
        double world = tilePixels * Math.pow(2, zoom);
        Finding anchor = tapped.representative(); List<FindingBundle> result = new ArrayList<>();
        for (Cluster cluster : clusters) {
            Finding f = cluster.representative();
            double dx = Math.abs(x(anchor.longitude(), world) - x(f.longitude(), world)); dx = Math.min(dx, world - dx);
            double dy = y(anchor.latitude(), world) - y(f.latitude(), world);
            if (cluster == tapped || Math.hypot(dx, dy) <= pixels) result.addAll(cluster.bundles());
        }
        return result;
    }
    private static double x(double lon, double world) { return (lon + 180) / 360 * world; }
    private static double y(double lat, double world) {
        double sin = Math.sin(Math.toRadians(Math.max(-85.05112878, Math.min(85.05112878, lat))));
        return (.5 - Math.log((1 + sin) / (1 - sin)) / (4 * Math.PI)) * world;
    }
}
