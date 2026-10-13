package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Select, inspect and hand off a draft; sending remains entirely with the mail client. */
public final class MailReportActivity extends Activity {
    private FindingStore store;
    private DistrictCatalog catalog;
    private BerlinDistricts boundaries;
    private SharedPreferences settings;
    private LinearLayout content, drafts;
    private CheckBox split, appNotice, notes;
    private EditText name, recipient, subject, body;
    private Spinner recipientOffice;
    private TextView source, handoff;
    private final List<Row> rows = new ArrayList<>();
    private List<MailReports.Group> groups = new ArrayList<>();
    private final Map<String, DraftState> saved = new HashMap<>();
    private MailReports.Group current;
    private boolean building;
    private record Row(long id, CheckBox selected, Spinner district, BerlinDistricts.Assignment automatic) {}
    private record DistrictOption(String id, String label) { @Override public String toString() { return label; } }
    private static final class DraftState {
        String address = "", authority = "", subject = "", body = "";
        boolean handedOff;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new FindingStore(this); settings = getSharedPreferences("mail_reports", MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        ScrollView scroll = new ScrollView(this); content = column(); content.setPadding(dp(20), dp(16), dp(20), dp(24)); scroll.addView(content);
        if (Build.VERSION.SDK_INT >= 30) content.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(dp(20) + bars.left, dp(16) + bars.top, dp(20) + bars.right, dp(24) + bars.bottom); return insets;
        });
        scroll.setBackgroundColor(0xfff4f7f5); setContentView(scroll);
        button(content, "Zurück zu den Fundstellen", this::finish);
        text(content, "Meldung an die Verwaltung", 24);
        text(content, "Wähle geprüfte Stellen aus, prüfe Bezirk und Text und öffne den Entwurf in deinem Mailprogramm. Erst dort versendest du die Mail.", 16);
        try { catalog = new DistrictCatalog(this); boundaries = new BerlinDistricts(this); }
        catch (IOException e) { text(content, "Bezirksdaten konnten nicht geladen werden. Bitte die App aktualisieren.", 16); return; }
        List<Finding> reviewed = new ArrayList<>(); for (Finding f : store.all()) if (f.reviewed()) reviewed.add(f);
        if (reviewed.isEmpty()) { text(content, "Bitte zuerst Fundstellen prüfen und freigeben.", 16); return; }
        boolean uncertain = reviewed.stream().anyMatch(f -> assignment(f).districtId().isEmpty());
        Button toggle = button(content, "Stellen und Bezirke prüfen · " + reviewed.size(), () -> {});
        LinearLayout selection = column(); content.addView(selection); selection.setVisibility(uncertain ? View.VISIBLE : View.GONE);
        toggle.setOnClickListener(v -> selection.setVisibility(selection.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        List<DistrictOption> options = new ArrayList<>();
        for (Finding f : reviewed) {
            BerlinDistricts.Assignment automatic = automaticWithoutOverride(f);
            String override = store.reportDistrict(f.id());
            CheckBox selected = check(selection, Classification.label(Classification.BUMPS, f.category()) + " · " + MailReports.calendarWeek(f.timeMillis()), true);
            selected.setId(View.generateViewId());
            text(selection, "Lage: " + String.format(java.util.Locale.ROOT, "%.5f, %.5f", f.latitude(), f.longitude()), 14);
            options = new ArrayList<>(); options.add(new DistrictOption("", "Automatisch: " + catalog.name(automatic.districtId())));
            for (DistrictCatalog.Authority a : catalog.all()) options.add(new DistrictOption(a.id(), a.name() + " · manuell"));
            Spinner district = spinner(selection, options); int index = 0;
            for (int i = 0; i < options.size(); i++) if (!override.isEmpty() && options.get(i).id().equals(override)) index = i;
            district.setSelection(index);
            district.setOnItemSelectedListener(listener(() -> {
                String id = ((DistrictOption) district.getSelectedItem()).id();
                if (!id.equals(store.reportDistrict(f.id()))) store.reportDistrict(f.id(), id);
            }));
            if (automatic.districtId().isEmpty()) text(selection, "Bezirk/Zuständigkeit unklar: Grenze, fehlende Zuordnung oder besondere Verkehrsfläche. Bitte prüfen oder später einen eigenen Empfänger wählen.", 14);
            rows.add(new Row(f.id(), selected, district, automatic));
        }
        text(content, "Die Bezirksämter sind vorgeschlagene Anlaufstellen. Nahe Grenzen und bei besonderen Straßen kann eine andere Stelle zuständig sein. Offline-Grenzen: älterer Datenstand (2013); Vorschläge bitte prüfen.", 14);
        TextView attribution = text(content, "Bezirksgrenzen: Amt für Statistik Berlin-Brandenburg, m-hoerz und Berliner Morgenpost · CC BY 3.0 DE · Quelle ansehen", 12);
        attribution.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/funkeinteraktiv/Berlin-Geodaten"))); }
            catch (ActivityNotFoundException e) { toast("Kein Browser gefunden."); }
        });
        split = check(content, "Nach Bezirk aufteilen", settings.getBoolean("split", true)); split.setId(R.id.mail_split);
        appNotice = check(content, "Kurzen Hinweis „mit Bodenwelle zusammengestellt“ ergänzen", settings.getBoolean("notice", true));
        notes = check(content, "Eigene Notizen übernehmen", false);
        text(content, "Automatische Zeitangaben werden auf Kalenderwochen reduziert. Notizen sind zunächst aus: Sie können Namen oder genaue Datumsangaben enthalten. Fahrtspur und Rohdatenexport werden nicht angehängt.", 14);
        text(content, "Dein Name (optional)", 15); name = edit(content, settings.getString("name", ""), false); name.setId(R.id.mail_name);
        button(content, "Mailentwürfe erzeugen", this::prepare).setId(R.id.mail_prepare);
        drafts = column(); content.addView(drafts);
        if (state != null) restore(state);
    }
    private BerlinDistricts.Assignment assignment(Finding f) {
        String manual = store.reportDistrict(f.id());
        if (!manual.isEmpty()) return new BerlinDistricts.Assignment(manual, "manual");
        String road = f.details().osm().tags().getOrDefault("highway", "");
        if ("motorway".equals(road) || "motorway_link".equals(road) || "trunk".equals(road) || "private".equals(f.details().osm().tags().get("access")))
            return new BerlinDistricts.Assignment("", "special_jurisdiction");
        return boundaries.assign(f, "");
    }
    private void prepare() {
        saveCurrent(); saved.clear(); current = null;
        settings.edit().putBoolean("split", split.isChecked()).putBoolean("notice", appNotice.isChecked()).putString("name", name.getText().toString().trim()).apply();
        List<MailReports.Item> items = new ArrayList<>();
        for (Row row : rows) if (row.selected.isChecked()) {
            Finding f = store.get(row.id); if (f == null || !f.reviewed()) continue;
            String manual = ((DistrictOption) row.district.getSelectedItem()).id();
            BerlinDistricts.Assignment selected = manual.isEmpty() ? automaticWithoutOverride(f) : new BerlinDistricts.Assignment(manual, "manual");
            items.add(new MailReports.Item(f, selected.districtId(), selected.method()));
        }
        groups = MailReports.groups(items, split.isChecked()); drafts.removeAllViews();
        if (groups.isEmpty()) { text(drafts, "Keine freigegebenen Stellen ausgewählt.", 16); return; }
        text(drafts, groups.size() + " Entwurf/Entwürfe · höchstens 20 Stellen pro Mail", 16);
        List<String> labels = new ArrayList<>();
        for (MailReports.Group group : groups) labels.add(groupLabel(group));
        Spinner groupChoice = spinner(drafts, labels); groupChoice.setId(R.id.mail_group);
        groupChoice.setOnItemSelectedListener(listener(() -> showGroup(groups.get(groupChoice.getSelectedItemPosition()))));
        showGroup(groups.get(0));
    }
    private BerlinDistricts.Assignment automaticWithoutOverride(Finding f) {
        String road = f.details().osm().tags().getOrDefault("highway", "");
        if ("motorway".equals(road) || "motorway_link".equals(road) || "trunk".equals(road) || "private".equals(f.details().osm().tags().get("access")))
            return new BerlinDistricts.Assignment("", "special_jurisdiction");
        return boundaries.assign(f, "");
    }
    private String groupLabel(MailReports.Group g) {
        return (split.isChecked() ? catalog.name(g.districtId()) : "Gesamte Auswahl") + " · " + g.items().size() + " Stellen"
                + (g.parts() > 1 ? " · Teil " + g.part() + "/" + g.parts() : "");
    }
    private LinearLayout preview;
    private void showGroup(MailReports.Group group) {
        if (current != null && current.key().equals(group.key())) return;
        saveCurrent(); current = group; building = true;
        if (preview != null) drafts.removeView(preview);
        preview = column(); drafts.addView(preview);
        DraftState state = saved.get(group.key());
        if (state == null) {
            state = new DraftState();
            String office = group.districtId();
            if (office.isEmpty() && !group.items().get(0).districtId().isEmpty()
                    && group.items().stream().allMatch(i -> i.districtId().equals(group.items().get(0).districtId()))) office = group.items().get(0).districtId();
            DistrictCatalog.Authority a = catalog.get(office);
            if (a != null) { state.authority = a.id(); state.address = settings.getString("address_" + a.id(), a.email()); }
            MailReports.Draft template = MailReports.draft(group, group.districtId().isEmpty() ? "" : catalog.name(group.districtId()), appNotice.isChecked(), notes.isChecked(), name.getText().toString());
            state.subject = template.subject(); state.body = template.body(); saved.put(group.key(), state);
        }
        List<DistrictOption> offices = new ArrayList<>(); offices.add(new DistrictOption("", "Andere Anlaufstelle / eigene E-Mail"));
        for (DistrictCatalog.Authority a : catalog.all()) offices.add(new DistrictOption(a.id(), "Bezirksamt " + a.name()));
        text(preview, "Anlaufstelle prüfen", 18); recipientOffice = spinner(preview, offices); int officeIndex = 0;
        for (int i = 0; i < offices.size(); i++) if (!state.authority.isEmpty() && offices.get(i).id().equals(state.authority)) officeIndex = i;
        recipientOffice.setSelection(officeIndex);
        source = text(preview, "", 14);
        button(preview, "Offizielle Kontaktseite ansehen", () -> {
            DistrictCatalog.Authority a = selectedAuthority();
            if (a == null) { toast("Wähle ein Bezirksamt, um seine Kontaktseite zu öffnen."); return; }
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(a.source()))); }
            catch (ActivityNotFoundException e) { toast("Kein Browser gefunden."); }
        });
        text(preview, "Empfänger", 15); recipient = edit(preview, state.address, false); recipient.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); recipient.setId(R.id.mail_recipient);
        text(preview, "Betreff", 15); subject = edit(preview, state.subject, false); subject.setId(R.id.mail_subject);
        text(preview, "Mailtext – vor der Übergabe prüfen", 18); body = edit(preview, state.body, true); body.setId(R.id.mail_body);
        handoff = text(preview, state.handedOff ? "Entwurf bereits an das Mailprogramm übergeben; Versand dort prüfen." : "Es wurde noch keine Mail versendet.", 14);
        recipientOffice.setOnItemSelectedListener(listener(() -> {
            if (building) return;
            DraftState currentState = saved.get(current.key()); String next = ((DistrictOption) recipientOffice.getSelectedItem()).id();
            if (!next.equals(currentState.authority)) {
                currentState.authority = next; DistrictCatalog.Authority a = catalog.get(next);
                if (a != null) recipient.setText(settings.getString("address_" + a.id(), a.email()));
                else recipient.setText("");
            }
            showSource();
        }));
        button(preview, "Im Mailprogramm öffnen", this::openMail).setId(R.id.mail_open);
        button(preview, "Mailtext kopieren", () -> {
            if (!stillReviewed()) return;
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Meldung", "Betreff: " + subject.getText() + "\n\n" + body.getText()));
            toast("Betreff und Text kopiert.");
        });
        building = false; showSource();
    }
    private DistrictCatalog.Authority selectedAuthority() { return catalog.get(((DistrictOption) recipientOffice.getSelectedItem()).id()); }
    private void showSource() {
        DistrictCatalog.Authority a = selectedAuthority();
        source.setText(a == null ? "Bitte eine passende Anlaufstelle und ihre Adresse selbst eintragen." : a.office() + "\nVeröffentlichter Kontakt: " + a.email() + "\nAuf Berlin.de geprüft am " + a.checkedAt() + ". Bitte Zuständigkeit und Aktualität prüfen; die Empfängeradresse ist bearbeitbar.");
    }
    private void saveCurrent() {
        if (current == null || recipient == null) return;
        DraftState d = saved.get(current.key()); if (d == null) return;
        d.address = recipient.getText().toString().trim(); d.subject = subject.getText().toString(); d.body = body.getText().toString();
        d.authority = ((DistrictOption) recipientOffice.getSelectedItem()).id();
    }
    private void openMail() {
        try {
            // Recheck consent on handoff; opening a stale screen cannot export a revoked/deleted entry.
            if (!stillReviewed()) return;
            Intent intent = MailIntent.create(recipient.getText().toString().trim(), subject.getText().toString(), body.getText().toString());
            startActivity(intent); saveCurrent(); saved.get(current.key()).handedOff = true;
            DistrictCatalog.Authority a = selectedAuthority();
            if (a != null) settings.edit().putString("address_" + a.id(), recipient.getText().toString().trim()).apply();
            handoff.setText(R.string.mail_handed_off);
        } catch (IllegalArgumentException e) { toast(e.getMessage()); }
        catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this).setTitle("Kein Mailprogramm gefunden").setMessage("Richte ein Mailprogramm auf dem Handy ein. Du kannst den Entwurf auch über „Mailtext kopieren“ übernehmen.").setPositiveButton("Verstanden", null).show();
        }
    }
    private boolean stillReviewed() {
        for (MailReports.Item item : current.items()) {
            Finding f = store.get(item.finding().id());
            if (f == null || !f.reviewed()) { toast("Eine Stelle ist nicht mehr freigegeben. Bitte die Entwürfe neu erzeugen."); return false; }
        }
        return true;
    }
    @Override public void onSaveInstanceState(Bundle out) {
        saveCurrent();
        if (split != null) {
            out.putBoolean("split", split.isChecked()); out.putBoolean("notice", appNotice.isChecked()); out.putBoolean("notes", notes.isChecked()); out.putString("name", name.getText().toString());
            ArrayList<String> selected = new ArrayList<>(); for (Row row : rows) if (row.selected.isChecked()) selected.add(Long.toString(row.id)); out.putStringArrayList("selected", selected);
            Bundle all = new Bundle();
            for (Map.Entry<String, DraftState> e : saved.entrySet()) {
                Bundle d = new Bundle(); d.putString("address", e.getValue().address); d.putString("authority", e.getValue().authority); d.putString("subject", e.getValue().subject); d.putString("body", e.getValue().body); d.putBoolean("handoff", e.getValue().handedOff); all.putBundle(e.getKey(), d);
            }
            out.putBundle("drafts", all); if (current != null) out.putString("group", current.key());
        }
        super.onSaveInstanceState(out);
    }
    private void restore(Bundle state) {
        split.setChecked(state.getBoolean("split", true)); appNotice.setChecked(state.getBoolean("notice", true)); notes.setChecked(state.getBoolean("notes")); name.setText(state.getString("name", ""));
        ArrayList<String> selected = state.getStringArrayList("selected"); if (selected != null) for (Row row : rows) row.selected.setChecked(selected.contains(Long.toString(row.id)));
        Bundle draftsState = state.getBundle("drafts"); if (draftsState == null || draftsState.isEmpty()) return;
        prepare(); saved.clear(); current = null;
        for (String key : draftsState.keySet()) {
            Bundle b = draftsState.getBundle(key); if (b == null) continue; DraftState d = new DraftState();
            d.address = b.getString("address", ""); d.authority = b.getString("authority", ""); d.subject = b.getString("subject", ""); d.body = b.getString("body", ""); d.handedOff = b.getBoolean("handoff"); saved.put(key, d);
        }
        Spinner choice = findViewById(R.id.mail_group);
        for (int i = 0; i < groups.size(); i++) if (groups.get(i).key().equals(state.getString("group"))) { choice.setSelection(i); showGroup(groups.get(i)); break; }
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private TextView text(LinearLayout parent, String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(0xff1b302e); v.setPadding(0, dp(6), 0, dp(6)); parent.addView(v); return v; }
    private Button button(LinearLayout parent, String value, Runnable action) { Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setMinHeight(dp(48)); b.setTextColor(0xff087f73); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffe8f2ee)); parent.addView(b); b.setOnClickListener(v -> action.run()); return b; }
    private CheckBox check(LinearLayout parent, String value, boolean checked) { CheckBox b = new CheckBox(this); b.setText(value); b.setChecked(checked); parent.addView(b); return b; }
    private EditText edit(LinearLayout parent, String value, boolean multi) { EditText e = new EditText(this); e.setText(value); e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | (multi ? android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0)); if (multi) { e.setMinLines(8); e.setMaxLines(18); } else e.setSingleLine(true); parent.addView(e); return e; }
    private <T> Spinner spinner(LinearLayout parent, List<T> choices) { Spinner s = new Spinner(this); s.setMinimumHeight(dp(48)); s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, choices)); parent.addView(s); return s; }
    private AdapterView.OnItemSelectedListener listener(Runnable action) { return new AdapterView.OnItemSelectedListener() { @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) { action.run(); } @Override public void onNothingSelected(AdapterView<?> p) {} }; }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override public void onDestroy() { if (store != null) store.close(); super.onDestroy(); }
}
