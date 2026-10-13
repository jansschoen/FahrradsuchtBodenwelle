package de.fahrradsucht.bodenwelle;

import java.time.Instant;
import java.util.List;

public final class FindingCsv {
    private FindingCsv() {}

    public static String exportReviewed(List<Finding> findings) {
        StringBuilder csv = new StringBuilder("time_utc,latitude,longitude,gps_accuracy_m,gps_fix_age_ms,speed_kmh,peak_vertical_acceleration_m_s2,threshold_m_s2,source,category,note,reviewed,detector_version,gps_latitude,gps_longitude,bearing_deg,position_distance_m,position_method,height_mm,intensity_value,intensity_unit,intensity_method,intensity_class,hazard_factors,legacy_category,traffic_type,traffic_source,surface_type,surface_source,osm_way_id,osm_tags,osm_queried_at_utc,osm_confidence,osm_status,osm_source,osm_distance_m,osm_license,osm_attribution,bundle_id\r\n");
        for (Finding f : findings) {
            if (!f.reviewed()) continue;
            csv.append(Instant.ofEpochMilli(f.timeMillis())).append(',')
                    .append(f.latitude()).append(',').append(f.longitude()).append(',').append(f.accuracy()).append(',')
                    .append(f.locationAgeMillis()).append(',')
                    .append(f.speed() == null ? "" : Float.toString(f.speed() * 3.6f)).append(',')
                    .append(f.peak() == null ? "" : f.peak()).append(',')
                    .append(f.threshold() == null ? "" : f.threshold()).append(',')
                    .append(cell(f.source())).append(',').append(cell(f.category())).append(',')
                    .append(cell(f.note())).append(",true,").append(cell(f.detectorVersion())).append(',')
                    .append(f.position().gpsLatitude()).append(',').append(f.position().gpsLongitude()).append(',')
                    .append(f.position().bearing() == null ? "" : f.position().bearing()).append(',')
                    .append(f.position().distanceMeters()).append(',').append(cell(f.position().method()));
            FindingDetails d = f.details(); OsmMatch m = d.osm();
            csv.append(',').append(d.heightMm() == null ? "" : d.heightMm())
                    .append(',').append(d.intensity().value() == null ? "" : d.intensity().value())
                    .append(',').append(cell(d.intensity().unit())).append(',').append(cell(d.intensity().method()))
                    .append(',').append(d.intensity().level()).append(',').append(cell(Classification.hazards(d.hazards())))
                    .append(',').append(cell(d.legacyCategory())).append(',').append(cell(d.traffic()))
                    .append(',').append(cell(d.trafficManual() ? "manual" : "osm"))
                    .append(',').append(cell(d.surface())).append(',').append(cell(d.surfaceManual() ? "manual" : "osm"))
                    .append(',').append(m.wayId() == null ? "" : m.wayId())
                    .append(',').append(cell(tagsJson(m.tags())))
                    .append(',').append(m.queriedAt() == 0 ? "" : Instant.ofEpochMilli(m.queriedAt()))
                    .append(',').append(cell(m.confidence())).append(',').append(cell(m.status())).append(',').append(cell(m.source()))
                    .append(',').append(m.distanceMeters() == null ? "" : m.distanceMeters())
                    .append(',').append(cell("ODbL 1.0 https://opendatacommons.org/licenses/odbl/1-0/"))
                    .append(',').append(cell("© OpenStreetMap contributors https://www.openstreetmap.org/copyright")).append(',').append(f.bundleId()>0?Long.toString(f.bundleId()):"").append("\r\n");
        }
        return csv.toString();
    }

    private static String tagsJson(java.util.Map<String, String> tags) {
        StringBuilder text = new StringBuilder("{");
        for (java.util.Map.Entry<String, String> e : new java.util.TreeMap<>(tags).entrySet()) {
            if (text.length() > 1) text.append(',');
            text.append(jsonString(e.getKey())).append(':').append(jsonString(e.getValue()));
        }
        return text.append('}').toString();
    }
    private static String jsonString(String value) {
        StringBuilder text = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') text.append('\\').append(c);
            else if (c < 32) text.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
            else text.append(c);
        }
        return text.append('"').toString();
    }

    // Treat notes as text even when a spreadsheet interprets formula prefixes.
    private static String cell(String text) {
        if (text == null) text = "";
        String stripped = text.trim();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
