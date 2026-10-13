package de.fahrradsucht.bodenwelle;

import java.time.Instant;

public final class GeodataDescription {
    private GeodataDescription() {}
    public static String summary(FindingDetails d) {
        return "Verkehrsfläche: " + Classification.label(Classification.ROADS, d.traffic()) + (d.trafficManual() ? " · manuell" : " · OSM-Vorschlag")
                + "\nOberfläche: " + Classification.label(Classification.SURFACES, d.surface()) + (d.surfaceManual() ? " · manuell" : " · OSM-Vorschlag")
                + "\n" + status(d.osm());
    }
    public static String status(OsmMatch match) {
        return switch (match.status()) {
            case "matched" -> "OSM-Zuordnung: " + ("medium".equals(match.confidence()) ? "mittlere Sicherheit" : "unsicher") + " · Angaben vor Ort prüfen";
            case "ambiguous" -> "OSM-Zuordnung mehrdeutig: mehrere Wege passen zur Position";
            case "offline" -> "OSM fehlt: offline, später ergänzbar";
            case "error" -> "OSM momentan nicht erreichbar oder Daten unvollständig";
            case "rate_limited" -> "OSM wartet auf die nächste erlaubte Abfrage";
            case "no_match" -> "Kein belastbarer OSM-Weg zur Position gefunden";
            default -> "OSM-Angaben noch nicht ergänzt";
        };
    }
    public static String details(FindingDetails d) {
        OsmMatch m = d.osm();
        return summary(d) + (m.wayId() == null ? "" : "\nOSM-Weg: " + m.wayId())
                + (m.queriedAt() == 0 ? "" : "\nAbgefragt: " + Instant.ofEpochMilli(m.queriedAt()))
                + (m.source().isEmpty() ? "" : "\nQuelle: " + m.source())
                + (m.tags().isEmpty() ? "" : "\nOSM-Tags: " + new java.util.TreeMap<>(m.tags()))
                + "\nOSM smoothness beschreibt die allgemeine Befahrbarkeit, nicht diesen einzelnen Stoß. © OpenStreetMap contributors · ODbL 1.0";
    }
}
