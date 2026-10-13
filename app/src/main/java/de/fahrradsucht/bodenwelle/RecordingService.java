package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import java.util.Locale;

public final class RecordingService extends Service implements SensorEventListener, LocationListener {
    public static final String STOP = "de.fahrradsucht.bodenwelle.STOP";
    public static final String MARK = "de.fahrradsucht.bodenwelle.MARK";
    public static volatile boolean running;
    public static volatile String status = "Noch keine Fahrt gestartet.";
    public static volatile String lastAction = "";
    private static final String CHANNEL = "ride";
    private final AccelerationProjector projector = new AccelerationProjector();
    private final BumpDetector detector = new BumpDetector();
    private final RouteTracker routeTracker = new RouteTracker();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SensorManager sensors;
    private LocationManager locations;
    private FindingStore store;
    private Location latest;
    private PowerManager.WakeLock wakeLock;
    private long lastNotification;
    private long startedAt;
    private long lastSensorAt;

    @Override public void onCreate() {
        super.onCreate();
        store = new FindingStore(this);
        sensors = getSystemService(SensorManager.class);
        locations = getSystemService(LocationManager.class);
        getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel(CHANNEL, "Fahrtaufzeichnung", NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) {
            status = "Fahrt beendet. Fundstellen bleiben auf diesem Handy.";
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && MARK.equals(intent.getAction())) {
            if (running && usableLocation()) {
                try {
                    store.insert(latest, null);
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
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status = "Für die Aufzeichnung ist genauer Standortzugriff erforderlich.";
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(1, notification("GPS wird gesucht …"));
        Sensor accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null || !sensors.registerListener(this, accelerometer, 10_000)) {
            status = "Kein nutzbarer Beschleunigungssensor gefunden.";
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            if (!locations.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                status = "Bitte den Standort / GPS am Handy einschalten.";
                stopSelf();
                return START_NOT_STICKY;
            }
            locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000, 0, this, Looper.getMainLooper());
        } catch (SecurityException | IllegalArgumentException e) {
            status = "GPS konnte nicht gestartet werden. Standortberechtigung prüfen.";
            stopSelf();
            return START_NOT_STICKY;
        }
        try { store.clearRoute(); }
        catch (RuntimeException e) {
            status = "Fahrtspur konnte nicht vorbereitet werden. Bitte Speicher prüfen.";
            stopSelf(); return START_NOT_STICKY;
        }
        running = true;
        startedAt = SystemClock.elapsedRealtime();
        status = "GPS wird gesucht …";
        lastAction = "";
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
        if (latest == null) status = "GPS wird gesucht – draußen kurz warten.";
        else if (!usableLocation()) status = "Automatik pausiert: GPS zu ungenau oder veraltet.";
        else if (SystemClock.elapsedRealtime() - startedAt > 3_000 && SystemClock.elapsedRealtime() - lastSensorAt > 3_000)
            status = "Automatik pausiert: keine aktuellen Sensordaten.";
        else if (!eligible()) status = "Automatik pausiert: Geschwindigkeit fehlt, ist ungenau oder unter 5 km/h.";
        else status = String.format(Locale.GERMANY, "Aufzeichnung · %.1f km/h · GPS ± %.0f m", latest.getSpeed() * 3.6, latest.getAccuracy());
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!running) return;
        lastSensorAt = SystemClock.elapsedRealtime();
        double vertical = projector.accept(event.timestamp, event.values[0], event.values[1], event.values[2]);
        BumpDetector.Detection detection = detector.accept(event.timestamp / 1_000_000L, vertical, eligible());
        if (detection != null && latest != null) {
            try {
                store.insert(latest, detection);
                lastAction = "Verdachtsstelle erfasst – noch ungeprüft.";
            } catch (RuntimeException e) {
                status = "Speichern fehlgeschlagen. Aufzeichnung beendet; Speicher prüfen.";
                stopSelf();
            }
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
        handler.removeCallbacksAndMessages(null);
        if (sensors != null) sensors.unregisterListener(this);
        if (locations != null) locations.removeUpdates(this);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (store != null) store.close();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
