package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSensor;
import org.robolectric.shadows.ShadowSensorManager;
import org.robolectric.shadows.ShadowSystemClock;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class RecordingWorkflowTest {
    private Application app;
    private LocationManager manager;
    private Sensor sensor;
    private SensorManager sensors;
    private ServiceController<RecordingService> service;

    @Before public void before() {
        app = RuntimeEnvironment.getApplication();
        app.deleteDatabase("findings.db");
        RecordingService.running = false;
        RecordingService.status = "Noch keine Fahrt gestartet.";
        RecordingService.lastAction = "";
        RecordingService.startFailure = "";
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.POST_NOTIFICATIONS");
        manager = app.getSystemService(LocationManager.class);
        Shadows.shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        Shadows.shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        sensors = app.getSystemService(SensorManager.class);
        sensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER);
        Shadows.shadowOf(sensors).addSensor(sensor);
    }
    @After public void after() {
        if (service != null) service.destroy();
        app.deleteDatabase("findings.db");
        RecordingService.running = false;
    }

    private RecordingService start() {
        service = Robolectric.buildService(RecordingService.class).create();
        service.get().onStartCommand(new Intent(app, RecordingService.class), 0, 1);
        return service.get();
    }
    private Location fix(String provider, float accuracy) {
        Location location = new Location(provider);
        location.setLatitude(52.5); location.setLongitude(13.4);
        location.setAccuracy(accuracy); location.setSpeed(5);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return location;
    }
    @Test public void foregroundServiceActuallyRecordsGpsRouteAndManualFinding() {
        RecordingService recording = start();
        assertTrue(RecordingService.running);
        assertNotNull(Shadows.shadowOf(recording).getLastForegroundNotification());
        assertTrue(Shadows.shadowOf(sensors).hasListener(recording));
        Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.GPS_PROVIDER, 5));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        recording.onStartCommand(new Intent(app, RecordingService.class).setAction(RecordingService.MARK), 0, 2);
        try (FindingStore store = new FindingStore(app)) {
            assertEquals(1, store.routeCount());
            assertEquals(1, store.count());
            assertEquals("manual", store.all().get(0).source());
        }
    }
    @Test public void realSensorCallbackPathCreatesAutomaticFinding() {
        start();
        Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.GPS_PROVIDER, 5));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        for (int i = 0; i < 300; i++) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(10));
            SensorEvent event = ShadowSensorManager.createSensorEvent(3, Sensor.TYPE_ACCELEROMETER);
            event.timestamp = SystemClock.elapsedRealtimeNanos();
            event.values[0] = 0; event.values[1] = 0;
            event.values[2] = i == 250 ? 17.81f : 9.81f;
            Shadows.shadowOf(sensors).sendSensorEventToListeners(event, sensor);
        }
        try (FindingStore store = new FindingStore(app)) {
            assertEquals(1, store.count());
            assertEquals("automatic", store.all().get(0).source());
        }
    }
    @Test public void networkPositionCanStartRideWhileGpsFixIsUnavailable() {
        Shadows.shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, false);
        start();
        assertTrue(RecordingService.running);
        Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.NETWORK_PROVIDER, 20));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        try (FindingStore store = new FindingStore(app)) { assertEquals(1, store.routeCount()); }
    }
    @Test public void startButtonExplainsDisabledLocationBeforeRequestingService() {
        Shadows.shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, false);
        Shadows.shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            Button button = findButton(controller.get().findViewById(android.R.id.content), "Fahrt starten");
            assertNotNull(button); button.performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNull(Shadows.shadowOf(app).getNextStartedService());
            assertNotNull(Shadows.shadowOf(app).getLatestAlertDialog());
        }
    }

    @Test public void startButtonStartsNativeServiceThenOpensMap() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            findButton(controller.get().findViewById(android.R.id.content), "Fahrt starten").performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Intent request = Shadows.shadowOf(app).getNextStartedService();
            assertNotNull(request);
            service = Robolectric.buildService(RecordingService.class).create();
            service.get().onStartCommand(request, 0, 1);
            assertTrue(RecordingService.running);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            Intent screen = Shadows.shadowOf(controller.get()).getNextStartedActivity();
            assertNotNull(screen);
            assertEquals(MapActivity.class.getName(), screen.getComponent().getClassName());
            assertTrue(controller.get().findViewById(R.id.ride_start_status) instanceof android.widget.TextView);
        }
    }

    @Test public void permissionCallbackWaitsForVisibleActivityBeforeServiceStart() {
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            findButton(controller.get().findViewById(android.R.id.content), "Fahrt starten").performClick();
            controller.pause();
            Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
            controller.get().onRequestPermissionsResult(10, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    new int[]{android.content.pm.PackageManager.PERMISSION_GRANTED});
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNull(Shadows.shadowOf(app).getNextStartedService());
            controller.resume();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(Shadows.shadowOf(app).getNextStartedService());
        }
    }

    @Test public void failedSensorRegistrationIsShownDirectlyAtStartButton() {
        Shadows.shadowOf(sensors).setForceListenersToFail(true);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            findButton(controller.get().findViewById(android.R.id.content), "Fahrt starten").performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Intent request = Shadows.shadowOf(app).getNextStartedService();
            assertNotNull(request);
            service = Robolectric.buildService(RecordingService.class).create();
            service.get().onStartCommand(request, 0, 1);
            assertFalse(RecordingService.running);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            android.widget.TextView message = controller.get().findViewById(R.id.ride_start_status);
            assertTrue(message.getText().toString().contains("Beschleunigungssensor"));
            assertTrue(controller.get().findViewById(R.id.ride_start_button).isEnabled());
            assertNotNull(Shadows.shadowOf(app).getLatestAlertDialog());
        }
    }

    private Button findButton(View view, String text) {
        if (view instanceof Button && text.contentEquals(((Button) view).getText())) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = findButton(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
