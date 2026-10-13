package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.location.Location;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PhotoPrivacyTest {
    private Application app;
    @Before public void before(){app=RuntimeEnvironment.getApplication();app.deleteDatabase("findings.db");PhotoFiles.clearLocal(app);}
    private long finding(){Location l=new Location("gps");l.setLatitude(52.5);l.setLongitude(13.4);l.setAccuracy(5);try(FindingStore s=new FindingStore(app)){return s.insert(l,null);}}
    private File source(int width,int height)throws Exception {
        File file=File.createTempFile("test-source-",".jpg",app.getCacheDir());Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);b.eraseColor(0xff348ac2);
        try(FileOutputStream out=new FileOutputStream(file)){assertTrue(b.compress(Bitmap.CompressFormat.JPEG,90,out));}b.recycle();return file;
    }
    @Test public void importStripsRealExifLocationTimeAndCameraMetadataAndHonorsOrientation()throws Exception {
        File source=source(4000,600);ExifInterface original=new ExifInterface(source.getPath());
        original.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,"2026:10:13 11:12:13");original.setAttribute(ExifInterface.TAG_GPS_LATITUDE,"52/1,30/1,0/1");original.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,"N");
        original.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,"13/1,24/1,0/1");original.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,"E");original.setAttribute(ExifInterface.TAG_MAKE,"Fairphone");
        original.setAttribute(ExifInterface.TAG_ORIENTATION,"6");original.saveAttributes();
        assertNotNull(new ExifInterface(source.getPath()).getAttribute(ExifInterface.TAG_GPS_LATITUDE));
        long id;try(FileInputStream in=new FileInputStream(source)){id=PhotoFiles.importStream(app,in,finding());}
        try(FindingStore s=new FindingStore(app)){FindingPhoto p=s.photo(id);File saved=PhotoFiles.localFile(app,p.filename());ExifInterface clean=new ExifInterface(saved.getPath());
            for(String tag:new String[]{ExifInterface.TAG_DATETIME_ORIGINAL,ExifInterface.TAG_DATETIME,ExifInterface.TAG_GPS_LATITUDE,ExifInterface.TAG_GPS_LONGITUDE,ExifInterface.TAG_MAKE})assertNull(tag,clean.getAttribute(tag));
            assertEquals(2048,p.height());assertEquals(307,p.width());assertTrue(p.bytes()<=PhotoFiles.MAX_JPEG_BYTES);assertEquals(saved.length(),p.bytes());
        }
        assertEquals("2026:10:13 11:12:13",new ExifInterface(source.getPath()).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));source.delete();
    }
    @Test public void fivePhotoLimitAndDeletionKeepStorageConsistent()throws Exception {
        long finding=finding();File source=source(32,24);
        for(int i=0;i<5;i++)try(FileInputStream in=new FileInputStream(source)){PhotoFiles.importStream(app,in,finding);}
        assertThrows(IllegalArgumentException.class,()->{try(FileInputStream in=new FileInputStream(source)){PhotoFiles.importStream(app,in,finding);}});
        try(FindingStore s=new FindingStore(app)){assertEquals(5,s.photos(finding).size());FindingPhoto first=s.photos(finding).get(0);s.deletePhoto(first.id());assertFalse(PhotoFiles.localFile(app,first.filename()).exists());assertEquals(4,s.photos(finding).size());s.delete(finding);assertEquals(0,s.photos(finding).size());}
        assertEquals(0,new File(app.getNoBackupFilesDir(),"photos").listFiles().length);source.delete();
    }
    @Test public void invalidImageNeverLeavesAStoredPhoto()throws Exception {
        long id=finding();assertThrows(java.io.IOException.class,()->PhotoFiles.importStream(app,new java.io.ByteArrayInputStream(new byte[]{1,2,3}),id));
        try(FindingStore s=new FindingStore(app)){assertTrue(s.photos(id).isEmpty());}assertEquals(0,new File(app.getNoBackupFilesDir(),"photos").listFiles().length);
    }
}
