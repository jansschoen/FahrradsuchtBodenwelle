package de.fahrradsucht.bodenwelle;

public record Finding(long id, long timeMillis, double latitude, double longitude, float accuracy,
                      Float speed, Double peak, Double threshold, String source,
                      String category, String note, boolean reviewed, long locationAgeMillis) {}
