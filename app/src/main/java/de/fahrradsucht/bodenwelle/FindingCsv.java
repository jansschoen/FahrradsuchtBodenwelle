package de.fahrradsucht.bodenwelle;

import java.time.Instant;
import java.util.List;

public final class FindingCsv {
    private FindingCsv() {}

    public static String exportReviewed(List<Finding> findings) {
        StringBuilder csv = new StringBuilder("time_utc,latitude,longitude,gps_accuracy_m,gps_fix_age_ms,speed_kmh,peak_vertical_acceleration_m_s2,threshold_m_s2,source,category,note,reviewed,detector_version\r\n");
        for (Finding f : findings) {
            if (!f.reviewed()) continue;
            csv.append(Instant.ofEpochMilli(f.timeMillis())).append(',')
                    .append(f.latitude()).append(',').append(f.longitude()).append(',').append(f.accuracy()).append(',')
                    .append(f.locationAgeMillis()).append(',')
                    .append(f.speed() == null ? "" : Float.toString(f.speed() * 3.6f)).append(',')
                    .append(f.peak() == null ? "" : f.peak()).append(',')
                    .append(f.threshold() == null ? "" : f.threshold()).append(',')
                    .append(cell(f.source())).append(',').append(cell(f.category())).append(',')
                    .append(cell(f.note())).append(",true,").append(BumpDetector.VERSION).append("\r\n");
        }
        return csv.toString();
    }

    // Treat notes as text even when a spreadsheet interprets formula prefixes.
    private static String cell(String text) {
        if (text == null) text = "";
        String stripped = text.trim();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
