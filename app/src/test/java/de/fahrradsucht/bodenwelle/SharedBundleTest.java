package de.fahrradsucht.bodenwelle;

import android.app.AlertDialog;
import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Spinner;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
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
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class SharedBundleTest {
    private Application app;
    @Before public void before() {
        app = RuntimeEnvironment.getApplication(); app.deleteDatabase("findings.db"); PhotoFiles.clearLocal(app);
        OsmEnricher.enabled(app, false); RecordingService.running = false;
    }
    private long finding(FindingStore store, boolean released) {
        Location fix = new Location("gps"); fix.setLatitude(52.52); fix.setLongitude(13.405); fix.setAccuracy(5);
        long id = store.insert(fix, new BumpDetector.Detection(SystemClock.elapsedRealtime(), 48, 40));
        store.review(id, "root_heave", "Notiz " + id, released); return id;
    }
    private void describe(FindingStore store, long id, String traffic, String surface, int flags, Double height) {
        Finding f = store.get(id);
        store.review(id, f.category(), f.note(), f.reviewed(), f.details().reviewed(height, f.peak(), flags, traffic, true, surface, true));
    }
    private OsmMatch osm(long id, String traffic, String surface) {
        return new OsmMatch("matched", id, Map.of("highway", "cycleway", "surface", surface), 1000, "low", 4.0, "test", traffic, surface);
    }
    private long photo(FindingStore store, long id) throws Exception {
        String filename = java.util.UUID.randomUUID() + ".jpg"; Files.write(PhotoFiles.localFile(app, filename).toPath(), new byte[]{1,2,3});
        return store.addPhoto(id, filename, 1, 1, 3);
    }
    private int count(FindingStore store, String table) {
        try (Cursor cursor = store.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + table, null)) { cursor.moveToFirst(); return cursor.getInt(0); }
    }
    private MailReports.Group group(FindingStore store, long... ids) {
        List<MailReports.Item> items = new ArrayList<>();
        for (long id : ids) items.add(new MailReports.Item(store.get(id), "01", "manual"));
        return MailReports.groups(items, true).get(0);
    }
    private void dialogButton(int button) {
        ShadowAlertDialog.getLatestAlertDialog().getButton(button).performClick(); Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    @Test public void mergeSharesManualDescriptionsAndHazardsButPreservesMeasurementsAndIndividualConsent() {
        try (FindingStore store = new FindingStore(app)) {
            long a = finding(store, true), b = finding(store, false);
            describe(store, a, "cycleway", "asphalt", 1, 5.0);
            Finding originalB = store.get(b); store.setOsm(b, osm(43, "carriageway", "gravel"));
            Finding bf = store.get(b); store.review(b, bf.category(), bf.note(), false,
                    bf.details().reviewed(12.0, bf.peak(), 16, bf.details().traffic(), false, bf.details().surface(), false));
            long key = store.mergeBundles(List.of(-a, -b));
            for (Finding f : store.observations(key)) {
                assertEquals("cycleway", f.details().traffic()); assertEquals("asphalt", f.details().surface());
                assertTrue(f.details().trafficManual()); assertEquals(17, f.details().hazards());
            }
            assertEquals(5.0, store.get(a).details().heightMm(), 0); assertEquals(12.0, store.get(b).details().heightMm(), 0);
            assertTrue(store.get(a).reviewed()); assertFalse(store.get(b).reviewed());
            assertEquals(originalB.timeMillis(), store.get(b).timeMillis()); assertEquals(originalB.position(), store.get(b).position());
            assertEquals("Notiz " + b, store.get(b).note()); assertEquals(Long.valueOf(43), store.get(b).details().osm().wayId());
        }
    }
    @Test public void conflictingManualDescriptionsRequireExplicitChoicesWithoutPartialWrites() throws Exception {
        try (FindingStore store = new FindingStore(app)) {
            long a = finding(store, true), b = finding(store, true); photo(store, a);
            describe(store, a, "cycleway", "asphalt", 1, null); describe(store, b, "carriageway", "concrete", 2, null);
            assertThrows(IllegalArgumentException.class, () -> store.mergeBundles(List.of(-a, -b)));
            assertEquals(0, store.get(a).bundleId()); assertEquals("asphalt", store.get(a).details().surface()); assertTrue(store.photos(b).isEmpty());
            long key = store.mergeBundles(List.of(-a, -b), "cycleway", "unknown");
            for (Finding f : store.observations(key)) {
                assertEquals("cycleway", f.details().traffic()); assertEquals("unknown", f.details().surface());
                assertTrue(f.details().surfaceManual()); assertEquals(3, f.details().hazards()); assertEquals(1, store.photos(f.id()).size());
            }
        }
    }
    @Test public void laterEditsAndRemovingHazardFlagsApplyToAllWithoutReleasingOtherMeasurements() {
        try (FindingStore store = new FindingStore(app)) {
            long a = finding(store, true), b = finding(store, false); describe(store, a, "cycleway", "asphalt", 17, 5.0);
            long key = store.mergeBundles(List.of(-a, -b)); Finding selected = store.get(b);
            store.review(b, "pothole", "Neue eigene Notiz", false, selected.details().reviewed(9.0, selected.peak(), 2, "shared_path", true, "concrete", true));
            for (Finding f : store.observations(key)) {
                assertEquals("shared_path", f.details().traffic()); assertEquals("concrete", f.details().surface()); assertEquals(2, f.details().hazards());
            }
            assertEquals(5.0, store.get(a).details().heightMm(), 0); assertEquals(9.0, store.get(b).details().heightMm(), 0);
            assertEquals("root_heave", store.get(a).category()); assertEquals("Notiz " + a, store.get(a).note());
            assertTrue(store.get(a).reviewed()); assertFalse(store.get(b).reviewed());
            store.setOsm(a, osm(44, "carriageway", "gravel")); assertEquals("concrete", store.get(a).details().surface()); assertEquals("concrete", store.get(b).details().surface());
        }
    }
    @Test public void automaticProposalsStaySharedAndManualOverrideCanBeRemovedForTheWholeBundle() {
        try (FindingStore store = new FindingStore(app)) {
            long a = finding(store, true), b = finding(store, false); long key = store.mergeBundles(List.of(-a, -b));
            store.setOsm(a, osm(42, "cycleway", "asphalt"));
            assertEquals("asphalt", store.get(b).details().surface()); assertFalse(store.get(b).details().surfaceManual());
            store.review(a, "root_heave", "", false); store.review(b, "root_heave", "", true);
            String body = MailReports.draft(group(store, b), "Mitte", true, false, "").body(); assertTrue(body.contains("OpenStreetMap"));
            store.setOsm(b, osm(43, "carriageway", "gravel"));
            for (Finding f : store.observations(key)) { assertEquals("unknown", f.details().traffic()); assertEquals("unknown", f.details().surface()); }
            describe(store, a, "shared_path", "concrete", 4, null);
            store.setOsm(b, osm(43, "cycleway", "asphalt")); store.setOsm(a, osm(42, "cycleway", "asphalt"));
            Finding f = store.get(b); store.review(b, f.category(), f.note(), false, f.details().reviewed(null, f.peak(), 4, "unknown", false, "unknown", false));
            for (Finding member : store.observations(key)) { assertEquals("asphalt", member.details().surface()); assertFalse(member.details().surfaceManual()); }
            assertEquals(Long.valueOf(42), store.get(a).details().osm().wayId()); assertEquals(Long.valueOf(43), store.get(b).details().osm().wayId());
        }
    }
    @Test public void transientOsmFailureKeepsInheritedDescriptionsAfterTheOriginalSourceWasDeleted() {
        try (FindingStore store = new FindingStore(app)) {
            long a = finding(store, true), b = finding(store, true); store.setOsm(a, osm(42, "cycleway", "asphalt"));
            store.mergeBundles(List.of(-a, -b)); store.delete(a);
            assertFalse(store.get(b).details().osm().hasSuggestions()); assertEquals("asphalt", store.get(b).details().surface());
            app.getSharedPreferences("osm_requests", 0).edit().clear().commit();
            try (OsmRepository repository = new OsmRepository(app, query -> { throw new java.io.IOException("network"); }, () -> 1_000_000)) {
                repository.enrich(b, false); assertEquals("asphalt", store.get(b).details().surface());
                repository.enrich(b, true); assertEquals("asphalt", store.get(b).details().surface()); assertEquals("cycleway", store.get(b).details().traffic());
            }
        }
    }
    @Test public void photosUseOneFileAndSurviveSplittingAndDeletingTheirOriginalOwner() throws Exception {
        try (FindingStore store = new FindingStore(app)) {
            long a = finding(store, true), b = finding(store, false); long pa = photo(store, a), pb = photo(store, b);
            long key = store.mergeBundles(List.of(-a, -b)); long pc = photo(store, b);
            assertEquals(3, store.photos(a).size()); assertEquals(store.photos(a), store.photos(b)); assertEquals(3, count(store, "finding_photos")); assertEquals(6, count(store, "photo_links"));
            File aFile = PhotoFiles.localFile(app, store.photo(pa).filename()), bFile = PhotoFiles.localFile(app, store.photo(pb).filename());
            store.separateBundle(key); store.deletePhoto(a, pb);
            assertFalse(store.hasPhoto(a, pb)); assertTrue(store.hasPhoto(b, pb)); assertTrue(bFile.exists());
            store.delete(a); assertTrue(aFile.exists()); assertEquals(3, store.photos(b).size()); assertEquals(b, store.photo(pa).findingId());
            store.delete(b); assertFalse(aFile.exists()); assertFalse(bFile.exists()); assertNull(store.photo(pc)); assertEquals(0, count(store, "photo_links"));
            try (Cursor errors = store.getReadableDatabase().rawQuery("PRAGMA foreign_key_check", null)) { assertFalse(errors.moveToFirst()); }
        }
    }
    @Test public void remergingSharedGalleriesDoesNotDuplicateFilesOrLoseExistingPhotosAboveTheSingleLimit() throws Exception {
        try (FindingStore store = new FindingStore(app)) {
            long a = finding(store, true), b = finding(store, true); long key = store.mergeBundles(List.of(-a, -b));
            for (int i = 0; i < 6; i++) photo(store, a);
            store.separateBundle(key); assertEquals(6, store.photos(a).size()); assertEquals(6, store.photos(b).size());
            assertThrows(IllegalArgumentException.class, () -> store.addPhoto(a, java.util.UUID.randomUUID()+".jpg", 1, 1, 3));
            store.mergeBundles(List.of(-a, -b)); assertEquals(6, count(store, "finding_photos")); assertEquals(6, MailAttachments.choices(group(store, a, b), store).size());
            store.deleteAll(); assertEquals(0, count(store, "photo_links")); assertEquals(0, count(store, "finding_photos"));
        }
    }
    @Test public void mailCanUseSharedPhotosOfAnUnreleasedOwnerButNeverUnrelatedPrivatePhotosOrRevokedConsent() throws Exception {
        try (FindingStore store = new FindingStore(app)) {
            long a = finding(store, false), b = finding(store, true), unrelated = finding(store, false);
            long shared = photo(store, a), privatePhoto = photo(store, unrelated); store.mergeBundles(List.of(-a, -b));
            MailReports.Group group = group(store, a, b); List<MailAttachments.Choice> choices = MailAttachments.choices(group, store);
            assertEquals(1, choices.size()); assertEquals(shared, choices.get(0).photo().id()); assertEquals(1, MailAttachments.prepare(app, choices, group).size());
            assertThrows(java.io.IOException.class, () -> MailAttachments.prepare(app, List.of(new MailAttachments.Choice(store.photo(privatePhoto), 1, "Stelle-01-Foto-01.jpg")), group));
            store.review(b, "root_heave", "", false); assertThrows(java.io.IOException.class, () -> MailAttachments.prepare(app, choices, group));
        }
    }
    @Test public void separatingOrRemovingTheSharedPhotoInvalidatesOldAttachmentChoices() throws Exception {
        try (FindingStore store = new FindingStore(app)) {
            long a = finding(store, true), b = finding(store, true); long shared = photo(store, a); long key = store.mergeBundles(List.of(-a, -b));
            MailReports.Group old = group(store, a, b); List<MailAttachments.Choice> choices = MailAttachments.choices(old, store);
            store.separateBundle(key); assertThrows(java.io.IOException.class, () -> MailAttachments.prepare(app, choices, old));
            MailReports.Group fresh = group(store, b); List<MailAttachments.Choice> current = MailAttachments.choices(fresh, store);
            store.deletePhoto(b, shared); assertNotNull(store.photo(shared));
            assertThrows(java.io.IOException.class, () -> MailAttachments.prepare(app, current, fresh));
        }
    }
    @Test public void upgradingVersionSixSharesExistingPhotosAndHazardsWhilePreservingConflictsUntilResolution() throws Exception {
        long a, b;
        try (FindingStore store = new FindingStore(app)) {
            a = finding(store, true); b = finding(store, false); describe(store, a, "cycleway", "asphalt", 1, 5.0); describe(store, b, "cycleway", "concrete", 16, 9.0);
            photo(store, a); photo(store, b); store.insertRoutePoint(new RoutePoint(1000, 500, 52.52, 13.405, 5, 0));
            store.getWritableDatabase().execSQL("INSERT INTO finding_bundles(id) VALUES(1)");
            store.getWritableDatabase().execSQL("UPDATE findings SET bundle_id=1");
        }
        try (SQLiteDatabase old = app.openOrCreateDatabase("findings.db", 0, null)) { old.execSQL("DROP TABLE photo_links"); old.setVersion(6); }
        try (FindingStore upgraded = new FindingStore(app)) {
            assertEquals(7, upgraded.getReadableDatabase().getVersion()); assertEquals(2, upgraded.count()); assertEquals(1, upgraded.routeCount());
            assertEquals(2, upgraded.photos(a).size()); assertEquals(2, upgraded.photos(b).size()); assertEquals(2, count(upgraded, "finding_photos"));
            assertEquals(17, upgraded.get(a).details().hazards()); assertEquals(17, upgraded.get(b).details().hazards());
            assertEquals("asphalt", upgraded.get(a).details().surface()); assertEquals("concrete", upgraded.get(b).details().surface());
            assertTrue(SharedFindingDetails.from(upgraded.observations(1)).conflicting());
            upgraded.resolveShared(1, null, "asphalt"); assertEquals("asphalt", upgraded.get(b).details().surface());
            assertEquals(5.0, upgraded.get(a).details().heightMm(), 0); assertEquals(9.0, upgraded.get(b).details().heightMm(), 0);
            assertTrue(upgraded.get(a).reviewed()); assertFalse(upgraded.get(b).reviewed()); assertEquals("Notiz " + b, upgraded.get(b).note());
        }
    }
    @Test public void confirmationDialogRequiresExplicitConflictChoiceAndSupportsCancellation() {
        try (FindingStore store = new FindingStore(app); var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            long a = finding(store, true), b = finding(store, false); describe(store, a, "cycleway", "asphalt", 1, null); describe(store, b, "cycleway", "concrete", 2, null);
            BundleConfirmation.show(controller.get(), store, List.of(-a,-b), () -> {});
            dialogButton(AlertDialog.BUTTON_POSITIVE); assertEquals(2, store.bundles().size()); assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            Spinner choice = ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.bundle_surface_choice); assertNotNull(choice); choice.setSelection(1);
            dialogButton(AlertDialog.BUTTON_NEGATIVE); assertEquals("asphalt", store.get(a).details().surface()); assertEquals("concrete", store.get(b).details().surface());
            BundleConfirmation.show(controller.get(), store, List.of(-a,-b), () -> {});
            ((Spinner) ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.bundle_surface_choice)).setSelection(1);
            dialogButton(AlertDialog.BUTTON_POSITIVE); assertEquals(1, store.bundles().size()); assertEquals(store.get(a).details().surface(), store.get(b).details().surface());
        }
    }
    @Test public void oldConflictingBundleIsResolvedBeforeNativeEditingOrGeneratingMail() {
        long a, b;
        try (FindingStore store = new FindingStore(app)) {
            a = finding(store, true); b = finding(store, true); describe(store, a, "cycleway", "asphalt", 1, null); describe(store, b, "cycleway", "concrete", 2, null);
            store.getWritableDatabase().execSQL("INSERT INTO finding_bundles(id) VALUES(1)"); store.getWritableDatabase().execSQL("UPDATE findings SET bundle_id=1");
        }
        try (SQLiteDatabase old = app.openOrCreateDatabase("findings.db", 0, null)) { old.execSQL("DROP TABLE photo_links"); old.setVersion(6); }
        try (var mail = Robolectric.buildActivity(MailReportActivity.class).setup()) {
            mail.get().findViewById(R.id.mail_prepare).performClick(); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNull(mail.get().findViewById(R.id.mail_open));
        }
        try (FindingStore store = new FindingStore(app); var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            FindingActions actions = new FindingActions(controller.get(), store, () -> {}); actions.review(store.get(a));
            Spinner surface = ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.bundle_surface_choice); assertNotNull(surface);
            dialogButton(AlertDialog.BUTTON_POSITIVE); assertTrue(SharedFindingDetails.from(store.observations(1)).conflicting());
            surface.setSelection(1); dialogButton(AlertDialog.BUTTON_POSITIVE);
            assertFalse(SharedFindingDetails.from(store.observations(1)).conflicting());
            assertNotNull(ShadowAlertDialog.getLatestAlertDialog().getListView()); assertEquals(2, ShadowAlertDialog.getLatestAlertDialog().getListView().getAdapter().getCount());
        }
    }
    private <T extends View> List<T> views(View root, Class<T> type) {
        List<T> result = new ArrayList<>(); if (type.isInstance(root)) result.add(type.cast(root));
        if (root instanceof ViewGroup group) for (int i=0; i<group.getChildCount(); i++) result.addAll(views(group.getChildAt(i), type)); return result;
    }
    private CheckBox box(View root, String label) { return views(root, CheckBox.class).stream().filter(b -> b.getText().toString().equals(label)).findFirst().orElseThrow(); }
    @Test public void nativeEditorMakesTheSharedScopeVisibleAndSavesItForAllMembers() {
        try (FindingStore store = new FindingStore(app); var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            long a = finding(store, true), b = finding(store, false); long key = store.mergeBundles(List.of(-a,-b));
            FindingActions actions = new FindingActions(controller.get(), store, () -> {}); actions.review(store.get(b));
            ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null,0,0); Shadows.shadowOf(Looper.getMainLooper()).idle();
            AlertDialog editor = ShadowAlertDialog.getLatestAlertDialog(); View root = editor.findViewById(android.R.id.content);
            assertTrue(views(root, android.widget.TextView.class).stream().anyMatch(v -> v.getText().toString().equals(app.getString(R.string.bundle_shared_hint))));
            box(root, "Verkehrsfläche selbst festlegen").setChecked(true); box(root, "Oberfläche selbst festlegen").setChecked(true);
            List<Spinner> spinners = views(root, Spinner.class); spinners.get(1).setSelection(Classification.index(Classification.ROADS, "shared_path"));
            spinners.get(2).setSelection(Classification.index(Classification.SURFACES, "concrete")); box(root, Classification.HAZARDS[4].label()).setChecked(true);
            dialogButton(AlertDialog.BUTTON_POSITIVE);
            for (Finding f : store.observations(key)) { assertEquals("shared_path", f.details().traffic()); assertEquals("concrete", f.details().surface()); assertEquals(16, f.details().hazards()); }
            assertTrue(store.get(a).reviewed()); assertFalse(store.get(b).reviewed());
        }
    }
}
