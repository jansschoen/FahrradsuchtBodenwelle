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
import android.widget.CheckBox;
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
    private static final int GREEN = Color.rgb(8, 127, 115);
    private static final int INK = Color.rgb(27, 48, 46);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.of("Europe/Berlin"));
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FindingStore store;
    private LinearLayout content;
    private TextView rideStatus;
    private TextView rideCount;
    private TextView lastAction;
    private boolean displayedRunning;
    private int displayedCount = -1;
    private long displayedRevision = -1;
    private boolean pendingStart;
    private RideMapPanel mapPanel;
    private boolean openMapAfterStart;
    private boolean resumed;
    private boolean startWhenResumed;
    private long serviceRequestedAt;
    private TextView startStatus;
    private Button startButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new FindingStore(this);
        findingActions = new FindingActions(this, store, this::render);
        findingActions.restore(state);
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
        super.onSaveInstanceState(state);
    }

    @Override public void onResume() {
        super.onResume(); resumed = true;
        if (mapPanel != null) mapPanel.resume();
        handler.post(tick); handler.post(this::startIfResumed);
        OsmEnricher.pending(this);
    }
    @Override public void onPause() {
        resumed = false; handler.removeCallbacks(tick);
        if (mapPanel != null) mapPanel.pause(); super.onPause();
    }
    @Override public void onDestroy() { if (mapPanel != null) mapPanel.destroy(); store.close(); super.onDestroy(); }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            int count = store.count();
            if (displayedRunning != RecordingService.running || (!RecordingService.running && (count != displayedCount || displayedRevision != store.revision()))) render();
            rideStatus.setText(RecordingService.status);
            rideCount.setText(getResources().getQuantityString(R.plurals.finding_count, count, count));
            lastAction.setText(RecordingService.lastAction);
            startStatus.setText(RecordingService.status);
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
        if (mapPanel != null) mapPanel.destroy();
        displayedRunning = RecordingService.running;
        displayedCount = store.count();
        displayedRevision = store.revision();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(244, 247, 245));
        content = column();
        content.setPadding(dp(24), dp(24), dp(24), dp(32));
        content.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top; bottom = bars.bottom;
            } else { top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom(); }
            v.setPadding(dp(24), dp(24) + top, dp(24), dp(32) + bottom);
            return insets;
        });
        scroll.addView(content);
        setContentView(scroll);
        addText(content, "BERLIN · ERSTER FAHRTEST", 12, GREEN, true);
        addText(content, "Bodenwelle", 36, INK, true);
        addText(content, "Unebenheiten aufzeichnen und nach der Fahrt in Ruhe prüfen.", 17, INK, false);
        if (!displayedRunning) startButton = button(content, "Fahrt starten", true, this::confirmStart);
        else startButton = button(content, "Zur Live-Karte", true, () -> startActivity(new Intent(this, MapActivity.class)));
        startButton.setId(R.id.ride_start_button);
        startStatus = addText(content, RecordingService.status, 15, INK, false);
        startStatus.setId(R.id.ride_start_status);
        startStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        space(content, 12);
        LinearLayout mapCard = card(content);
        addText(mapCard, "Deine Karte", 21, INK, true);
        mapPanel = new RideMapPanel(this, store, () -> requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, MAP_LOCATION), findingActions::show, false);
        mapCard.addView(mapPanel);
        if (resumed) mapPanel.resume();
        button(mapCard, "Karte vergrößern", false, () -> startActivity(new Intent(this, MapActivity.class)));
        addText(mapCard, "Karten werden aus dem Internet geladen. Deine Fahrtspur bleibt auf diesem Handy.", 14, INK, false);
        LinearLayout ride = card(content);
        addText(ride, displayedRunning ? "Deine Fahrt läuft" : "Bereit für deine nächste Fahrt", 21, INK, true);
        rideStatus = addText(ride, RecordingService.status, 16, INK, false);
        rideCount = addText(ride, getResources().getQuantityString(R.plurals.finding_count, displayedCount, displayedCount), 14, GREEN, true);
        lastAction = addText(ride, RecordingService.lastAction, 14, INK, false);
        if (displayedRunning) {
            button(ride, "Stelle markieren", true, () -> startService(new Intent(this, RecordingService.class).setAction(RecordingService.MARK)));
            button(ride, "Fahrt beenden", false, () -> startService(new Intent(this, RecordingService.class).setAction(RecordingService.STOP)));
            addText(ride, "Du kannst das Display ausschalten. Zum Markieren erst sicher anhalten.", 14, INK, false);
        } else {
            addText(ride, "Handy fest am Lenker befestigen. Nach dem Start zwei Sekunden im Stand ruhig halten, bis „Kalibriert“ erscheint. Dann GPS draußen abwarten. Die Automatik erkennt vertikale Erschütterungen ab etwa 5 km/h.", 14, INK, false);
        }
        button(content, "So funktioniert’s", false, this::showHelp);
        if (!displayedRunning) renderFindings();
        space(content, 20);
        LinearLayout privacy = card(content);
        addText(privacy, "Deine Daten bleiben bei dir", 18, INK, true);
        addText(privacy, "Fundstellen und Fahrtspur bleiben lokal. Nur du entscheidest, welche geprüften Stellen du als Datei speicherst und anschließend weitergibst.", 15, INK, false);
        addText(privacy, "OpenStreetMap erhält Anfragen für angezeigte Kartenausschnitte und deine IP-Adresse. Die Fahrtspur wird nicht als Datei übertragen und ist nicht Teil des Fundstellenexports.", 14, INK, false);
        if (!displayedRunning && store.routeCount() > 0) button(privacy, "Gespeicherte Fahrtspur löschen", false,
                () -> new AlertDialog.Builder(this).setTitle("Fahrtspur löschen?")
                        .setMessage("Die gespeicherte Strecke wird von diesem Handy entfernt. Deine Fundstellen bleiben erhalten.")
                        .setNegativeButton("Abbrechen", null).setPositiveButton("Löschen", (d, w) -> { store.clearRoute(); render(); }).show());
        CheckBox geodata = new CheckBox(this);
        geodata.setText(R.string.geodata_toggle);
        geodata.setChecked(OsmEnricher.enabled(this)); privacy.addView(geodata);
        geodata.setOnCheckedChangeListener((box, enabled) -> { OsmEnricher.enabled(this, enabled); if (enabled) OsmEnricher.pending(this); });
        addText(privacy, "Geodatenabfragen senden den Kartenausschnitt um eine Fundstelle und deine IP-Adresse an overpass-api.de. Keine Notizen oder Sensorwerte werden übertragen. OSM-Angaben sind Vorschläge, keine rechtliche Bewertung.", 14, INK, false);
        button(privacy, "Fehlende OSM-Angaben ergänzen", false, () -> {
            if (!OsmEnricher.enabled(this)) { toast("Bitte die OSM-Ergänzung einschalten."); return; }
            OsmEnricher.pending(this); toast("OSM-Ergänzung angefragt. Offline bleibt alles auf dem Handy gespeichert.");
        });
        addText(privacy, "Die Erkennung ist noch im Test. Sie misst weder die Höhe einer Bodenwelle noch, ob ein Regelverstoß vorliegt.", 14, INK, false);
    }

    private void renderFindings() {
        List<FindingBundle> bundles = store.bundles();
        int findingCount = bundles.stream().mapToInt(b -> b.observations().size()).sum();
        long reviewed = bundles.stream().mapToLong(FindingBundle::reviewedCount).sum();
        space(content, 24);
        addText(content, "Nach der Fahrt prüfen", 24, INK, true);
        addText(content, reviewed + " geprüft · " + (findingCount - reviewed) + " offen", 15, GREEN, true);
        if (findingCount == 0) {
            LinearLayout empty = card(content);
            addText(empty, "Hier ist Platz für deine ersten Fundstellen", 18, INK, true);
            addText(empty, "Starte oben eine Fahrt. Danach erscheinen hier mögliche Unebenheiten. Du kannst sie ansehen, beschreiben und Fehlalarme löschen.", 16, INK, false);
        } else {
            Button export = button(content, "Geprüfte Stellen als Datei speichern", true, this::confirmExport);
            export.setEnabled(reviewed > 0);
            Button mail = button(content, "Meldung an die Verwaltung vorbereiten", true, () -> startActivity(new Intent(this, MailReportActivity.class)));
            mail.setEnabled(reviewed > 0);
            addText(content, reviewed > 0
                    ? "Du erhältst eine Tabelle (CSV). An die Verwaltung wird noch nichts gesendet."
                    : "Öffne eine Fundstelle, prüfe sie und gib sie frei. Danach kannst du sie als Datei speichern.", 14, INK, false);
        }
        if (findingCount > 1) button(content, "Mehrfachtreffer bündeln", false,
                () -> startActivity(new Intent(this, BundleActivity.class))).setId(R.id.bundle_open);
        List<List<FindingBundle>> suggestions = BundleSuggestions.suggest(bundles);
        java.util.Set<Long> nearKeys = new java.util.HashSet<>();
        for (List<FindingBundle> suggestion : suggestions) for (FindingBundle b : suggestion) nearKeys.add(b.key());
        if (!suggestions.isEmpty()) {
            LinearLayout nearby = card(content);
            addText(nearby, "Zusammenfassbare Treffer", 20, INK, true);
            int groupsShown = 0;
            for (List<FindingBundle> group : suggestions) {
                if (groupsShown++ >= 10) break;
                addText(nearby, group.stream().mapToInt(b -> b.observations().size()).sum()
                        + " Beobachtungen · unter 2 m Abstand laut GPS", 15, GREEN, true);
            }
            addText(nearby, "GPS kann um mehrere Meter abweichen. Bitte dieselbe Unebenheit vor dem Bündeln bestätigen; Freigaben bleiben einzeln.", 14, INK, false);
            button(nearby, "Vorschläge prüfen", false, () -> startActivity(new Intent(this, BundleActivity.class)));
        }
        int shown = 0;
        for (FindingBundle bundle : bundles) {
            Finding f = bundle.representative();
            if (shown++ >= 100) break;
            LinearLayout item = card(content);
            String category = bundle.title();
            if (nearKeys.contains(bundle.key())) addText(item, "Zusammenfassbar · Treffer unter 2 m", 14, GREEN, true);
            addText(item, (bundle.reviewedCount()==bundle.observations().size() ? "✓ Geprüft · " : "Offen · ") + category, 18, INK, true);
            if(bundle.observations().size()>1) addText(item,bundle.observations().size()+" Beobachtungen · "+bundle.reviewedCount()+" freigegeben",15,GREEN,true);
            addText(item, DATE.format(Instant.ofEpochMilli(f.timeMillis())), 14, INK, false);
            addText(item, "manual".equals(f.source()) ? "Von dir markiert" : "Automatisch erfasst – bitte vor Ort zuordnen", 14, INK, false);
            if (f.details().section() != null) addText(item, f.details().section().label(), 15, GREEN, true);
            Intensity intensity = bundle.intensity();
            TextView rating = addText(item, intensity.label()+(bundle.observations().size()>1?" · bisheriger Höchstwert":""), 15, intensity.level() <= 1 || intensity.level() == 5 ? Color.WHITE : INK, true);
            rating.setBackgroundColor(intensity.color()); rating.setPadding(dp(8), dp(6), dp(8), dp(6));
            addText(item, GeodataDescription.summary(f.details()), 14, INK, false);
            if (f.position().projected()) addText(item, f.details().section() == null
                    ? "Position auf den Zeitpunkt der Erschütterung geschätzt"
                    : "Abschnittsbeginn aus der GPS-Messung geschätzt", 14, GREEN, false);
            if (!f.note().isEmpty()) addText(item, f.note(), 15, INK, false);
            else addText(item, "Ergänze, was du an dieser Stelle gesehen hast.", 15, INK, false);
            button(item, "Auf der Karte ansehen", false, () -> openMap(f));
            button(item, bundle.observations().size()>1?"Beobachtungen prüfen und Fotos ergänzen":"Stelle prüfen und Fotos ergänzen", true, () -> findingActions.review(f));
            Button delete = button(item, "Löschen", false, () -> findingActions.deleteObservation(f));
            delete.setId(R.id.finding_delete);
            delete.setTag(f.id());
            delete.setContentDescription(bundle.observations().size() > 1
                    ? "Eine Messung dieser Fundstelle löschen"
                    : "Messung vom " + DATE.format(Instant.ofEpochMilli(f.timeMillis())) + " löschen");
            LinearLayout details = column();
            details.setVisibility(View.GONE);
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
            addText(details, GeodataDescription.details(f.details()), 14, INK, false);
            if (f.details().osm().queriedAt() > 0 && System.currentTimeMillis() - f.details().osm().queriedAt() > 7L * 24 * 60 * 60 * 1000)
                addText(details, "OSM-Cache älter als sieben Tage; Angaben können veraltet sein.", 14, INK, false);
            String measurement = f.peak() == null ? "Manuelle Markierung" : String.format(Locale.GERMANY, "Sensorreaktion: %.1f m/s²", f.peak());
            if (f.speed() != null) measurement += String.format(Locale.GERMANY, " · %.1f km/h", f.speed() * 3.6f);
            addText(details, measurement, 14, INK, false);
            Button toggle = button(item, "Messdetails anzeigen", false, () -> {});
            toggle.setOnClickListener(v -> {
                boolean opening = details.getVisibility() != View.VISIBLE;
                details.setVisibility(opening ? View.VISIBLE : View.GONE);
                toggle.setText(opening ? R.string.hide_details : R.string.show_details);
            });
            item.addView(details);
        }
        if (bundles.size() > 100) addText(content, "Die Liste zeigt die neuesten 100 Fundstellen. Der Export enthält alle geprüften Beobachtungen.", 14, INK, false);
        if (findingCount > 0) button(content, "Alle Fundstellen löschen", false, () -> new AlertDialog.Builder(this)
                .setTitle("Alle Fundstellen löschen?").setMessage("Die Beobachtungen, Bündelungen und zugehörigen Fotos werden aus der App entfernt. Bereits exportierte Dateien und vorbereitete Mailkopien bleiben erhalten; lokale Mailkopien werden beim nächsten Start nach sieben Tagen bereinigt.")
                .setNegativeButton("Abbrechen", null).setPositiveButton("Löschen", (dialog, which) -> { store.deleteAll(); render(); }).show());
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
            startStatus.setText(RecordingService.status);
            new AlertDialog.Builder(this).setTitle("Standort einschalten")
                    .setMessage(RecordingService.status).setNegativeButton("Später", null)
                    .setPositiveButton("Einstellungen öffnen", (d, w) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))).show();
            return;
        }
        RecordingService.prepareStart();
        startStatus.setText(RecordingService.status);
        startButton.setEnabled(false);
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
        startStatus.setText(message);
        startButton.setEnabled(true);
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
                        + "3. Nach der Fahrt\nBeende die Fahrt. Sieh dir die Fundstellen an, beschreibe sie und gib passende Einträge frei. Dann kannst du eine Datei speichern oder eine Meldung an die Verwaltung vorbereiten. Der Mailentwurf enthält Kalenderwochen und bleibt bearbeitbar. Erst in deinem Mailprogramm versendest du die Nachricht.")
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
        button.setMinHeight(dp(56));
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
