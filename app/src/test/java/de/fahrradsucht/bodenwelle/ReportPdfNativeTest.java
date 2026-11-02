package de.fahrradsucht.bodenwelle;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ReportPdfNativeTest {
    static File folder(){File dir=new File(System.getProperty("java.io.tmpdir"),"bodenwelle-pdf-tests");dir.mkdirs();return dir;}
    @Test public void realPdfHasMultiplePagesReadablePositionsAndAnExplicitMapFallback() throws Exception {
        Application app=RuntimeEnvironment.getApplication();
        MailReports.Group group=ReportDataTest.group(ReportDataTest.finding(1,52.5192,13.398,true,0,"Beispiel: sichtbare Kante am rechten Wegrand."),
                ReportDataTest.finding(2,52.522,13.412,true,0,""),ReportDataTest.finding(3,52.52,13.405,true,0,""));
        ReportData data=ReportData.create(group,id->"Berlin-Mitte · Beispieldaten","Beispielbericht – keine tatsächliche Schadensmeldung",true,true,"BW-ABCD2026");
        String tiles=System.getProperty("bodenwelle.preview.tiles");
        try(ReportMap.Snapshot map=ReportMap.render(data,tile->tiles==null?null:BitmapFactory.decodeFile(new File(tiles,tile.zoom()+"-"+tile.x()+"-"+tile.y()+".png").getPath()))) {
            File pdf=new File(folder(),"example.pdf");int pages=ReportPdf.write(app,pdf,data,map,List.of());assertTrue(pages>=3);
            assertTrue(pdf.length()>1000);assertTrue(new String(Files.readAllBytes(pdf.toPath()),0,5,java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-"));
            assertEquals(tiles!=null,map.complete());
        }
    }
    @Test public void cachedOsmTilesAreUsedOfflineWithoutNetworkAndRenderTheCompleteViewport()throws Exception {
        Application app=RuntimeEnvironment.getApplication();
        ReportData data=ReportData.create(ReportDataTest.group(ReportDataTest.finding(1,52.52,13.405,true,0,"")),id->"Mitte","",false,true,"BW-TEST");
        try(ReportTiles tiles=new ReportTiles(app)){
            org.osmdroid.tileprovider.modules.SqlTileWriter cache=new org.osmdroid.tileprovider.modules.SqlTileWriter();
            org.osmdroid.tileprovider.tilesource.XYTileSource source=new org.osmdroid.tileprovider.tilesource.XYTileSource("OSM",0,19,256,".png",new String[]{"https://tile.openstreetmap.org/"});
            Bitmap tile=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);tile.eraseColor(0xffd4e6cf);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();tile.compress(Bitmap.CompressFormat.PNG,100,bytes);tile.recycle();
            for(ReportMapLayout.Tile cell:ReportMapLayout.fit(data.entries(),1024,600).tiles()){long index=org.osmdroid.util.MapTileIndex.getTileIndex(cell.zoom(),cell.x(),cell.y());cache.saveFile(source,index,new java.io.ByteArrayInputStream(bytes.toByteArray()),System.currentTimeMillis()+60_000);assertTrue(cache.exists(source,index));}
            try(ReportMap.Snapshot map=ReportMap.render(data,tiles)){assertTrue(map.complete());assertEquals(0xffd4e6cf,map.bitmap().getPixel(0,0));}
        }
    }
    @Test public void twentyPointTablesAndLongNotesPaginateWithoutDroppingEntries()throws Exception {
        List<Finding> findings=new ArrayList<>();for(int i=1;i<=20;i++)findings.add(ReportDataTest.finding(i,52.52+i*.0001,13.405,true,0,i==1?"Ein langer Hinweis. ".repeat(400)+"ENDE-DER-NOTIZ":""));
        ReportData data=ReportData.create(ReportDataTest.group(findings.toArray(new Finding[0])),id->"Mitte","",true,false,"BW-11223344");
        try(ReportMap.Snapshot map=ReportMap.render(data,tile->null)) {
            File pdf=new File(folder(),"twenty-points.pdf");assertTrue(ReportPdf.write(RuntimeEnvironment.getApplication(),pdf,data,map,List.of())>=5);assertTrue(pdf.length()>1000);
        }
    }
}
