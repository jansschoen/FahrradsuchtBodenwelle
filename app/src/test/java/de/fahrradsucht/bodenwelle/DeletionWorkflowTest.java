package de.fahrradsucht.bodenwelle;

import android.app.AlertDialog;
import android.app.Application;
import android.location.Location;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class DeletionWorkflowTest {
    private Application app;

    @Before public void before() {
        app = RuntimeEnvironment.getApplication();
        app.deleteDatabase("findings.db");
        PhotoFiles.clearLocal(app);
        OsmEnricher.enabled(app, false);
        RecordingService.running = false;
    }

    private long finding(FindingStore store, String note) {
        Location location = new Location("gps");
        location.setLatitude(52.52);
        location.setLongitude(13.405);
        location.setAccuracy(5);
        long id = store.insert(location, null);
        store.review(id, "root_heave", note, true);
        return id;
    }

    private File photo(FindingStore store, long id) throws Exception {
        String filename = UUID.randomUUID() + ".jpg";
        File file = PhotoFiles.localFile(app, filename);
        Files.write(file.toPath(), new byte[]{1, 2, 3});
        store.addPhoto(id, filename, 1, 1, 3);
        return file;
    }

    private void clickDialog(int button) {
        ShadowAlertDialog.getLatestAlertDialog().getButton(button).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private Button deleteButton(View view, long id) {
        if (view instanceof Button b && b.getId() == R.id.finding_delete && Long.valueOf(id).equals(b.getTag())) return b;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            Button found = deleteButton(group.getChildAt(i), id);
            if (found != null) return found;
        }
        return null;
    }

    @Test public void directDeleteNeedsConfirmationAndCancelPreservesTheMeasurementAndPhoto() throws Exception {
        try (FindingStore store = new FindingStore(app)) {
            long id = finding(store, "Behalten");
            File photo = photo(store, id);
            try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
                controller.get().findViewById(R.id.nav_findings).performClick();
                Button delete = deleteButton(controller.get().findViewById(android.R.id.content), id);
                assertNotNull(delete);
                assertEquals("Löschen", delete.getText().toString());
                delete.performClick();
                assertEquals(1, store.count());
                assertTrue(photo.exists());
                clickDialog(AlertDialog.BUTTON_NEGATIVE);
                assertEquals("Behalten", store.get(id).note());
                assertTrue(photo.exists());
            }
        }
    }

    @Test public void confirmedDeleteRemovesOnlyThatMeasurementAndItsPhotosFromTheListAndExport() throws Exception {
        try (FindingStore store = new FindingStore(app)) {
            long kept = finding(store, "Behalten"), removed = finding(store, "Entfernen");
            File keptPhoto = photo(store, kept), removedPhoto = photo(store, removed);
            store.insertRoutePoint(new RoutePoint(1000, 500, 52.52, 13.405, 5, 0));
            try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
                controller.get().findViewById(R.id.nav_findings).performClick();
                deleteButton(controller.get().findViewById(android.R.id.content), removed).performClick();
                clickDialog(AlertDialog.BUTTON_POSITIVE);
                assertNull(store.get(removed));
                assertTrue(store.photos(removed).isEmpty());
                assertFalse(removedPhoto.exists());
                assertTrue(keptPhoto.exists());
                assertTrue(store.get(kept).reviewed());
                assertEquals(1, store.routeCount());
                assertNull(deleteButton(controller.get().findViewById(android.R.id.content), removed));
                assertNotNull(deleteButton(controller.get().findViewById(android.R.id.content), kept));
                String csv = FindingCsv.exportReviewed(store.all());
                assertEquals(2, csv.lines().count());
                assertTrue(csv.contains("Behalten"));
                assertFalse(csv.contains("Entfernen"));
            }
        }
    }

    @Test public void bundleDeleteSelectsOneOriginalAndKeepsItsPeersAndTheirPhotos() throws Exception {
        try (FindingStore store = new FindingStore(app)) {
            long kept = finding(store, "Behalten"), removed = finding(store, "Entfernen");
            File keptPhoto = photo(store, kept), removedPhoto = photo(store, removed);
            long bundle = store.mergeBundles(List.of(-kept, -removed));
            try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
                controller.get().findViewById(R.id.nav_findings).performClick();
                deleteButton(controller.get().findViewById(android.R.id.content), removed).performClick();
                AlertDialog selection = ShadowAlertDialog.getLatestAlertDialog();
                assertEquals(2, selection.getListView().getAdapter().getCount());
                assertTrue(selection.getListView().getAdapter().getItem(0).toString().contains("#" + removed));
                selection.getListView().performItemClick(null, 0, 0);
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals(2, store.count());
                clickDialog(AlertDialog.BUTTON_POSITIVE);
                assertNull(store.get(removed));
                assertEquals(bundle, store.get(kept).bundleId());
                assertEquals(List.of(kept), store.observations(bundle).stream().map(Finding::id).toList());
                assertTrue(keptPhoto.exists());
                assertTrue(removedPhoto.exists());
                assertEquals(2, store.photos(kept).size());
                assertNotNull(deleteButton(controller.get().findViewById(android.R.id.content), kept));
            }
        }
    }

    @Test public void deletingTheLastBundledMeasurementLeavesNoEmptyBundleAndStaleKeysHaveNoMembers() {
        try (FindingStore store = new FindingStore(app)) {
            long first = finding(store, "Erste"), last = finding(store, "Letzte");
            long bundle = store.mergeBundles(List.of(-first, -last));
            assertTrue(store.observations(-first).isEmpty());
            store.delete(first);
            try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
                controller.get().findViewById(R.id.nav_findings).performClick();
                deleteButton(controller.get().findViewById(android.R.id.content), last).performClick();
                clickDialog(AlertDialog.BUTTON_POSITIVE);
                assertEquals(0, store.count());
                assertTrue(store.observations(bundle).isEmpty());
                assertTrue(store.bundles().isEmpty());
                try (android.database.Cursor cursor = store.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM finding_bundles", null)) {
                    assertTrue(cursor.moveToFirst());
                    assertEquals(0, cursor.getInt(0));
                }
                assertNull(controller.get().findViewById(R.id.finding_delete));
            }
        }
    }
}
