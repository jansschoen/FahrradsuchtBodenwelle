package de.fahrradsucht.bodenwelle;

import android.app.*;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;
import android.widget.TextView;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35})
public class RideReviewWorkflowTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");app.getSharedPreferences("ride_review",0).edit().clear().commit();OsmEnricher.enabled(app,false);RecordingService.running=false;}
    private long insert(FindingStore store,double lat){Location l=new Location("gps");l.setLatitude(lat);l.setLongitude(13.4);l.setAccuracy(5);return store.insert(l,null);}
    private Intent request(long after,long through){return new Intent(app,RideReviewActivity.class).putExtra("after",after).putExtra("through",through);}
    @Test public void stopIsClaimedOnceAndBackgroundStopCanWaitForTheNextVisibleScreen() {
        try(FindingStore store=new FindingStore(app);var main=Robolectric.buildActivity(MainActivity.class).setup()) {
            long old=insert(store,52.5);RideReviewState.begin(app,store);long fresh=insert(store,52.50001);
            RecordingService.running=true;RideReviewState.end(app,store);RideReviewState.maybeLaunch(main.get(),store);
            assertNull(Shadows.shadowOf(main.get()).getNextStartedActivity());
            RecordingService.running=false;RideReviewState.maybeLaunch(main.get(),store);
            Intent launched=Shadows.shadowOf(main.get()).getNextStartedActivity();assertNotNull(launched);
            assertEquals(RideReviewActivity.class.getName(),launched.getComponent().getClassName());assertEquals(old,launched.getLongExtra("after",-1));assertEquals(fresh,launched.getLongExtra("through",-1));
            RideReviewState.maybeLaunch(main.get(),store);assertNull(Shadows.shadowOf(main.get()).getNextStartedActivity());
        }
    }
    @Test public void noNewFindingsOrOnlyIsolatedPointsDoNotOpenTheWizard() {
        try(FindingStore store=new FindingStore(app);var main=Robolectric.buildActivity(MainActivity.class).setup()) {
            insert(store,52.5);insert(store,52.50001);RideReviewState.begin(app,store);RideReviewState.end(app,store);RideReviewState.maybeLaunch(main.get(),store);
            assertNull(Shadows.shadowOf(main.get()).getNextStartedActivity());
            RideReviewState.begin(app,store);insert(store,52.6);RideReviewState.end(app,store);RideReviewState.maybeLaunch(main.get(),store);
            assertNull(Shadows.shadowOf(main.get()).getNextStartedActivity());
        }
    }
    @Test public void immediatelyFinishingAndSkippingLeaveAllMeasurementsUntouched() {
        long a,b;try(FindingStore s=new FindingStore(app)){a=insert(s,52.5);b=insert(s,52.50001);}
        try(var c=Robolectric.buildActivity(RideReviewActivity.class,request(a,b)).setup()) {
            assertNotNull(c.get().findViewById(R.id.ride_review_map));c.get().findViewById(R.id.ride_review_finish).performClick();assertTrue(c.get().isFinishing());
        }
        try(var c=Robolectric.buildActivity(RideReviewActivity.class,request(a,b)).setup();FindingStore s=new FindingStore(app)) {
            c.get().findViewById(R.id.ride_review_skip).performClick();assertTrue(c.get().isFinishing());assertEquals(2,s.bundles().size());assertEquals(2,s.count());
        }
    }
    @Test public void mergeRequiresConfirmationAndRotationKeepsTheCurrentComparison() {
        long a,b,c;try(FindingStore s=new FindingStore(app)){a=insert(s,52.5);b=insert(s,52.50001);c=insert(s,52.50002);}
        try(var activity=Robolectric.buildActivity(RideReviewActivity.class,request(a,c)).setup();FindingStore s=new FindingStore(app)) {
            activity.get().findViewById(R.id.ride_review_merge).performClick();assertEquals(3,s.bundles().size());
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(3,s.bundles().size());
            activity.get().findViewById(R.id.ride_review_skip).performClick();String before=((TextView)activity.get().findViewById(R.id.ride_review_summary)).getText().toString();
            activity.recreate();assertEquals(before,((TextView)activity.get().findViewById(R.id.ride_review_summary)).getText().toString());
            activity.get().findViewById(R.id.ride_review_merge).performClick();
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(2,s.bundles().size());assertEquals(3,s.count());assertTrue(s.all().stream().noneMatch(Finding::reviewed));
        }
    }
    @Test public void staleDeletedAndAlreadyMergedPairsAreSkippedSafely() {
        long a,b;try(FindingStore s=new FindingStore(app)){a=insert(s,52.5);b=insert(s,52.50001);s.mergeBundles(List.of(-a,-b));}
        try(var activity=Robolectric.buildActivity(RideReviewActivity.class,request(a,b)).setup()) {assertTrue(activity.get().isFinishing());}
    }
}
