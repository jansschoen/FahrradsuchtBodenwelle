package de.fahrradsucht.bodenwelle;

import java.time.Instant;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class BundlingTest {
    private Finding finding(long id,double lat,String category,float accuracy,boolean reviewed,long bundle) {
        Finding f=new Finding(id,Instant.parse(id%2==0?"2026-10-13T07:12:34Z":"2026-10-20T11:45:19Z").toEpochMilli(),lat,13.4,accuracy,6f,40.0+id,40.0,"automatic",category,"",reviewed,1000);
        return new Finding(f.id(),f.timeMillis(),f.latitude(),f.longitude(),f.accuracy(),f.speed(),f.peak(),f.threshold(),f.source(),f.category(),f.note(),f.reviewed(),f.locationAgeMillis(),f.position(),f.detectorVersion(),f.details(),bundle);
    }
    private FindingBundle singleton(Finding f){return new FindingBundle(f.bundleKey(),List.of(f));}
    @Test public void suggestionsUseStrictTwoMeterDistanceAndRejectConflictingKnownTypes() {
        Finding a=finding(1,52.5,"root_heave",5,true,0),b=finding(2,52.50001,"root_heave",5,true,0);
        assertTrue(BundleSuggestions.compatible(singleton(a),singleton(b)));
        assertFalse(BundleSuggestions.compatible(singleton(a),singleton(finding(3,52.50001,"pothole",5,true,0))));
        assertFalse(BundleSuggestions.compatible(singleton(a),singleton(finding(3,52.50001,"root_heave",26,true,0))));
        assertTrue(BundleSuggestions.compatible(singleton(a),singleton(finding(3,52.50001,"other",5,false,0))));
        assertFalse(BundleSuggestions.compatible(singleton(a),singleton(finding(3,52.50002,"root_heave",5,true,0))));
        assertFalse(BundleSuggestions.compatible(singleton(a),singleton(finding(3,52.50001,"root_heave",Float.NaN,true,0))));
    }
    @Test public void suggestionsCannotChainTogetherDistantEndpoints() {
        List<List<FindingBundle>> proposals=BundleSuggestions.suggest(List.of(singleton(finding(1,52.5,"other",5,false,0)),singleton(finding(2,52.500012,"other",5,false,0)),singleton(finding(3,52.500024,"other",5,false,0))));
        assertEquals(1,proposals.size());assertEquals(2,proposals.get(0).size());
    }
    @Test public void confirmedRepeatIsOneMailPlaceWithCountsWeeksAndMeasurementRange() {
        Finding a=finding(1,52.5,"root_heave",8,true,7),b=finding(2,52.50005,"root_heave",5,true,7);
        MailReports.Group g=MailReports.groups(List.of(new MailReports.Item(a,"01","manual"),new MailReports.Item(b,"01","manual")),true).get(0);
        assertEquals(1,g.points().size());assertEquals(2,g.items().size());assertEquals(2,g.points().get(0).representative().id());
        MailReports.Draft d=MailReports.draft(g,"Mitte",true,false,"");assertTrue(d.subject().contains("1 Unebenheit"));
        assertTrue(d.body().contains("2 ausgewählte Beobachtungen"));assertTrue(d.body().contains("KW 42/2026"));assertTrue(d.body().contains("KW 43/2026"));
        assertTrue(d.body().contains("41,0–42,0 m/s²"));assertFalse(d.body().contains("07:12"));
    }
    @Test public void revocationSelectionAndDifferentRecipientsDoNotLeakOtherObservations() {
        Finding a=finding(1,52.5,"root_heave",5,true,7),b=finding(2,52.5,"root_heave",5,false,7);
        MailReports.Group g=MailReports.groups(List.of(new MailReports.Item(a,"01","manual"),new MailReports.Item(b,"01","manual")),true).get(0);
        assertEquals(1,g.items().size());assertFalse(MailReports.draft(g,"Mitte",true,false,"").body().contains("Wiederholt beobachtet"));
        b=finding(2,52.5,"root_heave",5,true,7);
        assertEquals(2,MailReports.groups(List.of(new MailReports.Item(a,"01","manual"),new MailReports.Item(b,"02","manual")),true).size());
    }
    @Test public void twentyPlaceLimitNeverSplitsObservationsOfOneConfirmedPlace() {
        java.util.List<MailReports.Item> items=new java.util.ArrayList<>();for(int i=1;i<=41;i++)items.add(new MailReports.Item(finding(i,52.5+i*.00001,"root_heave",5,true,7),"01","manual"));
        List<MailReports.Group> groups=MailReports.groups(items,true);assertEquals(1,groups.size());assertEquals(41,groups.get(0).items().size());assertEquals(1,groups.get(0).points().size());
    }
    @Test public void bundleDisplayUsesHighestRecordedValueWithoutMixingHeightAndSensorClasses() {
        Finding a=finding(1,52.5,"root_heave",5,true,7),b=finding(2,52.5,"root_heave",5,true,7);
        assertEquals(42,new FindingBundle(7,List.of(a,b)).intensity().value(),0);
        FindingDetails measured=a.details().reviewed(5.0,a.peak(),0,"unknown",false,"unknown",false);
        Finding height=new Finding(a.id(),a.timeMillis(),a.latitude(),a.longitude(),a.accuracy(),a.speed(),a.peak(),a.threshold(),a.source(),a.category(),a.note(),a.reviewed(),a.locationAgeMillis(),a.position(),a.detectorVersion(),measured,7);
        Intensity result=new FindingBundle(7,List.of(height,b)).intensity();assertTrue(result.heightBased());assertEquals("mm",result.unit());assertEquals(5,result.value(),0);
    }
}
