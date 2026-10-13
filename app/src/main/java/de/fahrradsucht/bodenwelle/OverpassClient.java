package de.fahrradsucht.bodenwelle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public final class OverpassClient {
    public static final String ENDPOINT = "https://overpass-api.de/api/interpreter";
    public interface Transport { String fetch(String query) throws IOException; }
    public static final class RequestFailure extends IOException {
        public final long retryAfterMillis;
        public RequestFailure(int status, long retryAfterMillis) { super("OSM request failed: " + status); this.retryAfterMillis = retryAfterMillis; }
    }
    public static String fetch(String query) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setConnectTimeout(10_000); connection.setReadTimeout(30_000);
        connection.setRequestMethod("POST"); connection.setDoOutput(true); connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "Bodenwelle/0.4.0 (https://github.com/jansschoen/FahrradsuchtBodenwelle)");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        byte[] request = ("data=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name())).getBytes(StandardCharsets.UTF_8);
        try {
            try (java.io.OutputStream out = connection.getOutputStream()) { out.write(request); }
            int status = connection.getResponseCode();
            if (status != 200) throw new RequestFailure(status, retryAfter(connection.getHeaderField("Retry-After")));
            try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int n;
                while ((n = in.read(buffer)) != -1) {
                    if (out.size() + n > 4 * 1024 * 1024) throw new IOException("OSM response too large");
                    out.write(buffer, 0, n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally { connection.disconnect(); }
    }
    static long retryAfter(String header) {
        if (header == null) return 0;
        try {
            long seconds = Long.parseLong(header.trim());
            return Math.min(7 * 24 * 60 * 60L, Math.max(0, seconds)) * 1000L;
        } catch (NumberFormatException e) {
            try {
                long date = java.time.ZonedDateTime.parse(header, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
                return Math.min(7 * 24 * 60 * 60 * 1000L, Math.max(0, date - System.currentTimeMillis()));
            } catch (java.time.format.DateTimeParseException ignored) { return 0; }
        }
    }
    public static List<OsmWay> parse(String response) throws IOException {
        try {
            JSONObject root = new JSONObject(response);
            if (root.has("remark")) throw new IOException("Incomplete OSM response");
            JSONArray elements = root.getJSONArray("elements");
            List<OsmWay> result = new ArrayList<>();
            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                if (!"way".equals(element.optString("type"))) continue;
                JSONObject raw = element.optJSONObject("tags"); JSONArray geometry = element.optJSONArray("geometry");
                if (raw == null || geometry == null) continue;
                Map<String, String> tags = new HashMap<>();
                for (java.util.Iterator<String> it = raw.keys(); it.hasNext();) { String key = it.next(); tags.put(key, raw.getString(key)); }
                List<OsmWay.Point> points = new ArrayList<>();
                for (int j = 0; j < geometry.length(); j++) {
                    JSONObject point = geometry.getJSONObject(j); double lat = point.getDouble("lat"), lon = point.getDouble("lon");
                    if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) throw new IOException("Invalid OSM geometry");
                    points.add(new OsmWay.Point(lat, lon));
                }
                result.add(new OsmWay(element.getLong("id"), points, tags));
            }
            return result;
        } catch (JSONException e) { throw new IOException("Invalid OSM response", e); }
    }
}
