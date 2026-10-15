package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** The map and list use the same explicit, reversible confirmation. */
public final class BundleConfirmation {
    private BundleConfirmation() {}
    public static void show(Activity activity, FindingStore store, List<Long> selected, Runnable changed) {
        List<Long> keys = new ArrayList<>(new LinkedHashSet<>(selected));
        List<Finding> members = new ArrayList<>();
        for (long key : keys) {
            List<Finding> observations = store.observations(key);
            if (observations.isEmpty()) { changed.run(); return; }
            members.addAll(observations);
        }
        if (keys.size() < 2) {
            Toast.makeText(activity, "Bitte mindestens zwei Fundstellen auswählen.", Toast.LENGTH_LONG).show(); return;
        }
        double span = 0;
        for (Finding a : members) for (Finding b : members) span = Math.max(span, BundleSuggestions.distance(a, b));
        SharedFindingDetails shared = SharedFindingDetails.from(members);
        SharedFieldsForm form = new SharedFieldsForm(activity, shared);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle("Dieselbe Unebenheit?")
                .setMessage("Messungen " + FindingBundle.references(members) + "\n\n" + members.size()
                        + " Beobachtungen zusammenfassen? Größter Abstand der Messpunkte: " + Math.round(span)
                        + " m. Prüfe Straßenseite und benachbarte Schäden. GPS kann ungenau sein."
                        + "\n\nFotos, Verkehrsfläche, Oberfläche und Gefährdungsmerkmale werden für alle Messungen übernommen. Manuelle Angaben haben Vorrang; Gefährdungsmerkmale werden vereinigt."
                        + (shared.conflicting() ? " Abweichende manuelle Angaben bitte unten auswählen." : "")
                        + " Messwerte und einzelne Freigaben bleiben erhalten. Die Bündelung lässt sich auflösen.")
                .setNegativeButton("Abbrechen", null).setPositiveButton("Zusammenfassen", null);
        if (shared.conflicting()) builder.setView(form.root);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try { store.mergeBundles(keys, form.traffic(), form.surface()); dialog.dismiss(); changed.run(); }
            catch (IllegalArgumentException e) { Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show(); }
        });
    }
}
