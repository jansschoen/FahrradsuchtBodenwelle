package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Spinner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class ReportingWorkflowTest {
    private Application app;
    @Before public void before() {
        app = RuntimeEnvironment.getApplication(); app.deleteDatabase("findings.db");
        app.getSharedPreferences("mail_reports", 0).edit().clear().commit(); OsmEnricher.enabled(app, false);
    }
    private long finding(double lat, double lon, boolean reviewed) {
        Location l = new Location("gps"); l.setLatitude(lat); l.setLongitude(lon); l.setAccuracy(5);
        try (FindingStore s = new FindingStore(app)) { long id = s.insert(l, null); s.review(id, "root_heave", "privater Name und 13.10.2026 10:22", reviewed); return id; }
    }
    private void prepare(MailReportActivity a) { a.findViewById(R.id.mail_prepare).performClick(); Shadows.shadowOf(Looper.getMainLooper()).idle(); }
    @Test public void reviewedFindingOpensNativeMailDraftWithVerifiedRecipientAndOnePdf() {
        finding(52.52, 13.405, true); finding(52.52, 13.405, false);
        try (ActivityController<MailReportActivity> c = Robolectric.buildActivity(MailReportActivity.class).setup()) {
            MailReportActivity a = c.get(); prepare(a);
            assertEquals("sga@ba-mitte.berlin.de", ((EditText) a.findViewById(R.id.mail_recipient)).getText().toString());
            String body = ((EditText) a.findViewById(R.id.mail_body)).getText().toString();
            assertFalse(body.contains("privater Name")); assertFalse(body.contains("10:22"));
            a.findViewById(R.id.mail_open).performClick(); PdfWorkflowSupport.waitFor(a);Intent chooser=Shadows.shadowOf(a).getNextStartedActivity();assertNotNull(chooser);Intent intent=chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            assertNotNull(intent);assertEquals(Intent.ACTION_SEND,intent.getAction());assertEquals("mailto",intent.getSelector().getData().getScheme());
            assertEquals(body, intent.getStringExtra(Intent.EXTRA_TEXT)); assertTrue(intent.hasExtra(Intent.EXTRA_STREAM));assertEquals(1,intent.getClipData().getItemCount());assertTrue(intent.getClipData().getItemAt(0).getUri().getLastPathSegment().endsWith(".pdf"));
        }
    }
    @Test public void switchingDistrictsChangesRecipientsButPreservesEditedDrafts() {
        finding(52.52, 13.405, true); finding(52.51, 13.30, true);
        try (ActivityController<MailReportActivity> c = Robolectric.buildActivity(MailReportActivity.class).setup()) {
            MailReportActivity a = c.get(); prepare(a); Spinner groups = a.findViewById(R.id.mail_group);
            assertEquals(2, groups.getCount());
            ((EditText) a.findViewById(R.id.mail_body)).setText("Mein bearbeiteter Entwurf");
            groups.setSelection(1); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals("tiefbau@charlottenburg-wilmersdorf.de", ((EditText) a.findViewById(R.id.mail_recipient)).getText().toString());
            groups.setSelection(0); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals("Mein bearbeiteter Entwurf", ((EditText) a.findViewById(R.id.mail_body)).getText().toString());
        }
    }
    @Test public void unknownLocationNeverInventsARecipientAndManualRecipientWorks() {
        finding(48.137, 11.575, true);
        try (ActivityController<MailReportActivity> c = Robolectric.buildActivity(MailReportActivity.class).setup()) {
            MailReportActivity a = c.get(); prepare(a);
            EditText recipient = a.findViewById(R.id.mail_recipient); assertEquals("", recipient.getText().toString());
            a.findViewById(R.id.mail_open).performClick(); assertNull(Shadows.shadowOf(a).getNextStartedActivity());
            recipient.setText("strassen@example.org"); a.findViewById(R.id.mail_open).performClick();PdfWorkflowSupport.waitFor(a);
            assertNotNull(Shadows.shadowOf(a).getNextStartedActivity());
        }
    }
    @Test public void revokedConsentPreventsHandoffOfAnOlderDraft() {
        long id = finding(52.52, 13.405, true);
        try (ActivityController<MailReportActivity> c = Robolectric.buildActivity(MailReportActivity.class).setup()) {
            MailReportActivity a = c.get(); prepare(a);
            try (FindingStore s = new FindingStore(app)) { s.review(id, "root_heave", "", false); }
            a.findViewById(R.id.mail_open).performClick(); assertNull(Shadows.shadowOf(a).getNextStartedActivity());
        }
    }
    @Test public void rotationKeepsEditedBodyAndAddress() {
        finding(52.52, 13.405, true);
        try (ActivityController<MailReportActivity> c = Robolectric.buildActivity(MailReportActivity.class).setup()) {
            prepare(c.get()); ((EditText) c.get().findViewById(R.id.mail_body)).setText("Eigener Text");
            ((EditText) c.get().findViewById(R.id.mail_recipient)).setText("andere@example.org"); c.recreate(); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals("Eigener Text", ((EditText) c.get().findViewById(R.id.mail_body)).getText().toString());
            assertEquals("andere@example.org", ((EditText) c.get().findViewById(R.id.mail_recipient)).getText().toString());
        }
    }
    @Test public void mailIntentEncodesHeadersAndBodyAndRejectsRecipientInjection() {
        String body = "Straße & Kante?\nBitte prüfen.";
        Intent intent = MailIntent.create("amt@example.org", "Bitte prüfen & melden", body);
        assertTrue(intent.getData().toString().contains("%26")); assertTrue(intent.getData().toString().contains("%0A"));
        assertEquals(body, intent.getStringExtra(Intent.EXTRA_TEXT));
        for (String invalid : new String[]{"", "amt@example.org?bcc=x@example.org", "amt@example.org\nBcc:x@example.org", "a@example.org,b@example.org", "a@example..org", "a@-example.org"})
            assertThrows(IllegalArgumentException.class, () -> MailIntent.create(invalid, "Betreff", body));
        assertThrows(IllegalArgumentException.class, () -> MailIntent.create("amt@example.org", "Betreff\nBcc: X", body));
    }
}
