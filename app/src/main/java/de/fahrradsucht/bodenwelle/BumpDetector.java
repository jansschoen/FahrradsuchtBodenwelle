package de.fahrradsucht.bodenwelle;

/** Experimental event detector. Thresholds require validation on real rides. */
public final class BumpDetector {
    public static final String VERSION = "experimental-2";
    public static final double DEFAULT_THRESHOLD = 40.0;
    private static final long WARMUP_MS = 2_000;
    private static final long WINDOW_MS = 250;
    private static final long COOLDOWN_MS = 1_500;
    private long firstMillis = -1;
    private long previousMillis = -1;
    private long lastEventMillis = -COOLDOWN_MS;
    private long candidateMillis = -1;
    private double candidatePeak;
    private double candidateThreshold;
    private long candidatePeakMillis;
    private double threshold = DEFAULT_THRESHOLD;

    public record Detection(long elapsedMillis, double peak, double threshold) {}

    public void setThreshold(double threshold) {
        if (!Double.isFinite(threshold) || threshold < 5 || threshold > 100)
            throw new IllegalArgumentException("Threshold must be between 5 and 100 m/s²");
        if (this.threshold != threshold) {
            this.threshold = threshold;
            candidateMillis = -1;
        }
    }

    public Detection accept(long millis, double vertical, boolean eligible) {
        if (!Double.isFinite(vertical) || millis < 0) return null;
        if (firstMillis < 0 || (previousMillis >= 0 && millis - previousMillis > 1_000)) {
            firstMillis = millis;
            candidateMillis = -1;
        }
        if (previousMillis >= 0 && millis < previousMillis) return null;
        previousMillis = millis;
        double magnitude = Math.abs(vertical);

        if (!eligible || millis - firstMillis < WARMUP_MS) {
            candidateMillis = -1;
            return null;
        }
        if (candidateMillis >= 0) {
            if (magnitude > candidatePeak) { candidatePeak = magnitude; candidatePeakMillis = millis; }
            if (millis - candidateMillis >= WINDOW_MS) {
                Detection result = new Detection(candidatePeakMillis, candidatePeak, candidateThreshold);
                lastEventMillis = millis;
                candidateMillis = -1;
                return result;
            }
        } else if (millis - lastEventMillis >= COOLDOWN_MS && magnitude >= threshold) {
            candidateMillis = millis;
            candidatePeak = magnitude;
            candidateThreshold = threshold;
            candidatePeakMillis = millis;
        }
        return null;
    }
}
