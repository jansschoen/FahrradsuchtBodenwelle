package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Native OSM map; only tile requests leave the device, not route or finding records. */
public final class RideMapPanel extends LinearLayout {
    private static final int TEAL = Color.rgb(8, 127, 115);
    private final Activity activity;
    private final FindingStore store;
    private final MapView map;
    private final MyLocationNewOverlay position;
    private final MapLocationProvider provider;
    private final TextView locationStatus;
    private final TextView routeStatus;
    private final Runnable requestLocation;
    private final Consumer<Finding> review;
    private final List<Polyline> lines = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();
    private List<RoutePoint> points = new ArrayList<>();
    private int shownRouteCount = -1;
    private int shownFindingCount = -1;
    private boolean initializedCenter;

    public RideMapPanel(Activity activity, FindingStore store, Runnable requestLocation, Consumer<Finding> review,
                        boolean fullScreen) {
        super(activity);
        this.activity = activity; this.store = store; this.requestLocation = requestLocation; this.review = review;
        setOrientation(VERTICAL);
        File base = new File(activity.getNoBackupFilesDir(), "osm");
        File cache = new File(base, "tiles");
        if (!cache.isDirectory() && !cache.mkdirs()) throw new IllegalStateException("Map cache unavailable");
        IConfigurationProvider config = Configuration.getInstance();
        config.setOsmdroidBasePath(base);
        config.setOsmdroidTileCache(cache);
        config.setUserAgentValue("Bodenwelle/0.2.1 (de.fahrradsucht.bodenwelle; https://github.com/jansschoen/FahrradsuchtBodenwelle)");
        config.setTileDownloadThreads((short) 2);
        config.setTileFileSystemCacheMaxBytes(60 * 1024 * 1024L);
        config.setTileFileSystemCacheTrimBytes(48 * 1024 * 1024L);
        map = new MapView(activity);
        map.setTileSource(new XYTileSource("OSM", 0, 19, 256, ".png",
                new String[]{"https://tile.openstreetmap.org/"}, "© OpenStreetMap contributors",
                new TileSourcePolicy(2, TileSourcePolicy.FLAG_NO_BULK | TileSourcePolicy.FLAG_NO_PREVENTIVE
                        | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL)));
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        map.getController().setZoom(16.0);
        map.getController().setCenter(new GeoPoint(52.5200, 13.4050));
        map.setContentDescription(activity.getString(R.string.map_description));
        // Let map gestures pan/zoom without the surrounding page consuming them.
        FrameLayout frame = new FrameLayout(activity) {
            @Override public boolean onInterceptTouchEvent(MotionEvent event) {
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(
                        event.getActionMasked() != MotionEvent.ACTION_UP && event.getActionMasked() != MotionEvent.ACTION_CANCEL);
                return super.onInterceptTouchEvent(event);
            }
        };
        frame.addView(map, new FrameLayout.LayoutParams(-1, -1));
        addView(frame, fullScreen ? new LayoutParams(-1, 0, 1) : new LayoutParams(-1, dp(280)));
        provider = new MapLocationProvider(activity, () -> post(this::refreshPositionStatus));
        position = new MyLocationNewOverlay(provider, map);
        position.setDrawAccuracyEnabled(true);
        position.enableFollowLocation();
        map.getOverlays().add(position);
        locationStatus = text(activity.getString(R.string.map_waiting), 14);
        locationStatus.setId(R.id.location_accuracy);
        locationStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        routeStatus = text("", 14);
        LinearLayout controls = new LinearLayout(activity);
        controls.setOrientation(HORIZONTAL);
        controls.addView(control(R.string.my_location, this::followLocation), new LayoutParams(0, -2, 1));
        controls.addView(control(R.string.show_route, this::showRoute), new LayoutParams(0, -2, 1));
        addView(controls);
        text(activity.getString(R.string.map_legend), 12);
        TextView attribution = text(activity.getString(R.string.osm_attribution), 12);
        attribution.setMinHeight(dp(40));
        attribution.setTextColor(TEAL);
        attribution.setOnClickListener(v -> activity.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.openstreetmap.org/copyright"))));
        refresh();
    }

    public void resume() {
        map.onResume();
        enablePosition();
        refresh();
    }
    public void pause() { position.disableMyLocation(); map.onPause(); }
    public void destroy() { pause(); map.onDetach(); }

    private void enablePosition() {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try { position.enableMyLocation(); }
            catch (SecurityException e) { locationStatus.setText(R.string.map_permission); }
        } else locationStatus.setText(R.string.map_permission);
    }

    private void followLocation() {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocation.run(); return;
        }
        enablePosition();
        position.enableFollowLocation();
        map.getController().setZoom(17.0);
        Location fix = provider.getLastKnownLocation();
        if (fix != null && age(fix) <= 10_000) map.getController().animateTo(new GeoPoint(fix));
        else locationStatus.setText(R.string.map_waiting);
    }

    private static long age(Location fix) { return (SystemClock.elapsedRealtimeNanos() - fix.getElapsedRealtimeNanos()) / 1_000_000L; }

    public void refresh() {
        int count = store.routeCount();
        if (shownRouteCount != count) {
            shownRouteCount = count;
            points = store.route();
            map.getOverlays().removeAll(lines); lines.clear();
            List<GeoPoint> segment = new ArrayList<>();
            int currentSegment = -1;
            for (RoutePoint point : points) {
                if (currentSegment != point.segment()) {
                    addLine(segment); segment = new ArrayList<>(); currentSegment = point.segment();
                }
                segment.add(new GeoPoint(point.latitude(), point.longitude()));
            }
            addLine(segment);
            if (!initializedCenter && !points.isEmpty()) {
                RoutePoint last = points.get(points.size() - 1);
                map.getController().setCenter(new GeoPoint(last.latitude(), last.longitude()));
                initializedCenter = true;
            }
        }
        int findings = store.count();
        if (shownFindingCount != findings) {
            shownFindingCount = findings;
            for (Marker marker : markers) { marker.closeInfoWindow(); marker.onDetach(map); }
            map.getOverlays().removeAll(markers); markers.clear();
            int shown = 0;
            for (Finding finding : store.all()) {
                if (shown++ >= 500) break;
                Marker marker = new Marker(map);
                marker.setPosition(new GeoPoint(finding.latitude(), finding.longitude()));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                GradientDrawable icon = new GradientDrawable();
                icon.setShape(GradientDrawable.OVAL);
                icon.setSize(dp(20), dp(20));
                icon.setColor(finding.reviewed() ? Color.rgb(22, 109, 56) : Color.rgb(189, 87, 10));
                icon.setStroke(dp(2), Color.WHITE);
                marker.setIcon(icon);
                marker.setTitle(finding.reviewed() ? "Geprüfte Fundstelle" : "Ungeprüfte Fundstelle");
                marker.setSnippet(finding.category() + (finding.note().isEmpty() ? "" : " · " + finding.note()));
                marker.setOnMarkerClickListener((m, view) -> {
                    if (review != null && !RecordingService.running) review.accept(finding);
                    else m.showInfoWindow();
                    return true;
                });
                markers.add(marker); map.getOverlays().add(marker);
            }
        }
        refreshPositionStatus();
        routeStatus.setText(points.isEmpty() ? R.string.route_empty : (RecordingService.running ? R.string.route_running : R.string.route_saved));
        // Draw the position over the route and finding markers.
        map.getOverlays().remove(position); map.getOverlays().add(position);
        map.invalidate();
    }

    private void refreshPositionStatus() {
        if (locationStatus == null) return;
        boolean fine = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Location fix = provider.getLastKnownLocation();
        locationStatus.setTextColor(Color.rgb(27, 48, 46));
        if (!fine && !coarse) locationStatus.setText(R.string.map_permission);
        else if (DeviceLocationSource.enabledProviders(activity.getSystemService(LocationManager.class)).isEmpty())
            locationStatus.setText(R.string.map_location_disabled);
        else if (fix == null) locationStatus.setText(R.string.map_waiting);
        else if (age(fix) < 0 || age(fix) > 10_000) {
            locationStatus.setText(activity.getString(R.string.map_accuracy_stale, fix.getAccuracy(), Math.max(0, age(fix) / 1000.0)));
            locationStatus.setTextColor(Color.rgb(143, 72, 7));
        } else {
            locationStatus.setText(activity.getString(fine ? R.string.map_accuracy_live : R.string.map_accuracy_approximate,
                    fix.getAccuracy(), age(fix) / 1000.0));
            locationStatus.setTextColor(fix.getAccuracy() <= 25 ? TEAL : Color.rgb(143, 72, 7));
        }
    }

    private void addLine(List<GeoPoint> segment) {
        if (segment.size() < 2) return;
        Polyline line = new Polyline(map);
        line.setPoints(segment);
        line.getOutlinePaint().setColor(TEAL);
        line.getOutlinePaint().setStrokeWidth(dp(4));
        lines.add(line); map.getOverlays().add(line);
    }

    public void showFinding(Finding finding) {
        showCoordinate(finding.latitude(), finding.longitude());
    }

    public void showCoordinate(double latitude, double longitude) {
        position.disableFollowLocation();
        map.getController().setZoom(18.0);
        map.getController().setCenter(new GeoPoint(latitude, longitude));
    }

    private void showRoute() {
        if (points.isEmpty()) { routeStatus.setText(R.string.route_empty); return; }
        position.disableFollowLocation();
        List<GeoPoint> path = new ArrayList<>();
        for (RoutePoint p : points) path.add(new GeoPoint(p.latitude(), p.longitude()));
        if (path.size() < 2) map.getController().animateTo(path.get(0));
        else map.zoomToBoundingBox(BoundingBox.fromGeoPoints(path), true, dp(24), 18.0, null);
    }

    private TextView text(String value, int size) {
        TextView text = new TextView(activity);
        text.setText(value); text.setTextSize(size); text.setTextColor(Color.rgb(27, 48, 46));
        text.setPadding(dp(4), dp(4), dp(4), dp(4)); addView(text);
        return text;
    }
    private Button control(int label, Runnable action) {
        Button button = new Button(activity);
        button.setText(label); button.setTextSize(14); button.setAllCaps(false); button.setMinHeight(dp(48));
        button.setTextColor(TEAL);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(232, 242, 238)));
        button.setOnClickListener(v -> action.run()); return button;
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
