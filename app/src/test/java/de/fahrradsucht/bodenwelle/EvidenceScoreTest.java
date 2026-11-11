package de.fahrradsucht.bodenwelle;

import org.junit.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.Assert.*;

public class EvidenceScoreTest {
    private Finding f(long t){return new Finding(1,t,52.52,13.405,5,null,48.0,40.0,"sensor","other","",false,0);}
    private EvidenceScore score(String... stamps){return EvidenceScore.from(Arrays.stream(stamps).map(t->f(Instant.parse(t).toEpochMilli())).toList());}
    @Test public void singleAndCorrelatedBurstsHaveLimitedSupport(){
        assertEquals(0,EvidenceScore.from(List.of()).score());
        assertEquals(1,score("2026-11-01T12:00:00Z").score());
        var burst=score("2026-11-01T12:08:00Z","2026-11-01T12:00:00Z","2026-11-01T12:04:00Z");
        assertEquals(2,burst.score());assertEquals(3,burst.measurements());assertEquals(1,burst.phases());assertEquals(1,burst.days());
    }
    @Test public void fiveMinuteGapAndSeparateDaysDetermineHigherLevels(){
        assertEquals(2,score("2026-11-01T12:00:00Z","2026-11-01T12:04:59.999Z").score());
        assertEquals(3,score("2026-11-01T12:00:00Z","2026-11-01T12:05:00Z").score());
        assertEquals(3,score("2026-11-01T12:00:00Z","2026-11-01T13:00:00Z","2026-11-01T14:00:00Z").score());
        assertEquals(4,score("2026-11-01T12:00:00Z","2026-11-01T13:00:00Z","2026-11-02T12:00:00Z").score());
        assertEquals(5,score("2026-11-01T12:00:00Z","2026-11-01T13:00:00Z","2026-11-02T12:00:00Z","2026-11-02T13:00:00Z","2026-11-03T12:00:00Z").score());
    }
    @Test public void calendarDaysUseBerlinIncludingTheDaylightSavingTransition(){
        assertEquals(1,score("2026-11-01T22:59:00Z","2026-11-01T23:01:00Z").phases());
        assertEquals(2,score("2026-11-01T22:59:00Z","2026-11-01T23:01:00Z").days());
        assertEquals(1,score("2026-10-25T00:30:00Z","2026-10-25T01:30:00Z").days());
        assertEquals(2,score("2026-10-25T00:30:00Z","2026-10-25T01:30:00Z").phases());
    }
}
