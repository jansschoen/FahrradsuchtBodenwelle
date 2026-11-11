package de.fahrradsucht.bodenwelle;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;

/** A confirmed description and the exact source state the user compared before merging. */
final class BundleDescription {
    private BundleDescription() {}
    static boolean differs(FindingStore store,List<Finding> members) {
        if(members.size()<2)return false;
        List<Object> first=common(store,members.get(0));
        for(Finding f:members)if(!first.equals(common(store,f)))return true;
        return false;
    }
    private static List<Object> common(FindingStore store,Finding f) {
        var d=f.details();return List.of(Classification.bumpId(f.category()),f.note(),d.hazards(),d.traffic(),d.trafficManual(),d.surface(),d.surfaceManual(),
                store.photos(f.id()).stream().map(FindingPhoto::id).sorted().collect(java.util.stream.Collectors.toList()));
    }
    static void validate(SharedFindingDetails value, List<Finding> members) {
        if(value==null||value.conflicting())throw new IllegalArgumentException("Bitte die gemeinsamen Angaben vollständig auswählen.");
        known(Classification.BUMPS,value.category().value());known(Classification.ROADS,value.traffic().value());known(Classification.SURFACES,value.surface().value());
        if(value.hazards()<0||value.hazards()>=(1<<Classification.HAZARDS.length))throw new IllegalArgumentException("Ungültige Gefährdungsmerkmale.");
        if(value.note()==null||value.note().length()>20000&&members.stream().anyMatch(f->!f.note().equals(value.note())))throw new IllegalArgumentException("Bitte die gemeinsame Beschreibung auf höchstens 20.000 Zeichen kürzen.");
        if(!value.traffic().manual()&&!value.traffic().value().equals(SharedFindingDetails.automatic(members,true).value())
                ||!value.surface().manual()&&!value.surface().value().equals(SharedFindingDetails.automatic(members,false).value()))
            throw new IllegalArgumentException("Die automatischen Angaben haben sich geändert. Bitte die Bündelung erneut prüfen.");
    }
    private static void known(Classification.Choice[] choices,String id) {
        for(var choice:choices)if(choice.id().equals(id))return;
        throw new IllegalArgumentException("Bitte einen gültigen gemeinsamen Wert auswählen.");
    }
    static String signature(FindingStore store,List<Finding> members) {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            for(Finding f:members.stream().sorted(Comparator.comparingLong(Finding::id)).collect(java.util.stream.Collectors.toList())) {
                for(String value:List.of(Long.toString(f.id()),Long.toString(f.bundleKey()),f.category(),f.note(),f.details().json(),Boolean.toString(f.reviewed())))add(digest,value);
                for(FindingPhoto photo:store.photos(f.id()))add(digest,photo.id()+":"+photo.filename());
                add(digest,"end");
            }
            return android.util.Base64.encodeToString(digest.digest(),android.util.Base64.NO_WRAP);
        }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private static void add(MessageDigest digest,String text) {
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);
    }
}
