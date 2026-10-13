package de.fahrradsucht.bodenwelle;

public final class RideEligibility {
    private RideEligibility() {}

    public static boolean locationUsable(long ageMillis, boolean hasAccuracy, float accuracy) {
        return ageMillis >= 0 && ageMillis <= 5_000 && hasAccuracy
                && Float.isFinite(accuracy) && accuracy >= 0 && accuracy <= 25;
    }

    public static boolean autoDetectionAllowed(long ageMillis, boolean hasAccuracy, float accuracy,
                                                boolean hasSpeed, float speed, boolean hasSpeedAccuracy,
                                                float speedAccuracy) {
        return locationUsable(ageMillis, hasAccuracy, accuracy) && hasSpeed && Float.isFinite(speed)
                && speed >= 1.4f && speed <= 20f
                && (!hasSpeedAccuracy || (Float.isFinite(speedAccuracy) && speedAccuracy >= 0 && speedAccuracy <= 1.5f));
    }
}
