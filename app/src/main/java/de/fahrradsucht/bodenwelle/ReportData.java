package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Explicit export model: no raw timestamps, route, GPS-fix age or unselected observations. */
public record ReportData(String reference, String district, String name, boolean appNotice, List<Entry> entries) {
    public record Section(double latitude, double longitude, double endLatitude, double endLongitude, double meters) {}
    public record Entry(int number, String title, String district, String roadName, double latitude, double longitude,
                        float accuracy, boolean projected, String weeks, String references, int observations,
                        String traffic, String surface, String hazards, String sensor, String height,
                        List<String> notes, List<Section> sections) {
        public Entry { notes=java.util.Collections.unmodifiableList(new ArrayList<>(notes)); sections=java.util.Collections.unmodifiableList(new ArrayList<>(sections)); }
        public String coordinates() { return String.format(Locale.ROOT,"%.6f, %.6f",latitude,longitude); }
        public String mapLink() { return String.format(Locale.ROOT,"https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=19/%.6f/%.6f",latitude,longitude,latitude,longitude); }
        public String action() {
            if (!sections.isEmpty()) return "Ausdehnung und Belagszustand vor Ort prüfen.";
            return "Unebenheit, Kanten und Befahrbarkeit vor Ort prüfen.";
        }
    }
    public ReportData { entries=java.util.Collections.unmodifiableList(new ArrayList<>(entries)); }
    public String weeks() { return entries.stream().map(Entry::weeks).distinct().collect(Collectors.joining(" · ")); }
    public static ReportData create(MailReports.Group group, Function<String,String> districts, String name,
                                    boolean includeNotes, boolean appNotice, String reference) {
        if(group.items().isEmpty()||group.items().stream().anyMatch(i->!i.finding().reviewed()))throw new IllegalArgumentException("Bitte nur freigegebene Messungen auswählen.");
        if(group.points().size()>MailReports.MAX_POINTS)throw new IllegalArgumentException("Bitte höchstens 20 Fundstellen je Bericht auswählen.");
        List<Entry> entries=new ArrayList<>();int number=0;int noteCharacters=0;
        for(MailReports.Point point:group.points()) {
            Finding f=point.representative();FindingDetails d=f.details();int flags=0;
            List<Double> peaks=new ArrayList<>(),heights=new ArrayList<>();Set<String> notes=new LinkedHashSet<>();List<Section> sections=new ArrayList<>();
            for(MailReports.Item item:point.observations()) {
                Finding x=item.finding(); flags|=x.details().hazards();
                if(x.peak()!=null)peaks.add(x.peak());if(x.details().heightMm()!=null)heights.add(x.details().heightMm());
                if(includeNotes&&!x.note().trim().isEmpty())notes.add(x.note());
                RoughSection section=x.details().section();
                if(section!=null)sections.add(new Section(x.latitude(),x.longitude(),section.endLatitude(),section.endLongitude(),section.lengthMeters()));
            }
            for(String note:notes)noteCharacters+=note.length();
            if(noteCharacters>100_000)throw new IllegalArgumentException("Die Notizen sind zu umfangreich. Bitte ohne Notizen berichten oder die Auswahl verkleinern.");
            String hazards="";for(int i=0;i<Classification.HAZARDS.length;i++)if((flags&(1<<i))!=0)hazards+=(hazards.isEmpty()?"":"; ")+Classification.HAZARDS[i].label();
            String road=d.osm().assigned()?d.osm().tags().getOrDefault("name",""):"";
            if(road.trim().isEmpty()&&d.osm().assigned())road=d.osm().tags().getOrDefault("ref","");
            String district=districts.apply(point.observations().get(0).districtId());
            entries.add(new Entry(++number,point.title(),district,road,f.latitude(),f.longitude(),f.accuracy(),f.position().projected(),point.weeks(),
                    FindingBundle.references(point.observations().stream().map(MailReports.Item::finding).collect(Collectors.toList())),point.observations().size(),
                    Classification.label(Classification.ROADS,d.traffic())+(d.trafficManual()?" · manuell":"unknown".equals(d.traffic())?"":" · OSM-Vorschlag, zu prüfen"),
                    Classification.label(Classification.SURFACES,d.surface())+(d.surfaceManual()?" · manuell":"unknown".equals(d.surface())?"":" · OSM-Vorschlag, zu prüfen"),
                    hazards,range(peaks,"m/s²"),range(heights,"mm"),new ArrayList<>(notes),sections));
        }
        String district=group.districtId().isEmpty()?"Bezirksübergreifende oder offene Zuordnung":districts.apply(group.districtId());
        return new ReportData(reference,district,name.trim(),appNotice,entries);
    }
    private static String range(List<Double> values,String unit) {
        if(values.isEmpty())return "Nicht erhoben";
        double min=java.util.Collections.min(values),max=java.util.Collections.max(values);
        return (min==max?String.format(Locale.GERMANY,"%.1f %s",min,unit):String.format(Locale.GERMANY,"%.1f–%.1f %s",min,max,unit))
                +" ("+values.size()+(values.size()==1?" Messung)":" Messungen)");
    }
}
