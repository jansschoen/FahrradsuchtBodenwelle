package de.fahrradsucht.bodenwelle;

import java.time.*;
import java.util.*;

/** Transparent repetition indicator, not a probability of a defect or independent verification. */
public record EvidenceScore(int score,int measurements,int phases,int days) {
    public static EvidenceScore from(List<Finding> findings) {
        List<Long> times=new ArrayList<>();for(Finding finding:findings)times.add(finding.timeMillis());Collections.sort(times);
        Set<LocalDate> dates=new HashSet<>();int phases=0;long previous=0;
        for(long time:times){if(phases==0||time-previous>=300_000L)phases++;previous=time;dates.add(Instant.ofEpochMilli(time).atZone(ZoneId.of("Europe/Berlin")).toLocalDate());}
        int n=times.size(),score=n==0?0:phases>=5&&dates.size()>=3?5:phases>=3&&dates.size()>=2?4:phases>=2?3:n>=2?2:1;
        return new EvidenceScore(score,n,phases,dates.size());
    }
    public String label(){return "Messbasis "+score+"/5"+(measurements>1?" · mehrfach erfasst":" · Einzelmessung");}
    public String explanation(){return label()+"\nMessungen: "+measurements+" · Messphasen: "+phases+" · Messtage: "+days+"\n\nTreffer mit weniger als fünf Minuten Abstand gehören zur selben Messphase. 1: Einzelmessung; 2: mehrere Treffer; 3: mehrere Messphasen; 4: mindestens drei Phasen an zwei Tagen; 5: mindestens fünf Phasen an drei Tagen.\n\nDer Wert beschreibt den Umfang der Wiederholungen, keine Trefferwahrscheinlichkeit, bestätigte Gefährlichkeit oder unabhängige Prüfung. Auch wiederholte Messungen können denselben Messfehler enthalten.";}
}
