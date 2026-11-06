package de.fahrradsucht.bodenwelle;

import android.app.*;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;
import android.view.*;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35})
public class ContextReviewWorkflowTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");app.getSharedPreferences("ride_review",0).edit().clear().commit();OsmEnricher.enabled(app,false);RecordingService.running=false;}
    private long finding(FindingStore s){Location l=new Location("gps");l.setLatitude(52.5);l.setLongitude(13.4);l.setAccuracy(5);return s.insert(l,null);}
    private void park(FindingStore s,long id){s.setOsm(id,new OsmMatch("matched",42L,Map.of(OsmPlaceContext.PARK,"way/123"),1000,"low",0.0,"test","cycleway","asphalt"));}
    private Intent request(long from,long to){return new Intent(app,RideReviewActivity.class).putExtra("after",from).putExtra("through",to).putExtra("context",true).putExtra("pairs",false);}
    @Test public void knownContextAppearsAfterRideWithoutANearbyPairAndOnlyOnce() {
        try(FindingStore s=new FindingStore(app);var main=Robolectric.buildActivity(MainActivity.class).setup()) {
            RideReviewState.begin(app,s);long id=finding(s);park(s,id);RideReviewState.end(app,s);RideReviewState.maybeLaunch(main.get(),s);
            Intent next=Shadows.shadowOf(main.get()).getNextStartedActivity();assertNotNull(next);assertTrue(next.getBooleanExtra("context",false));assertFalse(next.getBooleanExtra("pairs",true));
            RideReviewState.maybeLaunch(main.get(),s);assertNull(Shadows.shadowOf(main.get()).getNextStartedActivity());assertEquals(1,s.count());
        }
    }
    @Test public void lateOsmEvidenceCanTriggerOnceButExplicitExitSuppressesLatePrompts() {
        try(FindingStore s=new FindingStore(app);var main=Robolectric.buildActivity(MainActivity.class).setup()) {
            RideReviewState.begin(app,s);long id=finding(s);RideReviewState.end(app,s);RideReviewState.maybeLaunch(main.get(),s);assertNull(Shadows.shadowOf(main.get()).getNextStartedActivity());
            park(s,id);RideReviewState.maybeLaunch(main.get(),s);assertNotNull(Shadows.shadowOf(main.get()).getNextStartedActivity());
            RideReviewState.begin(app,s);long fresh=finding(s);RideReviewState.end(app,s);RideReviewState.dismissContextPrompt(app,id,fresh);park(s,fresh);
            app.getSharedPreferences("ride_review",0).edit().putBoolean("pending",false).commit();RideReviewState.maybeLaunch(main.get(),s);assertNull(Shadows.shadowOf(main.get()).getNextStartedActivity());
        }
    }
    @Test public void keepingAndCancellingDeletionPreserveDataAndContextSurvivesRotation() {
        long id;try(FindingStore s=new FindingStore(app)){id=finding(s);park(s,id);}
        try(var c=Robolectric.buildActivity(RideReviewActivity.class,request(0,id)).setup();FindingStore s=new FindingStore(app)) {
            assertEquals(View.VISIBLE,c.get().findViewById(R.id.context_review_delete).getVisibility());assertEquals(View.GONE,c.get().findViewById(R.id.ride_review_merge).getVisibility());
            c.recreate();assertTrue(((android.widget.TextView)c.get().findViewById(R.id.ride_review_summary)).getText().toString().contains("Park"));
            c.get().findViewById(R.id.context_review_delete).performClick();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(find(dialog.findViewById(android.R.id.content),SuggestionMapPreview.class));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(1,s.count());
            c.get().findViewById(R.id.ride_review_skip).performClick();assertTrue(c.get().isFinishing());assertEquals(1,s.count());
        }
    }
    @Test public void confirmedDeletionRemovesOnlyTheSelectedMeasurement() {
        long a,b;try(FindingStore s=new FindingStore(app)){a=finding(s);b=finding(s);park(s,a);s.mergeBundles(List.of(-a,-b));}
        try(var c=Robolectric.buildActivity(RideReviewActivity.class,request(0,b)).setup();FindingStore s=new FindingStore(app)) {
            c.get().findViewById(R.id.context_review_delete).performClick();ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNull(s.get(a));assertNotNull(s.get(b));assertEquals(1,s.count());
        }
    }
    private <T extends View>T find(View v,Class<T> type){if(type.isInstance(v))return type.cast(v);if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){T found=find(g.getChildAt(i),type);if(found!=null)return found;}return null;}
}
