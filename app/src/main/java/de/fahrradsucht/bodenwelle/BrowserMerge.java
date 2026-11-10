package de.fahrradsucht.bodenwelle;

import org.json.*;
import java.io.IOException;
import java.util.*;

/** Conservative three-way comparison. Connected old/new bundles are resolved as one coherent place. */
final class BrowserMerge {
    record Part(String key,List<String> ids,boolean conflict,String phone,String browser){}
    final List<Part> parts=new ArrayList<>();
    final JSONObject base,edited,live;
    final String fingerprint;
    private final Map<String,Set<String>> components=new LinkedHashMap<>();
    BrowserMerge(JSONObject base,JSONObject edited,JSONObject live)throws JSONException {
        this.base=base;this.edited=edited;this.live=live;fingerprint=BrowserJson.canonical(live);
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
                Object original=base.opt(id),browser=edited.opt(id),phone=live.opt(id);
                if(base.has(id)){changed|=!BrowserJson.same(original,browser);phoneChanged|=!BrowserJson.same(original,phone);same&=BrowserJson.same(browser,phone);}
                else phoneChanged=true;
            }
            if(!changed)continue;
            List<String> exported=ids.stream().filter(base::has).collect(java.util.stream.Collectors.toList());
            parts.add(new Part(component.getKey(),exported,phoneChanged&&!same,describe(live,ids),describe(edited,ids)));
        }
    }
    JSONObject chosen(Map<String,Boolean> useBrowser)throws IOException,JSONException {
        JSONObject result=new JSONObject();for(Part part:parts){Boolean take=part.conflict?useBrowser.get(part.key):Boolean.TRUE;if(take==null)throw new IOException("Bitte für jeden Konflikt Handy oder Browser auswählen.");if(take)for(String id:part.ids)result.put(id,edited.has(id)?edited.get(id):JSONObject.NULL);}return result;
    }
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
