package de.fahrradsucht.bodenwelle;

import org.json.JSONException;
import org.json.JSONObject;

/** End of a reported section; its start remains the finding's corrected coordinates. */
public record RoughSection(double endLatitude, double endLongitude, float endAccuracy,
                           long durationMillis, double lengthMeters, double endGpsLatitude,
                           double endGpsLongitude, double endPositionDistance, String endPositionMethod) {
    public RoughSection {
        if (!Double.isFinite(endLatitude) || Math.abs(endLatitude) > 90 || !Double.isFinite(endLongitude)
                || Math.abs(endLongitude) > 180 || !Float.isFinite(endAccuracy) || endAccuracy < 0
                || durationMillis < 0 || !Double.isFinite(lengthMeters) || lengthMeters < 0
                || !Double.isFinite(endGpsLatitude) || Math.abs(endGpsLatitude) > 90
                || !Double.isFinite(endGpsLongitude) || Math.abs(endGpsLongitude) > 180
                || !Double.isFinite(endPositionDistance) || endPositionMethod == null)
            throw new IllegalArgumentException("Invalid section geometry");
    }
    public JSONObject json() throws JSONException {
        return new JSONObject().put("end_latitude", endLatitude).put("end_longitude", endLongitude)
                .put("end_accuracy_m", endAccuracy).put("duration_ms", durationMillis).put("length_m", lengthMeters)
                .put("end_gps_latitude", endGpsLatitude).put("end_gps_longitude", endGpsLongitude)
                .put("end_position_distance_m", endPositionDistance).put("end_position_method", endPositionMethod);
    }
    public static RoughSection fromJson(JSONObject o) throws JSONException {
        return o == null ? null : new RoughSection(o.getDouble("end_latitude"), o.getDouble("end_longitude"),
                (float) o.getDouble("end_accuracy_m"), o.getLong("duration_ms"), o.getDouble("length_m"),
                o.getDouble("end_gps_latitude"), o.getDouble("end_gps_longitude"), o.getDouble("end_position_distance_m"),
                o.getString("end_position_method"));
    }
    public String label() {
        return String.format(java.util.Locale.GERMANY, "Unebener Abschnitt · etwa %.0f m · %.1f s", lengthMeters, durationMillis / 1000.0);
    }
}
