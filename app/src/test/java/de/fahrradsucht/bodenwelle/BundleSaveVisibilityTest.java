package de.fahrradsucht.bodenwelle;

import android.app.*;
import android.location.Location;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35},qualifiers="w360dp-h640dp")
public class BundleSaveVisibilityTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");OsmEnricher.enabled(app,false);RecordingService.running=false;}
    private long add(FindingStore s){Location l=new Location("gps");l.setLatitude(52.52);l.setLongitude(13.405);l.setAccuracy(5);return s.insert(l,null);}
    private void bounded(AlertDialog dialog,int button){
        View decor=dialog.getWindow().getDecorView();int width=360,height=480;
        decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));decor.layout(0,0,width,height);
        ScrollView form=dialog.findViewById(R.id.edit_form_scroll);View save=dialog.findViewById(button);assertNotNull(save);assertTrue(save.isShown());
        assertFalse(isChild(form,save));assertTrue(form.getMeasuredHeight()>0);assertTrue(form.getMeasuredHeight()<=Math.round(640*app.getResources().getDisplayMetrics().density*.55));
        android.graphics.Rect rect=new android.graphics.Rect(0,0,save.getWidth(),save.getHeight());((ViewGroup)decor).offsetDescendantRectToMyCoords(save,rect);assertTrue("Button position: "+rect,rect.height()>0&&rect.top>=0&&rect.bottom<=height);
        assertEquals(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,dialog.getWindow().getAttributes().softInputMode);
    }
    private boolean isChild(ViewGroup group,View child){for(int i=0;i<group.getChildCount();i++){View v=group.getChildAt(i);if(v==child||v instanceof ViewGroup g&&isChild(g,child))return true;}return false;}
    @Test public void commonEditingAndMergeHaveSaveOutsideTheScrollableForm(){
        try(FindingStore s=new FindingStore(app);var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            long a=add(s),b=add(s);BundleConfirmation.show(controller.get(),s,List.of(-a,-b),()->{});AlertDialog merge=ShadowAlertDialog.getLatestAlertDialog();bounded(merge,R.id.bundle_save);
            merge.findViewById(R.id.bundle_save).performClick();assertEquals(1,s.placeCount());
            new FindingActions(controller.get(),s,()->{}).review(s.get(a));AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Speichern",editor.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());bounded(editor,R.id.finding_save);
            editor.findViewById(R.id.finding_save).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(editor.isShowing());assertEquals(2,s.count());assertEquals(1,s.placeCount());
        }
    }
    @Test public void manualSelectionButtonStaysOutsideLongList(){
        try(FindingStore s=new FindingStore(app)){for(int i=0;i<30;i++)add(s);}
        try(var controller=Robolectric.buildActivity(BundleActivity.class).setup()){
            View root=controller.get().findViewById(android.R.id.content),save=root.findViewById(R.id.bundle_merge);assertNotNull(save);assertFalse(save.isEnabled());
            ViewParent parent=save.getParent();while(parent!=null){assertFalse(parent instanceof ScrollView);parent=parent.getParent();}
            List<CheckBox> boxes=new ArrayList<>();collect(root,boxes);boxes.get(0).setChecked(true);assertFalse(save.isEnabled());boxes.get(1).setChecked(true);assertTrue(save.isEnabled());
        }
    }
    private void collect(View root,List<CheckBox> boxes){if(root instanceof CheckBox b)boxes.add(b);if(root instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)collect(g.getChildAt(i),boxes);}
}
