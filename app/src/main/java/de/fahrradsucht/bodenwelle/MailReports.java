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
    public record Point(List<Item> observations) {
        public Point { observations=java.util.Collections.unmodifiableList(new ArrayList<>(observations)); }
        public Finding representative() { return observations.stream().map(Item::finding).min(java.util.Comparator.comparingDouble((Finding f)->f.accuracy()).thenComparingLong(f->-f.id())).orElseThrow(); }
        public String weeks() {
            return observations.stream().flatMap(i -> i.finding().details().section() == null
                    ? java.util.stream.Stream.of(calendarWeek(i.finding().timeMillis()))
                    : java.util.stream.Stream.of(calendarWeek(i.finding().timeMillis()),
                        calendarWeek(i.finding().timeMillis() + i.finding().details().section().durationMillis())))
                    .distinct().sorted().collect(java.util.stream.Collectors.joining(", "));
        }
        public String title() { return observations.stream().map(i->Classification.label(Classification.BUMPS,Classification.bumpId(i.finding().category()))).distinct().collect(java.util.stream.Collectors.joining(" / ")); }
    }
    public static List<Point> points(List<Item> items) {
        Map<String,List<Item>> points=new LinkedHashMap<>();
        for(Item item:items) if(item.finding().reviewed()) points.computeIfAbsent(item.districtId()+":"+item.finding().bundleKey(),k->new ArrayList<>()).add(item);
        List<Point> result=new ArrayList<>();for(List<Item> observations:points.values())result.add(new Point(observations));return result;
    }
    public record Group(String key, String districtId, List<Item> items, int part, int parts) {
        public Group { items = java.util.Collections.unmodifiableList(new ArrayList<>(items)); }
        public List<Point> points() { return MailReports.points(items); }
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
            List<Point> locations=points(e.getValue());
            int parts = (locations.size() + MAX_POINTS - 1) / MAX_POINTS;
            for (int i = 0; i < parts; i++) {
                List<Item> portion=new ArrayList<>();for(Point point:locations.subList(i*MAX_POINTS,Math.min((i+1)*MAX_POINTS,locations.size())))portion.addAll(point.observations());
                result.add(new Group(e.getKey() + ":" + i, splitDistricts ? e.getKey() : "", portion, i + 1, parts));
            }
        }
        return result;
    }
    public static Draft draft(Group group, String districtName, boolean appNotice, boolean includeNotes, String name) {
        if (group.items().isEmpty()) throw new IllegalArgumentException("No reviewed findings selected");
        for (Item item : group.items()) if (!item.finding().reviewed()) throw new IllegalArgumentException("Unreviewed finding");
        String area = districtName.isEmpty() ? "" : " im Bezirk " + districtName;
        String subject = "Bitte um Prüfung: " + group.points().size() + (group.points().size() == 1 ? " Unebenheit" : " Unebenheiten") + area;
        if (group.parts() > 1) subject += " · Teil " + group.part() + "/" + group.parts();
        StringBuilder body = new StringBuilder("Guten Tag,\n\nich möchte folgende Unebenheiten").append(area)
                .append(" melden und Sie um eine Prüfung vor Ort bitten.\n\n");
        boolean hasSensor = false, hasHeight = false, hasOsm = false;
        int index = 0;
        for (Point point : group.points()) {
            Finding f=point.representative(); FindingDetails d=f.details();
            body.append(++index).append(". ").append(point.title()).append(" · ").append(point.weeks()).append('\n');
            body.append("Messungsnummern: ").append(FindingBundle.references(point.observations().stream()
                    .map(Item::finding).collect(java.util.stream.Collectors.toList()))).append('\n');
            if(point.observations().size()>1)body.append("Wiederholt beobachtet: ").append(point.observations().size()).append(" ausgewählte Beobachtungen derselben bestätigten Fundstelle\n");
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
            List<Double> heights=new ArrayList<>(),peaks=new ArrayList<>();java.util.Set<String> notes=new java.util.LinkedHashSet<>();int flags=0;
            for(Item observation:point.observations()) {
                Finding x=observation.finding();if(x.details().heightMm()!=null)heights.add(x.details().heightMm());if(x.peak()!=null)peaks.add(x.peak());
                flags|=x.details().hazards();if(includeNotes&&!x.note().isEmpty())notes.add(x.note());hasOsm|=x.details().osm().hasSuggestions()
                        || x.details().hasAutomaticDescription();
                RoughSection section = x.details().section();
                if (section != null) {
                    body.append(String.format(Locale.GERMANY, "Unebener Abschnitt: etwa %.0f m (aus Fahrgeschwindigkeit und Dauer geschätzt).\n", section.lengthMeters()));
                    body.append(String.format(Locale.ROOT, "Anfang: %.6f, %.6f · Ende: %.6f, %.6f\n", x.latitude(), x.longitude(), section.endLatitude(), section.endLongitude()));
                    body.append("Endpunktkarte: https://www.openstreetmap.org/?mlat=").append(String.format(Locale.ROOT, "%.6f", section.endLatitude()))
                            .append("&mlon=").append(String.format(Locale.ROOT, "%.6f", section.endLongitude())).append('\n');
                    body.append("Die Abschnittserkennung ist ein vorläufiger Hinweis; Ausdehnung und Zustand bitte vor Ort prüfen.\n");
                }
            }
            if(!heights.isEmpty()) { hasHeight=true;body.append("Vor Ort gemessener Höhenunterschied: ").append(range(heights,2)).append(" mm (").append(heights.size()).append(" Messung(en))\n"); }
            if(!peaks.isEmpty()) { hasSensor=true;body.append("Smartphone-Sensorreaktion: ").append(range(peaks,1)).append(" m/s² (").append(peaks.size()).append(" Beobachtung(en))\n"); }
            for(int i=0;i<Classification.HAZARDS.length;i++)if((flags&(1<<i))!=0)body.append("Zusätzliche Beobachtung: ").append(Classification.HAZARDS[i].label()).append('\n');
            for(String note:notes)body.append("Beschreibung: ").append(note).append('\n');
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
    private static String range(List<Double> values,int precision) {
        double min=java.util.Collections.min(values),max=java.util.Collections.max(values);
        return String.format(Locale.GERMANY,min==max?"%."+precision+"f":"%."+precision+"f–%."+precision+"f",min,max);
    }
    private static String oneLine(String text) { return text.replace('\n', ' ').replace('\r', ' '); }
}
