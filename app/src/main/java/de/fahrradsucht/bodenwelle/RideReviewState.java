package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

/** Persistent ID boundaries distinguish a ride's new findings from old hotspots and survive background stops. */
// Persist these tiny boundaries before stopping the service or claiming the one-off UI.
@android.annotation.SuppressLint("ApplySharedPref")
final class RideReviewState {
    private RideReviewState(){}
    private static SharedPreferences preferences(Context c){return c.getSharedPreferences("ride_review",Context.MODE_PRIVATE);}
    private static long lastId(FindingStore store) {
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT COALESCE(MAX(id),0) FROM findings",null)){c.moveToFirst();return c.getLong(0);}
    }
    static void begin(Context c,FindingStore store) {
        preferences(c).edit().putBoolean("active",true).putLong("after",lastId(store)).commit();
    }
    static void end(Context c,FindingStore store) {
        SharedPreferences p=preferences(c);if(!p.getBoolean("active",false))return;
        p.edit().putBoolean("active",false).putBoolean("pending",true).putLong("pending_after",p.getLong("after",0))
                .putLong("pending_through",lastId(store)).commit();
    }
    /** Only a visible Activity launches UI; notification stops wait until the app is opened again. */
    static void maybeLaunch(Activity activity,FindingStore store) {
        SharedPreferences p=preferences(activity);
        if(RecordingService.running||activity.isFinishing()||activity.isDestroyed()||!p.getBoolean("pending",false))return;
        long after=p.getLong("pending_after",0),through=p.getLong("pending_through",0);
        p.edit().putBoolean("pending",false).commit();
        if(RideReviewCandidates.pairs(store.bundles(),after,through).isEmpty())return;
        activity.startActivity(new Intent(activity,RideReviewActivity.class).putExtra("after",after).putExtra("through",through));
    }
}
