package de.fahrradsucht.bodenwelle;

import android.app.*;
import android.location.Location;
import android.view.*;
import android.graphics.*;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35}) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MapReleaseBadgeTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");OsmEnricher.enabled(app,false);RecordingService.running=false;}
    private <T extends View>T view(View root,Class<T> type){if(type.isInstance(root))return type.cast(root);if(root instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){T found=view(g.getChildAt(i),type);if(found!=null)return found;}return null;}
    private FindingMarkerBadge badge(Activity activity){MapView map=view(activity.findViewById(android.R.id.content),MapView.class);Marker m=(Marker)map.getOverlays().stream().filter(o->o instanceof Marker).findFirst().orElseThrow();return (FindingMarkerBadge)m.getIcon();}
    private long insert(FindingStore s){Location l=new Location("gps");l.setLatitude(52.52);l.setLongitude(13.405);l.setAccuracy(5);return s.insert(l,null);}
    @Test public void bothMapsRefreshChecksAfterReleaseAndWithdrawAndMixedGroupsNeverLookComplete() {
        try(FindingStore store=new FindingStore(app)) {
            long a=insert(store),b=insert(store);store.mergeBundles(List.of(-a,-b));
            for(Class<? extends Activity> type:List.of(MainActivity.class,MapActivity.class)) {
                try(var controller=Robolectric.buildActivity(type).setup()) {
                    RideMapPanel panel=view(controller.get().findViewById(android.R.id.content),RideMapPanel.class);panel.refresh();assertFalse(badge(controller.get()).allReleased());
                    store.review(a,"pothole","",true);panel.refresh();assertFalse(badge(controller.get()).allReleased());
                    store.review(b,"pothole","",true);panel.refresh();assertTrue(badge(controller.get()).allReleased());
                    store.review(a,"pothole","",false);panel.refresh();assertFalse(badge(controller.get()).allReleased());
                    store.review(b,"pothole","",false);
                }
            }
        }
    }
    @Test public void checkIsRenderedInsideTheDrawableAndKeepsIntensityAndNumberVisible() {
        FindingMarkerBadge icon=new FindingMarkerBadge(app.getResources(),"#42",Intensity.evaluate(null,48.0),false,1,1);
        Bitmap bitmap=Bitmap.createBitmap(icon.getIntrinsicWidth(),icon.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
        icon.setBounds(0,0,bitmap.getWidth(),bitmap.getHeight());icon.draw(new Canvas(bitmap));
        int green=0,orange=0;for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++) {
            int color=bitmap.getPixel(x,y);if(color==0xff167b37)green++;if(color==Intensity.evaluate(null,48.0).color())orange++;
        }
        assertTrue(green>10);assertTrue(orange>10);bitmap.recycle();
    }
    @Test public void unresolvedLegacyBundleCannotLookReadyForExport() {
        try(FindingStore store=new FindingStore(app)) {
            long a=insert(store),b=insert(store);long bundle=store.mergeBundles(List.of(-a,-b));
            store.reviewPlace(a,"pothole","",store.get(a).details(),1);
            store.getWritableDatabase().execSQL("UPDATE findings SET category='root_heave' WHERE id=?",new Object[]{b});
            try(var controller=Robolectric.buildActivity(MapActivity.class).setup()) {
                assertFalse(badge(controller.get()).allReleased());store.resolveShared(bundle,null,null,"pothole");
                view(controller.get().findViewById(android.R.id.content),RideMapPanel.class).refresh();assertTrue(badge(controller.get()).allReleased());
            }
        }
    }
}
