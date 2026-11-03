package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/** Collapsible optional sections keep everyday review short. */
public final class FindingReviewForm {
    public final LinearLayout root;
    public final LinearLayout photos;
    private final Spinner bump, road, surface;
    public final EditText note, height;
    public final CheckBox release, heightMeasured, roadManual, surfaceManual;
    private final CheckBox[] hazards = new CheckBox[Classification.HAZARDS.length];
    private final Context context;
    private final boolean place;
    public final Spinner releaseScope;
    public final Button measurements;
    private TextView geodataStatus;
    public FindingReviewForm(Context c,Finding f){this(c,f,null);}
    public FindingReviewForm(Context c,Finding f,java.util.List<Finding> members) {
        place=members!=null&&members.size()>1;
        context = c; root = column(); root.setPadding(dp(20), dp(12), dp(20), 0);
        text(root, "Was hast du dort gesehen?");
        if (f.details().section() != null) {
            text(root, f.details().section().label());
            text(root, "Automatisch vorgeschlagener Abschnitt; Ausdehnung und Zustand bitte vor Ort prüfen. Anfang und Ende sind GPS-Schätzungen, die Länge stammt aus Geschwindigkeit und Dauer.");
        }
        bump = spinner(root, Classification.BUMPS, Classification.bumpId(f.category()));
        if (!f.details().legacyCategory().isEmpty()) text(root, "Frühere Angabe: " + f.details().legacyCategory());
        text(root, place ? members.size()+" Einzelmessungen · ursprüngliche Messwerte bleiben erhalten" : f.details().intensity().label());
        text(root, "Beschreibung der Stelle");
        note = new EditText(c); note.setHint("Straße, Fahrtrichtung, sichtbarer Schaden …"); note.setText(f.note());
        note.setMinLines(2); note.setMaxLines(6);
        note.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(note);
        photos = section(c.getString(R.string.photos_section));
        LinearLayout risk = section("Zusätzliche Gefährdungsmerkmale (optional)");
        for (int i = 0; i < hazards.length; i++) hazards[i] = check(risk, Classification.HAZARDS[i].label(), (f.details().hazards() & (1 << i)) != 0);
        LinearLayout geo = section("Verkehrsfläche und Oberfläche prüfen");
        geodataStatus=text(geo,GeodataDescription.summary(f.details()));
        Button retry=new Button(c);retry.setText(R.string.osm_retry);retry.setAllCaps(false);retry.setId(R.id.osm_retry);geo.addView(retry);
        retry.setOnClickListener(v->{if(OsmEnricher.enabled(c)){OsmEnricher.refresh(c,f.id());android.widget.Toast.makeText(c,"Neue OSM-Abfrage angefordert; Abfragepausen bleiben erhalten.",android.widget.Toast.LENGTH_LONG).show();}else android.widget.Toast.makeText(c,"Bitte unter Mehr → Karte & Geodaten die OSM-Ergänzung einschalten.",android.widget.Toast.LENGTH_LONG).show();});
        Button diagnose=new Button(c);diagnose.setText(R.string.osm_diagnose);diagnose.setAllCaps(false);diagnose.setId(R.id.osm_diagnose);geo.addView(diagnose);
        diagnose.setOnClickListener(v->new android.app.AlertDialog.Builder(c).setTitle("OSM-Abrufdiagnose").setMessage(new OsmDiagnostics(c).summary()).setPositiveButton("Schließen",null).show());
        roadManual = check(geo, "Verkehrsfläche selbst festlegen", f.details().trafficManual());
        road = spinner(geo, Classification.ROADS, f.details().traffic()); road.setEnabled(roadManual.isChecked());
        roadManual.setOnCheckedChangeListener((button, checked) -> road.setEnabled(checked));
        surfaceManual = check(geo, "Oberfläche selbst festlegen", f.details().surfaceManual());
        surface = spinner(geo, Classification.SURFACES, f.details().surface()); surface.setEnabled(surfaceManual.isChecked());
        surfaceManual.setOnCheckedChangeListener((button, checked) -> surface.setEnabled(checked));
        text(geo, "Manuelle Angaben bleiben bei späteren OSM-Abfragen erhalten. Häkchen entfernen übernimmt wieder den OSM-Vorschlag. Eine OSM-Angabe ersetzt keine Prüfung der Beschilderung vor Ort.");
        LinearLayout measurement = section("Vor Ort gemessene Höhe ergänzen (optional)");
        if(place)root.getChildAt(root.indexOfChild(measurement)-1).setVisibility(View.GONE);
        text(measurement, "Nur einen tatsächlich gemessenen Höhenunterschied eingeben. Sensorwerte sind keine Millimeterwerte. Die fünf Höhenklassen sind projektintern und keine gesetzlichen Grenzwerte.");
        heightMeasured = check(measurement, "Ich habe den Höhenunterschied vor Ort gemessen", f.details().heightMm() != null);
        height = new EditText(c); height.setHint("Höhenunterschied in mm");
        height.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (f.details().heightMm() != null) {
            java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance(java.util.Locale.GERMANY);
            format.setGroupingUsed(false); format.setMaximumFractionDigits(10); height.setText(format.format(f.details().heightMm()));
        }
        height.setEnabled(heightMeasured.isChecked()); measurement.addView(height);
        heightMeasured.setOnCheckedChangeListener((button, checked) -> height.setEnabled(checked));
        measurements=new Button(c);measurements.setText(R.string.bundle_measurements);measurements.setAllCaps(false);measurements.setId(R.id.bundle_measurements);measurements.setVisibility(place?View.VISIBLE:View.GONE);root.addView(measurements);
        releaseScope=spinner(root,new Classification.Choice[]{new Classification.Choice("keep","Freigaben unverändert lassen"),new Classification.Choice("all","Alle Messungen freigeben"),new Classification.Choice("none","Alle Freigaben zurücknehmen")},"keep");
        releaseScope.setId(R.id.bundle_release_scope);releaseScope.setVisibility(place?View.VISIBLE:View.GONE);
        if(place)text(root,members.stream().filter(Finding::reviewed).count()+" von "+members.size()+" Messungen sind bereits freigegeben. Änderungen der Beschreibung übernehmen die Freigaben nicht automatisch.");
        release = check(root, c.getString(R.string.review_release), f.reviewed());if(place)release.setVisibility(View.GONE);
        text(root, "Nur bestätigen, wenn du die Stelle zuordnen und beschreiben kannst. Ein Sensorwert allein belegt keinen Schaden.");
    }
    public String category() { return ((Classification.Choice) bump.getSelectedItem()).id(); }
    public FindingDetails details(Finding f) {
        Double mm = place?f.details().heightMm():null;
        if (!place&&heightMeasured.isChecked()) {
            try { mm = Double.valueOf(height.getText().toString().trim().replace(',', '.')); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Bitte eine gemessene Höhe in Millimetern eingeben."); }
            if (!Double.isFinite(mm) || mm < 0) throw new IllegalArgumentException("Die Höhe muss eine endliche Zahl ab 0 mm sein.");
        }
        int flags = 0; for (int i = 0; i < hazards.length; i++) if (hazards[i].isChecked()) flags |= 1 << i;
        return f.details().reviewed(mm, f.peak(), flags, ((Classification.Choice) road.getSelectedItem()).id(), roadManual.isChecked(),
                ((Classification.Choice) surface.getSelectedItem()).id(), surfaceManual.isChecked());
    }
    private LinearLayout section(String title) {
        Button button = new Button(context); button.setAllCaps(false); button.setText(title); root.addView(button);
        LinearLayout content = column(); content.setVisibility(View.GONE); root.addView(content);
        button.setOnClickListener(v -> content.setVisibility(content.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        return content;
    }
    private Spinner spinner(LinearLayout parent, Classification.Choice[] choices, String selected) {
        Spinner spinner = new Spinner(context); spinner.setMinimumHeight(dp(48));
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, choices));
        spinner.setSelection(Classification.index(choices, selected)); parent.addView(spinner); return spinner;
    }
    private CheckBox check(LinearLayout parent, String text, boolean checked) {
        CheckBox box = new CheckBox(context); box.setText(text); box.setChecked(checked); parent.addView(box); return box;
    }
    public void refreshAutomatic(Finding latest, java.util.List<Finding> members) {
        geodataStatus.setText(GeodataDescription.summary(latest.details()));
        if(!roadManual.isChecked())road.setSelection(Classification.index(Classification.ROADS,SharedFindingDetails.automatic(members,true).value()));
        if(!surfaceManual.isChecked())surface.setSelection(Classification.index(Classification.SURFACES,SharedFindingDetails.automatic(members,false).value()));
    }
    private TextView text(LinearLayout parent, String value) {
        TextView label = new TextView(context); label.setText(value); label.setTextSize(15); label.setTextColor(0xff1b302e);
        label.setPadding(0, dp(6), 0, dp(6)); parent.addView(label);return label;
    }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(context); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}
