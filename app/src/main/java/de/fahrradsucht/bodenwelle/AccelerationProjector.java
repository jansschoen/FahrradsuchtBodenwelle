package de.fahrradsucht.bodenwelle;

/** Calibrates at rest; driving acceleration never updates the gravity direction. */
public final class AccelerationProjector {
    private static final long CALIBRATION_NANOS = 2_000_000_000L;
    private final double[] mean = new double[3], anchor = new double[3], up = new double[3];
    private final double[] orientation = new double[3];
    private long firstNanos = -1, previousNanos = -1, orientationNanos = -1;
    private int samples;
    private double zero;
    private boolean calibrated;

    public void reset() {
        firstNanos = previousNanos = orientationNanos = -1;
        samples = 0;
        calibrated = false;
    }
    public boolean calibrated() { return calibrated; }
    public boolean orientationAvailable() { return orientationNanos >= 0; }
    public int progress() {
        return calibrated ? 100 : firstNanos < 0 ? 0 : (int) Math.min(99, (previousNanos - firstNanos) * 100 / CALIBRATION_NANOS);
    }
    /** World-up expressed in phone coordinates, supplied by Android rotation-vector fusion. */
    public void orientation(long nanos, double x, double y, double z) {
        double norm = Math.sqrt(x*x + y*y + z*z);
        if (nanos < 0 || nanos < orientationNanos || !Double.isFinite(norm) || norm < .5) return;
        orientation[0] = x / norm; orientation[1] = y / norm; orientation[2] = z / norm;
        orientationNanos = nanos;
    }
    public double accept(long nanos, double x, double y, double z) { return accept(nanos, x, y, z, true); }
    public double accept(long nanos, double x, double y, double z, boolean stationary) {
        if (nanos < 0 || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return Double.NaN;
        if (previousNanos >= 0 && nanos <= previousNanos) return Double.NaN;
        if (previousNanos >= 0 && nanos - previousNanos > 1_000_000_000L) {
            calibrated = false; firstNanos = -1; samples = 0;
        }
        previousNanos = nanos;
        double[] raw = {x, y, z};
        if (!calibrated) {
            double norm = Math.sqrt(x*x + y*y + z*z);
            double deviation = samples == 0 ? 0 : Math.sqrt(square(x-anchor[0]) + square(y-anchor[1]) + square(z-anchor[2]));
            if (!stationary || norm < 8.8 || norm > 10.8 || deviation > .45) {
                firstNanos = -1; samples = 0; return 0;
            }
            if (samples == 0) {
                firstNanos = nanos;
                System.arraycopy(raw, 0, anchor, 0, 3);
                System.arraycopy(raw, 0, mean, 0, 3);
            }
            samples++;
            for (int i = 0; i < 3; i++) mean[i] += (raw[i] - mean[i]) / samples;
            if (nanos - firstNanos < CALIBRATION_NANOS || samples < 100) return 0;
            zero = Math.sqrt(square(mean[0]) + square(mean[1]) + square(mean[2]));
            for (int i = 0; i < 3; i++) up[i] = mean[i] / zero;
            calibrated = true;
        }
        // If fusion was present, stale orientation must not silently become an old fixed axis.
        if (orientationNanos >= 0 && (nanos < orientationNanos || nanos - orientationNanos > 500_000_000L)) return Double.NaN;
        double[] axis = orientationNanos >= 0 ? orientation : up;
        return x*axis[0] + y*axis[1] + z*axis[2] - zero;
    }
    private static double square(double value) { return value * value; }
}
