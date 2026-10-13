package de.fahrradsucht.bodenwelle;

/** Separate provisional sensor response from a user-supplied measured height. */
public record Intensity(Double value, String unit, String method, int level) {
    private static final int[] COLORS = {0xff666666, 0xff176329, 0xff74ae32, 0xffffcd32, 0xffe58222, 0xffc43030};
    public static Intensity evaluate(Double measuredHeightMm, Double peak) {
        if (measuredHeightMm != null) {
            validate(measuredHeightMm);
            return new Intensity(measuredHeightMm, "mm", "manual_measured_height_v1", band(measuredHeightMm, 5, 10, 20, 30));
        }
        if (peak != null) {
            validate(peak);
            return new Intensity(peak, "m/s²", "sensor_peak_provisional_v1", band(peak, 10, 20, 40, 60));
        }
        return new Intensity(null, "", "unavailable", 0);
    }
    private static void validate(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Measurement must be finite and non-negative");
    }
    private static int band(double value, double a, double b, double c, double d) {
        return value <= a ? 1 : value <= b ? 2 : value <= c ? 3 : value <= d ? 4 : 5;
    }
    public boolean heightBased() { return "manual_measured_height_v1".equals(method); }
    public String label() {
        if (level < 1 || level > 5) return "Intensität unbekannt";
        String[] heights = {"Sehr gut", "Gut", "Beeinträchtigt", "Problematisch", "Potenziell gefährlich"};
        String[] sensors = {"Sehr schwache", "Schwache", "Mittlere", "Starke", "Sehr starke"};
        return heightBased() ? heights[level - 1] + " · gemessene Höhe"
                : sensors[level - 1] + " Sensorreaktion · vorläufig";
    }
    public int color() { return COLORS[Math.max(0, Math.min(5, level))]; }
}
