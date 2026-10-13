package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Shared native location acquisition for the map and recording service. */
public final class DeviceLocationSource implements LocationListener {
    private final Context context;
    private final LocationManager manager;
    private final Consumer<Location> listener;
    private Location latest;
    private boolean started;

    public DeviceLocationSource(Context context, Consumer<Location> listener) {
        this.context = context; this.listener = listener;
        manager = context.getSystemService(LocationManager.class);
    }

    public static List<String> enabledProviders(LocationManager manager) {
        List<String> providers = new ArrayList<>();
        List<String> enabled = manager.getProviders(true);
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, "fused", LocationManager.NETWORK_PROVIDER})
            if (enabled.contains(provider)) providers.add(provider);
        return providers;
    }

    public boolean start() {
        if (started) return true;
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return false;
        if (enabledProviders(manager).isEmpty()) return false;
        // Also register available but currently disabled providers, so GPS can recover
        // during the ride without restarting or discarding its existing trace.
        List<String> available = manager.getAllProviders();
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, "fused", LocationManager.NETWORK_PROVIDER}) {
            if (!available.contains(provider)) continue;
            try {
                manager.requestLocationUpdates(provider, 1_000, 0, this, Looper.getMainLooper());
                started = true;
                // Reuse only a recent fix; an old cached position is not a current position.
                Location known = manager.getLastKnownLocation(provider);
                if (known != null) onLocationChanged(known);
            } catch (SecurityException | IllegalArgumentException ignored) {
                // A provider may be unavailable to this permission level; try the others.
            }
        }
        return started;
    }

    public void stop() {
        manager.removeUpdates(this); started = false;
    }

    public Location latest() { return latest == null ? null : new Location(latest); }

    public static long ageMillis(Location location) {
        return (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1_000_000L;
    }

    public static boolean prefer(Location candidate, Location current) {
        if (candidate == null || !candidate.hasAccuracy() || !Float.isFinite(candidate.getAccuracy())
                || candidate.getAccuracy() < 0 || !Double.isFinite(candidate.getLatitude())
                || !Double.isFinite(candidate.getLongitude()) || Math.abs(candidate.getLatitude()) > 90
                || Math.abs(candidate.getLongitude()) > 180 || ageMillis(candidate) < 0 || ageMillis(candidate) > 10_000) return false;
        if (current == null || ageMillis(current) > 10_000) return true;
        long delta = (candidate.getElapsedRealtimeNanos() - current.getElapsedRealtimeNanos()) / 1_000_000L;
        if (delta >= 0 && candidate.getAccuracy() <= current.getAccuracy() * 1.5f + 5) return true;
        return delta >= -3_000 && candidate.getAccuracy() < current.getAccuracy();
    }

    @Override public void onLocationChanged(Location location) {
        if (!started || !prefer(location, latest)) return;
        latest = new Location(location);
        listener.accept(new Location(latest));
    }
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
}
