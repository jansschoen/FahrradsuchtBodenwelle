package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Common place descriptions; individual measurements, raw OSM evidence and consent stay separate. */
public record SharedFindingDetails(Field traffic, Field surface, int hazards) {
    public record Field(String value, boolean manual, List<String> conflicts) {
        public Field { conflicts = java.util.Collections.unmodifiableList(new ArrayList<>(conflicts)); }
        public boolean conflicting() { return !conflicts.isEmpty(); }
        public Field choose(String selected) {
            if (!conflicting()) return this;
            if (selected == null || !conflicts.contains(selected) && !"unknown".equals(selected))
                throw new IllegalArgumentException("Bitte widersprüchliche Angaben auswählen oder ausdrücklich auf Unbekannt setzen.");
            return new Field(selected, true, List.of());
        }
    }
    public boolean conflicting() { return traffic.conflicting() || surface.conflicting(); }
    public SharedFindingDetails choose(String road, String ground) { return new SharedFindingDetails(traffic.choose(road), surface.choose(ground), hazards); }
    public FindingDetails apply(FindingDetails original) {
        // Old conflicting descriptions survive migration until the person resolves them.
        return original.withShared(hazards, traffic.conflicting() ? original.traffic() : traffic.value(),
                traffic.conflicting() ? original.trafficManual() : traffic.manual(),
                surface.conflicting() ? original.surface() : surface.value(),
                surface.conflicting() ? original.surfaceManual() : surface.manual());
    }
    public static SharedFindingDetails from(List<Finding> members) {
        int hazards = 0; for (Finding finding : members) hazards |= finding.details().hazards();
        return new SharedFindingDetails(field(members, true), field(members, false), hazards);
    }
    private static Field field(List<Finding> members, boolean road) {
        Set<String> manual = new LinkedHashSet<>();
        for (Finding f : members) {
            FindingDetails d = f.details();
            if (road ? d.trafficManual() : d.surfaceManual()) manual.add(road ? d.traffic() : d.surface());
        }
        if (!manual.isEmpty()) return new Field(manual.size() == 1 ? manual.iterator().next() : "unknown", true,
                manual.size() > 1 ? new ArrayList<>(manual) : List.of());
        return automatic(members, road);
    }
    public static Field automatic(List<Finding> members, boolean road) {
        Set<String> known = new LinkedHashSet<>();
        for (Finding f : members) {
            String value = road ? f.details().osm().traffic() : f.details().osm().surface();
            if (!"unknown".equals(value)) known.add(value);
        }
        return new Field(known.size() == 1 ? known.iterator().next() : "unknown", false, List.of());
    }
    public static SharedFindingDetails edited(List<Finding> members, FindingDetails edit) {
        return new SharedFindingDetails(edit.trafficManual() ? new Field(edit.traffic(), true, List.of()) : automatic(members, true),
                edit.surfaceManual() ? new Field(edit.surface(), true, List.of()) : automatic(members, false), edit.hazards());
    }
}
