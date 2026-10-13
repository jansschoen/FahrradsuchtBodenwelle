package de.fahrradsucht.bodenwelle;

/** Stable identifiers; labels and sensor classes make no legal claim. */
public final class Classification {
    private Classification() {}
    public record Choice(String id, String label) { @Override public String toString() { return label; } }
    public static final Choice[] BUMPS = choices("root_heave|Wurzelaufbruch", "pothole|Schlagloch", "transverse_groove|Querrille",
            "longitudinal_groove|Längsrille", "paving_displacement|Pflasterversatz", "long_wave|Langgezogene Bodenwelle", "other|Sonstige Unebenheit");
    public static final Choice[] ROADS = choices("mandatory_cycleway|Benutzungspflichtiger Radweg", "cycleway|Sonstiger Radweg",
            "shared_path|Gemeinsamer Geh- und Radweg", "bicycle_road|Fahrradstraße", "carriageway|Reguläre Fahrbahn",
            "other|Sonstige Verkehrsfläche", "unknown|Unbekannt");
    public static final Choice[] SURFACES = choices("asphalt|Asphalt", "concrete|Beton", "concrete_paving|Betonpflaster",
            "stone_paving|Natursteinpflaster", "compacted|Wassergebundene Decke", "gravel|Schotter oder Kies",
            "unpaved|Unbefestigter Boden", "other|Sonstige Oberfläche", "unknown|Unbekannt");
    public static final Choice[] HAZARDS = choices("poor_visibility|Schlechte Sichtbarkeit der Unebenheit", "no_escape|Keine sichere Ausweichmöglichkeit",
            "downhill|Gefälle", "bend|Kurvenbereich", "standing_water|Wasseransammlung");
    private static Choice[] choices(String... values) {
        Choice[] result = new Choice[values.length];
        for (int i = 0; i < values.length; i++) { String[] parts = values[i].split("\\|", 2); result[i] = new Choice(parts[0], parts[1]); }
        return result;
    }
    public static String label(Choice[] choices, String id) {
        for (Choice c : choices) if (c.id.equals(id)) return c.label;
        return "Unbekannt";
    }
    public static int index(Choice[] choices, String id) {
        for (int i = 0; i < choices.length; i++) if (choices[i].id.equals(id)) return i;
        return choices.length - 1;
    }
    public static String bumpId(String old) {
        for (Choice c : BUMPS) if (c.id.equals(old) || c.label.equals(old)) return c.id;
        if ("Wurzel".equals(old)) return "root_heave";
        if ("Schiefe Bodenplatte".equals(old)) return "paving_displacement";
        return "other"; // A generic Bodenwelle/Straßenschaden has no unambiguous subtype.
    }
    public static String hazards(int mask) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < HAZARDS.length; i++) if ((mask & (1 << i)) != 0) {
            if (result.length() > 0) result.append(';'); result.append(HAZARDS[i].id);
        }
        return result.toString();
    }
}
