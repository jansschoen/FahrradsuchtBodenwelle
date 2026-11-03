package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.widget.Spinner;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNetworkCapabilities;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class OsmReviewTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");OsmEnricher.enabled(app,false);}
    private long finding(FindingStore store){Location l=new Location("gps");l.setLatitude(52.5);l.setLongitude(13.4);l.setAccuracy(5);return store.insert(l,null);}
    @Test public void internetCapableNetworkDoesNotNeedAndroidValidation() {
        ConnectivityManager manager=app.getSystemService(ConnectivityManager.class);
        NetworkCapabilities capabilities=ShadowNetworkCapabilities.newInstance();
        Shadows.shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        Shadows.shadowOf(capabilities).removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        Shadows.shadowOf(manager).setNetworkCapabilities(manager.getActiveNetwork(),capabilities);
        assertTrue(DeviceConnectivity.canRequest(app));
        Shadows.shadowOf(capabilities).removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        assertFalse(DeviceConnectivity.canRequest(app));Shadows.shadowOf(manager).clearAllNetworks();assertFalse(DeviceConnectivity.canRequest(app));
    }
    @Test public void openEditorUpdatesAutomaticSuggestionsButProtectsUnsavedManualChoices() {
        try(FindingStore store=new FindingStore(app);var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            long id=finding(store);FindingReviewForm form=new FindingReviewForm(controller.get(),store.get(id));
            store.setOsm(id,new OsmMatch("matched",42L,Map.of("highway","cycleway","surface","asphalt"),1000,"low",4.0,"test","cycleway","asphalt"));
            form.refreshAutomatic(store.get(id),List.of(store.get(id)));assertEquals("asphalt",form.details(store.get(id)).surface());
            form.surfaceManual.setChecked(true);
            // The surface spinner follows the checkbox in the geodata panel.
            android.view.ViewGroup parent=(android.view.ViewGroup)form.surfaceManual.getParent();
            Spinner surface=(Spinner)parent.getChildAt(parent.indexOfChild(form.surfaceManual)+1);
            surface.setSelection(Classification.index(Classification.SURFACES,"concrete"));
            store.setOsm(id,new OsmMatch("matched",43L,Map.of("highway","residential","surface","gravel"),2000,"low",4.0,"test","carriageway","gravel"));
            form.refreshAutomatic(store.get(id),List.of(store.get(id)));assertEquals("concrete",form.details(store.get(id)).surface());
            assertEquals("carriageway",form.details(store.get(id)).traffic());
            form.surfaceManual.setChecked(false);form.refreshAutomatic(store.get(id),List.of(store.get(id)));
            assertEquals("gravel",((Classification.Choice)surface.getSelectedItem()).id());
            assertNotNull(form.root.findViewById(R.id.osm_retry));assertNotNull(form.root.findViewById(R.id.osm_diagnose));
        }
    }
}
