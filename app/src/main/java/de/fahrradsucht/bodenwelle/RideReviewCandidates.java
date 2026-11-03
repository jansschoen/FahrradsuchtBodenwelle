package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.List;

/** Includes the five-metre boundary; unlike general suggestions, differing descriptions may be reconciled. */
final class RideReviewCandidates {
    static final double DISTANCE_METERS=5;
    record Pair(long first,long second){}
    private RideReviewCandidates(){}
    static List<Pair> pairs(List<FindingBundle> bundles,long after,long through) {
        List<Pair> result=new ArrayList<>();
        boolean[] fresh=new boolean[bundles.size()];
        for(int i=0;i<fresh.length;i++)fresh[i]=containsNew(bundles.get(i),after,through);
        for(int i=0;i<bundles.size();i++)if(fresh[i])for(int j=0;j<bundles.size();j++) {
            if(i==j||fresh[j]&&j<i)continue;
            FindingBundle a=bundles.get(i),b=bundles.get(j);
            if(near(a,b))result.add(new Pair(a.representative().id(),b.representative().id()));
        }
        return result;
    }
    private static boolean containsNew(FindingBundle bundle,long after,long through) {
        return bundle.observations().stream().anyMatch(f->f.id()>after&&f.id()<=through);
    }
    static boolean near(FindingBundle a,FindingBundle b) {
        if(a.key()==b.key())return false;
        for(Finding x:a.observations())for(Finding y:b.observations()) {
            if(!valid(x)||!valid(y)||BundleSuggestions.distance(x,y)>DISTANCE_METERS+1e-6)return false;
        }
        return true;
    }
    private static boolean valid(Finding f) {
        return Double.isFinite(f.latitude())&&Double.isFinite(f.longitude())&&Math.abs(f.latitude())<=90&&Math.abs(f.longitude())<=180;
    }
}
