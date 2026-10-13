package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.location.Location;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

/** Uses the same native provider selection and fix quality as the recorder. */
public final class MapLocationProvider implements IMyLocationProvider {
    private final DeviceLocationSource source;
    private final Runnable changed;
    private IMyLocationConsumer consumer;

    public MapLocationProvider(Context context, Runnable changed) {
        this.changed = changed;
        source = new DeviceLocationSource(context, fix -> {
            if (consumer != null) consumer.onLocationChanged(fix, this);
            this.changed.run();
        });
    }
    @Override public boolean startLocationProvider(IMyLocationConsumer consumer) {
        this.consumer = consumer; return source.start();
    }
    @Override public void stopLocationProvider() { source.stop(); consumer = null; }
    @Override public Location getLastKnownLocation() { return source.latest(); }
    @Override public void destroy() { stopLocationProvider(); }
}
