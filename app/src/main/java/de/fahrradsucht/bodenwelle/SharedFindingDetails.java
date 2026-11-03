package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** One place description; raw readings, heights, original GPS evidence and consent stay individual. */
public record SharedFindingDetails(Field traffic, Field surface, int hazards, Field category, String note) {
    public record Field(String value, boolean manual, List<String> conflicts) {
        public Field { conflicts = java.util.Collections.unmodifiableList(new ArrayList<>(conflicts)); }
        public boolean conflicting() { return !conflicts.isEmpty(); }
        public Field choose(String selected) {
            if (!conflicting()) return this;
            if (selected == null || !conflicts.contains(selected) && !"unknown".equals(selected) && !"other".equals(selected))
                throw new IllegalArgumentException("Bitte die abweichenden Angaben für die gemeinsame Fundstelle auswählen.");
            return new Field(selected, true, List.of());
        }
    }
    public boolean conflicting() { return traffic.conflicting() || surface.conflicting() || category.conflicting(); }
    public SharedFindingDetails choose(String road, String ground) { return choose(road,ground,null); }
    public SharedFindingDetails choose(String road,String ground,String bump) {
        return new SharedFindingDetails(traffic.choose(road),surface.choose(ground),hazards,category.choose(bump),note);
    }
    public FindingDetails apply(FindingDetails original) {
        return original.withShared(hazards, traffic.conflicting() ? original.traffic() : traffic.value(),
                traffic.conflicting() ? original.trafficManual() : traffic.manual(),
                surface.conflicting() ? original.surface() : surface.value(),
                surface.conflicting() ? original.surfaceManual() : surface.manual());
    }
    public static SharedFindingDetails from(List<Finding> members) {
        int hazards = 0;Set<String> categories=new LinkedHashSet<>(),notes=new LinkedHashSet<>();
        for (Finding finding : members) {
            hazards |= finding.details().hazards();
            String category=Classification.bumpId(finding.category());if(!"other".equals(category))categories.add(category);
            if(!finding.note().trim().isEmpty())notes.add(finding.note());
        }
        Field category=new Field(categories.size()==1?categories.iterator().next():"other",true,categories.size()>1?new ArrayList<>(categories):List.of());
        return new SharedFindingDetails(field(members,true),field(members,false),hazards,category,String.join("\n\n",notes));
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
    public static SharedFindingDetails edited(List<Finding> members,FindingDetails edit,String category,String note) {
        return new SharedFindingDetails(edit.trafficManual()?new Field(edit.traffic(),true,List.of()):automatic(members,true),
                edit.surfaceManual()?new Field(edit.surface(),true,List.of()):automatic(members,false),edit.hazards(),
                new Field(Classification.bumpId(category),true,List.of()),note);
    }
}
