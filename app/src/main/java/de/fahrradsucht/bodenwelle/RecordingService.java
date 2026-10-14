package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import java.util.Locale;

public final class RecordingService extends Service implements SensorEventListener, LocationListener {
    public static final String STOP = "de.fahrradsucht.bodenwelle.STOP";
    public static final String MARK = "de.fahrradsucht.bodenwelle.MARK";
    public static final String CALIBRATE = "de.fahrradsucht.bodenwelle.CALIBRATE";
    public static volatile boolean running;
    public static volatile String status = "Noch keine Fahrt gestartet.";
    public static volatile String lastAction = "";
    public static volatile String startFailure = "";
    public static volatile String calibrationStatus = "Vor der Fahrt das Handy fest befestigen und zwei Sekunden ruhig halten.";
    private static final String CHANNEL = "ride";
    private final AccelerationProjector projector = new AccelerationProjector();
    private final SurfaceDetector detector = new SurfaceDetector();
    private final java.util.TreeMap<Long, Location> locationHistory = new java.util.TreeMap<>();
    private long lastLocationSnapshot = -1;
    private final RouteTracker routeTracker = new RouteTracker();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SensorManager sensors;
    private LocationManager locations;
    private DeviceLocationSource locationSource;
    private FindingStore store;
    private DetectionPreferences preferences;
    private Location latest;
    private PowerManager.WakeLock wakeLock;
    private long lastNotification;
    private long startedAt;
    private long lastSensorAt;

    @Override public void onCreate() {
        super.onCreate();
        store = new FindingStore(this);
        preferences = new DetectionPreferences(this);
        sensors = getSystemService(SensorManager.class);
        locations = getSystemService(LocationManager.class);
        locationSource = new DeviceLocationSource(this, this::onLocationChanged);
        getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel(CHANNEL, "Fahrtaufzeichnung", NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) {
            boolean saved = saveUpdate(detector.finish());
            running = false;
            if (saved) status = "Fahrt beendet. Fundstellen bleiben auf diesem Handy.";
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && CALIBRATE.equals(intent.getAction())) {
            if (running) {
                saveUpdate(detector.finish());
                projector.reset();
                calibrationStatus = "Kalibrierung · bitte anhalten und das montierte Handy zwei Sekunden ruhig halten.";
                refreshStatus();
            } else stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && MARK.equals(intent.getAction())) {
            if (running && usableLocation()) {
                try {
                    long id = store.insert(latest, null);
                    OsmEnricher.enqueue(this, id);
                    lastAction = "Stelle markiert – bitte nach der Fahrt prüfen.";
                } catch (RuntimeException e) {
                    lastAction = "Speichern fehlgeschlagen. Bitte verfügbaren Speicher prüfen.";
                }
            } else lastAction = "Markierung nicht gespeichert: aktuelle genaue Position fehlt.";
            updateNotification();
            if (!running) stopSelf();
            return START_NOT_STICKY;
        }
        if (running) return START_NOT_STICKY;
        startFailure = "";
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            failStart("Für die Aufzeichnung ist genauer Standortzugriff erforderlich.");
            return START_NOT_STICKY;
        }
        try {
            if (Build.VERSION.SDK_INT >= 29)
                startForeground(1, notification("Fahrt wird gestartet …"), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            else startForeground(1, notification("Fahrt wird gestartet …"));
        } catch (RuntimeException e) {
            failStart("Android konnte die Aufzeichnung nicht starten (" + e.getClass().getSimpleName() + "). Bitte die App geöffnet lassen und erneut versuchen.");
            return START_NOT_STICKY;
        }
        Sensor accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null || !sensors.registerListener(this, accelerometer, 10_000)) {
            failStart("Kein nutzbarer Beschleunigungssensor gefunden.");
            return START_NOT_STICKY;
        }
        Sensor rotation = sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (rotation == null) rotation = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotation != null) sensors.registerListener(this, rotation, 10_000);
        try {
            if (DeviceLocationSource.enabledProviders(locations).isEmpty()) {
                failStart("Der Standort ist am Handy ausgeschaltet. Bitte einschalten und erneut starten.");
                return START_NOT_STICKY;
            }
        } catch (SecurityException | IllegalArgumentException e) {
            failStart("Der Standortzugriff ist nicht verfügbar. Bitte den genauen Standort für Bodenwelle erlauben.");
            return START_NOT_STICKY;
        }
        try { store.clearRoute(); }
        catch (RuntimeException e) {
            failStart("Fahrtspur konnte nicht vorbereitet werden. Bitte Speicher prüfen.");
            return START_NOT_STICKY;
        }
        running = true;
        projector.reset();
        calibrationStatus = "Kalibrierung · bitte im Stand das montierte Handy zwei Sekunden ruhig halten.";
        startedAt = SystemClock.elapsedRealtime();
        status = "Fahrt läuft · Standort wird gesucht …";
        lastAction = "";
        if (!locationSource.start()) {
            running = false;
            failStart("Keine Standortquelle konnte gestartet werden. Bitte Standort und Berechtigungen prüfen.");
            return START_NOT_STICKY;
        }
        wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Bodenwelle:ride");
        // Screen-off sensor sampling; bounded recording to avoid forgotten battery use.
        wakeLock.acquire(6 * 60 * 60 * 1_000L);
        handler.postDelayed(() -> {
            status = "Aufzeichnung nach sechs Stunden beendet.";
            stopSelf();
        }, 6 * 60 * 60 * 1_000L);
        handler.post(tick);
        return START_NOT_STICKY;
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            refreshStatus();
            updateNotification();
            handler.postDelayed(this, 2_000);
        }
    };

    private long ageMillis() {
        return latest == null ? Long.MAX_VALUE : (SystemClock.elapsedRealtimeNanos() - latest.getElapsedRealtimeNanos()) / 1_000_000L;
    }

    private boolean usableLocation() {
        return latest != null && RideEligibility.locationUsable(ageMillis(), latest.hasAccuracy(), latest.getAccuracy());
    }

    private boolean eligible() {
        return latest != null && RideEligibility.autoDetectionAllowed(ageMillis(), latest.hasAccuracy(), latest.getAccuracy(),
                latest.hasSpeed(), latest.getSpeed(), latest.hasSpeedAccuracy(), latest.getSpeedAccuracyMetersPerSecond());
    }

    private void refreshStatus() {
        if (!projector.calibrated()) status = calibrationStatus;
        else if (latest == null) status = "Fahrt läuft · Standort wird gesucht. Draußen kurz warten.";
        else if (!usableLocation()) status = "Fahrt läuft · Automatik wartet auf einen aktuellen, genaueren Standort.";
        else if (SystemClock.elapsedRealtime() - startedAt > 3_000 && SystemClock.elapsedRealtime() - lastSensorAt > 3_000)
            status = "Fahrt läuft · Automatik wartet auf Sensordaten.";
        else if (!eligible()) status = "Fahrt läuft · Automatik wird bei genauer Position und Fahrgeschwindigkeit ab etwa 5 km/h aktiv.";
        else status = String.format(Locale.GERMANY, "Aufzeichnung · %.1f km/h · GPS ± %.0f m", latest.getSpeed() * 3.6, latest.getAccuracy());
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!running) return;
        if (event.sensor != null && (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR
                || event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)) {
            float[] matrix = new float[9];
            SensorManager.getRotationMatrixFromVector(matrix, event.values);
            projector.orientation(event.timestamp, matrix[6], matrix[7], matrix[8]);
            return;
        }
        if (event.sensor != null && event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        lastSensorAt = SystemClock.elapsedRealtime();
        boolean stationary = latest == null || !usableLocation() || !latest.hasSpeed() || latest.getSpeed() < .8;
        double vertical = projector.accept(event.timestamp, event.values[0], event.values[1], event.values[2], stationary);
        calibrationStatus = projector.calibrated()
                ? !Double.isFinite(vertical) ? "Kalibriert · Automatik wartet auf aktuelle Lagedaten."
                    : projector.orientationAvailable() ? "Kalibriert · vertikale Beschleunigung · Lage wird mitgeführt."
                    : "Kalibriert · vertikale Beschleunigung · feste Lage; bei Halterungsänderung neu kalibrieren."
                : "Kalibrierung · " + projector.progress() + " % · bitte anhalten und das montierte Handy ruhig halten.";
        long millis = event.timestamp / 1_000_000L;
        boolean allowed = projector.calibrated() && Double.isFinite(vertical) && eligible();
        if (allowed && (lastLocationSnapshot < 0 || millis - lastLocationSnapshot >= 100)) {
            locationHistory.put(millis, new Location(latest));
            lastLocationSnapshot = millis;
        }
        saveUpdate(detector.accept(millis, vertical, preferences.threshold(), allowed,
                latest != null && latest.hasSpeed() ? latest.getSpeed() : 0));
        locationHistory.headMap(millis - 130_000).clear();
        if (detector.sectionActive()) lastAction = "Unebener Straßenabschnitt erkannt · wird bis zum Ende der Störung aufgezeichnet.";
    }

    private Location locationAt(long millis) {
        java.util.Map.Entry<Long, Location> entry = locationHistory.floorEntry(millis);
        if (entry == null) entry = locationHistory.ceilingEntry(millis);
        return entry != null && Math.abs(entry.getKey() - millis) <= 500 ? entry.getValue() : null;
    }

    private boolean saveUpdate(SurfaceDetector.Update update) {
        try {
            for (BumpDetector.Detection detection : update.singles()) {
                Location location = locationAt(detection.elapsedMillis());
                if (location == null) { lastAction = "Messung verworfen: passende Position fehlt."; continue; }
                long id = store.insert(location, detection);
                OsmEnricher.enqueue(this, id);
                lastAction = "Verdachtsstelle erfasst – noch ungeprüft.";
            }
            if (update.section() != null) {
                SurfaceDetector.Section section = update.section();
                Location start = locationAt(section.startMillis()), end = locationAt(section.endMillis());
                if (start == null || end == null) { lastAction = "Abschnitt verworfen: passende Position fehlt."; return true; }
                long id = store.insertSection(start, end, section);
                OsmEnricher.enqueue(this, id);
                lastAction = "Unebener Straßenabschnitt gespeichert – bitte nach der Fahrt prüfen.";
            }
            return true;
        } catch (RuntimeException e) {
            status = "Speichern fehlgeschlagen. Aufzeichnung beendet; Speicher prüfen.";
            stopSelf();
            return false;
        }
    }

    @Override public void onLocationChanged(Location location) {
        latest = new Location(location);
        if (running && location.hasAccuracy()) {
            RoutePoint point = routeTracker.accept(location.getTime(), location.getElapsedRealtimeNanos() / 1_000_000L,
                    SystemClock.elapsedRealtime(), location.getLatitude(), location.getLongitude(), location.getAccuracy());
            if (point != null) {
                try { store.insertRoutePoint(point); }
                catch (RuntimeException e) {
                    status = "Fahrtspur konnte nicht gespeichert werden. Fahrt beendet; Speicher prüfen.";
                    stopSelf(); return;
                }
            }
        }
        refreshStatus();
    }
    @Override public void onProviderDisabled(String provider) { latest = null; refreshStatus(); }
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private Notification notification(String text) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), flags);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, RecordingService.class).setAction(STOP), flags);
        PendingIntent mark = PendingIntent.getService(this, 2, new Intent(this, RecordingService.class).setAction(MARK), flags);
        return new Notification.Builder(this, CHANNEL).setSmallIcon(de.fahrradsucht.bodenwelle.R.drawable.ic_bike)
                .setContentTitle("Bodenwelle · Fahrt läuft").setContentText(text).setContentIntent(open)
                .setOngoing(true).setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(null, "Markieren", mark).build())
                .addAction(new Notification.Action.Builder(null, "Beenden", stop).build()).build();
    }

    private void updateNotification() {
        if (!running) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastNotification < 1_000) return;
        lastNotification = now;
        getSystemService(NotificationManager.class).notify(1, notification(status));
    }

    @Override public void onDestroy() {
        running = false;
        if (store != null) saveUpdate(detector.finish());
        handler.removeCallbacksAndMessages(null);
        if (sensors != null) sensors.unregisterListener(this);
        if (locationSource != null) locationSource.stop();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (store != null) store.close();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
    private void failStart(String message) {
        status = message; startFailure = message; stopSelf();
    }

    public static void prepareStart() {
        startFailure = ""; status = "Fahrt wird gestartet …";
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
