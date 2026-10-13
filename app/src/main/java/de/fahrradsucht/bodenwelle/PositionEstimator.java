package de.fahrradsucht.bodenwelle;

/** Constant-course/speed geodesic projection to the sensor-event time, not a surveyed road position. */
public final class PositionEstimator {
    private static final double EARTH_RADIUS = 6_371_000;
    private PositionEstimator() {}
    public static PositionEstimate estimate(double latitude, double longitude, long timeOffsetMillis,
                                             Float speed, Float bearing, Float bearingAccuracy) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || Math.abs(latitude) > 90 || Math.abs(longitude) > 180)
            throw new IllegalArgumentException("Invalid GPS coordinates");
        String reason = null;
        if (Math.abs(timeOffsetMillis) > 5_000) reason = "time_gap";
        else if (timeOffsetMillis == 0) reason = "gps_at_event";
        else if (speed == null || !Float.isFinite(speed) || speed < 0 || speed > 20) reason = "speed_unavailable";
        else if (speed == 0) reason = "stationary";
        else if (bearing == null || !Float.isFinite(bearing)) reason = "bearing_unavailable";
        else if (bearingAccuracy != null && (!Float.isFinite(bearingAccuracy) || bearingAccuracy < 0 || bearingAccuracy > 45))
            reason = "bearing_uncertain";
        if (reason != null) return new PositionEstimate(latitude, longitude, latitude, longitude, timeOffsetMillis, bearing, 0, reason);
        double distance = speed * timeOffsetMillis / 1000.0;
        double arc = distance / EARTH_RADIUS;
        double lat1 = Math.toRadians(latitude), lon1 = Math.toRadians(longitude), course = Math.toRadians(bearing);
        double lat2 = Math.asin(Math.max(-1, Math.min(1, Math.sin(lat1) * Math.cos(arc)
                + Math.cos(lat1) * Math.sin(arc) * Math.cos(course))));
        double lon2 = lon1 + Math.atan2(Math.sin(course) * Math.sin(arc) * Math.cos(lat1),
                Math.cos(arc) - Math.sin(lat1) * Math.sin(lat2));
        return new PositionEstimate(Math.toDegrees(lat2), (Math.toDegrees(lon2) + 540) % 360 - 180,
                latitude, longitude, timeOffsetMillis, bearing, distance, "course_speed_time");
    }
}
