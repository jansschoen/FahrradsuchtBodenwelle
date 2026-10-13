package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.content.SharedPreferences;

public final class DetectionPreferences {
    public static final int MIN = 5;
    public static final int MAX = 100;
    public static final int DEFAULT = 40;
    private final SharedPreferences preferences;
    public DetectionPreferences(Context context) {
        preferences = context.getSharedPreferences("detection", Context.MODE_PRIVATE);
    }
    public int threshold() { return Math.max(MIN, Math.min(MAX, preferences.getInt("threshold", DEFAULT))); }
    public void setThreshold(int value) {
        if (value < MIN || value > MAX) throw new IllegalArgumentException("Threshold out of range");
        preferences.edit().putInt("threshold", value).apply();
    }
}
