package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.widget.CheckBox;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ReportWorkflowTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");PhotoFiles.clearLocal(app);OsmEnricher.enabled(app,false);RecordingService.running=false;app.getSharedPreferences("mail_reports",0).edit().clear().commit();}
    private long finding(FindingStore store,boolean released,String note){android.location.Location fix=new android.location.Location("gps");fix.setLatitude(52.52);fix.setLongitude(13.405);fix.setAccuracy(6);long id=store.insert(fix,null);store.review(id,"root_heave",note,released);return id;}
    private void prepare(MailReportActivity activity){activity.findViewById(R.id.mail_prepare).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
    @Test public void previewIsReadableAndTheMailUsesExactlyTheSameReadOnlyPdf()throws Exception {
        try(FindingStore store=new FindingStore(app)){finding(store,true,"PRIVATE-NOTIZ 26.10.2026 08:24");finding(store,false,"GEHEIM");}
        try(var controller=Robolectric.buildActivity(MailReportActivity.class).setup()){
            MailReportActivity activity=controller.get();prepare(activity);activity.findViewById(R.id.report_preview).performClick();PdfWorkflowSupport.waitFor(activity);
            Intent preview=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(preview);assertEquals(ReportPreviewActivity.class.getName(),preview.getComponent().getClassName());Uri pdf=preview.getData();assertEquals("application/pdf",app.getContentResolver().getType(pdf));
            try(PDDocument document=PDDocument.load(app.getContentResolver().openInputStream(pdf))){String text=new PDFTextStripper().getText(document);assertTrue(text.contains("Fundstellen im Überblick"));assertTrue(text.contains("52.520000"));assertFalse(text.contains("PRIVATE-NOTIZ"));assertFalse(text.contains("GEHEIM"));assertTrue(document.getPage(1).getAnnotations().size()>0);}
            try(var reader=Robolectric.buildActivity(ReportPreviewActivity.class,preview).setup()){
                android.widget.TextView count=reader.get().findViewById(R.id.report_page_count);assertTrue(count.getText().toString().startsWith("Seite 1 von"));reader.get().findViewById(R.id.report_next).performClick();assertTrue(count.getText().toString().startsWith("Seite 2 von"));reader.recreate();assertTrue(((android.widget.TextView)reader.get().findViewById(R.id.report_page_count)).getText().toString().startsWith("Seite 2 von"));
            }
            activity.findViewById(R.id.mail_open).performClick();PdfWorkflowSupport.waitFor(activity);Intent chooser=Shadows.shadowOf(activity).getNextStartedActivity();Intent mail=chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            assertEquals(Intent.ACTION_SEND,mail.getAction());assertEquals(pdf,mail.getClipData().getItemAt(0).getUri());assertTrue((chooser.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);assertEquals(0,mail.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            assertThrows(java.io.FileNotFoundException.class,()->app.getContentResolver().openFileDescriptor(pdf,"w"));
        }
    }
    @Test public void selectedPhotosAreInsideThePdfAndRevocationBlocksCachedHandoff()throws Exception {
        long id;
        try(FindingStore store=new FindingStore(app)){id=finding(store,true,"");String filename=java.util.UUID.randomUUID()+".jpg";byte[] bytes=PdfWorkflowSupport.photo();Files.write(PhotoFiles.localFile(app,filename).toPath(),bytes);store.addPhoto(id,filename,12,8,bytes.length);}
        try(var controller=Robolectric.buildActivity(MailReportActivity.class).setup()){
            MailReportActivity activity=controller.get();prepare(activity);activity.findViewById(R.id.report_preview).performClick();PdfWorkflowSupport.waitFor(activity);Uri pdf=Shadows.shadowOf(activity).getNextStartedActivity().getData();
            try(PDDocument document=PDDocument.load(app.getContentResolver().openInputStream(pdf))){String text=new PDFTextStripper().getText(document);assertTrue(text.contains("Fotodokumentation"));assertTrue(text.contains("Stelle-01-Foto-01.jpg"));assertTrue(document.getNumberOfPages()>=3);}
            try(FindingStore store=new FindingStore(app)){store.review(id,"root_heave","",false);}
            activity.findViewById(R.id.mail_open).performClick();assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
    @Test public void leavingWhileThePdfIsBuiltNeverOpensTheMailProgramAfterwards()throws Exception {
        try(FindingStore store=new FindingStore(app)){finding(store,true,"");}
        try(var controller=Robolectric.buildActivity(MailReportActivity.class).setup()){
            MailReportActivity activity=controller.get();prepare(activity);activity.findViewById(R.id.mail_open).performClick();activity.finish();PdfWorkflowSupport.waitFor(activity);
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
    @Test public void reportProviderRejectsTraversalAndHtmlEscapesAllEditedText() {
        assertThrows(IllegalArgumentException.class,()->ReportFiles.file(app,"../findings.db","Fundstellenbericht.pdf"));
        assertThrows(IllegalArgumentException.class,()->ReportFiles.file(app,java.util.UUID.randomUUID().toString(),"../findings.db"));
        Uri pdf=Uri.parse("content://"+app.getPackageName()+".photos/report/token/Fundstellenbericht.pdf");
        Intent intent=MailIntent.withReport("amt@example.org","Prüfung","Hallo <script> & Grüße",pdf,new java.util.ArrayList<>());
        String html=intent.getStringExtra(Intent.EXTRA_HTML_TEXT);assertFalse(html.contains("<script>"));assertTrue(html.contains("&lt;script&gt;"));assertTrue(intent.getClipData().getDescription().hasMimeType("application/pdf"));assertEquals("Hallo <script> & Grüße",intent.getStringExtra(Intent.EXTRA_TEXT));
    }
}
