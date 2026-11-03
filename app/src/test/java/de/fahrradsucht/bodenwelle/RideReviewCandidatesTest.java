package de.fahrradsucht.bodenwelle;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class RideReviewCandidatesTest {
    private Finding f(long id,double metres) {
        return new Finding(id,1000,0,Math.toDegrees(metres/6371000),45,5f,48.0,40.0,"automatic","other","",false,0);
    }
    private FindingBundle b(long id,double metres){return new FindingBundle(-id,List.of(f(id,metres)));}
    @Test public void includesExactlyFiveMetresAndNewToOldButNeverOldToOldOrLaterRides() {
        var oldA=b(1,0);var oldB=b(2,1);var fresh=b(3,5);var later=b(4,20);
        assertTrue(RideReviewCandidates.near(oldA,fresh));assertFalse(RideReviewCandidates.near(oldA,b(5,5.001)));
        var pairs=RideReviewCandidates.pairs(List.of(oldA,oldB,fresh,later),2,3);
        assertEquals(2,pairs.size());assertTrue(pairs.stream().allMatch(p->p.first()==3||p.second()==3));
        assertTrue(RideReviewCandidates.pairs(List.of(oldA,oldB),2,3).isEmpty());
    }
    @Test public void comparisonIsNotDuplicatedWhenBothPlacesAreNew() {
        assertEquals(1,RideReviewCandidates.pairs(List.of(b(1,0),b(2,1)),0,2).size());
    }
    @Test public void cannotBridgeAChainAfterConfirmingTheFirstPair() {
        Finding a=f(1,0),b=f(2,4),c=f(3,8);
        assertEquals(2,RideReviewCandidates.pairs(List.of(new FindingBundle(-1,List.of(a)),new FindingBundle(-2,List.of(b)),new FindingBundle(-3,List.of(c))),0,3).size());
        assertFalse(RideReviewCandidates.near(new FindingBundle(7,List.of(a,b)),new FindingBundle(-3,List.of(c))));
    }
    @Test public void differentDescriptionsAndImpreciseGpsAreShownForHumanJudgement() {
        Finding a=f(1,0),raw=f(2,1);
        Finding other=new Finding(raw.id(),raw.timeMillis(),raw.latitude(),raw.longitude(),raw.accuracy(),raw.speed(),raw.peak(),raw.threshold(),raw.source(),"pothole","",false,0);
        assertTrue(RideReviewCandidates.near(new FindingBundle(-1,List.of(a)),new FindingBundle(-2,List.of(other))));
    }
}
