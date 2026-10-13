package de.fahrradsucht.bodenwelle;

public record RoutePoint(long timeMillis, long elapsedMillis, double latitude, double longitude,
                         float accuracy, int segment) {}
