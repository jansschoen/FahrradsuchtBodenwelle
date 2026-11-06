package de.fahrradsucht.bodenwelle;

import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class OsmPlaceContextTest {
    private JSONObject point(double lat,double lon)throws Exception{return new JSONObject().put("lat",lat).put("lon",lon);}
    private JSONArray square(double size)throws Exception{return new JSONArray().put(point(52.5-size,13.4-size)).put(point(52.5-size,13.4+size)).put(point(52.5+size,13.4+size)).put(point(52.5+size,13.4-size)).put(point(52.5-size,13.4-size));}
    private JSONObject way(int id,JSONObject tags,JSONArray geometry)throws Exception{return new JSONObject().put("type","way").put("id",id).put("tags",tags).put("geometry",geometry);}
    private String response(JSONObject... elements)throws Exception{JSONArray array=new JSONArray();for(JSONObject e:elements)array.put(e);return new JSONObject().put("elements",array).toString();}
    private Map<String,String> inspect(String response,double lat,double lon)throws Exception{return OsmPlaceContext.inspect(response,lat,lon,5,OsmMatch.empty("no_match"));}
    @Test public void parkRequiresContainmentAndNeverUsesJustBoundingBoxOrName()throws Exception {
        String response=response(way(42,new JSONObject().put("leisure","park"),square(.001)));
        assertEquals("way/42",inspect(response,52.5,13.4).get(OsmPlaceContext.PARK));
        assertFalse(inspect(response,52.502,13.4).containsKey(OsmPlaceContext.PARK));
        assertFalse(inspect(response(way(43,new JSONObject().put("name","Privatpark"),square(.001))),52.5,13.4).containsKey(OsmPlaceContext.PARK));
    }
    @Test public void privateAccessAndOwnershipRemainDistinctAndRoadLoopsAreNotParcels()throws Exception {
        JSONObject privateArea=new JSONObject().put("landuse","residential").put("access","private");
        Map<String,String> result=inspect(response(way(7,privateArea,square(.001))),52.5,13.4);
        assertTrue(result.containsKey(OsmPlaceContext.ACCESS));assertFalse(result.containsKey(OsmPlaceContext.OWNERSHIP));
        result=inspect(response(way(7,new JSONObject().put("landuse","residential").put("ownership","private"),square(.001))),52.5,13.4);
        assertTrue(result.containsKey(OsmPlaceContext.OWNERSHIP));
        OsmMatch boundary=new OsmMatch("matched",7L,Map.of(),1000,"medium",1.0,"test","other","unknown");
        assertFalse(OsmPlaceContext.inspect(response(way(7,new JSONObject().put("highway","pedestrian").put("area","yes").put("access","private"),square(.001))),52.50101,13.4,5,boundary).containsKey(OsmPlaceContext.ACCESS));
        assertFalse(inspect(response(way(7,new JSONObject().put("highway","residential").put("access","private"),square(.001))),52.5,13.4).containsKey(OsmPlaceContext.ACCESS));
    }
    @Test public void onlyCloseUnambiguousPrivateWaysSupplyAccessEvidence()throws Exception {
        String response=response(way(7,new JSONObject().put("highway","service").put("access","private"),new JSONArray().put(point(52.49,13.4)).put(point(52.51,13.4))));
        OsmMatch match=new OsmMatch("matched",7L,Map.of("access","private"),1000,"medium",0.0,"test","carriageway","unknown");
        assertEquals("way/7",OsmPlaceContext.inspect(response,52.5,13.4,5,match).get(OsmPlaceContext.ACCESS));
        assertFalse(OsmPlaceContext.inspect(response,52.5,13.4,30,match).containsKey(OsmPlaceContext.ACCESS));
        OsmMatch ambiguous=new OsmMatch("nearest_suggestion",7L,match.tags(),1000,"low",0.0,"test","carriageway","unknown");
        assertFalse(OsmPlaceContext.inspect(response,52.5,13.4,5,ambiguous).containsKey(OsmPlaceContext.ACCESS));
    }
    @Test public void multipolygonHolesAndFragmentedOuterRingsAreRespected()throws Exception {
        JSONArray outside=square(.001),hole=square(.0001);
        JSONArray part1=new JSONArray().put(outside.get(0)).put(outside.get(1)).put(outside.get(2));
        JSONArray part2=new JSONArray().put(outside.get(0)).put(outside.get(3)).put(outside.get(2));
        JSONArray members=new JSONArray().put(new JSONObject().put("type","way").put("role","outer").put("geometry",part1))
                .put(new JSONObject().put("type","way").put("role","outer").put("geometry",part2))
                .put(new JSONObject().put("type","way").put("role","inner").put("geometry",hole));
        JSONObject relation=new JSONObject().put("type","relation").put("id",90).put("tags",new JSONObject().put("type","multipolygon").put("leisure","park")).put("members",members);
        assertFalse(inspect(response(relation),52.5,13.4).containsKey(OsmPlaceContext.PARK));
        assertEquals("relation/90",inspect(response(relation),52.5005,13.4).get(OsmPlaceContext.PARK));
        members.getJSONObject(2).put("geometry",new JSONArray().put(hole.get(0)).put(hole.get(1)));
        assertFalse(inspect(response(relation),52.5,13.4).containsKey(OsmPlaceContext.PARK));
    }
    @Test public void incompleteGeometryNeverSuppliesPositiveEvidence()throws Exception {
        JSONArray invalid=new JSONArray().put(point(52.49,13.39)).put(point(52.51,13.41));
        assertFalse(inspect(response(way(1,new JSONObject().put("leisure","park"),invalid)),52.5,13.4).containsKey(OsmPlaceContext.PARK));
    }
}
