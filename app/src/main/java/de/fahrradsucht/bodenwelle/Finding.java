package de.fahrradsucht.bodenwelle;

public record Finding(long id, long timeMillis, double latitude, double longitude, float accuracy,
                      Float speed, Double peak, Double threshold, String source,
                      String category, String note, boolean reviewed, long locationAgeMillis,
                      PositionEstimate position, String detectorVersion) {
    public Finding(long id, long timeMillis, double latitude, double longitude, float accuracy,
                   Float speed, Double peak, Double threshold, String source,
                   String category, String note, boolean reviewed, long locationAgeMillis) {
        this(id, timeMillis, latitude, longitude, accuracy, speed, peak, threshold, source, category, note,
                reviewed, locationAgeMillis, new PositionEstimate(latitude, longitude, latitude, longitude,
                        locationAgeMillis, null, 0, "legacy_gps"), "experimental-1");
    }
}
