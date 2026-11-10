package de.fahrradsucht.bodenwelle;

import android.graphics.*;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BrowserPhotoMetadataTest {
    @Test public void browserImportAppliesOrientationAndRemovesIncomingExif()throws Exception {
        File dir=new File(RuntimeEnvironment.getApplication().getCacheDir(),"browser-photo-test");dir.mkdirs();File source=new File(dir,"input.jpg");Bitmap b=Bitmap.createBitmap(60,30,Bitmap.Config.ARGB_8888);b.eraseColor(0xff177c66);try(FileOutputStream out=new FileOutputStream(source)){b.compress(Bitmap.CompressFormat.JPEG,90,out);}b.recycle();
        ExifInterface exif=new ExifInterface(source.getPath());exif.setAttribute(ExifInterface.TAG_ORIENTATION,"6");exif.setAttribute(ExifInterface.TAG_DATETIME,"2026:11:10 12:34:56");exif.setAttribute(ExifInterface.TAG_MAKE,"PRIVATE");exif.setLatLong(52.52,13.405);exif.saveAttributes();
        BrowserPhotos.Prepared prepared=BrowserPhotos.prepare(source,dir);assertEquals(30,prepared.width());assertEquals(60,prepared.height());ExifInterface saved=new ExifInterface(prepared.source().getPath());
        assertNull(saved.getAttribute(ExifInterface.TAG_GPS_LATITUDE));assertNull(saved.getAttribute(ExifInterface.TAG_DATETIME));assertNull(saved.getAttribute(ExifInterface.TAG_MAKE));assertEquals("PRIVATE",new ExifInterface(source.getPath()).getAttribute(ExifInterface.TAG_MAKE));
    }
}
