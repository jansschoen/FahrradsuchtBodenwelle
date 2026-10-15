package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A confirmed place retains all original observations and their individual consent. */
public record FindingBundle(long key, List<Finding> observations) {
    public FindingBundle { observations = java.util.Collections.unmodifiableList(new ArrayList<>(observations)); }
    public Finding representative() {
        return observations.stream().min(Comparator.comparingDouble((Finding f) -> f.accuracy())
                .thenComparingLong(f -> -f.id())).orElseThrow();
    }
    public long reviewedCount() { return observations.stream().filter(Finding::reviewed).count(); }
    public String category() {
        String first = observations.get(0).category();
        return observations.stream().allMatch(f -> f.category().equals(first)) ? first : "other";
    }
    public String title() {
        return observations.stream().map(f->Classification.label(Classification.BUMPS,f.category())).distinct().collect(java.util.stream.Collectors.joining(" / "));
    }
    public boolean confirmed() { return key > 0; }
    /** Stable local IDs survive editing, bundling, splitting and subsequent rides. */
    public String references() { return references(observations); }
    public String referenceLabel() { return (observations.size() == 1 ? "Messung " : "Messungen ") + references(); }
    public static String references(List<Finding> findings) {
        return findings.stream().map(Finding::id).sorted().map(id -> "#" + id)
                .collect(java.util.stream.Collectors.joining(", "));
    }
    public Intensity intensity() {
        // A real height stays separate from acceleration; do not compare their numeric classes.
        return observations.stream().map(f->f.details().intensity()).max(Comparator.comparingInt((Intensity i)->i.heightBased()?1:0)
                .thenComparingDouble(i->i.value()==null?-1:i.value())).orElseThrow();
    }
}
