package de.fahrradsucht.bodenwelle;

import java.time.Instant;

public final class GeodataDescription {
    private GeodataDescription() {}
    public static String summary(FindingDetails d) {
        return "Verkehrsfläche: " + Classification.label(Classification.ROADS, d.traffic()) + (d.trafficManual() ? " · manuell" : " · OSM-Vorschlag")
                + "\nOberfläche: " + Classification.label(Classification.SURFACES, d.surface()) + (d.surfaceManual() ? " · manuell" : " · OSM-Vorschlag")
                + "\n" + status(d.osm())
                + ((d.trafficManual()||d.surfaceManual())&&d.osm().hasSuggestions()?"\nAutomatischer Vorschlag: "+Classification.label(Classification.ROADS,d.osm().traffic())+" / "+Classification.label(Classification.SURFACES,d.osm().surface()):"")
                + ("gps_circle_center".equals(d.osm().tags().get("bodenwelle:position_basis"))?"\nAbfragepunkt: ursprünglicher GPS-Mittelpunkt":"");
    }
    public static String status(OsmMatch match) {
        return switch (match.status()) {
            case "matched" -> "OSM-Zuordnung: " + ("medium".equals(match.confidence()) ? "mittlere Sicherheit" : "unsicher") + " · Wegabstand etwa " + (match.distanceMeters() == null ? "?" : Math.round(match.distanceMeters())) + " m · Angaben vor Ort prüfen"
                    + ("unknown".equals(match.surface()) ? " · Oberfläche nicht eindeutig in OSM hinterlegt" : "");
            case "nearest_suggestion" -> "Unsicherer Vorschlag am GPS-Mittelpunkt: mehrere Wege kommen infrage. Verkehrsfläche und Belag bitte vor Ort prüfen.";
            case "gps_uncertain" -> "Ursprüngliche GPS-Messung zu ungenau oder ungültig; neue Datenbankabfragen verbessern diese Position nicht.";
            case "area_suggestion" -> "OSM-Vorschlag aus übereinstimmenden Angaben mehrerer naher Wege · unsicher, vor Ort prüfen";
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
