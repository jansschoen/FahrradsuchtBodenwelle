# Browserkarte zeigt „403 – Access blocked“ · Korrektur 0.15.1

Die Meldung stammt vom Kartenserver. Die Projektdatei und die Messpunkte sind dabei bereits geladen. Version 0.15.0 hat aus einer lokalen HTML-Datei direkt Kacheln von `tile.openstreetmap.org` angefordert. Lokale Dateien senden keine normale Webseitenadresse als HTTP-Referer. Die [OSM-Kachelnutzungsrichtlinie](https://operations.osmfoundation.org/policies/tiles/) verlangt diese Kennzeichnung bei Webseiten. Diese Browseranbindung war deshalb ungeeignet.

## Vorhandene Datei weiterverwenden

1. Falls du im alten Fenster Angaben geändert hast: dort zuerst **Bearbeitete Datei speichern** wählen und den Download prüfen.
2. Den [korrigierten Browsereditor 0.15.1 als ZIP herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-Browser-0.15.1.zip), entpacken und **Bodenwelle-Browser.html** im Browser öffnen.
3. **Projektdatei öffnen** anklicken und die zuletzt gespeicherte eigene HTML-Datei auswählen. Messungen, Fotos, Bündelungen und bereits gespeicherte Änderungen werden übernommen. Die alte Datei wird als Datenquelle gelesen; ihr alter Kartencode wird nicht ausgeführt.
4. Nach der Bearbeitung wie gewohnt **Bearbeitete Datei speichern** wählen. Diese neue Datei enthält auch den korrigierten Editor und kann künftig direkt geöffnet werden. Der Rückimport auf dem Handy funktioniert weiterhin über das bisherige Projektformat.

Für diesen Weg ist kein erneuter Handyexport und kein App-Update nötig. Die Android-Testversion **0.15.1** ergänzt denselben Editor bei künftigen Exporten. Sie wird als Update installiert, ohne die vorhandene App zu deinstallieren.

## Was geändert wurde

Der Browser verwendet nun **OpenFreeMap** mit **MapLibre GL JS**. Die Karten beruhen weiterhin auf OpenStreetMap-Daten. OpenFreeMap erlaubt laut [offizieller Beschreibung](https://github.com/hyperknot/openfreemap#readme) die öffentliche Nutzung ohne Registrierung, API-Schlüssel oder Abrufkontingent. Die [öffentliche Serverkonfiguration](https://github.com/hyperknot/openfreemap/blob/main/linux_host/linux_host_lib/nginx_config_gen.py) setzt `Access-Control-Allow-Origin: *`, sodass auch die Herkunft einer lokalen Datei unterstützt wird. Die alte OSM-Sperre wird weder mit gefälschten Headern noch über einen Proxy umgangen.

Kartenbibliothek, Gestaltung und Lizenz sind direkt in die HTML-Datei eingebettet. Zur Laufzeit werden nur Kartenstil, Kartenkacheln, Symbole und Kartenbeschriftungszeichen von OpenFreeMap angefordert. Messbeschreibungen, Fotos und Rohmesswerte bleiben lokal. Die sichtbare Quellenangabe nennt OpenFreeMap, OpenMapTiles und OpenStreetMap.

Der Straßenhintergrund braucht Internet und WebGL-Grafikunterstützung. Bei Nichterreichbarkeit oder fehlendem WebGL bleiben Messpunkte, Spur und Bearbeitung auf einem Raster verfügbar. Die App zeigt einen lesbaren Hinweis statt eines sich wiederholenden Sperrbildes. Der Straßenkarten-Schalter lässt einen neuen Versuch zu.

## Was geprüft wurde

Die automatisierte Browserprüfung betreibt den echten WebGL-Renderer mit ausdrücklich künstlichen Kartenantworten. Sie prüft die Ausrichtung der Karten- und Messpunktkoordinaten auch nach dem Zoomen, das Öffnen bisheriger Dateien mit unveränderten Daten, HTTP-403-Fehler, erneutes Laden und die Bedienung ohne WebGL. Der Bearbeitungs-/Foto-/Speicherlauf und der Android-Rückimport werden zusätzlich mit einer tatsächlich gespeicherten Projektdatei geprüft.

**Die Live-Verbindung zu `tiles.openfreemap.org` ist in der Cloud durch den HTTPS-Proxy blockiert.** Die Cloud-Browserrichtlinie sperrt außerdem direkte `file://`-Navigation. Diese Einschränkungen wurden nicht umgangen. Der Browsertest rendert denselben HTML-Inhalt als Testdokument; er ersetzt weder einen Live-Verbindungstest auf deinem Computer noch einen vollständigen Firefox-/Safari-Test.
