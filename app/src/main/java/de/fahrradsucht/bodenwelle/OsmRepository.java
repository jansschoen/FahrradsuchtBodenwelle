package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/** OSM association at the original accuracy-circle centre, with a shared rate budget and bounded cache. */
public final class OsmRepository implements AutoCloseable {
    private static final long TTL = 7 * 24 * 60 * 60 * 1000L;
    private final FindingStore store;
    private final SharedPreferences rate;
    private final OverpassClient.Transport transport;
    private final LongSupplier clock;
    private final OsmDiagnostics diagnostics;

    public OsmRepository(Context context, OverpassClient.Transport transport, LongSupplier clock) {
        store = new FindingStore(context);
        rate = context.getSharedPreferences("osm_requests", Context.MODE_PRIVATE);
        this.transport = transport;
        this.clock = clock;
        diagnostics = new OsmDiagnostics(context);
    }
    public static String cell(double lat, double lon) {
        return "context2:" + (long) Math.floor(lat * 500) + ":" + (long) Math.floor(lon * 500);
    }
    public static String query(double lat, double lon) {
        double south = Math.floor(lat * 500) / 500 - .003, west = Math.floor(lon * 500) / 500 - .003;
        String box=String.format(Locale.ROOT,"(%.6f,%.6f,%.6f,%.6f)",south,west,south+.008,west+.008);
        String containing="(";
        for(double[] point:new double[][]{{south,west},{south,west+.008},{south+.008,west},{south+.008,west+.008},{south+.004,west+.004}})
            containing+=String.format(Locale.ROOT,"is_in(%.6f,%.6f);",point[0],point[1]);
        containing+=")->.containing;(area.containing[leisure~\"^(park|garden)$\"];area.containing[access=private];area.containing[ownership=private];)->.context;";
        return "[out:json][timeout:20][maxsize:4194304];"+containing+"(way[highway]"+box+";"
                +"way[leisure~\"^(park|garden)$\"]"+box+";relation[leisure~\"^(park|garden)$\"]"+box+";"
                +"way[access=private]"+box+";relation[access=private]"+box+";"
                +"way[ownership=private]"+box+";relation[ownership=private]"+box+";way(pivot.context);relation(pivot.context););out tags geom;";
    }

    public long enrich(long id, boolean online) { return enrich(id, online, 0); }

    /** A manual refresh shares one new cache answer with all entries queued at the same instant. */
    public long enrich(long id, boolean online, long refreshSince) {
        Finding finding = store.get(id);
        if (finding == null) return 0;
        double lat = finding.position().gpsLatitude(), lon = finding.position().gpsLongitude();
        long now = clock.getAsLong();
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180
                || !Float.isFinite(finding.accuracy()) || finding.accuracy() < 0 || finding.accuracy() > 50) {
            mark(id, "gps_uncertain");
            diagnostics.result("gps_uncertain", false);
            diagnostics.waiting("Ursprüngliche GPS-Genauigkeit ist für eine Wegzuordnung nicht ausreichend.");
            return 0;
        }
        FindingStore.CachedOsm cached = cachedAround(lat, lon, now);
        boolean fresh = cached != null && now >= cached.queriedAt() && now - cached.queriedAt() <= TTL;
        boolean refreshed = refreshSince == 0 || cached != null && cached.queriedAt() >= refreshSince;
        if ((fresh && refreshed) || (!online && cached != null)) {
            apply(finding, cached, !fresh, true);
            return !online && refreshSince > 0 ? now + 60_000 : 0;
        }
        if (!online) {
            mark(id, "offline");
            diagnostics.waiting("Kein Internet-fähiges aktives Netz. Erneuter Versuch bei aktiver App oder Fahrt.");
            return now + 60_000;
        }
        long next = rate.getLong("next_request_ms", 0);
        if (now < next) {
            if (cached != null) apply(finding, cached, !fresh, true);
            else mark(id, "rate_limited");
            diagnostics.waiting("Abfragebudget: nächster Netzversuch frühestens in "
                    + Math.max(1, (next - now + 999) / 1000) + " Sekunden.");
            return next;
        }
        rate.edit().putLong("next_request_ms", now + 60_000).apply();
        diagnostics.request(now);
        diagnostics.waiting("");
        try {
            String response = transport.fetch(query(lat, lon));
            List<OsmWay> ways = OverpassClient.parse(response);
            diagnostics.success(ways.size());
            store.cacheOsm(cell(lat, lon), now, response);
            apply(finding, new FindingStore.CachedOsm(now, response), false, false);
            return 0;
        } catch (IOException | IllegalArgumentException failure) {
            IOException error = failure instanceof IOException io ? io : new IOException(failure);
            diagnostics.failure(error);
            long backoff = error instanceof OverpassClient.RequestFailure request
                    ? Math.max(300_000, request.retryAfterMillis) : 300_000;
            long deadline = clock.getAsLong() + backoff;
            rate.edit().putLong("next_request_ms", deadline).apply();
            if (cached != null) apply(finding, cached, true, true);
            else mark(id, "error");
            diagnostics.waiting("Fehlerpause; die Aufzeichnung bleibt unabhängig verfügbar.");
            return deadline;
        }
    }
    private FindingStore.CachedOsm cachedAround(double lat, double lon, long now) {
        long row = (long) Math.floor(lat * 500), column = (long) Math.floor(lon * 500);
        FindingStore.CachedOsm newest = null;
        for (long dy = -1; dy <= 1; dy++) for (long dx = -1; dx <= 1; dx++) {
            double south = (row + dy) / 500.0 - .003, west = (column + dx) / 500.0 - .003;
            double latMargin = 50.0 / 111_000, lonMargin = latMargin / Math.max(.01, Math.cos(Math.toRadians(lat)));
            if (lat - latMargin < south || lat + latMargin > south + .008
                    || lon - lonMargin < west || lon + lonMargin > west + .008) continue;
            FindingStore.CachedOsm candidate = store.cachedOsm("context2:"+(row + dy) + ":" + (column + dx));
            if (candidate != null && candidate.queriedAt() <= now
                    && (newest == null || candidate.queriedAt() > newest.queriedAt())) newest = candidate;
        }
        return newest;
    }
    private void apply(Finding finding, FindingStore.CachedOsm cached, boolean stale, boolean fromCache) {
        try {
            Float bearing = finding.speed() != null && finding.speed() >= 1.4f
                    && !"bearing_uncertain".equals(finding.position().method()) ? finding.position().bearing() : null;
            OsmMatch match = OsmMatcher.match(finding.position().gpsLatitude(), finding.position().gpsLongitude(),
                    finding.accuracy(), bearing, OverpassClient.parse(cached.response()), cached.queriedAt(), OverpassClient.ENDPOINT);
            Map<String, String> metadata = new HashMap<>(match.tags());
            metadata.put("bodenwelle:matcher_version", OsmMatcher.VERSION);
            metadata.put("bodenwelle:position_basis", "gps_circle_center");
            metadata.putAll(OsmPlaceContext.inspect(cached.response(),finding.position().gpsLatitude(),finding.position().gpsLongitude(),finding.accuracy(),match));
            match = new OsmMatch(match.status(), match.wayId(), metadata, match.queriedAt(),
                    stale && match.hasSuggestions() ? "low" : match.confidence(), match.distanceMeters(),
                    match.source(), match.traffic(), match.surface());
            store.setOsm(finding.id(), match);
            diagnostics.result(match.status(), fromCache);
            diagnostics.waiting("");
        } catch (IOException failure) {
            diagnostics.failure(failure);
            mark(finding.id(), "error");
        }
    }
    private void mark(long id, String status) {
        Finding current = store.get(id);
        if (current != null && !current.details().osm().hasSuggestions() && !current.details().hasAutomaticDescription())
            store.setOsm(id, new OsmMatch(status, null, java.util.Collections.emptyMap(), 0, "unknown", null,
                    OverpassClient.ENDPOINT, "unknown", "unknown"));
    }
    @Override public void close() { store.close(); }
}
