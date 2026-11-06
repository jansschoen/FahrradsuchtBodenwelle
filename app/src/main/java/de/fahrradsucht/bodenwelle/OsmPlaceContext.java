package de.fahrradsucht.bodenwelle;

import java.io.IOException;
import java.util.*;
import org.json.*;

/** Explicit OSM evidence only. Access restrictions are not cadastral ownership or authority boundaries. */
final class OsmPlaceContext {
    static final String PARK="bodenwelle:park",ACCESS="bodenwelle:private_access",OWNERSHIP="bodenwelle:private_ownership";
    private record Point(double lat,double lon){}
    private OsmPlaceContext(){}
    static Map<String,String> inspect(String response,double lat,double lon,float accuracy,OsmMatch match)throws IOException {
        Map<String,String> result=new HashMap<>();result.put("bodenwelle:context_checked","yes");
        try {
            JSONArray elements=new JSONObject(response).getJSONArray("elements");
            for(int i=0;i<elements.length();i++) {
                JSONObject element=elements.getJSONObject(i),tags=element.optJSONObject("tags");if(tags==null)continue;
                boolean park=Set.of("park","garden").contains(tags.optString("leisure"));
                boolean access="private".equals(tags.optString("access")),ownership="private".equals(tags.optString("ownership"));
                if(!park&&!access&&!ownership)continue;
                String type=element.optString("type");
                boolean area=park||"yes".equals(tags.optString("area"))||tags.has("landuse")||tags.has("building")||tags.has("amenity")||"multipolygon".equals(tags.optString("type"));
                boolean contains=false;
                try {
                    if(area&&"way".equals(type)){List<Point> ring=geometry(element.optJSONArray("geometry"));contains=closed(ring)&&inside(ring,lat,lon);}
                    else if(area&&"relation".equals(type)&&"multipolygon".equals(tags.optString("type"))) {
                        List<List<Point>> outer=rings(element.optJSONArray("members"),false),inner=rings(element.optJSONArray("members"),true);
                        contains=outer!=null&&inner!=null&&outer.stream().anyMatch(r->inside(r,lat,lon))&&inner.stream().noneMatch(r->inside(r,lat,lon));
                    }
                }catch(JSONException|IllegalArgumentException malformed){/* An incomplete polygon is no positive evidence. */}
                boolean matchedWay=!area&&"way".equals(type)&&match.assigned()&&Objects.equals(match.wayId(),element.optLong("id"))
                        &&match.distanceMeters()!=null&&match.distanceMeters()<=5&&accuracy<=25;
                String source=type+"/"+element.optLong("id");
                if(contains&&park)result.put(PARK,source);
                if((contains||matchedWay)&&access)result.put(ACCESS,source);
                if((contains||matchedWay)&&ownership)result.put(OWNERSHIP,source);
            }
            return result;
        }catch(JSONException invalid){throw new IOException("Invalid OSM context response",invalid);}
    }
    static boolean suggested(Finding f){Map<String,String> tags=f.details().osm().tags();return tags.containsKey(PARK)||tags.containsKey(ACCESS)||tags.containsKey(OWNERSHIP);}
    static String reason(Finding f) {return reason(f.details().osm());}
    static String reason(OsmMatch match) {
        Map<String,String> tags=match.tags();List<String> reasons=new ArrayList<>();
        if(tags.containsKey(PARK))reasons.add("GPS-Mittelpunkt innerhalb einer OSM-Park-/Gartenfläche ("+tags.get(PARK)+")");
        if(tags.containsKey(ACCESS))reasons.add("OSM kennzeichnet privaten Zugang (access=private; "+tags.get(ACCESS)+")");
        if(tags.containsKey(OWNERSHIP))reasons.add("OSM kennzeichnet privates Eigentum (ownership=private; "+tags.get(OWNERSHIP)+")");
        return String.join("\n",reasons);
    }
    static List<Finding> candidates(List<Finding> all,long after,long through){return all.stream().filter(f->f.id()>after&&f.id()<=through&&suggested(f)).collect(java.util.stream.Collectors.toList());}
    private static List<Point> geometry(JSONArray values)throws JSONException {
        List<Point> points=new ArrayList<>();if(values==null)return points;
        for(int i=0;i<values.length();i++){JSONObject p=values.getJSONObject(i);double lat=p.getDouble("lat"),lon=p.getDouble("lon");
            if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new IllegalArgumentException("Invalid coordinates");points.add(new Point(lat,lon));}
        return points;
    }
    private static boolean same(Point a,Point b){return Math.abs(a.lat-b.lat)<1e-7&&Math.abs(a.lon-b.lon)<1e-7;}
    private static boolean closed(List<Point> ring){return ring.size()>=4&&same(ring.get(0),ring.get(ring.size()-1));}
    private static List<List<Point>> rings(JSONArray members,boolean inner)throws JSONException {
        if(members==null)return null;List<List<Point>> remaining=new ArrayList<>(),complete=new ArrayList<>();
        for(int i=0;i<members.length();i++){JSONObject m=members.getJSONObject(i);if(!"way".equals(m.optString("type")))continue;
            String role=m.optString("role");if(inner?!"inner".equals(role):!role.isEmpty()&&!"outer".equals(role))continue;
            List<Point> points=geometry(m.optJSONArray("geometry"));if(points.size()<2)return null;remaining.add(points);}
        while(!remaining.isEmpty()) {
            List<Point> ring=new ArrayList<>(remaining.remove(0));boolean extended=true;
            while(!closed(ring)&&extended) {
                extended=false;
                for(int i=0;i<remaining.size();i++){
                    List<Point> part=new ArrayList<>(remaining.get(i));
                    if(same(ring.get(ring.size()-1),part.get(part.size()-1)))Collections.reverse(part);
                    if(same(ring.get(ring.size()-1),part.get(0))){ring.addAll(part.subList(1,part.size()));remaining.remove(i);extended=true;break;}
                }
            }
            if(!closed(ring))return null;complete.add(ring);
        }
        return complete;
    }
    private static boolean inside(List<Point> ring,double lat,double lon) {
        boolean inside=false;
        for(int i=0,j=ring.size()-1;i<ring.size();j=i++) {Point a=ring.get(i),b=ring.get(j);
            if((a.lat>lat)!=(b.lat>lat)&&lon<(b.lon-a.lon)*(lat-a.lat)/(b.lat-a.lat)+a.lon)inside=!inside;
        }
        return inside;
    }
}
