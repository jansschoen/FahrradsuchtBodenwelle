package de.fahrradsucht.bodenwelle;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONException;

public record FindingDetails(String legacyCategory, Double heightMm, Intensity intensity, int hazards,
                             String traffic, boolean trafficManual, String surface, boolean surfaceManual, OsmMatch osm, RoughSection section) {
    public FindingDetails(String legacyCategory, Double heightMm, Intensity intensity, int hazards,
                          String traffic, boolean trafficManual, String surface, boolean surfaceManual, OsmMatch osm) {
        this(legacyCategory, heightMm, intensity, hazards, traffic, trafficManual, surface, surfaceManual, osm, null);
    }
    public FindingDetails withSection(RoughSection section) {
        return new FindingDetails(legacyCategory, heightMm, intensity, hazards, traffic, trafficManual,
                surface, surfaceManual, osm, section);
    }
    public static FindingDetails initial(String oldCategory, Double peak) {
        String id = Classification.bumpId(oldCategory);
        String legacy = "other".equals(id) && !"other".equals(oldCategory) && !"Sonstige Unebenheit".equals(oldCategory) ? oldCategory : "";
        return new FindingDetails(legacy, null, Intensity.evaluate(null, peak), 0, "unknown", false, "unknown", false, OsmMatch.empty("pending"));
    }
    public FindingDetails withOsm(OsmMatch result) {
        return new FindingDetails(legacyCategory, heightMm, intensity, hazards,
                trafficManual ? traffic : result.traffic(), trafficManual, surfaceManual ? surface : result.surface(), surfaceManual, result, section);
    }
    public FindingDetails reviewed(Double height, Double peak, int flags, String road, boolean roadManual, String ground, boolean groundManual) {
        return new FindingDetails(legacyCategory, height, Intensity.evaluate(height, peak), flags,
                roadManual ? road : osm.traffic(), roadManual, groundManual ? ground : osm.surface(), groundManual, osm, section);
    }
    public String json() {
        try {
            JSONObject o = new JSONObject();
            if (section != null) o.put("rough_section", section.json());
            o.put("legacy_category", legacyCategory).put("height_mm", heightMm).put("hazards", hazards)
                    .put("traffic", traffic).put("traffic_manual", trafficManual).put("surface", surface).put("surface_manual", surfaceManual);
            o.put("intensity_value", intensity.value()).put("intensity_unit", intensity.unit()).put("intensity_method", intensity.method()).put("intensity_level", intensity.level());
            JSONObject geo = new JSONObject();
            geo.put("status", osm.status()).put("way_id", osm.wayId()).put("tags", new JSONObject(osm.tags()))
                    .put("queried_at", osm.queriedAt()).put("confidence", osm.confidence()).put("distance_m", osm.distanceMeters())
                    .put("source", osm.source()).put("traffic", osm.traffic()).put("surface", osm.surface());
            return o.put("osm", geo).toString();
        } catch (JSONException e) { throw new IllegalStateException(e); }
    }
    public static FindingDetails fromJson(String text, String category, Double peak) {
        try {
            JSONObject o = new JSONObject(text);
            if (!o.has("intensity_method")) return initial(category, peak);
            JSONObject geo = o.optJSONObject("osm");
            Map<String, String> tags = new HashMap<>();
            JSONObject raw = geo == null ? null : geo.optJSONObject("tags");
            if (raw != null) for (java.util.Iterator<String> it = raw.keys(); it.hasNext();) { String k = it.next(); tags.put(k, raw.getString(k)); }
            OsmMatch match = geo == null ? OsmMatch.empty("pending") : new OsmMatch(geo.optString("status", "pending"),
                    geo.has("way_id") ? geo.getLong("way_id") : null, tags, geo.optLong("queried_at"),
                    geo.optString("confidence", "unknown"), number(geo, "distance_m"), geo.optString("source"),
                    geo.optString("traffic", "unknown"), geo.optString("surface", "unknown"));
            return new FindingDetails(o.optString("legacy_category"), number(o, "height_mm"),
                    new Intensity(number(o, "intensity_value"), o.optString("intensity_unit"), o.optString("intensity_method"), o.optInt("intensity_level")),
                    o.optInt("hazards"), o.optString("traffic", "unknown"), o.optBoolean("traffic_manual"),
                    o.optString("surface", "unknown"), o.optBoolean("surface_manual"), match, RoughSection.fromJson(o.optJSONObject("rough_section")));
        } catch (JSONException e) { throw new IllegalArgumentException("Unreadable finding metadata", e); }
    }
    private static Double number(JSONObject o, String key) throws JSONException { return o.has(key) && !o.isNull(key) ? o.getDouble(key) : null; }
}
