package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class PhotoAttachmentTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");PhotoFiles.clearLocal(app);}
    private long observation(FindingStore s,boolean released){Location l=new Location("gps");l.setLatitude(52.5);l.setLongitude(13.4);l.setAccuracy(5);long id=s.insert(l,null);s.review(id,"root_heave","",released);return id;}
    private long photo(FindingStore s,long id)throws Exception {String filename=java.util.UUID.randomUUID()+".jpg";Files.write(PhotoFiles.localFile(app,filename).toPath(),new byte[]{1,2,3});return s.addPhoto(id,filename,1,1,3);}
    private MailReports.Group group(FindingStore s,long...ids){List<MailReports.Item> items=new ArrayList<>();for(long id:ids)items.add(new MailReports.Item(s.get(id),"01","manual"));return MailReports.groups(items,true).get(0);}
    @Test public void onlyPhotosOfSelectedReleasedObservationsAreIncludedEvenWithinOneBundle()throws Exception {
        try(FindingStore s=new FindingStore(app)){long a=observation(s,true),b=observation(s,false);photo(s,a);photo(s,b);long key=s.mergeBundles(List.of(-a,-b));
            MailReports.Group g=group(s,a,b);List<MailAttachments.Choice> choices=MailAttachments.choices(g,s);assertEquals(1,choices.size());assertEquals(a,choices.get(0).photo().findingId());
            s.separateBundle(key);assertEquals(1,s.photos(a).size());assertEquals(1,s.photos(b).size());
        }
    }
    @Test public void snapshotsAreReadOnlyTimestampFreeAndRemainReadableAfterLocalPhotoDeletion()throws Exception {
        try(FindingStore s=new FindingStore(app)){long a=observation(s,true);long photo=photo(s,a);MailReports.Group g=group(s,a);List<MailAttachments.Choice> choices=MailAttachments.choices(g,s);
            ArrayList<Uri> uris=MailAttachments.prepare(app,choices,g);Uri uri=uris.get(0);assertEquals("Stelle-01-Foto-01.jpg",uri.getLastPathSegment());
            s.deletePhoto(photo);
            try(ParcelFileDescriptor descriptor=app.getContentResolver().openFileDescriptor(uri,"r")){assertNotNull(descriptor);assertEquals(3,descriptor.getStatSize());}
            assertThrows(java.io.FileNotFoundException.class,()->app.getContentResolver().openFileDescriptor(uri,"w"));
            assertThrows(IllegalArgumentException.class,()->PhotoFiles.outgoingFile(app,"../photos","Stelle-01-Foto-01.jpg"));
        }
    }
    @Test public void revokedOrDeletedPhotoBlocksPreparationAndLimitsNeverSilentlyDropPhotos()throws Exception {
        try(FindingStore s=new FindingStore(app)){long a=observation(s,true);long p=photo(s,a);MailReports.Group g=group(s,a);List<MailAttachments.Choice> choices=MailAttachments.choices(g,s);
            s.review(a,"root_heave","",false);assertThrows(java.io.IOException.class,()->MailAttachments.prepare(app,choices,g));
            s.review(a,"root_heave","",true);s.deletePhoto(p);assertThrows(java.io.IOException.class,()->MailAttachments.prepare(app,choices,g));
            List<MailAttachments.Choice> tooMany=new ArrayList<>();for(int i=0;i<11;i++)tooMany.add(choices.get(0));assertThrows(IllegalArgumentException.class,()->MailAttachments.prepare(app,tooMany,g));
        }
    }
    @Test public void nativeMailAttachmentIntentGrantsReadOnlyAccessToAllStreams() {
        ArrayList<Uri> photos=new ArrayList<>(List.of(Uri.parse("content://"+app.getPackageName()+".photos/mail/a/Stelle-01-Foto-01.jpg"),Uri.parse("content://"+app.getPackageName()+".photos/mail/a/Stelle-01-Foto-02.jpg")));
        Intent intent=MailIntent.withPhotos("amt@example.org","Bitte prüfen","KW 42/2026",photos);
        assertEquals(Intent.ACTION_SEND_MULTIPLE,intent.getAction());assertEquals(2,intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM).size());assertEquals(2,intent.getClipData().getItemCount());
        assertTrue((intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);assertEquals(0,intent.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertEquals(Intent.ACTION_SENDTO,intent.getSelector().getAction());assertEquals("mailto",intent.getSelector().getData().getScheme());
        assertEquals(Intent.ACTION_SEND,MailIntent.withPhotos("amt@example.org","B","T",new ArrayList<>(List.of(photos.get(0)))).getAction());
    }
}
