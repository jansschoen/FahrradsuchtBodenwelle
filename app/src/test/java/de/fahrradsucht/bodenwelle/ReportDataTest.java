package de.fahrradsucht.bodenwelle;

import java.time.Instant;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReportDataTest {
    static Finding finding(long id,double lat,double lon,boolean reviewed,long bundle,String note) {
        Finding raw=new Finding(id,Instant.parse("2026-10-26T07:24:19Z").toEpochMilli(),lat,lon,7,5f,48.0,40.0,"automatic","root_heave",note,reviewed,1234);
        return new Finding(raw.id(),raw.timeMillis(),lat,lon,raw.accuracy(),raw.speed(),raw.peak(),raw.threshold(),raw.source(),raw.category(),note,reviewed,raw.locationAgeMillis(),raw.position(),raw.detectorVersion(),raw.details(),bundle);
    }
    static MailReports.Group group(Finding... findings) {return MailReports.groups(java.util.Arrays.stream(findings).map(f->new MailReports.Item(f,"01","manual")).collect(java.util.stream.Collectors.toList()),true).get(0);}
    @Test public void modelExcludesUnreleasedMeasurementsAndUsesWeeksAndOnlyExplicitNotes() {
        Finding released=finding(1,52.52,13.405,true,0,"Privat am 26.10.2026 um 08:24"),hidden=finding(2,52.53,13.407,false,0,"Geheim");
        ReportData data=ReportData.create(group(released,hidden),id->"Mitte","",false,true,"BW-TEST");
        assertEquals(1,data.entries().size());ReportData.Entry e=data.entries().get(0);assertEquals("#1",e.references());assertEquals("KW 44/2026",e.weeks());assertTrue(e.notes().isEmpty());
        assertEquals("48,0 m/s² (1 Messung)",e.sensor());assertEquals("Nicht erhoben",e.height());
        String representation=data.toString();for(String forbidden:List.of("2026-10-26","07:24","1234","Privat","Geheim","#2"))assertFalse(forbidden,representation.contains(forbidden));
        assertEquals(List.of(released.note()),ReportData.create(group(released),id->"Mitte","",true,false,"BW-TEST").entries().get(0).notes());
    }
    @Test public void numberingUsesExactlyTheSameConfirmedPlacesAsMailAndPhotos() {
        Finding a=finding(1,52.52,13.405,true,7,""),b=finding(2,52.52001,13.405,true,7,""),c=finding(3,52.521,13.407,true,0,"");
        MailReports.Group group=group(a,b,c);ReportData data=ReportData.create(group,id->"Mitte","",false,false,"BW-TEST");
        assertEquals(2,data.entries().size());assertEquals("#1, #2",data.entries().get(0).references());assertEquals(2,data.entries().get(0).observations());assertEquals(2,data.entries().get(1).number());
    }
    @Test public void allPointsAndSectionEndpointsFitAndTwentyOverlappingNumbersRemainSeparate() {
        Finding f=finding(1,52.52,13.405,true,0,"");ReportData.Entry base=ReportData.create(group(f),id->"Mitte","",false,false,"BW-TEST").entries().get(0);
        java.util.ArrayList<ReportData.Entry> same=new java.util.ArrayList<>();for(int i=0;i<20;i++)same.add(base);
        ReportMapLayout layout=ReportMapLayout.fit(same,1024,600);assertTrue(layout.tiles().size()<=20);
        List<ReportMapLayout.Pixel> labels=layout.labels(same);for(int i=0;i<labels.size();i++)for(int j=i+1;j<labels.size();j++)assertTrue(Math.hypot(labels.get(i).x()-labels.get(j).x(),labels.get(i).y()-labels.get(j).y())>=38);
        ReportData.Entry section=new ReportData.Entry(1,base.title(),base.district(),"",base.latitude(),base.longitude(),7,false,base.weeks(),"#1",1,base.traffic(),base.surface(),"",base.sensor(),base.height(),List.of(),List.of(new ReportData.Section(52.52,13.405,52.57,13.48,500)));
        ReportMapLayout wide=ReportMapLayout.fit(List.of(section),1024,600);ReportMapLayout.Pixel end=wide.project(52.57,13.48);assertTrue(end.x()>=60&&end.x()<=964);assertTrue(end.y()>=60&&end.y()<=540);
    }
}
