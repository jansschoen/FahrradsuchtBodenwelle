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
        android.widget.LinearLayout content=new android.widget.LinearLayout(activity);content.setOrientation(android.widget.LinearLayout.VERTICAL);int padding=Math.round(20*activity.getResources().getDisplayMetrics().density);content.setPadding(padding,padding,padding,padding);
        android.widget.TextView summary=new android.widget.TextView(activity);summary.setTextSize(15);
        summary.setText(activity.getString(R.string.bundle_save_summary,FindingBundle.references(members),members.size(),Math.round(span),
                activity.getString(different?R.string.bundle_save_different:R.string.bundle_save_matching)));
        content.addView(summary);if(form!=null)content.addView(form.root);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle("Dieselbe Unebenheit?")
                .setView(FormDialog.content(activity,content)).setNegativeButton("Abbrechen", null).setPositiveButton("Bündel speichern", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        FormDialog.prepare(dialog);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setId(R.id.bundle_save);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try { store.mergeDescribedBundles(keys,form==null?SharedFindingDetails.from(members):form.value(),expected); dialog.dismiss(); changed.run(); }
            catch (IllegalArgumentException e) { Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show(); }
        });
    }
}
