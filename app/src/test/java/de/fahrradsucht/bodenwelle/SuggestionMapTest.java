package de.fahrradsucht.bodenwelle;

import android.app.*;
import android.location.Location;
import android.view.*;
import android.widget.*;
import android.os.Looper;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.osmdroid.views.MapView;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35},qualifiers="w411dp-h891dp")
public class SuggestionMapTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");OsmEnricher.enabled(app,false);RecordingService.running=false;}
    private long finding(FindingStore s,double lat){Location l=new Location("gps");l.setLatitude(lat);l.setLongitude(13.4);l.setAccuracy(5);return s.insert(l,null);}
    private <T extends View>List<T> views(View v,Class<T> type){List<T> result=new ArrayList<>();if(type.isInstance(v))result.add(type.cast(v));if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)result.addAll(views(g.getChildAt(i),type));return result;}
    private void layout(Activity a) {
        View root=a.getWindow().getDecorView();
        // Robolectric's WindowManager does not dispatch app visibility with manually measured frames.
        Object viewRoot=org.robolectric.util.ReflectionHelpers.callInstanceMethod(root,"getViewRootImpl");
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(viewRoot,"handleAppVisibility",org.robolectric.util.ReflectionHelpers.ClassParameter.from(boolean.class,true));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        var metrics=a.getResources().getDisplayMetrics();
        root.measure(View.MeasureSpec.makeMeasureSpec(metrics.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(metrics.heightPixels,View.MeasureSpec.EXACTLY));
        root.layout(0,0,metrics.widthPixels,metrics.heightPixels);Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    @Test public void eachProposalContainsAMapAndOnlyVisibleCardsCreateMapProviders() {
        try(FindingStore s=new FindingStore(app)){for(int i=0;i<12;i++){finding(s,52.5+i*.002);finding(s,52.50001+i*.002);}}
        try(var c=Robolectric.buildActivity(BundleActivity.class).setup().visible().windowFocusChanged(true)) {
            layout(c.get());View root=c.get().findViewById(android.R.id.content);
            List<SuggestionMapPreview> previews=views(root,SuggestionMapPreview.class);assertEquals(12,previews.size());
            for(SuggestionMapPreview preview:previews)preview.updateVisibility();
            int maps=views(root,MapView.class).size();assertTrue(maps>0);assertTrue("Off-screen cards must not download map tiles",maps<previews.size());
            assertTrue(views(root,MapView.class).stream().allMatch(m->m.getOverlays().stream().anyMatch(o->o instanceof FindingMapOverlay)));
            c.pause();assertTrue(views(root,MapView.class).isEmpty());
        }
    }
    @Test public void previewIsCentredOnItsOwnFindingsAndDetachesWithTheOwner() {
        List<Finding> members;try(FindingStore s=new FindingStore(app)){long a=finding(s,52.6),b=finding(s,52.60001);members=List.of(s.get(a),s.get(b));}
        try(var c=Robolectric.buildActivity(Activity.class).setup().visible().windowFocusChanged(true)) {
            SuggestionMapPreview preview=new SuggestionMapPreview(c.get(),members);c.get().setContentView(preview);layout(c.get());preview.active(true);layout(c.get());
            MapView map=views(preview,MapView.class).get(0);assertEquals(52.600005,map.getMapCenter().getLatitude(),.00001);assertEquals(13.4,map.getMapCenter().getLongitude(),.00001);
            preview.active(false);assertTrue(views(preview,MapView.class).isEmpty());
        }
    }
}
