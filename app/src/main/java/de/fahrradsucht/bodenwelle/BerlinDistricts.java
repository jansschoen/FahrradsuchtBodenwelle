package de.fahrradsucht.bodenwelle;

import android.content.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/** Offline polygons are an approximate geographical proposal, not a road-owner register. */
public final class BerlinDistricts {
    public record Assignment(String districtId, String method) {}
    private record Polygon(String id, double[][][] rings, double minLat, double maxLat, double minLon, double maxLon) {}
    private final List<Polygon> polygons = new ArrayList<>();
    public BerlinDistricts(Context context) throws IOException {
        try (java.io.InputStream in = context.getAssets().open("berlin-districts.geojson")) {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream(); byte[] bytes = new byte[8192]; int n;
            while ((n = in.read(bytes)) != -1) buffer.write(bytes, 0, n);
            JSONArray features = new JSONObject(buffer.toString("UTF-8")).getJSONArray("features");
            for (int i = 0; i < features.length(); i++) {
                JSONObject f = features.getJSONObject(i), geometry = f.getJSONObject("geometry");
                String id = f.getJSONObject("properties").getString("district_id");
                JSONArray coords = geometry.getJSONArray("coordinates");
                if ("Polygon".equals(geometry.getString("type"))) add(id, coords);
                else if ("MultiPolygon".equals(geometry.getString("type"))) for (int j = 0; j < coords.length(); j++) add(id, coords.getJSONArray(j));
                else throw new IOException("Unsupported district geometry");
            }
        } catch (JSONException e) { throw new IOException("Invalid district boundaries", e); }
    }
    private void add(String id, JSONArray source) throws JSONException {
        double[][][] rings = new double[source.length()][][];
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
        for (int r = 0; r < rings.length; r++) {
            JSONArray ring = source.getJSONArray(r); rings[r] = new double[ring.length()][2];
            for (int p = 0; p < ring.length(); p++) {
                JSONArray point = ring.getJSONArray(p); double lon = point.getDouble(0), lat = point.getDouble(1);
                rings[r][p] = new double[]{lon, lat};
                minLat = Math.min(minLat, lat); maxLat = Math.max(maxLat, lat); minLon = Math.min(minLon, lon); maxLon = Math.max(maxLon, lon);
            }
        }
        polygons.add(new Polygon(id, rings, minLat, maxLat, minLon, maxLon));
    }
    public Assignment assign(Finding f, String manualId) {
        if (manualId != null && !manualId.isEmpty()) return new Assignment(manualId, "manual");
        return locate(f.latitude(), f.longitude(), f.accuracy(), f.position().distanceMeters());
    }
    public Assignment locate(double lat, double lon, float accuracy, double projectionDistance) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || !Double.isFinite(projectionDistance) || !Float.isFinite(accuracy) || accuracy < 0)
            return new Assignment("", "invalid_position");
        double margin = Math.max(50, accuracy + Math.abs(projectionDistance) * .25);
        String district = ""; double borderDistance = Double.POSITIVE_INFINITY;
        for (Polygon p : polygons) {
            double latMargin = margin / 111000, lonMargin = margin / (111000 * Math.cos(Math.toRadians(lat)));
            if (lat < p.minLat - latMargin || lat > p.maxLat + latMargin || lon < p.minLon - lonMargin || lon > p.maxLon + lonMargin) continue;
            boolean inside = inside(p.rings[0], lat, lon);
            for (int r = 1; r < p.rings.length; r++) if (inside(p.rings[r], lat, lon)) inside = false;
            if (inside) {
                if (!district.isEmpty() && !district.equals(p.id)) return new Assignment("", "overlap");
                district = p.id;
            }
            for (double[][] ring : p.rings) borderDistance = Math.min(borderDistance, distance(ring, lat, lon));
        }
        if (borderDistance <= margin) return new Assignment("", "near_boundary");
        return new Assignment(district, district.isEmpty() ? "outside_berlin" : "offline_polygons_2013");
    }
    private static boolean inside(double[][] ring, double lat, double lon) {
        boolean result = false;
        for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
            double[] a = ring[i], b = ring[j];
            if ((a[1] > lat) != (b[1] > lat) && lon < (b[0] - a[0]) * (lat - a[1]) / (b[1] - a[1]) + a[0]) result = !result;
        }
        return result;
    }
    private static double distance(double[][] ring, double lat, double lon) {
        double result = Double.POSITIVE_INFINITY, scale = 111320 * Math.cos(Math.toRadians(lat));
        for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
            double ax = (ring[j][0] - lon) * scale, ay = (ring[j][1] - lat) * 111320;
            double bx = (ring[i][0] - lon) * scale, by = (ring[i][1] - lat) * 111320;
            double dx = bx - ax, dy = by - ay, length = dx * dx + dy * dy;
            double t = length == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / length));
            result = Math.min(result, Math.hypot(ax + t * dx, ay + t * dy));
        }
        return result;
    }
}
