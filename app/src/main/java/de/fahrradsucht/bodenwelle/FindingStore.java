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
    private final Context context;
    public FindingStore(Context context) { super(context, "findings.db", null, 7); this.context = context.getApplicationContext(); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE findings (id INTEGER PRIMARY KEY AUTOINCREMENT, time_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracy REAL NOT NULL, speed REAL, peak REAL, threshold REAL, source TEXT NOT NULL, category TEXT NOT NULL DEFAULT 'Ungeklärt', note TEXT NOT NULL DEFAULT '', reviewed INTEGER NOT NULL DEFAULT 0, location_age_ms INTEGER NOT NULL)");
        createRouteTable(db);
        addPositionColumns(db);
        addClassification(db);
        addMailColumn(db);
        addBundlesAndPhotos(db);
        addSharedPhotos(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createRouteTable(db);
        if (oldVersion < 3) addPositionColumns(db);
        if (oldVersion < 4) addClassification(db);
        if (oldVersion < 5) addMailColumn(db);
        if (oldVersion < 6) addBundlesAndPhotos(db);
        if (oldVersion < 7) addSharedPhotos(db);
    }
    private static void addBundlesAndPhotos(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE finding_bundles (id INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("ALTER TABLE findings ADD COLUMN bundle_id INTEGER REFERENCES finding_bundles(id)");
        db.execSQL("CREATE INDEX findings_bundle ON findings(bundle_id)");
        db.execSQL("CREATE TABLE finding_photos(id INTEGER PRIMARY KEY AUTOINCREMENT,finding_id INTEGER NOT NULL REFERENCES findings(id) ON DELETE CASCADE,filename TEXT NOT NULL UNIQUE,width INTEGER NOT NULL,height INTEGER NOT NULL,bytes INTEGER NOT NULL)");
    }
    private static void addSharedPhotos(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE photo_links(photo_id INTEGER NOT NULL REFERENCES finding_photos(id) ON DELETE CASCADE,finding_id INTEGER NOT NULL REFERENCES findings(id) ON DELETE CASCADE,PRIMARY KEY(photo_id,finding_id))");
        db.execSQL("CREATE INDEX photo_links_finding ON photo_links(finding_id)");
        db.execSQL("INSERT INTO photo_links SELECT id,finding_id FROM finding_photos");
        java.util.Map<Long,List<Finding>> bundles = new java.util.LinkedHashMap<>();
        try (Cursor c = db.rawQuery("SELECT * FROM findings WHERE bundle_id IS NOT NULL ORDER BY id DESC", null)) {
            while (c.moveToNext()) { Finding f = readFinding(c); bundles.computeIfAbsent(f.bundleId(), unused -> new ArrayList<>()).add(f); }
        }
        for (java.util.Map.Entry<Long,List<Finding>> bundle : bundles.entrySet()) {
            shareBundlePhotos(db, bundle.getKey());
            applyShared(db, bundle.getValue(), SharedFindingDetails.from(bundle.getValue()));
        }
    }
    private static void shareBundlePhotos(SQLiteDatabase db, long bundleId) {
        db.execSQL("INSERT OR IGNORE INTO photo_links(photo_id,finding_id) SELECT links.photo_id,member.id FROM photo_links links JOIN findings origin ON origin.id=links.finding_id JOIN findings member ON member.bundle_id=origin.bundle_id WHERE origin.bundle_id=?", new Object[]{bundleId});
    }
    private static void applyShared(SQLiteDatabase db, List<Finding> members, SharedFindingDetails shared) {
        for (Finding f : members) {
            ContentValues values = new ContentValues(); values.put("details", shared.apply(f.details()).json());
            db.update("findings", values, "id = ?", new String[]{Long.toString(f.id())});
        }
    }
    private static void addMailColumn(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE findings ADD COLUMN report_district TEXT NOT NULL DEFAULT ''");
    }
    public String reportDistrict(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT report_district FROM findings WHERE id = ?", new String[]{Long.toString(id)})) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }
    public void reportDistrict(long id, String district) {
        if (!district.isEmpty() && !district.matches("0[1-9]|1[0-2]")) throw new IllegalArgumentException("Unknown Berlin district");
        ContentValues values = new ContentValues(); values.put("report_district", district);
        getWritableDatabase().update("findings", values, "id = ?", new String[]{Long.toString(id)}); CHANGES.incrementAndGet();
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

    public long insertSection(Location start, Location end, SurfaceDetector.Section section) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long id = insert(start, new BumpDetector.Detection(section.startMillis(), section.peak(), section.threshold()));
            long offset = section.endMillis() - end.getElapsedRealtimeNanos() / 1_000_000L;
            PositionEstimate estimate = PositionEstimator.estimate(end.getLatitude(), end.getLongitude(), offset,
                    end.hasSpeed() ? end.getSpeed() : null, end.hasBearing() ? end.getBearing() : null,
                    end.hasBearingAccuracy() ? end.getBearingAccuracyDegrees() : null);
            RoughSection geometry = new RoughSection(estimate.latitude(), estimate.longitude(), end.getAccuracy(),
                    section.endMillis() - section.startMillis(), section.lengthMeters(), end.getLatitude(), end.getLongitude(),
                    estimate.distanceMeters(), estimate.method());
            ContentValues values = new ContentValues();
            values.put("category", "rough_road");
            values.put("details", FindingDetails.initial("rough_road", section.peak()).withSection(geometry).json());
            db.update("findings", values, "id = ?", new String[]{Long.toString(id)});
            db.setTransactionSuccessful();
            return id;
        } finally { db.endTransaction(); }
    }

    /** Read only the original observations belonging to one displayed place. */
    public List<Finding> observations(long bundleKey) {
        List<Finding> result = new ArrayList<>();
        String where = bundleKey > 0 ? "bundle_id = ?" : "id = ? AND bundle_id IS NULL";
        long id = bundleKey > 0 ? bundleKey : -bundleKey;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM findings WHERE " + where + " ORDER BY id DESC",
                new String[]{Long.toString(id)})) {
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
                            c.getLong(12), c.isNull(15) ? null : c.getFloat(15), c.getDouble(16), c.getString(17)), c.getString(18), FindingDetails.fromJson(c.getString(19), c.getString(9), c.isNull(6) ? null : c.getDouble(6)), c.isNull(21) ? 0 : c.getLong(21));
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
            if (current != null && current.bundleId() > 0) {
                List<Finding> members = observations(current.bundleKey());
                applyShared(db, members, SharedFindingDetails.edited(members, details));
            }
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
                if (current.bundleId() > 0) {
                    List<Finding> members = observations(current.bundleKey());
                    applyShared(db, members, SharedFindingDetails.from(members));
                }
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

    public List<FindingBundle> bundles() {
        java.util.Map<Long,List<Finding>> groups = new java.util.LinkedHashMap<>();
        for (Finding f : all()) groups.computeIfAbsent(f.bundleKey(), k -> new ArrayList<>()).add(f);
        List<FindingBundle> result = new ArrayList<>();
        for (java.util.Map.Entry<Long,List<Finding>> e : groups.entrySet()) result.add(new FindingBundle(e.getKey(), e.getValue()));
        return result;
    }
    public long mergeBundles(List<Long> keys) { return mergeBundles(keys, null, null); }
    public long mergeBundles(List<Long> keys, String traffic, String surface) {
        java.util.Set<Long> chosen = new java.util.HashSet<>(keys);
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            List<Finding> members = new ArrayList<>();
            java.util.Set<Long> present = new java.util.HashSet<>();
            for (Finding f : all()) if (chosen.contains(f.bundleKey())) { members.add(f); present.add(f.bundleKey()); }
            if (present.size() < 2 || !present.equals(chosen)) throw new IllegalArgumentException("Bitte mindestens zwei vorhandene Fundstellen auswählen.");
            SharedFindingDetails shared = SharedFindingDetails.from(members).choose(traffic, surface);
            long id = db.insertOrThrow("finding_bundles", "id", new ContentValues());
            ContentValues v = new ContentValues(); v.put("bundle_id", id);
            for (Finding f : members) db.update("findings", v, "id = ?", new String[]{Long.toString(f.id())});
            applyShared(db, members, shared); shareBundlePhotos(db, id);
            removeEmptyBundles(db); db.setTransactionSuccessful(); CHANGES.incrementAndGet(); return id;
        } finally { db.endTransaction(); }
    }
    public void resolveShared(long bundleKey, String traffic, String surface) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            List<Finding> members = observations(bundleKey);
            if (members.isEmpty()) throw new IllegalArgumentException("Diese Fundstelle ist nicht mehr vorhanden.");
            applyShared(db, members, SharedFindingDetails.from(members).choose(traffic, surface));
            db.setTransactionSuccessful(); CHANGES.incrementAndGet();
        } finally { db.endTransaction(); }
    }
    public void separateBundle(long key) {
        ContentValues v = new ContentValues(); v.putNull("bundle_id");
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try { db.update("findings", v, "bundle_id = ?", new String[]{Long.toString(key)}); removeEmptyBundles(db); db.setTransactionSuccessful(); CHANGES.incrementAndGet(); }
        finally { db.endTransaction(); }
    }
    private static void removeEmptyBundles(SQLiteDatabase db) {
        db.execSQL("DELETE FROM finding_bundles WHERE id NOT IN (SELECT bundle_id FROM findings WHERE bundle_id IS NOT NULL)");
    }
    public List<FindingPhoto> photos(long findingId) {
        List<FindingPhoto> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT p.id,p.finding_id,p.filename,p.width,p.height,p.bytes FROM finding_photos p JOIN photo_links links ON links.photo_id=p.id WHERE links.finding_id=? ORDER BY p.id", new String[]{Long.toString(findingId)})) {
            while (c.moveToNext()) result.add(readPhoto(c));
        }
        return result;
    }
    private static FindingPhoto readPhoto(Cursor c) {
        return new FindingPhoto(c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3), c.getInt(4), c.getLong(5));
    }
    public FindingPhoto photo(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,finding_id,filename,width,height,bytes FROM finding_photos WHERE id = ?", new String[]{Long.toString(id)})) {
            return c.moveToFirst() ? readPhoto(c) : null;
        }
    }
    public boolean hasPhoto(long findingId, long photoId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM photo_links WHERE finding_id=? AND photo_id=?", new String[]{Long.toString(findingId), Long.toString(photoId)})) { return c.moveToFirst(); }
    }
    public int photoLimit(long findingId) {
        Finding finding = get(findingId);
        return PhotoFiles.MAX_PER_OBSERVATION * (finding == null ? 1 : Math.max(1, observations(finding.bundleKey()).size()));
    }
    public synchronized long addPhoto(long findingId, String filename, int width, int height, long bytes) {
        if (!filename.matches("[a-f0-9-]{36}\\.jpg")) throw new IllegalArgumentException("Invalid photo filename");
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            Finding finding = get(findingId);
            if (finding == null) throw new IllegalArgumentException("Diese Messung wurde inzwischen gelöscht.");
            if (photos(findingId).size() >= photoLimit(findingId)) throw new IllegalArgumentException("Der Fotospeicher dieser Fundstelle ist voll. Vorhandene Fotos bleiben erhalten.");
            ContentValues v = new ContentValues(); v.put("finding_id",findingId); v.put("filename",filename); v.put("width",width); v.put("height",height); v.put("bytes",bytes);
            long id = db.insertOrThrow("finding_photos",null,v);
            for (Finding member : observations(finding.bundleKey())) {
                db.execSQL("INSERT INTO photo_links(photo_id,finding_id) VALUES(?,?)", new Object[]{id, member.id()});
            }
            db.setTransactionSuccessful(); CHANGES.incrementAndGet(); return id;
        } finally { db.endTransaction(); }
    }
    public void deletePhoto(long id) {
        FindingPhoto photo = photo(id); if (photo != null) deletePhoto(photo.findingId(), id);
    }
    /** Remove the photo from this place, retaining links and the file for other, separated places. */
    public void deletePhoto(long findingId, long photoId) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction(); List<FindingPhoto> removed = new ArrayList<>();
        try {
            Finding finding = get(findingId); FindingPhoto photo = photo(photoId);
            if (finding != null && photo != null && hasPhoto(findingId, photoId)) {
                for (Finding member : observations(finding.bundleKey()))
                    db.delete("photo_links", "finding_id=? AND photo_id=?", new String[]{Long.toString(member.id()), Long.toString(photoId)});
                keepOrRemovePhoto(db, photo, removed);
            }
            db.setTransactionSuccessful(); CHANGES.incrementAndGet();
        } finally { db.endTransaction(); }
        removeLocalPhotos(removed);
    }
    private static void keepOrRemovePhoto(SQLiteDatabase db, FindingPhoto photo, List<FindingPhoto> removed) {
        try (Cursor c = db.rawQuery("SELECT finding_id FROM photo_links WHERE photo_id=? ORDER BY finding_id LIMIT 1", new String[]{Long.toString(photo.id())})) {
            if (c.moveToFirst()) {
                ContentValues owner = new ContentValues(); owner.put("finding_id", c.getLong(0));
                db.update("finding_photos", owner, "id=?", new String[]{Long.toString(photo.id())});
            } else {
                db.delete("finding_photos", "id=?", new String[]{Long.toString(photo.id())}); removed.add(photo);
            }
        }
    }
    public void delete(long id) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction(); List<FindingPhoto> removed = new ArrayList<>();
        try {
            List<FindingPhoto> photos = photos(id);
            // Transfer the technical file owner before its foreign-key cascade; other links survive.
            db.delete("photo_links", "finding_id=?", new String[]{Long.toString(id)});
            for (FindingPhoto photo : photos) keepOrRemovePhoto(db, photo, removed);
            db.delete("findings", "id = ?", new String[]{Long.toString(id)});
            removeEmptyBundles(db); db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        removeLocalPhotos(removed); CHANGES.incrementAndGet();
    }
    public void deleteAll() {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction(); List<FindingPhoto> files = new ArrayList<>();
        try {
            try (Cursor c = db.rawQuery("SELECT id,finding_id,filename,width,height,bytes FROM finding_photos", null)) { while (c.moveToNext()) files.add(readPhoto(c)); }
            db.delete("findings", null, null); db.delete("finding_bundles", null, null);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        removeLocalPhotos(files); CHANGES.incrementAndGet();
    }
    private void removeLocalPhotos(List<FindingPhoto> photos) {
        for (FindingPhoto photo : photos) PhotoFiles.localFile(context, photo.filename()).delete();
    }
}
