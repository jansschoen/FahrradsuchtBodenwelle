package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;

/** Bounded, on-device diagnostics; deliberately contains no coordinates, notes or sensor readings. */
final class OsmDiagnostics {
    private final SharedPreferences preferences;
    OsmDiagnostics(Context context){preferences=context.getSharedPreferences("osm_diagnostics",Context.MODE_PRIVATE);}
    void request(long time){preferences.edit().putLong("attempt",time).putLong("requests",preferences.getLong("requests",0)+1).putString("network","Abfrage läuft …").apply();}
    void success(int ways){preferences.edit().putString("network","HTTP 200 · "+ways+" OSM-Wege empfangen").apply();}
    void failure(IOException failure){
        String reason=failure instanceof OverpassClient.RequestFailure http?"HTTP "+http.status:
                failure instanceof java.net.UnknownHostException?"Servername nicht erreichbar (DNS)":
                failure instanceof java.net.SocketTimeoutException?"Zeitüberschreitung":failure instanceof javax.net.ssl.SSLException?"TLS-/Zertifikatsfehler":"Verbindungsfehler oder unvollständige Serverantwort";
        preferences.edit().putString("network",reason).apply();
    }
    void result(String status,boolean cached){preferences.edit().putString("result",status).putBoolean("cached",cached).apply();}
    void waiting(String reason){preferences.edit().putString("waiting",reason).apply();}
    String summary(){
        long timestamp=preferences.getLong("attempt",0);
        String time=timestamp==0?"Noch keine Netzabfrage":java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(java.time.ZoneId.of("Europe/Berlin")).format(java.time.Instant.ofEpochMilli(timestamp));
        return "OSM-Abrufdiagnose\nLetzter Netzversuch: "+time+"\n"+preferences.getString("network","Noch keine Serverantwort")
                +"\nNetzabfragen insgesamt: "+preferences.getLong("requests",0)
                +"\nLetzte Auswertung: "+preferences.getString("result","noch nicht ausgewertet")+(preferences.getBoolean("cached",false)?" · aus Cache":"")
                +"\n"+preferences.getString("waiting","")+"\nAbfragepunkt: ursprünglicher GPS-Mittelpunkt. Kartendarstellung und OSM-Datenabruf sind unabhängige Verbindungen.";
    }
}
