package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int PERMISSIONS = 10;
    private static final int EXPORT = 11;
    private static final int MAP_LOCATION = 12;
    private FindingActions findingActions;
    private AppMenus menus;
    private static final int RIDE = 0, FINDINGS = 1, MORE = 2;
    private int selectedTab, renderedTab, findingFilter;
    private final int[] scrollPositions = new int[3];
    private ScrollView contentScroll;
    private static final int GREEN = Color.rgb(8, 127, 115);
    private static final int INK = Color.rgb(27, 48, 46);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.of("Europe/Berlin"));
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FindingStore store;
    private LinearLayout content;
    private TextView rideCount;
    private TextView lastAction;
    private boolean displayedRunning;
    private int displayedCount = -1;
    private int findingLimit = 100;
    private long displayedRevision = -1;
    private boolean pendingStart;
    private RideMapPanel mapPanel;
    private Bundle mapSelection = new Bundle();
    private boolean openMapAfterStart;
    private boolean resumed;
    private boolean startWhenResumed;
    private long serviceRequestedAt;
    private TextView startStatus;
    private Button startButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new FindingStore(this);
        findingActions = new FindingActions(this, store, this::refreshCurrentPage);
        findingActions.restore(state);
        if (state != null && state.getBundle("mapSelection") != null) mapSelection = state.getBundle("mapSelection");
        menus = new AppMenus(this, store, this::refreshCurrentPage);
        if (state != null) {
            selectedTab = Math.max(0, Math.min(2, state.getInt("tab")));
            findingFilter = Math.max(0, Math.min(2, state.getInt("findingFilter")));
            findingLimit = Math.max(100, state.getInt("findingLimit", 100));
            int[] saved = state.getIntArray("scrollPositions");
            if (saved != null && saved.length == 3) System.arraycopy(saved, 0, scrollPositions, 0, 3);
        }
        if (state != null) {
            pendingStart = state.getBoolean("pendingStart");
            openMapAfterStart = state.getBoolean("openMapAfterStart");
            startWhenResumed = state.getBoolean("startWhenResumed");
            serviceRequestedAt = state.getLong("serviceRequestedAt");
        }
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        render();
        if (state == null) PhotoFiles.cleanup(this);
    }

    @Override public void onSaveInstanceState(Bundle state) {
        state.putBoolean("pendingStart", pendingStart);
        state.putBoolean("openMapAfterStart", openMapAfterStart);
        state.putBoolean("startWhenResumed", startWhenResumed);
        state.putLong("serviceRequestedAt", serviceRequestedAt);
        findingActions.save(state);
        if (mapPanel != null) mapPanel.saveSelection(mapSelection);
        state.putBundle("mapSelection", mapSelection);
        state.putInt("findingLimit", findingLimit);
        state.putInt("tab", selectedTab); state.putInt("findingFilter", findingFilter);
        if (contentScroll != null) scrollPositions[selectedTab] = contentScroll.getScrollY();
        state.putIntArray("scrollPositions", scrollPositions);
        super.onSaveInstanceState(state);
    }

    @Override public void onResume() {
        super.onResume(); resumed = true;
        if (mapPanel != null) mapPanel.resume();
        handler.post(tick); handler.post(this::startIfResumed);
        OsmEnricher.visible(this, true);
    }
    @Override public void onPause() {
        resumed = false; handler.removeCallbacks(tick);
        OsmEnricher.visible(this, false);
        if (mapPanel != null) mapPanel.pause(); super.onPause();
    }
    @Override public void onDestroy() { if (mapPanel != null) mapPanel.destroy(); menus.close(); store.close(); super.onDestroy(); }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            int count = store.count();
            if (displayedRunning != RecordingService.running || (selectedTab == FINDINGS
                    && (count != displayedCount || displayedRevision != store.revision()))) render();
            if (rideCount != null) rideCount.setText(getResources().getQuantityString(R.plurals.finding_count, count, count));
            if (lastAction != null) {
                lastAction.setText(RecordingService.lastAction);
                lastAction.setVisibility(RecordingService.lastAction.isEmpty() ? View.GONE : View.VISIBLE);
            }
            if (startStatus != null) startStatus.setText(RecordingService.status);
            if (mapPanel != null) mapPanel.refresh();
            if (openMapAfterStart && RecordingService.running) {
                openMapAfterStart = false;
                startActivity(new Intent(MainActivity.this, MapActivity.class));
            } else if (openMapAfterStart && !RecordingService.startFailure.isEmpty()) {
                showStartFailure(RecordingService.startFailure);
            } else if (openMapAfterStart && android.os.SystemClock.elapsedRealtime() - serviceRequestedAt > 10_000) {
                showStartFailure("Android hat den Start noch nicht bestätigt. Bitte die App geöffnet lassen und erneut versuchen.");
            }
            handler.postDelayed(this, 1_000);
        }
    };

    private void render() {
        if (contentScroll != null) scrollPositions[renderedTab] = contentScroll.getScrollY();
        if (mapPanel != null) { mapPanel.saveSelection(mapSelection); mapPanel.destroy(); mapPanel = null; }
        startButton = null; startStatus = null; rideCount = null; lastAction = null;
        displayedRunning = RecordingService.running;
        displayedCount = store.count(); displayedRevision = store.revision();
        LinearLayout root = column();
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        root.setBackgroundColor(Color.rgb(244, 247, 245));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        contentScroll = new ScrollView(this); contentScroll.setFillViewport(true);
        contentScroll.setId(R.id.page_scroll);
        content = column(); content.setPadding(dp(20), dp(20), dp(20), dp(24));
        contentScroll.addView(content); root.addView(contentScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (selectedTab == RIDE) renderRide();
        else if (selectedTab == FINDINGS) renderFindings();
        else renderMore();
        LinearLayout navigation = new LinearLayout(this);
        navigation.setBackgroundColor(Color.WHITE); navigation.setPadding(dp(8), dp(8), dp(8), dp(8));
        String[] labels = {"Fahrt", "Fundstellen", "Mehr"};
        int[] ids = {R.id.nav_ride, R.id.nav_findings, R.id.nav_more};
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            Button nav = button(navigation, labels[i], selectedTab == i, () -> switchTab(tab));
            nav.setId(ids[i]); nav.setSelected(selectedTab == i);
            nav.setContentDescription(labels[i] + (selectedTab == i ? " · ausgewählt" : " öffnen"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
            params.setMargins(dp(3), 0, dp(3), 0); nav.setLayoutParams(params);
        }
        root.addView(navigation); setContentView(root);
        renderedTab = selectedTab;
        int y = scrollPositions[selectedTab];
        ScrollView restored = contentScroll;
        int tab = selectedTab;
        restored.post(() -> { if (contentScroll == restored && selectedTab == tab) restored.scrollTo(0, y); });
    }

    private void switchTab(int tab) {
        if (selectedTab == tab) return;
        selectedTab = tab; render();
    }

    @Override public void onBackPressed() {
        if (selectedTab != RIDE) switchTab(RIDE); else super.onBackPressed();
    }

    private void renderRide() {
        addText(content, "Bodenwelle", 30, INK, true);
        addText(content, "Berlin · Unebenheiten erfassen", 14, GREEN, false);
        LinearLayout ride = card(content);
        addText(ride, displayedRunning ? "Deine Fahrt läuft" : "Bereit für deine Fahrt", 21, INK, true);
        startStatus = addText(ride, RecordingService.status, 15, INK, false);
        startStatus.setId(R.id.ride_start_status);
        startStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        if (!displayedRunning) {
            startButton = button(ride, "Fahrt starten", true, this::confirmStart);
            startButton.setEnabled(!pendingStart && !startWhenResumed && !openMapAfterStart);
            addText(ride, "Handy befestigen. Nach dem Start zwei Sekunden im Stand bis „Kalibriert“ warten.", 14, INK, false);
        } else {
            LinearLayout actions = new LinearLayout(this); ride.addView(actions);
            Button mark = button(actions, "Stelle markieren", true,
                    () -> startService(new Intent(this, RecordingService.class).setAction(RecordingService.MARK)));
            startButton = button(actions, "Fahrt beenden", false,
                    () -> startService(new Intent(this, RecordingService.class).setAction(RecordingService.STOP)));
            equalWidth(mark); equalWidth(startButton);
        }
        startButton.setId(R.id.ride_start_button);
        rideCount = addText(ride, getResources().getQuantityString(R.plurals.finding_count, displayedCount, displayedCount), 14, GREEN, true);
        lastAction = addText(ride, RecordingService.lastAction, 14, INK, false);
        lastAction.setVisibility(RecordingService.lastAction.isEmpty() ? View.GONE : View.VISIBLE);
        LinearLayout mapCard = card(content);
        addText(mapCard, "Deine Karte", 20, INK, true);
        mapPanel = new RideMapPanel(this, store, () -> requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, MAP_LOCATION), findingActions::show, false);
        findingActions.setMapBundler(mapPanel::startBundling);
        mapPanel.restoreSelection(mapSelection);
        mapCard.addView(mapPanel); if (resumed) mapPanel.resume();
        button(mapCard, "Karte vergrößern", false, () -> startActivity(new Intent(this, MapActivity.class)));
    }

    private void renderFindings() {
        List<FindingBundle> bundles = store.bundles();
        long reviewed = bundles.stream().mapToLong(FindingBundle::reviewedCount).sum();
        int count = bundles.stream().mapToInt(b -> b.observations().size()).sum();
        addText(content, "Fundstellen", 30, INK, true);
        addText(content, (count - reviewed) + " offen · " + reviewed + " freigegeben", 15, GREEN, true);
        if (displayedRunning) addText(content, "Die Fahrt läuft weiter. Bitte nur im sicheren Stand prüfen.", 14, INK, false);
        if (count == 0) {
            LinearLayout empty = card(content);
            addText(empty, "Noch keine Fundstellen", 20, INK, true);
            addText(empty, "Starte im Bereich „Fahrt“ eine Aufzeichnung. Hier kannst du danach die Messungen prüfen.", 15, INK, false);
            button(empty, "Zur Fahrt", true, () -> switchTab(RIDE)); return;
        }
        Button mail = button(content, "Meldung vorbereiten", true, () -> startActivity(new Intent(this, MailReportActivity.class)));
        mail.setId(R.id.findings_mail); mail.setEnabled(reviewed > 0);
        if (reviewed == 0) addText(content, "Zuerst eine Fundstelle prüfen und freigeben.", 14, INK, false);
        button(content, "Weitere Aktionen", false, () -> new AlertDialog.Builder(this).setTitle("Fundstellen verwalten")
                .setItems(new String[]{"Mehrfachtreffer bündeln", "Geprüfte Stellen als CSV speichern"}, (d, index) -> {
                    if (index == 0) startActivity(new Intent(this, BundleActivity.class)); else confirmExport();
                }).setNegativeButton("Schließen", null).show()).setId(R.id.findings_actions);
        List<List<FindingBundle>> suggestions = BundleSuggestions.suggest(bundles);
        java.util.Set<Long> nearKeys = new java.util.HashSet<>();
        for (List<FindingBundle> group : suggestions) for (FindingBundle b : group) nearKeys.add(b.key());
        if (!suggestions.isEmpty()) button(content, suggestions.size() + " zusammenfassbare Gruppe(n) · unter 4 m", false,
                () -> startActivity(new Intent(this, BundleActivity.class))).setId(R.id.bundle_open);
        LinearLayout filters = new LinearLayout(this); content.addView(filters);
        String[] labels = {"Alle", "Offen", "Freigegeben"};
        int[] ids = {R.id.filter_all, R.id.filter_open, R.id.filter_reviewed};
        for (int i = 0; i < labels.length; i++) {
            final int filter = i;
            Button choice = button(filters, labels[i], findingFilter == i, () -> {
                findingFilter = filter; findingLimit = 100; contentScroll.scrollTo(0, 0); render();
            }); choice.setId(ids[i]); choice.setSelected(findingFilter == i); equalWidth(choice); choice.setTextSize(14);
        }
        int shown = 0, matches = 0;
        for (FindingBundle bundle : bundles) {
            if (findingFilter == 1 && bundle.reviewedCount() == bundle.observations().size()) continue;
            if (findingFilter == 2 && bundle.reviewedCount() == 0) continue;
            matches++;
            if (shown++ >= findingLimit) continue;
            Finding f = bundle.representative();
            LinearLayout item = card(content);
            String state = bundle.reviewedCount() == bundle.observations().size() ? "Freigegeben"
                    : bundle.reviewedCount() == 0 ? "Offen" : "Teilweise freigegeben";
            addText(item, state + (nearKeys.contains(bundle.key()) ? " · zusammenfassbar" : ""), 13, GREEN, true);
            if (SharedFindingDetails.from(bundle.observations()).conflicting()) addText(item, "Gemeinsame Angaben prüfen", 14, GREEN, true);
            addText(item, bundle.title(), 20, INK, true);
            addText(item, bundle.referenceLabel(), 14, GREEN, true);
            addText(item, DATE.format(Instant.ofEpochMilli(f.timeMillis()))
                    + (bundle.observations().size() > 1 ? " · " + bundle.observations().size() + " Beobachtungen" : ""), 14, INK, false);
            Intensity intensity = bundle.intensity();
            TextView rating = addText(item, intensity.label() + (bundle.observations().size() > 1 ? " · bisheriger Höchstwert" : ""),
                    14, intensity.level() <= 1 || intensity.level() == 5 ? Color.WHITE : INK, true);
            GradientDrawable badge = new GradientDrawable(); badge.setColor(intensity.color()); badge.setCornerRadius(dp(8));
            rating.setBackground(badge); rating.setPadding(dp(8), dp(6), dp(8), dp(6));
            if (f.details().section() != null) addText(item, f.details().section().label(), 14, GREEN, false);
            if (!f.note().isEmpty()) {
                TextView note = addText(item, f.note(), 15, INK, false);
                note.setMaxLines(2); note.setEllipsize(android.text.TextUtils.TruncateAt.END);
            }
            LinearLayout actions = new LinearLayout(this); item.addView(actions);
            Button review = button(actions, "Prüfen & Fotos", true, () -> findingActions.review(f));
            review.setId(R.id.finding_review); review.setTag(f.id()); equalWidth(review);
            Button delete = button(actions, "Löschen", false, () -> findingActions.deleteObservation(f));
            delete.setId(R.id.finding_delete); delete.setTag(f.id()); equalWidth(delete);
            delete.setContentDescription("Messung vom " + DATE.format(Instant.ofEpochMilli(f.timeMillis())) + " löschen");
            Button details = button(item, "Details & Karte", false, () -> showFindingDetails(bundle));
            details.setId(R.id.finding_details); details.setTag(f.id());
            details.setBackgroundColor(Color.TRANSPARENT);
        }
        if (matches == 0) addText(content, "Keine Fundstellen in diesem Filter.", 16, INK, false);
        if (matches > findingLimit) {
            addText(content, findingLimit + " von " + matches + " passenden Fundstellen angezeigt. Alle Messungen bleiben gespeichert und auf der Karte erreichbar.", 14, INK, false);
            button(content, "Weitere Fundstellen anzeigen", false, () -> { findingLimit += 100; render(); }).setId(R.id.findings_more);
        }
    }

    private void showFindingDetails(FindingBundle bundle) {
        Finding f = bundle.representative();
        LinearLayout details = column(); details.setPadding(dp(20), dp(8), dp(20), dp(12));
        addText(details, bundle.title(), 20, INK, true);
        addText(details, bundle.referenceLabel() + "\nMessdetails: #" + f.id(), 14, GREEN, true);
        if (!f.note().isEmpty()) addText(details, f.note(), 16, INK, false);
        if (bundle.observations().size() > 1) addText(details, bundle.observations().size() + " Beobachtungen · "
                + bundle.reviewedCount() + " freigegeben. Messdetails beziehen sich auf die repräsentative Beobachtung.", 14, INK, false);
        if (f.details().section() != null) addText(details, f.details().section().label(), 15, GREEN, true);
        addText(details, String.format(Locale.GERMANY, "Angezeigte Position: %.5f, %.5f\nUrsprüngliche GPS-Position: %.5f, %.5f\nGenauigkeit der GPS-Messung: ± %.0f m\nGPS-Messung: %.2f s %s dem Ereignis", f.latitude(), f.longitude(),
                f.position().gpsLatitude(), f.position().gpsLongitude(), f.accuracy(), Math.abs(f.locationAgeMillis()) / 1000.0,
                f.locationAgeMillis() >= 0 ? "vor" : "nach"), 14, INK, false);
        if (f.position().projected()) {
            addText(details, String.format(Locale.GERMANY, "Positionskorrektur: %.1f m %s der Fahrtrichtung. Schätzung bei gleichbleibender Geschwindigkeit und Richtung; die GPS-Genauigkeit gilt für die ursprüngliche Messung.",
                    Math.abs(f.position().distanceMeters()), f.position().distanceMeters() >= 0 ? "entlang" : "entgegen"), 14, INK, false);
        } else if ("automatic".equals(f.source()) && !"legacy_gps".equals(f.position().method())) {
            addText(details, "Die ursprüngliche GPS-Position bleibt erhalten: keine zeitliche Verschiebung nötig oder keine brauchbare Richtung/Geschwindigkeit verfügbar.", 14, INK, false);
        }
        if (!f.details().legacyCategory().isEmpty()) addText(details, "Frühere Angabe: " + f.details().legacyCategory(), 14, INK, false);
        Intensity score = f.details().intensity();
        addText(details, "Bewertungsgrundlage: " + (score.value() == null ? "keine Messung" : score.value() + " " + score.unit()) + " · " + score.method(), 14, INK, false);
        for (int i = 0; i < Classification.HAZARDS.length; i++) if ((f.details().hazards() & (1 << i)) != 0)
            addText(details, "Zusatzmerkmal: " + Classification.HAZARDS[i].label(), 14, INK, false);
        if (bundle.observations().size() > 1) addText(details, "Verkehrsfläche, Oberfläche und Gefährdungsmerkmale gelten gemeinsam für das Bündel. OSM-Abfragedetails gehören zur angezeigten Einzelmessung #" + f.id() + ".", 14, INK, false);
        addText(details, GeodataDescription.details(f.details()), 14, INK, false);
        if (f.details().osm().queriedAt() > 0 && System.currentTimeMillis() - f.details().osm().queriedAt() > 7L * 24 * 60 * 60 * 1000)
            addText(details, "OSM-Cache älter als sieben Tage; Angaben können veraltet sein.", 14, INK, false);
        String measurement = f.peak() == null ? "Manuelle Markierung" : String.format(Locale.GERMANY, "Sensorreaktion: %.1f m/s²", f.peak());
        if (f.speed() != null) measurement += String.format(Locale.GERMANY, " · %.1f km/h", f.speed() * 3.6f);
        addText(details, measurement, 14, INK, false);
        ScrollView scroll = new ScrollView(this); scroll.addView(details);
        new AlertDialog.Builder(this).setTitle("Messdetails").setView(scroll)
                .setPositiveButton("Auf der Karte ansehen", (d, w) -> openMap(f))
                .setNegativeButton("Schließen", null).show();
    }

    private void renderMore() {
        addText(content, "Mehr", 30, INK, true);
        addText(content, "Einstellungen, Daten und Hilfe", 15, INK, false);
        LinearLayout menu = card(content);
        button(menu, "Messung einstellen", false, menus::measurement).setId(R.id.menu_measurement);
        button(menu, "Karte & Geodaten", false, menus::geodata).setId(R.id.menu_geodata);
        button(menu, "Daten verwalten", false, menus::data).setId(R.id.menu_data);
        button(menu, "Datenschutz", false, menus::privacy).setId(R.id.menu_privacy);
        button(menu, "Hilfe", false, this::showHelp);
        addText(content, "Bodenwelle " + BuildConfig.VERSION_NAME + " · Testversion", 13, GREEN, false);
    }

    private void equalWidth(Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(dp(3), dp(8), dp(3), dp(4)); button.setLayoutParams(params);
    }

    private void refreshCurrentPage() {
        if (selectedTab == RIDE && mapPanel != null) mapPanel.refresh(); else render();
    }

    private void confirmStart() {
        if (store.routeCount() == 0) { requestStart(); return; }
        new AlertDialog.Builder(this).setTitle("Neue Fahrt starten?")
                .setMessage("Die bisherige Fahrtspur wird durch die neue Strecke ersetzt. Deine Fundstellen bleiben erhalten.")
                .setNegativeButton("Abbrechen", null).setPositiveButton("Fahrt starten", (d, w) -> requestStart()).show();
    }

    private void requestStart() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingStart = true;
            String[] permissions = Build.VERSION.SDK_INT >= 33
                    ? new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS}
                    : new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            requestPermissions(permissions, PERMISSIONS);
        } else if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingStart = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSIONS);
        } else startRide();
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == MAP_LOCATION && mapPanel != null) mapPanel.resume();
        if (request == PERMISSIONS && pendingStart) {
            pendingStart = false;
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) startRide();
            else new AlertDialog.Builder(this).setTitle("Genauen Standort erlauben")
                    .setMessage("Die App benötigt eine genaue Position, damit Fundstellen zugeordnet werden können. Erlaube in den Android-App-Einstellungen den genauen Standort während der Nutzung.")
                    .setNegativeButton("Später", null)
                    .setPositiveButton("Einstellungen öffnen", (dialog, which) -> startActivity(new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))).show();
        }
    }

    private void startRide() {
        startWhenResumed = true;
        handler.post(this::startIfResumed);
    }

    private void startIfResumed() {
        if (!startWhenResumed || !resumed) return;
        startWhenResumed = false;
        if (DeviceLocationSource.enabledProviders(getSystemService(LocationManager.class)).isEmpty()) {
            RecordingService.status = "Der Standort ist am Handy ausgeschaltet. Bitte einschalten und erneut starten.";
            if (startStatus != null) startStatus.setText(RecordingService.status);
            new AlertDialog.Builder(this).setTitle("Standort einschalten")
                    .setMessage(RecordingService.status).setNegativeButton("Später", null)
                    .setPositiveButton("Einstellungen öffnen", (d, w) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))).show();
            return;
        }
        RecordingService.prepareStart();
        if (startStatus != null) startStatus.setText(RecordingService.status);
        if (startButton != null) startButton.setEnabled(false);
        try {
            startForegroundService(new Intent(this, RecordingService.class));
            openMapAfterStart = true;
            serviceRequestedAt = android.os.SystemClock.elapsedRealtime();
        }
        catch (RuntimeException e) {
            showStartFailure("Android konnte die Aufzeichnung nicht starten (" + e.getClass().getSimpleName()
                    + "). Bitte die App geöffnet lassen und den genauen Standort erlauben.");
        }
    }

    private void showStartFailure(String message) {
        openMapAfterStart = false;
        RecordingService.status = message;
        if (startStatus != null) startStatus.setText(message);
        if (startButton != null) startButton.setEnabled(true);
        new AlertDialog.Builder(this).setTitle("Fahrt konnte nicht starten").setMessage(message)
                .setPositiveButton("Verstanden", null).show();
    }

    private void confirmExport() {
        long count = store.all().stream().filter(Finding::reviewed).count();
        if (count == 0) { toast("Bitte zuerst mindestens eine Fundstelle prüfen."); return; }
        new AlertDialog.Builder(this).setTitle(count + " geprüfte Fundstellen speichern?")
                .setMessage("Die Datei enthält genaue Standorte, Zeitpunkte und deine Notizen. Wähle im nächsten Schritt einen Speicherort. Es wird keine Meldung an die Verwaltung versendet.")
                .setNegativeButton("Abbrechen", null).setPositiveButton("Datei speichern", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("text/csv").putExtra(Intent.EXTRA_TITLE, "bodenwelle-" + java.time.LocalDate.now(ZoneId.of("Europe/Berlin")) + ".csv");
                    startActivityForResult(intent, EXPORT);
                }).show();
    }

    private void showHelp() {
        new AlertDialog.Builder(this).setTitle("Deine erste Fahrt")
                .setMessage("1. Vor der Fahrt\nBefestige das Handy fest am Lenker, tippe auf „Fahrt starten“ und erlaube den genauen Standort. Halte es im Stand zwei Sekunden ruhig, bis „Kalibriert“ erscheint. Warte draußen, bis die App die Position gefunden hat.\n\n"
                        + "2. Unterwegs\nDie Karte zeigt deinen Standort und die Fahrtspur. Mit „Mein Standort“ folgt sie dir, mit „Karte vergrößern“ nutzt du den ganzen Bildschirm. Die App erfasst mögliche Unebenheiten automatisch. Du kannst das Display ausschalten. Für eine eigene Markierung erst sicher anhalten und „Stelle markieren“ wählen.\n\n"
                        + "3. Nach der Fahrt\nBeende die Fahrt. Öffne unten „Fundstellen“, beschreibe sie und gib passende Einträge frei. Dann kannst du eine Datei speichern oder eine Meldung an die Verwaltung vorbereiten. Der Mailentwurf enthält Kalenderwochen und bleibt bearbeitbar. Erst in deinem Mailprogramm versendest du die Nachricht.")
                .setPositiveButton("Los geht’s", null).show();
    }

    private void openMap(Finding f) {
        startActivity(new Intent(this, MapActivity.class).putExtra("latitude", f.latitude()).putExtra("longitude", f.longitude()));
    }

    @Override public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (findingActions.onActivityResult(request, result, data)) return;
        if (request != EXPORT || result != RESULT_OK || data == null || data.getData() == null) return;
        try (OutputStream stream = getContentResolver().openOutputStream(data.getData(), "wt")) {
            if (stream == null) throw new java.io.IOException("No output stream");
            stream.write(FindingCsv.exportReviewed(store.all()).getBytes(StandardCharsets.UTF_8));
            toast("Geprüfte Fundstellen gespeichert. Es wurde keine Meldung versendet.");
        } catch (Exception e) { toast("Export fehlgeschlagen. Speicherort und freien Speicher prüfen."); }
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return layout;
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout layout = column();
        layout.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE); background.setCornerRadius(dp(20));
        layout.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(12), 0, dp(4));
        parent.addView(layout, params);
        return layout;
    }
    private TextView addText(LinearLayout parent, String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(4), 0, dp(6));
        parent.addView(view);
        return view;
    }
    private Button button(LinearLayout parent, String text, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(text); button.setAllCaps(false); button.setTextSize(16);
        button.setMinHeight(dp(48));
        button.setTextColor(primary ? Color.WHITE : GREEN);
        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? GREEN : Color.rgb(232, 242, 238));
        background.setCornerRadius(dp(14));
        GradientDrawable disabled = new GradientDrawable();
        disabled.setColor(Color.rgb(225, 232, 229)); disabled.setCornerRadius(dp(14));
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled}, disabled);
        states.addState(new int[]{}, background);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(45, 0, 0, 0)), states, null));
        button.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}},
                new int[]{Color.rgb(82, 100, 94), primary ? Color.WHITE : GREEN}));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(12), 0, dp(4));
        parent.addView(button, params);
        button.setOnClickListener(v -> action.run());
        return button;
    }
    private void space(LinearLayout parent, int height) {
        View spacer = new View(this); parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(height)));
    }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
