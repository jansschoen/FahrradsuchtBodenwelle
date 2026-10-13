package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Optional geodata work runs separately from the recording and UI threads. */
public final class OsmEnricher {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "Bodenwelle-OSM"); t.setDaemon(true); return t; });
    private static final LinkedHashSet<Long> QUEUED = new LinkedHashSet<>();
    private static boolean draining;
    private OsmEnricher() {}
    public static boolean enabled(Context c) { return c.getSharedPreferences("geodata", Context.MODE_PRIVATE).getBoolean("enabled", true); }
    public static void enabled(Context c, boolean enabled) { c.getSharedPreferences("geodata", Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).apply(); }
    public static void enqueue(Context context, long id) {
        if (!enabled(context)) return;
        Context app = context.getApplicationContext();
        synchronized (QUEUED) {
            if (QUEUED.size() < 200) QUEUED.add(id);
            if (draining) return; draining = true;
        }
        EXECUTOR.execute(() -> {
            try (OsmRepository repository = new OsmRepository(app, OverpassClient::fetch, System::currentTimeMillis)) {
                while (true) {
                    Long next;
                    synchronized (QUEUED) {
                        if (QUEUED.isEmpty()) { draining = false; return; }
                        next = QUEUED.iterator().next(); QUEUED.remove(next);
                    }
                    if (enabled(app)) repository.enrich(next, online(app));
                }
            } catch (RuntimeException ignored) {
                // A cache/database issue must never stop the sensor recorder. Entries stay locally available.
                synchronized (QUEUED) { draining = false; QUEUED.clear(); }
            }
        });
    }
    public static void pending(Context c) {
        if (!enabled(c)) return;
        try (FindingStore store = new FindingStore(c)) {
            int count = 0;
            for (Finding f : store.all()) {
                OsmMatch m = f.details().osm();
                boolean finished = m.assigned() || "ambiguous".equals(m.status()) || "no_match".equals(m.status());
                boolean stale = System.currentTimeMillis() - m.queriedAt() > 7L * 24 * 60 * 60 * 1000;
                if ((!finished || stale) && count++ < 200) enqueue(c, f.id());
            }
        }
    }
    private static boolean online(Context c) {
        ConnectivityManager manager = c.getSystemService(ConnectivityManager.class);
        NetworkCapabilities capabilities = manager == null ? null : manager.getNetworkCapabilities(manager.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
