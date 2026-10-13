package de.fahrradsucht.bodenwelle;

/** Experimental event detector. Thresholds require validation on real rides. */
public final class BumpDetector {
    public static final String VERSION = "experimental-1";
    private static final long WARMUP_MS = 2_000;
    private static final long WINDOW_MS = 250;
    private static final long COOLDOWN_MS = 1_500;
    private long firstMillis = -1;
    private long previousMillis = -1;
    private long lastEventMillis = -COOLDOWN_MS;
    private long candidateMillis = -1;
    private double candidatePeak;
    private double candidateThreshold;
    private double noisePower = 0.16;

    public record Detection(long elapsedMillis, double peak, double threshold) {}

    public Detection accept(long millis, double vertical, boolean eligible) {
        if (!Double.isFinite(vertical) || millis < 0) return null;
        if (firstMillis < 0 || (previousMillis >= 0 && millis - previousMillis > 1_000)) {
            firstMillis = millis;
            candidateMillis = -1;
        }
        if (previousMillis >= 0 && millis < previousMillis) return null;
        double dt = previousMillis < 0 ? 0.01 : Math.max(0, (millis - previousMillis) / 1000.0);
        previousMillis = millis;
        double magnitude = Math.abs(vertical);
        double threshold = Math.max(3.0, 5.0 * Math.sqrt(noisePower));
        double clipped = Math.min(magnitude, 3.0);
        double weight = Math.exp(-dt / 3.0);
        noisePower = weight * noisePower + (1 - weight) * clipped * clipped;

        if (!eligible || millis - firstMillis < WARMUP_MS) {
            candidateMillis = -1;
            return null;
        }
        if (candidateMillis >= 0) {
            candidatePeak = Math.max(candidatePeak, magnitude);
            if (millis - candidateMillis >= WINDOW_MS) {
                Detection result = new Detection(candidateMillis, candidatePeak, candidateThreshold);
                lastEventMillis = millis;
                candidateMillis = -1;
                return result;
            }
        } else if (millis - lastEventMillis >= COOLDOWN_MS && magnitude >= threshold) {
            candidateMillis = millis;
            candidatePeak = magnitude;
            candidateThreshold = threshold;
        }
        return null;
    }
}
