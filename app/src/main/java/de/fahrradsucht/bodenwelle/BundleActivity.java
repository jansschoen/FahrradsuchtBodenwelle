package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Human-confirmed place grouping; originals and individual consent always remain available. */
public final class BundleActivity extends Activity {
    private static final int INK = 0xff1b302e, GREEN = 0xff087f73;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.of("Europe/Berlin"));
    private FindingStore store;
    private LinearLayout content;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store = new FindingStore(this);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
    }
    @Override public void onResume() { super.onResume(); render(); }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(0xfff4f7f5);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(24)); scroll.addView(content);
        if (Build.VERSION.SDK_INT >= 30) content.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets b = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(dp(20) + b.left, dp(16) + b.top, dp(20) + b.right, dp(24) + b.bottom); return insets;
        });
        setContentView(scroll); button(content, "Zurück", this::finish);
        text(content, "Mehrfachtreffer bündeln", 26, true);
        text(content, "Dieselbe Unebenheit mehrfach gemessen? Vergleiche die nummerierten Messungen und bestätige die passenden Fundstellen. Originale, Freigaben und Fotos bleiben erhalten.", 16, false);
        List<FindingBundle> all = store.bundles();
        List<List<FindingBundle>> suggestions = BundleSuggestions.suggest(all);
        text(content, "Vorschläge · unter 4 m", 20, true);
        text(content, "GPS kann ungenauer sein. Prüfe Straßenseite und benachbarte Schäden; es wird nichts automatisch zusammengefasst.", 14, false);
        if (suggestions.isEmpty()) text(content, "Zurzeit keine passenden Vorschläge. Du kannst unten selbst auswählen oder Punkte auf der Karte bündeln.", 15, false);
        int index = 0;
        for (List<FindingBundle> suggestion : suggestions) {
            LinearLayout group = card();
            int count = suggestion.stream().mapToInt(b -> b.observations().size()).sum();
            TextView badge = text(group, "VORSCHLAG " + (++index) + "   ·   " + count + " Messungen", 13, true);
            badge.setTextColor(GREEN);
            text(group, suggestion.get(0).title(), 21, true);
            List<Finding> members = new ArrayList<>();
            for (FindingBundle bundle : suggestion) members.addAll(bundle.observations());
            double span = 0;
            for (Finding a : members) for (Finding b : members) span = Math.max(span, BundleSuggestions.distance(a, b));
            text(group, String.format(Locale.GERMANY, "Messpunkte bis %.1f m auseinander", span), 14, false);
            for (FindingBundle bundle : suggestion) observationRows(group, bundle);
            button(group, "Als dieselbe Fundstelle bestätigen", () -> confirm(suggestion.stream().map(FindingBundle::key).collect(Collectors.toList())));
        }
        text(content, "Fundstellen selbst auswählen", 20, true);
        text(content, "Auch für ungenaues GPS geeignet. Die Bestätigung zeigt nochmals alle Nummern und den größten Abstand.", 14, false);
        List<Long> keys = new ArrayList<>(); List<CheckBox> boxes = new ArrayList<>();
        for (FindingBundle bundle : all) {
            LinearLayout item = card();
            CheckBox chosen = new CheckBox(this);
            chosen.setText(getString(R.string.bundle_choice, bundle.title(), bundle.referenceLabel())); chosen.setTextSize(16); chosen.setTextColor(INK);
            item.addView(chosen); keys.add(bundle.key()); boxes.add(chosen);
            observationRows(item, bundle);
            if (bundle.observations().size() > 1) button(item, "Diese Bündelung auflösen", () -> new AlertDialog.Builder(this)
                    .setTitle("Beobachtungen wieder einzeln anzeigen?").setMessage("Alle Messungen und Fotos bleiben erhalten.")
                    .setNegativeButton("Abbrechen", null).setPositiveButton("Auflösen", (d, w) -> { store.separateBundle(bundle.key()); render(); }).show());
        }
        button(content, "Ausgewählte Fundstellen bündeln", () -> {
            List<Long> selected = new ArrayList<>();
            for (int i = 0; i < keys.size(); i++) if (boxes.get(i).isChecked()) selected.add(keys.get(i));
            confirm(selected);
        }).setId(R.id.bundle_merge);
    }
    private void observationRows(LinearLayout parent, FindingBundle bundle) {
        for (Finding f : bundle.observations()) {
            TextView title = text(parent, "#" + f.id() + "   " + (f.reviewed() ? "✓ Freigegeben" : "○ Offen"), 16, true);
            title.setTextColor(GREEN);
            String measured = f.peak() == null ? "Manuell markiert" : String.format(Locale.GERMANY, "Sensorreaktion: %.1f m/s²", f.peak());
            text(parent, DATE.format(Instant.ofEpochMilli(f.timeMillis())) + "\n" + measured
                    + String.format(Locale.GERMANY, " · GPS ± %.0f m", f.accuracy()), 14, false);
        }
    }
    private void confirm(List<Long> keys) { BundleConfirmation.show(this, store, keys, this::render); }
    private LinearLayout card() {
        LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable background = new GradientDrawable(); background.setColor(0xffffffff); background.setCornerRadius(dp(18));
        background.setStroke(dp(1), 0xffdce9e3); item.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, dp(8), 0, dp(8)); content.addView(item, params); return item;
    }
    private TextView text(LinearLayout parent, String value, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(INK);
        if (bold) t.setTypeface(null, Typeface.BOLD); t.setPadding(0, dp(6), 0, dp(6)); parent.addView(t); return t;
    }
    private Button button(LinearLayout parent, String value, Runnable action) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setMinHeight(dp(48)); b.setTextColor(GREEN);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffe8f2ee)); parent.addView(b); b.setOnClickListener(v -> action.run()); return b;
    }
    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
    @Override public void onDestroy() { store.close(); super.onDestroy(); }
}
