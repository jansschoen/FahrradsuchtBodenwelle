package de.fahrradsucht.bodenwelle;

/** Estimates gravity from the accelerometer and projects the residual onto it.
 * Phone orientation is arbitrary; this measures a response, not road geometry. */
public final class AccelerationProjector {
    private final double[] gravity = new double[3];
    private long previousNanos;

    public double accept(long nanos, double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return 0;
        if (previousNanos == 0 || nanos <= previousNanos || nanos - previousNanos > 1_000_000_000L) {
            gravity[0] = x;
            gravity[1] = y;
            gravity[2] = z;
            previousNanos = nanos;
            return 0;
        }
        double dt = (nanos - previousNanos) / 1e9;
        previousNanos = nanos;
        double weight = Math.exp(-dt / 0.6);
        double[] raw = {x, y, z};
        for (int i = 0; i < 3; i++) gravity[i] = weight * gravity[i] + (1 - weight) * raw[i];
        double magnitude = Math.sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2]);
        if (magnitude < 1) return 0;
        double vertical = 0;
        for (int i = 0; i < 3; i++) vertical += (raw[i] - gravity[i]) * gravity[i] / magnitude;
        return vertical;
    }
}
