package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.app.AlertDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import java.nio.file.Files;
import java.util.List;
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
@Config(sdk={28,35})
public class PhotoWorkflowTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");PhotoFiles.clearLocal(app);OsmEnricher.enabled(app,false);RecordingService.running=false;app.getSharedPreferences("mail_reports",0).edit().clear().commit();}
    private long finding(FindingStore s,double lat){Location l=new Location("gps");l.setLatitude(lat);l.setLongitude(13.405);l.setAccuracy(5);long id=s.insert(l,null);s.review(id,"root_heave","",true);return id;}
    private void photo(FindingStore s,long id)throws Exception {String name=java.util.UUID.randomUUID()+".jpg";Files.write(PhotoFiles.localFile(app,name).toPath(),new byte[]{1,2,3});s.addPhoto(id,name,1,1,3);}
    private void prepare(MailReportActivity a){a.findViewById(R.id.mail_prepare).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();}
    @Test public void mergedPlaceIsOneMailEntryAndBothPhotosReachTheMailChooser()throws Exception {
        try(FindingStore s=new FindingStore(app)){long a=finding(s,52.52),b=finding(s,52.52001);photo(s,a);photo(s,b);s.mergeBundles(List.of(-a,-b));}
        try(ActivityController<MailReportActivity> c=Robolectric.buildActivity(MailReportActivity.class).setup()){
            prepare(c.get());String body=((android.widget.EditText)c.get().findViewById(R.id.mail_body)).getText().toString();assertTrue(body.contains("2 ausgewählte Beobachtungen"));
            c.get().findViewById(R.id.mail_open).performClick();Intent chooser=Shadows.shadowOf(c.get()).getNextStartedActivity();assertEquals(Intent.ACTION_CHOOSER,chooser.getAction());
            Intent mail=chooser.getParcelableExtra(Intent.EXTRA_INTENT);assertEquals(Intent.ACTION_SEND_MULTIPLE,mail.getAction());assertEquals(2,mail.getClipData().getItemCount());assertTrue((chooser.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
            assertTrue(mail.getClipData().getItemAt(0).getUri().getLastPathSegment().startsWith("Stelle-01-Foto-"));assertTrue(mail.getClipData().getItemAt(1).getUri().getLastPathSegment().startsWith("Stelle-01-Foto-"));
        }
    }
    @Test public void explicitPhotoChoiceSurvivesRotationAndOnlyChosenPhotoIsShared()throws Exception {
        try(FindingStore s=new FindingStore(app)){long a=finding(s,52.52);photo(s,a);photo(s,a);}
        try(ActivityController<MailReportActivity> c=Robolectric.buildActivity(MailReportActivity.class).setup()){
            prepare(c.get());CheckBox first=photoCheckbox(c.get().findViewById(android.R.id.content));assertNotNull(first);first.setChecked(false);c.recreate();Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(photoCheckbox(c.get().findViewById(android.R.id.content)).isChecked());c.get().findViewById(R.id.mail_open).performClick();Intent chooser=Shadows.shadowOf(c.get()).getNextStartedActivity();Intent mail=chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            assertEquals(Intent.ACTION_SEND,mail.getAction());assertEquals(1,mail.getClipData().getItemCount());assertEquals("Stelle-01-Foto-02.jpg",mail.getClipData().getItemAt(0).getUri().getLastPathSegment());
        }
    }
    @Test public void cameraRequestUsesScopedOutputAndCancellationRemovesRawCapture()throws Exception {
        try(FindingStore s=new FindingStore(app)){finding(s,52.52);}
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            c.get().findViewById(R.id.nav_findings).performClick();reviewButton(c.get().findViewById(android.R.id.content)).performClick();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();dialog.findViewById(R.id.photo_capture).performClick();
            org.robolectric.shadows.ShadowActivity.IntentForResult request=Shadows.shadowOf(c.get()).getNextStartedActivityForResult();assertEquals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE,request.intent.getAction());
            android.net.Uri uri=request.intent.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT);assertEquals("content",uri.getScheme());assertTrue((request.intent.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION)!=0);assertEquals(uri,request.intent.getClipData().getItemAt(0).getUri());
            try(java.io.OutputStream out=app.getContentResolver().openOutputStream(uri,"w")){out.write(new byte[]{1,2,3});}c.get().onActivityResult(request.requestCode,android.app.Activity.RESULT_CANCELED,null);assertFalse(PhotoFiles.captureFile(app,uri.getLastPathSegment()).exists());
            String[] permissions=app.getPackageManager().getPackageInfo(app.getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;assertFalse(java.util.Arrays.asList(permissions).contains(android.Manifest.permission.CAMERA));dialog.dismiss();
        }
    }
    @Test public void photoPickerUsesUserSelectedImagesWithoutBroadPhotoLibraryPermission() {
        try(FindingStore s=new FindingStore(app)){finding(s,52.52);}
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            c.get().findViewById(R.id.nav_findings).performClick();reviewButton(c.get().findViewById(android.R.id.content)).performClick();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();dialog.findViewById(R.id.photo_pick).performClick();
            Intent intent=Shadows.shadowOf(c.get()).getNextStartedActivityForResult().intent;assertEquals(Intent.ACTION_OPEN_DOCUMENT,intent.getAction());assertEquals("image/*",intent.getType());assertTrue(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));dialog.dismiss();
        }
    }
    @Test public void bundlingUiRequiresHumanConfirmationBeforeChangingStoredPlaces() {
        try(FindingStore s=new FindingStore(app)){finding(s,52.52);finding(s,52.52001);}
        try(ActivityController<BundleActivity> c=Robolectric.buildActivity(BundleActivity.class).setup();FindingStore s=new FindingStore(app)){
            confirmButton(c.get().findViewById(android.R.id.content)).performClick();assertEquals(2,s.bundles().size());AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,s.bundles().size());assertEquals(2,s.count());
        }
    }
    private CheckBox photoCheckbox(View v){if(v instanceof CheckBox b&&b.getText().toString().startsWith("Stelle-01-Foto-01"))return b;if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){CheckBox b=photoCheckbox(group.getChildAt(i));if(b!=null)return b;}return null;}
    private Button reviewButton(View v){return button(v,"Prüfen & Fotos");}
    private Button confirmButton(View v){return button(v,"Als dieselbe Fundstelle bestätigen");}
    private Button button(View v,String label){if(v instanceof Button b&&label.contentEquals(b.getText()))return b;if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){Button b=button(group.getChildAt(i),label);if(b!=null)return b;}return null;}
}
