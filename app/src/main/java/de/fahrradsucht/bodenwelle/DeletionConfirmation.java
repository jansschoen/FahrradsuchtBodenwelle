package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** The same numbered map and explicit single-measurement confirmation in every deletion path. */
final class DeletionConfirmation {
    private DeletionConfirmation(){}
    static void show(Activity activity,Finding finding,Runnable delete) {
        int pad=Math.round(16*activity.getResources().getDisplayMetrics().density);
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(pad,0,pad,pad);
        String date=DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.of("Europe/Berlin")).format(Instant.ofEpochMilli(finding.timeMillis()));
        TextView description=new TextView(activity);description.setTextSize(15);
        String reason=OsmPlaceContext.reason(finding);
        description.setText(activity.getString(R.string.delete_measurement_details,finding.id(),date,Classification.label(Classification.BUMPS,finding.category()),
                reason.isEmpty()?"":"\n\n"+reason+"\n"+activity.getString(R.string.context_review_limit)));content.addView(description);
        SuggestionMapPreview preview=new SuggestionMapPreview(activity,List.of(finding));content.addView(preview,0,new LinearLayout.LayoutParams(-1,Math.round(210*activity.getResources().getDisplayMetrics().density)));
        TextView attribution=new TextView(activity);attribution.setText(R.string.suggestion_map_hint);attribution.setTextSize(12);content.addView(attribution,1);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Messung #"+finding.id()+" löschen?").setView(scroll)
                .setNegativeButton("Abbrechen",null).setPositiveButton("Messung löschen",(d,w)->delete.run()).create();
        scroll.setOnScrollChangeListener((v,x,y,oldx,oldy)->preview.updateVisibility());
        dialog.setOnShowListener(d->preview.post(()->preview.active(true)));dialog.setOnDismissListener(d->preview.active(false));dialog.show();
    }
}
