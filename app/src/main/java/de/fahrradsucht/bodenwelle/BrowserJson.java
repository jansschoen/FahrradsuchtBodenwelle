package de.fahrradsucht.bodenwelle;

import android.util.JsonReader;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Stable comparisons and bounded data parsing; imported HTML/JavaScript is never executed by Android. */
final class BrowserJson {
    private BrowserJson(){}
    static String canonical(Object value)throws JSONException {
        if(value==null||value==JSONObject.NULL)return "null";
        if(value instanceof JSONObject o){List<String> keys=new ArrayList<>();o.keys().forEachRemaining(keys::add);Collections.sort(keys);StringJoiner j=new StringJoiner(",","{","}");for(String k:keys)j.add(JSONObject.quote(k)+":"+canonical(o.get(k)));return j.toString();}
        if(value instanceof JSONArray a){StringJoiner j=new StringJoiner(",","[","]");for(int i=0;i<a.length();i++)j.add(canonical(a.get(i)));return j.toString();}
        if(value instanceof Number n){double d=n.doubleValue();if(!Double.isFinite(d))throw new JSONException("Ungültige Zahl");return java.math.BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();}
        return value instanceof Boolean?value.toString():JSONObject.quote(value.toString());
    }
    static boolean same(Object a,Object b){try{return canonical(a).equals(canonical(b));}catch(JSONException e){return false;}}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    static String sha(byte[] bytes){try{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
    static String sha(File file)throws IOException {try(InputStream in=new FileInputStream(file)){MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)d.update(b,0,n);return hex(d.digest());}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    static Object read(JsonReader r,int depth,int[] budget)throws IOException,JSONException {
        if(depth>32||--budget[0]<0)throw new IOException("Die Projektstruktur ist zu groß oder zu tief verschachtelt.");
        return switch(r.peek()){
            case BEGIN_OBJECT->{JSONObject o=new JSONObject();r.beginObject();while(r.hasNext()){String k=r.nextName();if(o.has(k))throw new IOException("Doppeltes Datenfeld.");o.put(k,read(r,depth+1,budget));}r.endObject();yield o;}
            case BEGIN_ARRAY->{JSONArray a=new JSONArray();r.beginArray();while(r.hasNext())a.put(read(r,depth+1,budget));r.endArray();yield a;}
            case STRING->r.nextString();case BOOLEAN->r.nextBoolean();case NULL->{r.nextNull();yield JSONObject.NULL;}
            case NUMBER->{String v=r.nextString();try{yield v.contains(".")||v.contains("e")||v.contains("E")?Double.valueOf(v):Long.valueOf(v);}catch(NumberFormatException e){throw new IOException("Ungültige Zahl.");}}
            default->throw new IOException("Ungültige Projektdaten.");
        };
    }
    static String safeJson(Object value){return value.toString().replace("<","\\u003c").replace(">","\\u003e").replace("&","\\u0026");}
    /** A malicious single string must not cause JsonReader to allocate the entire input file. */
    static final class BoundedReader extends FilterReader {
        private boolean string,escape;private int length;private long count,limit=Long.MAX_VALUE;
        void sectionLimit(long chars){limit=count+chars;}
        BoundedReader(Reader in){super(in);}
        private void check(int c)throws IOException {if(c<0)return;if(++count>limit)throw new IOException("Die Metadaten sind zu groß.");if(string){if(++length>2_200_000)throw new IOException("Ein Datenfeld ist zu groß.");if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='"'){string=false;length=0;}}else if(c=='"'){string=true;length=0;}}
        @Override public int read()throws IOException {int c=super.read();check(c);return c;}
        @Override public int read(char[] b,int off,int len)throws IOException {int n=super.read(b,off,len);for(int i=0;i<n;i++)check(b[off+i]);return n;}
    }
}
