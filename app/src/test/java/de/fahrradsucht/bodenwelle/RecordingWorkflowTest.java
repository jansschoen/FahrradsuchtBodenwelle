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
        new DetectionPreferences(app).setThreshold(40);
        RecordingService.running = false;
        OsmEnricher.enabled(RuntimeEnvironment.getApplication(), false);
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
        for (int i = 0; i < 220; i++) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(10));
            SensorEvent event = ShadowSensorManager.createSensorEvent(3, Sensor.TYPE_ACCELEROMETER);
            event.timestamp = SystemClock.elapsedRealtimeNanos(); event.values[2] = 9.81f;
            Shadows.shadowOf(sensors).sendSensorEventToListeners(event, sensor);
        }
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
    private void sample(float x, float y, float z) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(10));
        SensorEvent event = ShadowSensorManager.createSensorEvent(3, Sensor.TYPE_ACCELEROMETER);
        event.timestamp = SystemClock.elapsedRealtimeNanos();
        event.values[0] = x; event.values[1] = y; event.values[2] = z;
        Shadows.shadowOf(sensors).sendSensorEventToListeners(event, sensor);
    }
    @Test public void movingStartWaitsForStandstillAndHorizontalAccelerationDoesNotCreateFindings() {
        service = Robolectric.buildService(RecordingService.class).create();
        service.get().onStartCommand(new Intent(app, RecordingService.class), 0, 1);
        service.get().onLocationChanged(fix(LocationManager.GPS_PROVIDER, 5));
        for (int i = 0; i < 220; i++) sample(0, 0, 9.81f);
        assertTrue(RecordingService.calibrationStatus.startsWith("Kalibrierung"));
        Location stopped = fix(LocationManager.GPS_PROVIDER, 5); stopped.setSpeed(0);
        service.get().onLocationChanged(stopped);
        for (int i = 0; i < 220; i++) sample(0, 0, 9.81f);
        assertTrue(RecordingService.calibrationStatus.startsWith("Kalibriert"));
        service.get().onLocationChanged(fix(LocationManager.GPS_PROVIDER, 5));
        for (int i = 0; i < 200; i++) sample(60, 0, 9.81f);
        try (FindingStore store = new FindingStore(app)) { assertEquals(0, store.count()); }
        sample(60, 0, 69.81f);
        for (int i = 0; i < 30; i++) sample(60, 0, 9.81f);
        try (FindingStore store = new FindingStore(app)) { assertEquals(1, store.count()); }
    }
    @Test public void continuousRoughnessIsOneUnreviewedSectionAndRideStopSavesIt() {
        RecordingService recording = start();
        for (int i = 0; i < 550; i++) {
            if (i % 100 == 0) {
                Location location = fix(LocationManager.GPS_PROVIDER, 5);
                location.setLatitude(52.5 + (i / 100) * .00005); location.setBearing(0);
                recording.onLocationChanged(location);
            }
            sample(0, 0, i % 2 == 0 ? 69.81f : -50.19f);
        }
        recording.onStartCommand(new Intent(app, RecordingService.class).setAction(RecordingService.STOP), 0, 2);
        try (FindingStore store = new FindingStore(app)) {
            assertEquals(1, store.count()); Finding finding = store.all().get(0);
            assertEquals("rough_road", finding.category()); assertFalse(finding.reviewed());
            assertNotNull(finding.details().section()); assertTrue(finding.details().section().lengthMeters() >= 25);
            assertTrue(finding.details().section().endLatitude() > finding.latitude());
            assertEquals(1, FindingCsv.exportReviewed(store.all()).lines().count());
        }
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
            event.values[2] = i == 250 ? 69.81f : 9.81f;
            Shadows.shadowOf(sensors).sendSensorEventToListeners(event, sensor);
        }
        try (FindingStore store = new FindingStore(app)) {
            assertEquals(1, store.count());
            assertEquals("automatic", store.all().get(0).source());
            assertEquals(40, store.all().get(0).threshold(), 0);
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
    @Test public void thresholdChangeAffectsRunningRecorderWithoutRestart() {
        start();
        Shadows.shadowOf(manager).simulateLocation(fix(LocationManager.GPS_PROVIDER, 5));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        for (int i = 0; i < 380; i++) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(10));
            if (i == 290) new DetectionPreferences(app).setThreshold(15);
            SensorEvent event = ShadowSensorManager.createSensorEvent(3, Sensor.TYPE_ACCELEROMETER);
            event.timestamp = SystemClock.elapsedRealtimeNanos();
            event.values[0] = 0; event.values[1] = 0;
            event.values[2] = i == 250 || i == 320 ? 29.81f : 9.81f;
            Shadows.shadowOf(sensors).sendSensorEventToListeners(event, sensor);
        }
        try (FindingStore store = new FindingStore(app)) {
            assertEquals(1, store.count());
            assertEquals(15, store.all().get(0).threshold(), 0);
        }
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

    @Test public void switchingTabsKeepsTheNativeRecorderAndPreventsBulkDeletionDuringTheRide() {
        RecordingService recording = start(); recording.onLocationChanged(fix(LocationManager.GPS_PROVIDER, 5));
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().findViewById(R.id.nav_more).performClick(); controller.get().findViewById(R.id.menu_data).performClick();
            android.app.AlertDialog menu = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            assertFalse(menu.findViewById(R.id.delete_route).isEnabled()); assertFalse(menu.findViewById(R.id.delete_all).isEnabled());
            assertTrue(RecordingService.running); assertTrue(Shadows.shadowOf(sensors).hasListener(recording)); menu.dismiss();
            controller.get().findViewById(R.id.nav_ride).performClick();
            findButton(controller.get().findViewById(android.R.id.content), "Stelle markieren").performClick();
            Intent mark = Shadows.shadowOf(app).getNextStartedService(); assertEquals(RecordingService.MARK, mark.getAction());
            recording.onStartCommand(mark, 0, 2);
            try (FindingStore store = new FindingStore(app)) { assertEquals(1, store.count()); }
            assertTrue(RecordingService.running);
        }
    }

    @Test public void permissionResultStartsRideEvenWhenUserHasSwitchedToMore() {
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().findViewById(R.id.ride_start_button).performClick(); controller.get().findViewById(R.id.nav_more).performClick();
            Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
            controller.get().onRequestPermissionsResult(10, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, new int[]{android.content.pm.PackageManager.PERMISSION_GRANTED});
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(Shadows.shadowOf(app).getNextStartedService());
            controller.get().findViewById(R.id.nav_ride).performClick(); assertFalse(controller.get().findViewById(R.id.ride_start_button).isEnabled());
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
