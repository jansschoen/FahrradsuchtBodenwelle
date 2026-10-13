package de.fahrradsucht.bodenwelle;

import android.content.Context;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public final class DistrictCatalog {
    public record Authority(String id, String name, String office, String email, String source, String checkedAt) {
        @Override public String toString() { return name; }
    }
    private final List<Authority> authorities;
    public DistrictCatalog(Context context) throws IOException {
        try (java.io.InputStream in = context.getAssets().open("berlin-authorities.json")) {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream(); byte[] bytes = new byte[4096]; int n;
            while ((n = in.read(bytes)) != -1) buffer.write(bytes, 0, n);
            JSONArray array = new JSONObject(buffer.toString(StandardCharsets.UTF_8.name())).getJSONArray("authorities");
            authorities = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject a = array.getJSONObject(i);
                authorities.add(new Authority(a.getString("id"), a.getString("name"), a.getString("office"), a.getString("email"), a.getString("source"), a.getString("checked_at")));
            }
        } catch (JSONException e) { throw new IOException("Invalid district contacts", e); }
    }
    public List<Authority> all() { return java.util.Collections.unmodifiableList(authorities); }
    public Authority get(String id) { for (Authority a : authorities) if (a.id().equals(id)) return a; return null; }
    public String name(String id) { Authority a = get(id); return a == null ? "Bezirk prüfen" : a.name(); }
}
