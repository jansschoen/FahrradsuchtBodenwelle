package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class OsmWorkQueueTest {
    private static final class Scheduler implements OsmWorkQueue.Scheduler {
        Runnable next; long delay;
        @Override public Runnable schedule(Runnable action, long millis) { next = action; delay = millis; return () -> { if (next == action) next = null; }; }
        void run() { Runnable action = next; next = null; assertNotNull(action); action.run(); }
    }
    @Test public void rateLimitedItemsAreRetriedAtTheirDeadlineWithoutBlockingOtherCachedItems() {
        Scheduler scheduler = new Scheduler(); AtomicLong now = new AtomicLong(1000); List<Long> calls = new ArrayList<>();
        OsmWorkQueue queue = new OsmWorkQueue(scheduler, id -> { calls.add(id); return id == 1 && now.get() < 61_000 ? 61_000 : 0; }, now::get, () -> true);
        queue.add(1); queue.add(2); queue.add(2); scheduler.run();
        assertEquals(List.of(1L, 2L), calls); assertEquals(60_000, scheduler.delay);
        now.set(61_000); scheduler.run(); assertEquals(List.of(1L, 2L, 1L), calls); assertNull(scheduler.next);
    }
    @Test public void newlyArrivingItemsWakeADeferredQueueAndCanUseTheCacheImmediately() {
        Scheduler scheduler = new Scheduler(); AtomicLong now = new AtomicLong(1000); List<Long> calls = new ArrayList<>();
        OsmWorkQueue queue = new OsmWorkQueue(scheduler, id -> { calls.add(id); return id == 1 ? 61_000 : 0; }, now::get, () -> true);
        queue.add(1); scheduler.run(); queue.add(2); assertEquals(0, scheduler.delay); scheduler.run();
        assertEquals(List.of(1L, 2L), calls); assertEquals(60_000, scheduler.delay);
    }
    @Test public void leavingTheAppOrDisablingGeodataStopsScheduledWork() {
        Scheduler scheduler = new Scheduler(); AtomicBoolean enabled = new AtomicBoolean(true); List<Long> calls = new ArrayList<>();
        OsmWorkQueue queue = new OsmWorkQueue(scheduler, id -> { calls.add(id); return 61_000; }, () -> 1000, enabled::get);
        queue.add(1); scheduler.run(); enabled.set(false); scheduler.run(); assertEquals(List.of(1L), calls); assertNull(scheduler.next);
        enabled.set(true); queue.add(2); queue.clear(); assertNull(scheduler.next);
    }
    @Test public void oneBrokenCacheItemDoesNotStopOtherMeasurements() {
        Scheduler scheduler = new Scheduler(); List<Long> calls = new ArrayList<>();
        OsmWorkQueue queue = new OsmWorkQueue(scheduler, id -> { calls.add(id); if (id == 1) throw new IllegalStateException(); return 0; }, () -> 1000, () -> true);
        queue.add(1); queue.add(2); scheduler.run(); assertEquals(List.of(1L, 2L), calls); assertNull(scheduler.next);
    }
}
