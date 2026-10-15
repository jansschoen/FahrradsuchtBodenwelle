package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Optional serial geodata work, retried while an app screen or a recording is active. */
public final class OsmEnricher {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Bodenwelle-OSM"); t.setDaemon(true); return t;
    });
    private static OsmWorkQueue queue;
    private static android.app.Application owner;
    private static volatile int visibleScreens;
    private OsmEnricher() {}
    public static boolean enabled(Context c) { return c.getSharedPreferences("geodata", Context.MODE_PRIVATE).getBoolean("enabled", true); }
    public static synchronized void enabled(Context c, boolean enabled) {
        c.getSharedPreferences("geodata", Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).apply();
        if (!enabled && queue != null) queue.clear();
    }
    public static synchronized void visible(Context c, boolean visible) {
        visibleScreens = Math.max(0, visibleScreens + (visible ? 1 : -1));
        if (!visible && visibleScreens == 0 && !RecordingService.running && queue != null) queue.clear();
        if (visible) pending(c);
    }
    public static synchronized void enqueue(Context context, long id) {
        if (!enabled(context)) return;
        Context app = context.getApplicationContext();
        if (queue == null || owner != app) {
            if (queue != null) queue.clear(); owner = (android.app.Application) app;
            queue = new OsmWorkQueue((action, delay) -> {
                java.util.concurrent.ScheduledFuture<?> scheduled = EXECUTOR.schedule(action, delay, TimeUnit.MILLISECONDS);
                return () -> scheduled.cancel(false);
            }, next -> {
                try (OsmRepository repository = new OsmRepository(app, OverpassClient::fetch, System::currentTimeMillis)) {
                    return repository.enrich(next, online(app));
                }
            }, System::currentTimeMillis, () -> enabled(app) && (visibleScreens > 0 || RecordingService.running));
        }
        queue.add(id);
    }
    public static void pending(Context c) {
        if (!enabled(c)) return;
        try (FindingStore store = new FindingStore(c)) {
            for (Finding f : store.all()) {
                OsmMatch m = f.details().osm();
                boolean finished = m.hasSuggestions() || "ambiguous".equals(m.status()) || "no_match".equals(m.status());
                boolean stale = System.currentTimeMillis() - m.queriedAt() > 7L * 24 * 60 * 60 * 1000;
                boolean oldMatcher = !OsmMatcher.VERSION.equals(m.tags().get("bodenwelle:matcher_version"));
                if (!finished || stale || oldMatcher) enqueue(c, f.id());
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
