package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.Locale;
import java.util.function.LongSupplier;

/** Local bounded cache; at most one network request/minute, five-minute backoff after failure. */
public final class OsmRepository implements AutoCloseable {
    private static final long TTL = 7 * 24 * 60 * 60 * 1000L;
    private final FindingStore store;
    private final SharedPreferences rate;
    private final OverpassClient.Transport transport;
    private final LongSupplier clock;
    public OsmRepository(Context context, OverpassClient.Transport transport, LongSupplier clock) {
        store = new FindingStore(context); rate = context.getSharedPreferences("osm_requests", Context.MODE_PRIVATE);
        this.transport = transport; this.clock = clock;
    }
    public static String cell(double lat, double lon) { return (long) Math.floor(lat * 500) + ":" + (long) Math.floor(lon * 500); }
    public static String query(double lat, double lon) {
        double south = Math.floor(lat * 500) / 500 - .003, west = Math.floor(lon * 500) / 500 - .003;
        return String.format(Locale.ROOT, "[out:json][timeout:20][maxsize:4194304];way[highway](%.6f,%.6f,%.6f,%.6f);out tags geom;", south, west, south + .008, west + .008);
    }
    /** Returns the earliest retry time, or zero when this item is complete. */
    public long enrich(long id, boolean online) {
        Finding finding = store.get(id); if (finding == null) return 0;
        long now = clock.getAsLong(); String cell = cell(finding.latitude(), finding.longitude());
        FindingStore.CachedOsm cached = cachedAround(finding, now);
        boolean fresh = cached != null && now >= cached.queriedAt() && now - cached.queriedAt() <= TTL;
        if (fresh || (!online && cached != null)) {
            apply(finding, cached, !fresh); return 0;
        }
        if (!online) { mark(id, "offline"); return now + 60_000; }
        long next = rate.getLong("next_request_ms", 0);
        if (now < next) { mark(id, "rate_limited"); return next; }
        rate.edit().putLong("next_request_ms", now + 60_000).apply();
        try {
            String response = transport.fetch(query(finding.latitude(), finding.longitude()));
            OverpassClient.parse(response); // Never cache a partial or invalid response.
            store.cacheOsm(cell, now, response);
            apply(finding, new FindingStore.CachedOsm(now, response), false);
        } catch (IOException | IllegalArgumentException e) {
            long backoff = e instanceof OverpassClient.RequestFailure failure ? Math.max(300_000, failure.retryAfterMillis) : 300_000;
            rate.edit().putLong("next_request_ms", now + backoff).apply();
            if (cached != null) apply(finding, cached, true); else mark(id, "error");
            return now + backoff;
        }
        return 0;
    }
    private FindingStore.CachedOsm cachedAround(Finding finding, long now) {
        long row = (long) Math.floor(finding.latitude() * 500), column = (long) Math.floor(finding.longitude() * 500);
        FindingStore.CachedOsm newest = null;
        // Neighbouring query boxes overlap widely; crossing a grid boundary needs no new request.
        for (long dy = -1; dy <= 1; dy++) for (long dx = -1; dx <= 1; dx++) {
            double south = (row + dy) / 500.0 - .003, west = (column + dx) / 500.0 - .003;
            double latMargin = 50.0 / 111_000;
            double lonMargin = latMargin / Math.max(.01, Math.cos(Math.toRadians(finding.latitude())));
            if (finding.latitude() - latMargin < south || finding.latitude() + latMargin > south + .008
                    || finding.longitude() - lonMargin < west || finding.longitude() + lonMargin > west + .008) continue;
            FindingStore.CachedOsm candidate = store.cachedOsm((row + dy) + ":" + (column + dx));
            if (candidate != null && candidate.queriedAt() <= now && (newest == null || candidate.queriedAt() > newest.queriedAt())) newest = candidate;
        }
        return newest;
    }
    private void apply(Finding finding, FindingStore.CachedOsm cached, boolean stale) {
        try {
            Float course = finding.speed() != null && finding.speed() >= 1.4f && !"bearing_uncertain".equals(finding.position().method()) ? finding.position().bearing() : null;
            // Projection uncertainty is not known; add a conservative heuristic margin for matching only.
            float margin = (float) Math.min(10, Math.abs(finding.position().distanceMeters()) * .25);
            OsmMatch match = OsmMatcher.match(finding.latitude(), finding.longitude(), finding.accuracy(), margin,
                    course, OverpassClient.parse(cached.response()), cached.queriedAt(), OverpassClient.ENDPOINT);
            java.util.Map<String, String> metadata = new java.util.HashMap<>(match.tags());
            metadata.put("bodenwelle:matcher_version", OsmMatcher.VERSION);
            match = new OsmMatch(match.status(), match.wayId(), metadata, match.queriedAt(), match.confidence(),
                    match.distanceMeters(), match.source(), match.traffic(), match.surface());
            if (stale && match.hasSuggestions()) match = new OsmMatch(match.status(), match.wayId(), match.tags(), match.queriedAt(),
                    "low", match.distanceMeters(), match.source(), match.traffic(), match.surface());
            store.setOsm(finding.id(), match);
        } catch (IOException e) { mark(finding.id(), "error"); }
    }
    private void mark(long id, String status) {
        // Network failures do not destroy a prior usable assignment or manual overrides.
        Finding current = store.get(id);
        if (current != null && !current.details().osm().hasSuggestions() && !current.details().hasAutomaticDescription())
            store.setOsm(id, new OsmMatch(status, null, java.util.Collections.emptyMap(), 0, "unknown", null, OverpassClient.ENDPOINT, "unknown", "unknown"));
    }
    @Override public void close() { store.close(); }
}
