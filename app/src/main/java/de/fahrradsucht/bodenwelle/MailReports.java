package de.fahrradsucht.bodenwelle;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Curated report data: never includes a track or full timestamps from the raw export. */
public final class MailReports {
    public static final int MAX_POINTS = 20;
    private MailReports() {}
    public record Item(Finding finding, String districtId, String assignmentMethod) {}
    public record Group(String key, String districtId, List<Item> items, int part, int parts) {
        public Group { items = java.util.Collections.unmodifiableList(new ArrayList<>(items)); }
    }
    public record Draft(String subject, String body) {}
    public static String calendarWeek(long millis) {
        java.time.LocalDate date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("Europe/Berlin")).toLocalDate();
        return String.format(Locale.GERMANY, "KW %02d/%d", date.get(WeekFields.ISO.weekOfWeekBasedYear()), date.get(WeekFields.ISO.weekBasedYear()));
    }
    public static List<Group> groups(List<Item> items, boolean splitDistricts) {
        Map<String, List<Item>> buckets = new LinkedHashMap<>();
        List<Item> sorted = new ArrayList<>(items);
        sorted.sort(java.util.Comparator.comparing(Item::districtId).thenComparingDouble(i -> i.finding().latitude())
                .thenComparingDouble(i -> i.finding().longitude()).thenComparing(i -> i.finding().category()));
        for (Item item : sorted) {
            if (!item.finding().reviewed()) continue;
            String key = splitDistricts ? item.districtId() : "all";
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        List<Group> result = new ArrayList<>();
        for (Map.Entry<String, List<Item>> e : buckets.entrySet()) {
            int parts = (e.getValue().size() + MAX_POINTS - 1) / MAX_POINTS;
            for (int i = 0; i < parts; i++) {
                List<Item> portion = e.getValue().subList(i * MAX_POINTS, Math.min((i + 1) * MAX_POINTS, e.getValue().size()));
                result.add(new Group(e.getKey() + ":" + i, splitDistricts ? e.getKey() : "", portion, i + 1, parts));
            }
        }
        return result;
    }
    public static Draft draft(Group group, String districtName, boolean appNotice, boolean includeNotes, String name) {
        if (group.items().isEmpty()) throw new IllegalArgumentException("No reviewed findings selected");
        for (Item item : group.items()) if (!item.finding().reviewed()) throw new IllegalArgumentException("Unreviewed finding");
        String area = districtName.isEmpty() ? "" : " im Bezirk " + districtName;
        String subject = "Bitte um Prüfung: " + group.items().size() + (group.items().size() == 1 ? " Unebenheit" : " Unebenheiten") + area;
        if (group.parts() > 1) subject += " · Teil " + group.part() + "/" + group.parts();
        StringBuilder body = new StringBuilder("Guten Tag,\n\nich möchte folgende Unebenheiten").append(area)
                .append(" melden und Sie um eine Prüfung vor Ort bitten.\n\n");
        boolean hasSensor = false, hasHeight = false, hasOsm = false;
        int index = 0;
        for (Item item : group.items()) {
            Finding f = item.finding(); FindingDetails d = f.details();
            body.append(++index).append(". ").append(Classification.label(Classification.BUMPS, Classification.bumpId(f.category())))
                    .append(" · ").append(calendarWeek(f.timeMillis())).append('\n');
            String roadName = d.osm().assigned() ? d.osm().tags().getOrDefault("name", "") : "";
            if (!roadName.isEmpty()) body.append("Straße/Weg laut OSM: ").append(oneLine(roadName)).append('\n');
            String lat = String.format(Locale.ROOT, "%.6f", f.latitude()), lon = String.format(Locale.ROOT, "%.6f", f.longitude());
            body.append("Lage: ").append(lat).append(", ").append(lon)
                    .append(String.format(Locale.GERMANY, " · ursprüngliche GPS-Genauigkeit etwa ± %.0f m", f.accuracy()));
            if (f.position().projected()) body.append(" · Position zum Sensorstoß geschätzt");
            body.append("\nKarte: https://www.openstreetmap.org/?mlat=").append(lat).append("&mlon=").append(lon)
                    .append("#map=19/").append(lat).append('/').append(lon).append('\n');
            if (!"unknown".equals(d.traffic())) body.append("Verkehrsfläche: ").append(Classification.label(Classification.ROADS, d.traffic()))
                    .append(d.trafficManual() ? " (manuell geprüft)\n" : " (OSM-Vorschlag, vor Ort zu prüfen)\n");
            if (!"unknown".equals(d.surface())) body.append("Oberfläche: ").append(Classification.label(Classification.SURFACES, d.surface()))
                    .append(d.surfaceManual() ? " (manuell angegeben)\n" : " (OSM-Angabe)\n");
            if (d.heightMm() != null) {
                hasHeight = true; body.append(String.format(Locale.GERMANY, "Vor Ort gemessener Höhenunterschied: %.2f mm\n", d.heightMm()));
            }
            if (f.peak() != null) {
                hasSensor = true; body.append(String.format(Locale.GERMANY, "Smartphone-Sensorreaktion: %.1f m/s²", f.peak()));
                if (f.threshold() != null) body.append(String.format(Locale.GERMANY, " (Auslöseschwelle %.1f m/s²)", f.threshold()));
                body.append('\n');
            }
            for (int i = 0; i < Classification.HAZARDS.length; i++) if ((d.hazards() & (1 << i)) != 0)
                body.append("Zusätzliche Beobachtung: ").append(Classification.HAZARDS[i].label()).append('\n');
            if (includeNotes && !f.note().isEmpty()) body.append("Beschreibung: ").append(f.note()).append('\n');
            hasOsm |= d.osm().assigned();
            body.append('\n');
        }
        if (hasSensor) body.append("Die Beschleunigungswerte beschreiben die Reaktion des Handys am Lenker. Sie sind Hinweise auf Erschütterungen und keine gemessenen Schadenshöhen.\n\n");
        if (hasHeight) body.append("Die Höhenangaben stammen aus eigenen Messungen vor Ort; daraus leite ich keine rechtlichen Grenzwerte ab.\n\n");
        body.append("Könnten Sie den Zustand prüfen und mir eine kurze Rückmeldung zum weiteren Vorgehen geben? Falls eine andere Stelle zuständig ist, wäre ich für eine Weiterleitung oder einen Hinweis dankbar.\n\nVielen Dank für Ihre Unterstützung.\n\nFreundliche Grüße");
        if (!name.trim().isEmpty()) body.append('\n').append(name.trim());
        if (appNotice) body.append("\n\nDiese Meldung wurde mit Bodenwelle zusammengestellt. Die aufgeführten Fundstellen habe ich geprüft.");
        if (hasOsm) body.append("\n\nKartendaten: © OpenStreetMap contributors, ODbL 1.0 · https://www.openstreetmap.org/copyright");
        return new Draft(subject, body.toString());
    }
    private static String oneLine(String text) { return text.replace('\n', ' ').replace('\r', ' '); }
}
