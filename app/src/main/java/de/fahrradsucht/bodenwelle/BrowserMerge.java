package de.fahrradsucht.bodenwelle;

import org.json.*;
import java.io.IOException;
import java.util.*;

/** Conservative three-way comparison. Connected old/new bundles are resolved as one coherent place. */
final class BrowserMerge {
    record Part(String key,List<String> ids,boolean conflict,String phone,String browser){}
    final List<Part> parts=new ArrayList<>();
    final JSONObject base,edited,live;
    private final JSONObject normalizedBase,normalizedEdited,normalizedLive,survivingBase;
    final String fingerprint;
    private final Map<String,Set<String>> components=new LinkedHashMap<>();
    BrowserMerge(JSONObject base,JSONObject edited,JSONObject live)throws JSONException {
        this.base=base;this.edited=edited;this.live=live;fingerprint=BrowserJson.canonical(live);
        normalizedBase=normalized(base);normalizedEdited=normalized(edited);normalizedLive=normalized(live);
        JSONObject retained=new JSONObject(base.toString());for(String id:keys(base))if(edited.isNull(id))retained.put(id,JSONObject.NULL);survivingBase=normalized(retained);
        Map<String,String> parent=new HashMap<>();for(String id:keys(base))parent.put(id,id);for(String id:keys(live))parent.putIfAbsent(id,id);
        for(JSONObject side:List.of(base,edited,live)){
            Map<String,String> first=new HashMap<>();for(String id:keys(side))if(!side.isNull(id)){
                String group=side.getJSONObject(id).getString("group"),other=first.putIfAbsent(group,id);if(other!=null)parent.put(root(parent,id),root(parent,other));
            }
        }
        for(String id:new TreeSet<>(parent.keySet()))components.computeIfAbsent(root(parent,id),k->new TreeSet<>()).add(id);
        for(var component:components.entrySet()) {
            Set<String> ids=component.getValue();boolean changed=false,phoneChanged=false,same=true;
            for(String id:ids) {
                Object original=normalizedBase.opt(id),browser=normalizedEdited.opt(id),phone=normalizedLive.opt(id);
                if(base.has(id)){changed|=!BrowserJson.same(original,browser);phoneChanged|=!BrowserJson.same(original,phone);same&=BrowserJson.same(browser,phone);}
                else phoneChanged=true;
            }
            if(!changed)continue;
            List<String> exported=ids.stream().filter(base::has).collect(java.util.stream.Collectors.toList());
            parts.add(new Part(component.getKey(),exported,phoneChanged&&!same,describe(live,ids),describe(edited,ids)));
        }
    }
    JSONObject chosen(Map<String,Boolean> useBrowser)throws IOException,JSONException {
        JSONObject result=new JSONObject();for(Part part:parts){Boolean take=part.conflict?useBrowser.get(part.key):Boolean.TRUE;if(take==null)throw new IOException("Bitte für jeden Konflikt Handy oder Browser auswählen.");if(take){
            if(unchangedStructure(part))mergeFieldsIntoPhoneGroups(part,result);
            else for(String id:part.ids)result.put(id,edited.has(id)?edited.get(id):JSONObject.NULL);
        }}return result;
    }
    /** Group names are local IDs; compare their memberships rather than their spelling. */
    private static JSONObject normalized(JSONObject side)throws JSONException {
        Map<String,List<String>> groups=groups(side);JSONObject result=new JSONObject();
        for(List<String> ids:groups.values()) {
            Collections.sort(ids);String first=ids.get(0),label=side.getJSONObject(first).getString("group");
            String identity=(ids.size()==1&&label.equals("s"+first)?"single:":"place:")+BrowserJson.sha(String.join(",",ids).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            for(String id:ids){JSONObject row=new JSONObject(side.getJSONObject(id).toString());row.put("group",identity);result.put(id,row);}
        }
        for(String id:keys(side))if(side.isNull(id))result.put(id,JSONObject.NULL);return result;
    }
    private static Map<String,List<String>> groups(JSONObject side)throws JSONException {Map<String,List<String>> groups=new LinkedHashMap<>();for(String id:keys(side))if(!side.isNull(id))groups.computeIfAbsent(side.getJSONObject(id).getString("group"),k->new ArrayList<>()).add(id);return groups;}
    private boolean unchangedStructure(Part part)throws JSONException {
        for(String id:part.ids){if(live.optJSONObject(id)==null)return false;if(edited.isNull(id))continue;if(!survivingBase.getJSONObject(id).getString("group").equals(normalizedEdited.getJSONObject(id).getString("group")))return false;}
        return true;
    }
    /** A description/consent edit must never undo a newer phone-side bundling. */
    private void mergeFieldsIntoPhoneGroups(Part part,JSONObject result)throws JSONException,IOException {
        Set<String> touched=new HashSet<>();for(String id:part.ids)touched.add(live.getJSONObject(id).getString("group"));
        for(var group:groups(live).entrySet())if(touched.contains(group.getKey())) {
            JSONObject overrides=new JSONObject();Set<String> additions=new LinkedHashSet<>(),removals=new LinkedHashSet<>();
            for(String id:group.getValue())if(base.has(id)&&edited.optJSONObject(id)!=null) {
                JSONObject original=base.getJSONObject(id),browser=edited.getJSONObject(id);
                for(String field:List.of("category","note","hazards","traffic","surface"))if(!BrowserJson.same(original.get(field),browser.get(field))) {
                    if(overrides.has(field)&&!BrowserJson.same(overrides.get(field),browser.get(field)))throw new IOException("Die inzwischen gemeinsam gespeicherte Fundstelle hat widersprüchliche Browserangaben. Bitte eine aktuelle Projektdatei exportieren und gemeinsam bearbeiten.");
                    overrides.put(field,browser.get(field));
                }
                Set<String> old=photoKeys(original),next=photoKeys(browser);for(String key:next)if(!old.contains(key))additions.add(key);for(String key:old)if(!next.contains(key))removals.add(key);
            }
            Set<String> gallery=new LinkedHashSet<>();for(String id:group.getValue())gallery.addAll(photoKeys(live.getJSONObject(id)));gallery.removeAll(removals);gallery.addAll(additions);
            for(String id:group.getValue()) {
                if(base.has(id)&&edited.isNull(id)){result.put(id,JSONObject.NULL);continue;}
                JSONObject row=new JSONObject(live.getJSONObject(id).toString());for(String field:keys(overrides))row.put(field,overrides.get(field));row.put("photos",new JSONArray(gallery));
                if(base.has(id)&&edited.optJSONObject(id)!=null)for(String field:List.of("heightMm","reviewed","district"))if(!BrowserJson.same(base.getJSONObject(id).get(field),edited.getJSONObject(id).get(field)))row.put(field,edited.getJSONObject(id).get(field));
                result.put(id,row);
            }
        }
    }
    private static Set<String> photoKeys(JSONObject row)throws JSONException {Set<String> keys=new LinkedHashSet<>();JSONArray photos=row.getJSONArray("photos");for(int i=0;i<photos.length();i++)keys.add(photos.getString(i));return keys;}
    int conflicts(){return (int)parts.stream().filter(Part::conflict).count();}
    private static String root(Map<String,String> parent,String id){String p=parent.get(id);if(!p.equals(id)){p=root(parent,p);parent.put(id,p);}return p;}
    static Set<String> keys(JSONObject object){Set<String> result=new LinkedHashSet<>();object.keys().forEachRemaining(result::add);return result;}
    private static String describe(JSONObject side,Set<String> ids)throws JSONException {
        List<String> lines=new ArrayList<>();for(String id:ids){JSONObject e=side.optJSONObject(id);if(e==null){lines.add("#"+id+": gelöscht / hier nicht vorhanden");continue;}
            String note=e.getString("note");if(note.length()>160)note=note.substring(0,160)+"…";
            lines.add("#"+id+": "+Classification.label(Classification.BUMPS,e.getString("category"))+" · "+(e.getBoolean("reviewed")?"freigegeben":"offen")
                    +"\n"+note+"\n"+(e.isNull("traffic")?"Automatisch":Classification.label(Classification.ROADS,e.getString("traffic")))+" / "+(e.isNull("surface")?"Automatisch":Classification.label(Classification.SURFACES,e.getString("surface")))
                    +" · Bezirk "+(e.getString("district").isEmpty()?"automatisch":e.getString("district"))+" · Höhe "+(e.isNull("heightMm")?"keine":e.get("heightMm")+" mm")+" · "+e.getJSONArray("photos").length()+" Foto(s) · Gruppe "+e.getString("group")+" · Merkmale "+Classification.hazards(e.getInt("hazards")));}
        return String.join("\n\n",lines);
    }
}
