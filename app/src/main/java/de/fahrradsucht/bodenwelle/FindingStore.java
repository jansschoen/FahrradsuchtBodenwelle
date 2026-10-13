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
    private static final java.util.concurrent.atomic.AtomicLong CHANGES = new java.util.concurrent.atomic.AtomicLong();
    public FindingStore(Context context) { super(context, "findings.db", null, 4); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE findings (id INTEGER PRIMARY KEY AUTOINCREMENT, time_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracy REAL NOT NULL, speed REAL, peak REAL, threshold REAL, source TEXT NOT NULL, category TEXT NOT NULL DEFAULT 'Ungeklärt', note TEXT NOT NULL DEFAULT '', reviewed INTEGER NOT NULL DEFAULT 0, location_age_ms INTEGER NOT NULL)");
        createRouteTable(db);
        addPositionColumns(db);
        addClassification(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createRouteTable(db);
        if (oldVersion < 3) addPositionColumns(db);
        if (oldVersion < 4) addClassification(db);
    }

    private static void addClassification(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE findings ADD COLUMN details TEXT NOT NULL DEFAULT '{}'");
        db.execSQL("CREATE TABLE osm_cache (cell TEXT PRIMARY KEY, queried_at INTEGER NOT NULL, response TEXT NOT NULL)");
        try (Cursor c = db.rawQuery("SELECT id,category,peak FROM findings", null)) {
            while (c.moveToNext()) {
                String old = c.getString(1);
                ContentValues values = new ContentValues();
                values.put("category", Classification.bumpId(old));
                values.put("details", FindingDetails.initial(old, c.isNull(2) ? null : c.getDouble(2)).json());
                db.update("findings", values, "id = ?", new String[]{Long.toString(c.getLong(0))});
            }
        }
    }

    private static void addPositionColumns(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE findings ADD COLUMN gps_latitude REAL");
        db.execSQL("ALTER TABLE findings ADD COLUMN gps_longitude REAL");
        db.execSQL("ALTER TABLE findings ADD COLUMN bearing_deg REAL");
        db.execSQL("ALTER TABLE findings ADD COLUMN position_distance_m REAL NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE findings ADD COLUMN position_method TEXT NOT NULL DEFAULT 'legacy_gps'");
        db.execSQL("ALTER TABLE findings ADD COLUMN detector_version TEXT NOT NULL DEFAULT 'experimental-1'");
        db.execSQL("UPDATE findings SET gps_latitude = latitude, gps_longitude = longitude");
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

    public long insert(Location location, BumpDetector.Detection detection) {
        ContentValues values = new ContentValues();
        long nowElapsed = android.os.SystemClock.elapsedRealtime();
        long eventElapsed = detection == null ? nowElapsed : detection.elapsedMillis();
        long offset = eventElapsed - location.getElapsedRealtimeNanos() / 1_000_000L;
        Float speed = location.hasSpeed() ? location.getSpeed() : null;
        Float bearing = location.hasBearing() ? location.getBearing() : null;
        PositionEstimate estimate = detection == null
                ? new PositionEstimate(location.getLatitude(), location.getLongitude(), location.getLatitude(),
                    location.getLongitude(), offset, bearing, 0, "manual_gps")
                : PositionEstimator.estimate(location.getLatitude(), location.getLongitude(), offset, speed,
                    bearing, location.hasBearingAccuracy() ? location.getBearingAccuracyDegrees() : null);
        // Store sensor peak time, not the later end of the detection window.
        values.put("time_ms", System.currentTimeMillis() - (nowElapsed - eventElapsed));
        values.put("latitude", estimate.latitude());
        values.put("longitude", estimate.longitude());
        values.put("gps_latitude", location.getLatitude());
        values.put("gps_longitude", location.getLongitude());
        if (bearing != null) values.put("bearing_deg", bearing);
        values.put("position_distance_m", estimate.distanceMeters());
        values.put("position_method", estimate.method());
        values.put("detector_version", detection == null ? "manual" : BumpDetector.VERSION);
        values.put("accuracy", location.getAccuracy());
        values.put("location_age_ms", offset);
        if (location.hasSpeed()) values.put("speed", location.getSpeed());
        values.put("source", detection == null ? "manual" : "automatic");
        if (detection != null) {
            values.put("peak", detection.peak());
            values.put("threshold", detection.threshold());
        }
        values.put("category", "other");
        values.put("details", FindingDetails.initial("other", detection == null ? null : detection.peak()).json());
        long id = getWritableDatabase().insertOrThrow("findings", null, values);
        CHANGES.incrementAndGet(); return id;
    }

    public List<Finding> all() {
        List<Finding> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM findings ORDER BY id DESC", null)) {
            while (c.moveToNext()) result.add(readFinding(c));
        }
        return result;
    }

    private static Finding readFinding(Cursor c) {
        return new Finding(c.getLong(0), c.getLong(1), c.getDouble(2),
                    c.getDouble(3), c.getFloat(4), c.isNull(5) ? null : c.getFloat(5),
                    c.isNull(6) ? null : c.getDouble(6), c.isNull(7) ? null : c.getDouble(7),
                    c.getString(8), c.getString(9), c.getString(10), c.getInt(11) != 0, c.getLong(12),
                    new PositionEstimate(c.getDouble(2), c.getDouble(3), c.getDouble(13), c.getDouble(14),
                            c.getLong(12), c.isNull(15) ? null : c.getFloat(15), c.getDouble(16), c.getString(17)), c.getString(18), FindingDetails.fromJson(c.getString(19), c.getString(9), c.isNull(6) ? null : c.getDouble(6)));
    }

    public int count() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM findings", null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    public void review(long id, String category, String note, boolean reviewed) {
        ContentValues v = new ContentValues();
        v.put("category", Classification.bumpId(category)); v.put("note", note); v.put("reviewed", reviewed ? 1 : 0);
        getWritableDatabase().update("findings", v, "id = ?", new String[]{Long.toString(id)});
        CHANGES.incrementAndGet();
    }

    public Finding get(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM findings WHERE id = ?", new String[]{Long.toString(id)})) {
            return c.moveToFirst() ? readFinding(c) : null;
        }
    }
    public void review(long id, String category, String note, boolean reviewed, FindingDetails details) {
        ContentValues v = new ContentValues();
        v.put("category", Classification.bumpId(category)); v.put("note", note); v.put("reviewed", reviewed ? 1 : 0);
        // Review may race a background OSM response: always retain the latest automatic metadata.
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            Finding current = get(id);
            if (current != null) v.put("details", details.withOsm(current.details().osm()).json());
            db.update("findings", v, "id = ?", new String[]{Long.toString(id)});
            db.setTransactionSuccessful(); CHANGES.incrementAndGet();
        } finally { db.endTransaction(); }
    }
    public void setOsm(long id, OsmMatch match) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            Finding current = get(id);
            if (current != null) {
                ContentValues v = new ContentValues(); v.put("details", current.details().withOsm(match).json());
                db.update("findings", v, "id = ?", new String[]{Long.toString(id)});
            }
            db.setTransactionSuccessful(); CHANGES.incrementAndGet();
        } finally { db.endTransaction(); }
    }
    public record CachedOsm(long queriedAt, String response) {}
    public CachedOsm cachedOsm(String cell) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT queried_at,response FROM osm_cache WHERE cell = ?", new String[]{cell})) {
            return c.moveToFirst() ? new CachedOsm(c.getLong(0), c.getString(1)) : null;
        }
    }
    public void cacheOsm(String cell, long time, String response) {
        ContentValues v = new ContentValues(); v.put("cell", cell); v.put("queried_at", time); v.put("response", response);
        SQLiteDatabase db = getWritableDatabase();
        db.insertWithOnConflict("osm_cache", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        db.execSQL("DELETE FROM osm_cache WHERE cell NOT IN (SELECT cell FROM osm_cache ORDER BY queried_at DESC LIMIT 64)");
    }
    public long revision() { return CHANGES.get(); }

    public void delete(long id) {
        getWritableDatabase().delete("findings", "id = ?", new String[]{Long.toString(id)}); CHANGES.incrementAndGet();
    }
    public void deleteAll() { getWritableDatabase().delete("findings", null, null); CHANGES.incrementAndGet(); }
}
