package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
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
import android.widget.ScrollView;
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
    private SensitivityControl sensitivity;
    private final TextView calibration;
    private TextView optionCalibration;
    private AlertDialog options;
    private long shownRevision = -1;
    private final List<Polyline> lines = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();
    private final List<Polyline> sectionLines = new ArrayList<>();
    private List<RoutePoint> points = new ArrayList<>();
    private int shownRouteCount = -1;
    private int shownFindingCount = -1;
    private boolean initializedCenter;
    private List<FindingBundle> savedBundles = new ArrayList<>();
    private List<MapClusters.Cluster> clusters = new ArrayList<>();
    private double shownZoom = -1;
    private final java.util.LinkedHashSet<Long> selected = new java.util.LinkedHashSet<>();
    private boolean selecting;
    private final LinearLayout selectionControls;
    private final TextView selectionStatus;
    private final Button mergeSelection;
    private final Runnable zoomRefresh = this::refresh;

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
        config.setUserAgentValue("Bodenwelle/" + BuildConfig.VERSION_NAME + " (de.fahrradsucht.bodenwelle; https://github.com/jansschoen/FahrradsuchtBodenwelle)");
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
        Button optionButton = control(R.string.map_options, this::showOptions);
        optionButton.setId(R.id.map_options);
        controls.addView(optionButton, new LayoutParams(0, -2, 1));
        addView(controls);
        selectionControls = new LinearLayout(activity); selectionControls.setOrientation(VERTICAL);
        selectionStatus = new TextView(activity); selectionStatus.setTextSize(14); selectionControls.addView(selectionStatus);
        LinearLayout selectionButtons = new LinearLayout(activity); selectionControls.addView(selectionButtons);
        mergeSelection = control("Auswahl bündeln", () -> BundleConfirmation.show(activity, store,
                new ArrayList<>(selected), () -> { selecting = false; selected.clear(); shownZoom = -1; refresh(); }));
        mergeSelection.setId(R.id.map_bundle_confirm);
        selectionButtons.addView(mergeSelection, new LayoutParams(0, -2, 1));
        Button cancelSelection = control("Abbrechen", () -> { selecting = false; selected.clear(); shownZoom = -1; refresh(); });
        cancelSelection.setId(R.id.map_bundle_cancel); selectionButtons.addView(cancelSelection, new LayoutParams(0, -2, 1));
        addView(selectionControls); updateSelectionControls();
        map.addMapListener(new org.osmdroid.events.MapListener() {
            @Override public boolean onScroll(org.osmdroid.events.ScrollEvent event) { return false; }
            @Override public boolean onZoom(org.osmdroid.events.ZoomEvent event) {
                removeCallbacks(zoomRefresh); postDelayed(zoomRefresh, 120); return false;
            }
        });
        calibration = text(RecordingService.calibrationStatus, 14);
        calibration.setId(R.id.calibration_status);
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
    public void destroy() { removeCallbacks(zoomRefresh); if (options != null) options.dismiss(); pause(); map.onDetach(); }

    private void showOptions() {
        if (options != null && options.isShowing()) return;
        LinearLayout content = new LinearLayout(activity); content.setOrientation(VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(12));
        Button route = control(R.string.show_route, () -> { showRoute(); if (options != null) options.dismiss(); });
        content.addView(route);
        Button bundle = control("Punkte auf der Karte bündeln", () -> {
            if (options != null) options.dismiss(); startBundling(null);
        }); bundle.setId(R.id.map_bundle_start); content.addView(bundle);
        sensitivity = new SensitivityControl(activity); sensitivity.expand(); content.addView(sensitivity);
        TextView sectionHint = new TextView(activity);
        sectionHint.setText(R.string.section_sensitivity_hint);
        sectionHint.setTextSize(14); content.addView(sectionHint);
        optionCalibration = new TextView(activity); optionCalibration.setTextSize(14);
        optionCalibration.setText(RecordingService.running ? RecordingService.calibrationStatus
                : activity.getString(R.string.calibration_start_hint)); content.addView(optionCalibration);
        Button calibrate = control(R.string.recalibrate, () -> activity.startService(
                new Intent(activity, RecordingService.class).setAction(RecordingService.CALIBRATE)));
        calibrate.setId(R.id.recalibrate); calibrate.setEnabled(RecordingService.running); content.addView(calibrate);
        TextView legend = new TextView(activity); legend.setText(R.string.map_legend); legend.setTextSize(14);
        legend.setPadding(0, dp(12), 0, 0); content.addView(legend);
        ScrollView scroll = new ScrollView(activity); scroll.addView(content);
        options = new AlertDialog.Builder(activity).setTitle("Kartenoptionen").setView(scroll).setPositiveButton("Schließen", null).create();
        options.setOnDismissListener(d -> { if (options == d) { sensitivity = null; optionCalibration = null; options = null; } });
        options.show();
    }

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
        if (sensitivity != null) sensitivity.refresh();
        if (calibration != null) {
            String status = RecordingService.calibrationStatus;
            calibration.setText(status.startsWith("Kalibriert") && !status.contains("wartet")
                    ? status.contains("feste Lage") ? "Kalibriert · feste Lage" : "Kalibriert · vertikale Messung" : status);
            calibration.setVisibility(RecordingService.running ? View.VISIBLE : View.GONE);
        }
        if (optionCalibration != null && RecordingService.running) optionCalibration.setText(RecordingService.calibrationStatus);
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
        boolean changed = shownFindingCount != findings || shownRevision != store.revision();
        if (changed) {
            shownRevision = store.revision(); shownFindingCount = findings;
            savedBundles = store.bundles();
            selected.retainAll(savedBundles.stream().map(FindingBundle::key).collect(java.util.stream.Collectors.toSet()));
        }
        double zoom = map.getZoomLevelDouble();
        if (changed || Math.abs(zoom - shownZoom) > .01) {
            shownZoom = zoom; rebuildMarkers();
        }
        updateSelectionControls();
        refreshPositionStatus();
        routeStatus.setText(points.isEmpty() ? R.string.route_summary_empty : RecordingService.running ? R.string.route_running : R.string.route_saved);
        // Keep findings tappable even while standing directly on a hotspot.
        map.getOverlays().remove(position); map.getOverlays().add(0, position);
        map.invalidate();
    }

    private void rebuildMarkers() {
        for (Marker marker : markers) { marker.closeInfoWindow(); marker.onDetach(map); }
        map.getOverlays().removeAll(markers); markers.clear();
        map.getOverlays().removeAll(sectionLines); sectionLines.clear();
        clusters = MapClusters.group(savedBundles, shownZoom, org.osmdroid.util.TileSystem.getTileSize(), dp(44));
        // Show every saved section, including originals inside confirmed bundles.
        for (FindingBundle bundle : savedBundles) for (Finding finding : bundle.observations()) {
            RoughSection section = finding.details().section();
            if (section == null) continue;
            Polyline line = new Polyline(map);
            line.setPoints(java.util.Arrays.asList(new GeoPoint(finding.latitude(), finding.longitude()),
                    new GeoPoint(section.endLatitude(), section.endLongitude())));
            line.getOutlinePaint().setColor(Color.rgb(207, 109, 17)); line.getOutlinePaint().setStrokeWidth(dp(7));
            line.setOnClickListener((l, view, point) -> {
                if (selecting) toggle(bundle.key()); else if (review != null) review.accept(finding); return true;
            });
            sectionLines.add(line); map.getOverlays().add(line);
        }
        for (MapClusters.Cluster cluster : clusters) {
            Finding finding = cluster.representative(); Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(finding.latitude(), finding.longitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            String label = cluster.count() == 1 ? "#" + finding.id() : "×" + cluster.count();
            boolean marked = cluster.bundles().stream().anyMatch(b -> selected.contains(b.key()));
            marker.setIcon(new MarkerBadge(label, cluster.intensity(), marked));
            marker.setRelatedObject(cluster);
            boolean released = cluster.bundles().stream().allMatch(b -> b.reviewedCount() == b.observations().size());
            marker.setTitle((released ? "Geprüfte Fundstelle" : "Fundstelle mit offenen Beobachtungen")
                    + " · " + cluster.count() + " Messung(en)" + (marked ? " · ausgewählt" : ""));
            marker.setSnippet(cluster.bundles().stream().map(FindingBundle::references).collect(java.util.stream.Collectors.joining(", ")));
            marker.setOnMarkerClickListener((m, view) -> {
                choose(MapClusters.atMarker(cluster, clusters, shownZoom, org.osmdroid.util.TileSystem.getTileSize(), dp(50)));
                return true;
            });
            markers.add(marker); map.getOverlays().add(marker);
        }
    }

    private void choose(List<FindingBundle> bundles) {
        if (bundles.size() == 1) {
            if (selecting) toggle(bundles.get(0).key());
            else if (review != null) review.accept(bundles.get(0).representative());
            return;
        }
        String[] labels = bundles.stream().map(b -> b.references() + " · " + b.title()
                + " · " + b.observations().size() + " Messung(en)").toArray(String[]::new);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(selecting ? "Fundstellen auswählen" : "Nahe Fundstellen")
                .setNegativeButton("Schließen", null);
        if (selecting) {
            boolean[] checked = new boolean[bundles.size()];
            for (int i = 0; i < checked.length; i++) checked[i] = selected.contains(bundles.get(i).key());
            builder.setMultiChoiceItems(labels, checked, (d, index, value) -> checked[index] = value)
                    .setPositiveButton("Auswahl übernehmen", (d, w) -> {
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) selected.add(bundles.get(i).key()); else selected.remove(bundles.get(i).key());
                        }
                        shownZoom = -1; refresh();
                    });
        } else builder.setItems(labels, (d, index) -> { if (review != null) review.accept(bundles.get(index).representative()); });
        builder.show();
    }
    private void toggle(long key) {
        if (!selected.remove(key)) selected.add(key);
        shownZoom = -1; refresh();
    }
    public void startBundling(Finding finding) {
        selecting = true; selected.clear(); if (finding != null) selected.add(finding.bundleKey());
        shownZoom = -1; refresh();
    }
    private void updateSelectionControls() {
        if (selectionControls == null) return;
        selectionControls.setVisibility(selecting ? View.VISIBLE : View.GONE);
        selectionStatus.setText(activity.getResources().getQuantityString(R.plurals.map_bundle_selection, selected.size(), selected.size()));
        mergeSelection.setEnabled(selected.size() >= 2);
    }
    public void saveSelection(Bundle state) {
        state.putBoolean("mapSelecting", selecting);
        state.putLongArray("mapSelected", selected.stream().mapToLong(Long::longValue).toArray());
    }
    public void restoreSelection(Bundle state) {
        if (state == null) return; selecting = state.getBoolean("mapSelecting"); selected.clear();
        long[] keys = state.getLongArray("mapSelected"); if (keys != null) for (long key : keys) selected.add(key);
        shownZoom = -1; refresh();
    }
    private final class MarkerBadge extends GradientDrawable {
        private final String label;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        MarkerBadge(String label, Intensity intensity, boolean selected) {
            this.label = label; paint.setTextSize(dp(13)); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(intensity.level() <= 1 || intensity.level() == 5 ? Color.WHITE : Color.rgb(27, 48, 46));
            setColor(intensity.color()); setCornerRadius(dp(18));
            setSize(Math.max(dp(36), (int) paint.measureText(label) + dp(18)), dp(36));
            setStroke(dp(selected ? 4 : 2), selected ? TEAL : Color.WHITE);
        }
        @Override public void draw(Canvas canvas) {
            super.draw(canvas); android.graphics.Rect bounds = getBounds();
            canvas.drawText(label, bounds.exactCenterX(), bounds.exactCenterY() - (paint.ascent() + paint.descent()) / 2, paint);
        }
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
    private Button control(int label, Runnable action) { return control(activity.getString(label), action); }
    private Button control(String label, Runnable action) {
        Button button = new Button(activity);
        button.setText(label); button.setTextSize(14); button.setAllCaps(false); button.setMinHeight(dp(48));
        button.setTextColor(TEAL);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(232, 242, 238)));
        button.setOnClickListener(v -> action.run()); return button;
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
