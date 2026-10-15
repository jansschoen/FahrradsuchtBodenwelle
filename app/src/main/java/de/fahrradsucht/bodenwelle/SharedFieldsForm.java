package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Requires an explicit choice when merging conflicting manual place descriptions. */
final class SharedFieldsForm {
    final LinearLayout root;
    private final Spinner traffic, surface;
    SharedFieldsForm(Context context, SharedFindingDetails shared) {
        root = new LinearLayout(context); root.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * context.getResources().getDisplayMetrics().density); root.setPadding(pad, 0, pad, 0);
        traffic = field(context, "Verkehrsfläche für alle Messungen", shared.traffic(), Classification.ROADS, R.id.bundle_traffic_choice);
        surface = field(context, "Oberfläche für alle Messungen", shared.surface(), Classification.SURFACES, R.id.bundle_surface_choice);
    }
    private Spinner field(Context c, String label, SharedFindingDetails.Field field, Classification.Choice[] choices, int id) {
        if (!field.conflicting()) return null;
        TextView title = new TextView(c); title.setText(label); title.setTextSize(16); root.addView(title);
        List<Classification.Choice> values = new ArrayList<>(); values.add(new Classification.Choice("", "Bitte auswählen …"));
        for (String conflict : field.conflicts()) values.add(new Classification.Choice(conflict, Classification.label(choices, conflict)));
        if (!field.conflicts().contains("unknown")) values.add(new Classification.Choice("unknown", "Unbekannt"));
        Spinner spinner = new Spinner(c); spinner.setId(id); spinner.setMinimumHeight(Math.round(48 * c.getResources().getDisplayMetrics().density));
        spinner.setAdapter(new ArrayAdapter<>(c, android.R.layout.simple_spinner_dropdown_item, values)); root.addView(spinner); return spinner;
    }
    String traffic() { return selected(traffic); }
    String surface() { return selected(surface); }
    private static String selected(Spinner spinner) { return spinner == null ? null : ((Classification.Choice) spinner.getSelectedItem()).id(); }
}
