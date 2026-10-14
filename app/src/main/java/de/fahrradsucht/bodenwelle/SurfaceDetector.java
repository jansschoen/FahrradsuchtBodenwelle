package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.List;

/** Experimental sustained vibration classifier; isolated shocks retain the user threshold. */
public final class SurfaceDetector {
    public record Section(long startMillis, long endMillis, double peak, double threshold, double lengthMeters) {}
    public record Update(List<BumpDetector.Detection> singles, Section section) {}
    private final BumpDetector bumps = new BumpDetector();
    private final List<BumpDetector.Detection> pending = new ArrayList<>();
    private final List<Double> window = new ArrayList<>();
    private long previous = -1, windowStart = -1, start = -1, lastActive = -1, activeMillis;
    private double threshold = BumpDetector.DEFAULT_THRESHOLD, length, lastActiveLength, peak;
    private boolean confirmed;

    public boolean sectionActive() { return confirmed; }
    public Update accept(long millis, double vertical, double newThreshold, boolean eligible, double speed) {
        List<BumpDetector.Detection> singles = new ArrayList<>();
        Section complete = null;
        if (!Double.isFinite(vertical) || millis < 0 || (previous >= 0 && millis <= previous)) return finish();
        if (newThreshold != threshold || !eligible || !Double.isFinite(speed) || speed < 1.4 || speed > 20
                || (previous >= 0 && millis - previous > 500)) {
            Update end = finish(); singles.addAll(end.singles()); complete = end.section();
            bumps.accept(millis, 0, false);
        }
        threshold = newThreshold; bumps.setThreshold(threshold); previous = millis;
        if (!eligible || !Double.isFinite(speed) || speed < 1.4 || speed > 20) return new Update(singles, complete);
        if (windowStart < 0) windowStart = millis;
        if (millis - windowStart >= 250) {
            long duration = millis - windowStart;
            boolean rough = activeWindow();
            if (rough) {
                if (start < 0) start = windowStart;
                activeMillis += duration;
                length += speed * duration / 1000.0;
                lastActive = millis;
                lastActiveLength = length;
                for (double value : window) peak = Math.max(peak, Math.abs(value));
                if (millis - start >= 3000 && activeMillis >= .8 * (millis - start) && length >= 10) confirmed = true;
            } else if (start >= 0) length += speed * duration / 1000.0;
            if (start >= 0 && (millis - lastActive >= 500 || millis - start >= 120_000
                    || (!confirmed && millis - start >= 3000 && activeMillis < .8 * (millis - start)))) {
                if (confirmed) complete = section();
                discardSectionSingles(confirmed ? start : -1, lastActive);
                clearSection();
            }
            window.clear(); windowStart = millis;
        }
        window.add(vertical);
        BumpDetector.Detection event = bumps.accept(millis, vertical, true);
        if (event != null && !(confirmed && event.elapsedMillis() >= start)) pending.add(event);
        if (confirmed) discardSectionSingles(start, millis);
        for (int i = 0; i < pending.size();) {
            BumpDetector.Detection candidate = pending.get(i);
            if (start < 0 || candidate.elapsedMillis() < start || millis - candidate.elapsedMillis() > 4200) {
                singles.add(candidate); pending.remove(i);
            } else i++;
        }
        return new Update(singles, complete);
    }
    private boolean activeWindow() {
        if (window.size() < 8) return false;
        double mean = 0; for (double value : window) mean += value; mean /= window.size();
        double floor = Math.max(1.5, threshold * .15), variance = 0; int moving = 0;
        for (double value : window) {
            double residual = value - mean;
            variance += residual * residual;
            if (Math.abs(residual) >= floor) moving++;
        }
        return Math.sqrt(variance / window.size()) >= floor && moving >= .2 * window.size();
    }
    private Section section() { return new Section(start, lastActive, peak, threshold, lastActiveLength); }
    private void discardSectionSingles(long from, long to) {
        if (from >= 0) pending.removeIf(d -> d.elapsedMillis() >= from && d.elapsedMillis() <= to);
    }
    private void clearSection() { start = lastActive = -1; activeMillis = 0; length = lastActiveLength = peak = 0; confirmed = false; }
    public Update finish() {
        bumps.cancelPending();
        Section section = confirmed ? section() : null;
        if (confirmed) discardSectionSingles(start, lastActive);
        List<BumpDetector.Detection> singles = new ArrayList<>(pending); pending.clear();
        clearSection(); window.clear(); windowStart = previous = -1;
        return new Update(singles, section);
    }
}
