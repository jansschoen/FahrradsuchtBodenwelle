package de.fahrradsucht.bodenwelle;

/** Keeps plausible GPS fixes, splitting gaps instead of inventing travelled links. */
public final class RouteTracker {
    private RoutePoint previous;

    public RoutePoint accept(long timeMillis, long elapsedMillis, long nowMillis,
                             double latitude, double longitude, float accuracy) {
        long age = nowMillis - elapsedMillis;
        if (age < 0 || age > 5_000 || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180
                || !Float.isFinite(accuracy) || accuracy < 0 || accuracy > 50) return null;
        int segment = previous == null ? 0 : previous.segment();
        if (previous != null) {
            long dt = elapsedMillis - previous.elapsedMillis();
            if (dt <= 0) return null;
            double distance = distanceMeters(previous.latitude(), previous.longitude(), latitude, longitude);
            if (dt > 15_000) segment++;
            else {
                if (distance > 25 * dt / 1000.0 + Math.max(15, previous.accuracy() + accuracy)) return null;
                if (distance < 3 && dt < 5_000) return null;
            }
        }
        previous = new RoutePoint(timeMillis, elapsedMillis, latitude, longitude, accuracy, segment);
        return previous;
    }

    public static double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double dLat = Math.toRadians(latitude2 - latitude1);
        double dLon = Math.toRadians(longitude2 - longitude1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }
}
