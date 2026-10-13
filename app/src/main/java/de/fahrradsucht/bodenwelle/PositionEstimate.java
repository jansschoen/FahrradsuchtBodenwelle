package de.fahrradsucht.bodenwelle;

public record PositionEstimate(double latitude, double longitude, double gpsLatitude, double gpsLongitude,
                               long timeOffsetMillis, Float bearing, double distanceMeters, String method) {
    public boolean projected() { return "course_speed_time".equals(method); }
}
