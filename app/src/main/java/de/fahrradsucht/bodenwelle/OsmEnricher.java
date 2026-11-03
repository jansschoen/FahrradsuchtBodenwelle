package de.fahrradsucht.bodenwelle;

import android.content.Context;
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
    private static final java.util.concurrent.ConcurrentHashMap<Long,Long> refreshes=new java.util.concurrent.ConcurrentHashMap<>();
    private OsmEnricher() {}
    public static boolean enabled(Context c) { return c.getSharedPreferences("geodata", Context.MODE_PRIVATE).getBoolean("enabled", true); }
    public static synchronized void enabled(Context c, boolean enabled) {
        c.getSharedPreferences("geodata", Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).apply();
        if(!enabled){refreshes.clear();if(queue!=null)queue.clear();}
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
                    long generation=refreshes.getOrDefault(next,0L);
                    long retry=repository.enrich(next,DeviceConnectivity.canRequest(app),generation);
                    if(retry==0)refreshes.remove(next,generation);return retry;
                }
            }, System::currentTimeMillis, () -> enabled(app) && (visibleScreens > 0 || RecordingService.running));
        }
        queue.add(id);
    }
    public static void refresh(Context c,long id){if(enabled(c)){refreshes.put(id,System.currentTimeMillis());enqueue(c,id);}}
    public static void refreshMissing(Context c){
        if(!enabled(c))return;long since=System.currentTimeMillis();try(FindingStore store=new FindingStore(c)){for(Finding f:store.all())if("unknown".equals(f.details().traffic())||"unknown".equals(f.details().surface())){refreshes.put(f.id(),since);enqueue(c,f.id());}}
    }
    public static void pending(Context c) {
        if (!enabled(c)) return;
        try (FindingStore store = new FindingStore(c)) {
            for (Finding f : store.all()) {
                OsmMatch m = f.details().osm();
                boolean finished = m.hasSuggestions() || "ambiguous".equals(m.status()) || "no_match".equals(m.status()) || "gps_uncertain".equals(m.status());
                boolean stale = System.currentTimeMillis() - m.queriedAt() > 7L * 24 * 60 * 60 * 1000;
                boolean oldMatcher = !OsmMatcher.VERSION.equals(m.tags().get("bodenwelle:matcher_version"));
                if (!finished || stale || oldMatcher) enqueue(c, f.id());
            }
        }
    }
}
