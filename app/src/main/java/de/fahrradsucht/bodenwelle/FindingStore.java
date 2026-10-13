package de.fahrradsucht.bodenwelle;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import java.util.ArrayList;
import java.util.List;

public final class FindingStore extends SQLiteOpenHelper {
    public FindingStore(Context context) { super(context, "findings.db", null, 2); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE findings (id INTEGER PRIMARY KEY AUTOINCREMENT, time_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracy REAL NOT NULL, speed REAL, peak REAL, threshold REAL, source TEXT NOT NULL, category TEXT NOT NULL DEFAULT 'Ungeklärt', note TEXT NOT NULL DEFAULT '', reviewed INTEGER NOT NULL DEFAULT 0, location_age_ms INTEGER NOT NULL)");
        createRouteTable(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createRouteTable(db);
    }

    private static void createRouteTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE route_points (id INTEGER PRIMARY KEY AUTOINCREMENT, time_ms INTEGER NOT NULL, elapsed_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracy REAL NOT NULL, segment INTEGER NOT NULL)");
    }

    public void clearRoute() { getWritableDatabase().delete("route_points", null, null); }

    public void insertRoutePoint(RoutePoint point) {
        ContentValues v = new ContentValues();
        v.put("time_ms", point.timeMillis()); v.put("elapsed_ms", point.elapsedMillis());
        v.put("latitude", point.latitude()); v.put("longitude", point.longitude());
        v.put("accuracy", point.accuracy()); v.put("segment", point.segment());
        getWritableDatabase().insertOrThrow("route_points", null, v);
    }

    public int routeCount() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM route_points", null)) {
            c.moveToFirst(); return c.getInt(0);
        }
    }

    public List<RoutePoint> route() {
        List<RoutePoint> points = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT time_ms, elapsed_ms, latitude, longitude, accuracy, segment FROM route_points ORDER BY id", null)) {
            while (c.moveToNext()) points.add(new RoutePoint(c.getLong(0), c.getLong(1), c.getDouble(2), c.getDouble(3), c.getFloat(4), c.getInt(5)));
        }
        return points;
    }

    public void insert(Location location, BumpDetector.Detection detection) {
        ContentValues values = new ContentValues();
        // Event wall-clock time and GPS-fix age are recorded separately.
        values.put("time_ms", System.currentTimeMillis());
        values.put("latitude", location.getLatitude());
        values.put("longitude", location.getLongitude());
        values.put("accuracy", location.getAccuracy());
        values.put("location_age_ms", (android.os.SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1_000_000L);
        if (location.hasSpeed()) values.put("speed", location.getSpeed());
        values.put("source", detection == null ? "manual" : "automatic");
        if (detection != null) {
            values.put("peak", detection.peak());
            values.put("threshold", detection.threshold());
        }
        getWritableDatabase().insertOrThrow("findings", null, values);
    }

    public List<Finding> all() {
        List<Finding> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM findings ORDER BY id DESC", null)) {
            while (c.moveToNext()) result.add(new Finding(c.getLong(0), c.getLong(1), c.getDouble(2),
                    c.getDouble(3), c.getFloat(4), c.isNull(5) ? null : c.getFloat(5),
                    c.isNull(6) ? null : c.getDouble(6), c.isNull(7) ? null : c.getDouble(7),
                    c.getString(8), c.getString(9), c.getString(10), c.getInt(11) != 0, c.getLong(12)));
        }
        return result;
    }

    public int count() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM findings", null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    public void review(long id, String category, String note, boolean reviewed) {
        ContentValues v = new ContentValues();
        v.put("category", category); v.put("note", note); v.put("reviewed", reviewed ? 1 : 0);
        getWritableDatabase().update("findings", v, "id = ?", new String[]{Long.toString(id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete("findings", "id = ?", new String[]{Long.toString(id)});
    }
    public void deleteAll() { getWritableDatabase().delete("findings", null, null); }
}
