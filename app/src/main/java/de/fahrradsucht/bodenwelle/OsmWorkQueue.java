package de.fahrradsucht.bodenwelle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** Serial, deduplicated work with per-item deadlines; waiting items never block cached neighbours. */
final class OsmWorkQueue {
    interface Scheduler { Runnable schedule(Runnable action, long delayMillis); }
    interface Processor { long process(long id); }
    private final Map<Long, Long> pending = new LinkedHashMap<>();
    private final Scheduler scheduler;
    private final Processor processor;
    private final LongSupplier clock;
    private final BooleanSupplier enabled;
    private Runnable cancel;
    private boolean running;
    OsmWorkQueue(Scheduler scheduler, Processor processor, LongSupplier clock, BooleanSupplier enabled) {
        this.scheduler = scheduler; this.processor = processor; this.clock = clock; this.enabled = enabled;
    }
    synchronized void add(long id) {
        if (!enabled.getAsBoolean()) return;
        pending.putIfAbsent(id, 0L);
        if (!running) schedule();
    }
    synchronized void clear() {
        pending.clear(); if (cancel != null) cancel.run(); cancel = null;
    }
    private void schedule() {
        if (cancel != null) cancel.run(); cancel = null;
        if (pending.isEmpty() || !enabled.getAsBoolean()) return;
        long first = pending.values().stream().mapToLong(Long::longValue).min().orElse(0);
        cancel = scheduler.schedule(this::drain, Math.max(0, first - clock.getAsLong()));
    }
    private void drain() {
        synchronized (this) { if (running) return; running = true; cancel = null; }
        while (true) {
            long id;
            synchronized (this) {
                if (!enabled.getAsBoolean()) { pending.clear(); running = false; return; }
                Long ready = null;
                for (Map.Entry<Long, Long> item : pending.entrySet()) if (item.getValue() <= clock.getAsLong()) { ready = item.getKey(); break; }
                if (ready == null) { running = false; schedule(); return; }
                id = ready; pending.remove(id);
            }
            long retry = 0;
            try { retry = processor.process(id); }
            catch (RuntimeException ignored) { /* A geodata failure never stops sensor recording. */ }
            synchronized (this) {
                if (retry > 0 && enabled.getAsBoolean()) pending.put(id, Math.max(clock.getAsLong() + 1000, retry));
            }
        }
    }
}
