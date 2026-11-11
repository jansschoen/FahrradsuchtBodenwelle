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
        boolean different=BundleDescription.differs(store,members);
        BundleMergeForm form = different?new BundleMergeForm(activity,store,members):null;
        String expected=BundleDescription.signature(store,members);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle("Dieselbe Unebenheit?")
                .setMessage("Messungen " + FindingBundle.references(members) + "\n\n" + members.size()
                        + " Beobachtungen zusammenfassen? Größter Abstand der Messpunkte: " + Math.round(span)
                        + " m. Prüfe Straßenseite und benachbarte Schäden. GPS kann ungenau sein."
                        + (different?"\n\nDie Angaben unterscheiden sich. Prüfe unten das gemeinsame Ergebnis.":"\n\nDie gemeinsamen Angaben und Fotos stimmen überein und werden unverändert übernommen. Messwerte, Höhen und einzelne Freigaben bleiben erhalten."))
                .setNegativeButton("Abbrechen", null).setPositiveButton("Bündeln & übernehmen", null);
        if(form!=null){android.widget.ScrollView scroll=new android.widget.ScrollView(activity);scroll.addView(form.root);builder.setView(scroll);}
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try { store.mergeDescribedBundles(keys,form==null?SharedFindingDetails.from(members):form.value(),expected); dialog.dismiss(); changed.run(); }
            catch (IllegalArgumentException e) { Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show(); }
        });
    }
}
