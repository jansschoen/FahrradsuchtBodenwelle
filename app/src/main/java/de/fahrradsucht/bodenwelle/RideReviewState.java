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
    private static android.app.Application checkedOwner;
    private static long checkedAfter=-1,checkedThrough=-1,checkedRevision=-1;
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
                .putLong("pending_through",lastId(store)).putBoolean("context_prompted",false).commit();
    }
    static void dismissContextPrompt(Context c,long after,long through) {
        SharedPreferences p=preferences(c);
        if(p.getLong("pending_after",-1)==after&&p.getLong("pending_through",-1)==through)p.edit().putBoolean("context_prompted",true).commit();
    }
    /** Only a visible Activity launches UI; notification stops wait until the app is opened again. */
    static void maybeLaunch(Activity activity,FindingStore store) {
        SharedPreferences p=preferences(activity);
        if(RecordingService.running||activity.isFinishing()||activity.isDestroyed())return;
        long after=p.getLong("pending_after",0),through=p.getLong("pending_through",0);
        boolean pending=p.getBoolean("pending",false);
        boolean canCheckContext=!p.getBoolean("context_prompted",false)&&through>after;
        if(!pending&&!canCheckContext)return;
        long revision=store.revision();android.app.Application owner=activity.getApplication();
        boolean changed=owner!=checkedOwner||after!=checkedAfter||through!=checkedThrough||revision!=checkedRevision;
        if(!pending&&!changed)return;
        boolean context=false;
        if(canCheckContext&&(pending||changed)) {
            checkedOwner=owner;checkedAfter=after;checkedThrough=through;checkedRevision=revision;
            context=!OsmPlaceContext.candidates(store.all(),after,through).isEmpty();
        }
        boolean pairs=pending&&!RideReviewCandidates.pairs(store.bundles(),after,through).isEmpty();
        if(!p.getBoolean("pending",false)&&!context)return;
        p.edit().putBoolean("pending",false).putBoolean("context_prompted",context||p.getBoolean("context_prompted",false)).commit();
        if(!pairs&&!context)return;
        activity.startActivity(new Intent(activity,RideReviewActivity.class).putExtra("after",after).putExtra("through",through)
                .putExtra("context",context).putExtra("pairs",pairs));
    }
}
