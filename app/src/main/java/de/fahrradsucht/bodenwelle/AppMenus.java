package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Secondary options stay out of the ride and review screens. */
public final class AppMenus {
    private final Activity activity;
    private final FindingStore store;
    private final Runnable changed;
    private AlertDialog dialog;
    public AppMenus(Activity activity, FindingStore store, Runnable changed) {
        this.activity = activity; this.store = store; this.changed = changed;
    }
    public void close() { if (dialog != null) dialog.dismiss(); }
    public void measurement() {
        LinearLayout content = column();
        SensitivityControl sensitivity = new SensitivityControl(activity); sensitivity.expand(); content.addView(sensitivity);
        text(content, "Einzelstöße verwenden diese Schwelle. Anhaltende Vibrationen können auch darunter als unebener Abschnitt erfasst werden.");
        text(content, "Nach dem Fahrtstart zwei Sekunden im Stand bis „Kalibriert“ warten. Bei geänderter Halterung im Stand neu kalibrieren.");
        Button calibrate = button(content, "Im Stand neu kalibrieren", () -> {
            activity.startService(new Intent(activity, RecordingService.class).setAction(RecordingService.CALIBRATE)); close();
        }); calibrate.setId(R.id.recalibrate); calibrate.setEnabled(RecordingService.running);
        show("Messung einstellen", content);
    }
    public void geodata() {
        LinearLayout content = column();
        text(content, "Kartenkacheln werden aus dem Internet geladen und lokal zwischengespeichert. Die GPS-Aufzeichnung funktioniert unabhängig davon.");
        CheckBox enabled = new CheckBox(activity); enabled.setText(R.string.geodata_toggle);
        enabled.setId(R.id.geodata_toggle); enabled.setChecked(OsmEnricher.enabled(activity)); content.addView(enabled);
        enabled.setOnCheckedChangeListener((v, checked) -> {
            OsmEnricher.enabled(activity, checked); if (checked) OsmEnricher.pending(activity);
        });
        text(content, "Die OSM-Ergänzung fragt den Kartenausschnitt um eine Fundstelle bei overpass-api.de ab. Der Anbieter sieht Ausschnitt und IP-Adresse, keine Notizen oder Sensorwerte. Manuelle Angaben bleiben erhalten.");
        button(content, "Fehlende OSM-Angaben ergänzen", () -> {
            if (!OsmEnricher.enabled(activity)) { toast("Bitte die OSM-Ergänzung einschalten."); return; }
            OsmEnricher.pending(activity); toast("OSM-Ergänzung angefragt. Offline bleiben die Messungen gespeichert.");
        });
        show("Karte & Geodaten", content);
    }
    public void data() {
        LinearLayout content = column();
        text(content, store.count() + " Messungen auf diesem Handy. Fahrtspur und Fundstellen können getrennt gelöscht werden.");
        Button route = button(content, "Gespeicherte Fahrtspur löschen", () -> confirm("Fahrtspur löschen?",
                "Die gespeicherte Strecke wird entfernt. Deine Fundstellen bleiben erhalten.", store::clearRoute));
        route.setId(R.id.delete_route); route.setEnabled(!RecordingService.running && store.routeCount() > 0);
        Button all = button(content, "Alle Fundstellen löschen", () -> confirm("Alle Fundstellen löschen?",
                "Die Messungen, Bündelungen und zugehörigen Fotos werden aus der App entfernt. Fahrtspur, Albumoriginale und bereits exportierte Kopien bleiben erhalten.", store::deleteAll));
        all.setId(R.id.delete_all); all.setEnabled(!RecordingService.running && store.count() > 0);
        if (RecordingService.running) text(content, "Zum Löschen gespeicherter Daten zuerst die Fahrt beenden.");
        text(content, "Einzelne Messungen löschst du direkt unter „Fundstellen“ oder über ihren Kartenpunkt. CSV und Mail findest du im Bereich „Fundstellen“.");
        show("Daten verwalten", content);
    }
    private void confirm(String title, String message, Runnable action) {
        close();
        dialog = new AlertDialog.Builder(activity).setTitle(title).setMessage(message)
                .setNegativeButton("Abbrechen", null).setPositiveButton("Löschen", (d, w) -> {
                    action.run(); changed.run(); toast("Daten gelöscht.");
                }).show();
    }
    public void privacy() {
        close();
        dialog = new AlertDialog.Builder(activity).setTitle("Deine Daten bleiben bei dir")
                .setMessage("Fundstellen, Fotos und Fahrtspur bleiben lokal. Nur du entscheidest, welche geprüften Stellen du weitergibst. Erst im Mailprogramm wird eine vorbereitete Meldung versendet.\n\n"
                        + "OpenStreetMap erhält Anfragen für angezeigte Kartenausschnitte und deine IP-Adresse. Die optionale OSM-Ergänzung sendet den Ausschnitt um eine Fundstelle und deine IP-Adresse an overpass-api.de. Notizen und Sensorwerte werden dabei nicht übertragen.\n\n"
                        + "Die Fahrtspur gehört nicht zum Export oder zur Meldung. CSV-Dateien enthalten genaue Standorte, Zeitpunkte und Notizen; automatisch erzeugte Mailtexte enthalten nur Kalenderwochen.\n\n"
                        + "Fotokopien werden verkleinert und ohne ursprüngliche EXIF-Orts- und Zeitangaben gespeichert. Bitte sichtbare private Details selbst prüfen.\n\n"
                        + "Alte temporäre Kamera-Dateien werden nach 24 Stunden und ausgehende Mail-Fotokopien nach sieben Tagen beim App-Start bereinigt. Bereits weitergegebene Kopien werden nicht zurückgerufen.\n\n"
                        + "Die Erkennung ist experimentell. Sie berechnet keine Schadenshöhe und keine rechtliche Einstufung.")
                .setPositiveButton("Schließen", null).show();
    }
    private LinearLayout column() {
        LinearLayout c = new LinearLayout(activity); c.setOrientation(LinearLayout.VERTICAL);
        int p = Math.round(20 * activity.getResources().getDisplayMetrics().density); c.setPadding(p, p/2, p, p/2); return c;
    }
    private void text(LinearLayout parent, String value) {
        TextView text = new TextView(activity); text.setText(value); text.setTextSize(15); text.setTextColor(0xff1b302e);
        int p = Math.round(8 * activity.getResources().getDisplayMetrics().density); text.setPadding(0, p, 0, p); parent.addView(text);
    }
    private Button button(LinearLayout parent, String label, Runnable action) {
        Button b = new Button(activity); b.setText(label); b.setAllCaps(false); b.setTextColor(0xff087f73);
        b.setMinHeight(Math.round(48 * activity.getResources().getDisplayMetrics().density)); parent.addView(b);
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void show(String title, LinearLayout content) {
        close(); ScrollView scroll = new ScrollView(activity); scroll.addView(content);
        dialog = new AlertDialog.Builder(activity).setTitle(title).setView(scroll).setPositiveButton("Schließen", null).show();
    }
    private void toast(String message) { Toast.makeText(activity, message, Toast.LENGTH_LONG).show(); }
}
