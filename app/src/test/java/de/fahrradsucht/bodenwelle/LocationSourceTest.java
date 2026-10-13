package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.app.Application;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.TextView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class LocationSourceTest {
    private Application app;
    private LocationManager manager;
    private DeviceLocationSource source;
    private final List<Location> received = new ArrayList<>();
    @Before public void before() {
        app = RuntimeEnvironment.getApplication();
        app.deleteDatabase("findings.db");
        RecordingService.running = false;
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
        manager = app.getSystemService(LocationManager.class);
        Shadows.shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        Shadows.shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        source = new DeviceLocationSource(app, received::add);
    }
    @After public void after() { source.stop(); app.deleteDatabase("findings.db"); }
    private Location fix(String provider, float accuracy) {
        Location fix = new Location(provider); fix.setLatitude(52.5); fix.setLongitude(13.4);
        fix.setAccuracy(accuracy); fix.setTime(System.currentTimeMillis());
        fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos()); return fix;
    }
    @Test public void recentCachedFixIsDeliveredImmediately() {
        Shadows.shadowOf(manager).setLastKnownLocation(LocationManager.GPS_PROVIDER, fix(LocationManager.GPS_PROVIDER, 8));
        assertTrue(source.start());
        assertEquals(1, received.size()); assertEquals(8, received.get(0).getAccuracy(), 0);
    }
    @Test public void staleCachedFixIsNotPresentedAsCurrent() {
        Location stale = fix(LocationManager.GPS_PROVIDER, 8);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20));
        Shadows.shadowOf(manager).setLastKnownLocation(LocationManager.GPS_PROVIDER, stale);
        source.start(); assertTrue(received.isEmpty());
    }
    @Test public void inaccurateNetworkFixCannotDisplaceFreshAccurateGps() {
        source.start();
        Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.GPS_PROVIDER, 5));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.NETWORK_PROVIDER, 200));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(5, source.latest().getAccuracy(), 0);
    }
    @Test public void gpsEnabledDuringRideBecomesAvailableWithoutRestart() {
        Shadows.shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, false);
        assertTrue(source.start());
        Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.NETWORK_PROVIDER, 200));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        Shadows.shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.GPS_PROVIDER, 6));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(6, source.latest().getAccuracy(), 0);
    }
    @Test public void mapShowsLiveAccuracyAndLabelsOldMeasurement() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.GPS_PROVIDER, 12));
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            TextView accuracy = controller.get().findViewById(R.id.location_accuracy);
            assertTrue(accuracy.getText().toString().contains("12 m"));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11));
            assertTrue(accuracy.getText().toString().contains("veraltet"));
            Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.GPS_PROVIDER, 4));
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertTrue(accuracy.getText().toString().contains("4 m"));
            assertFalse(accuracy.getText().toString().contains("veraltet"));
        }
    }
}
